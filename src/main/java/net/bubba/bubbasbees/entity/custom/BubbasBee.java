package net.bubba.bubbasbees.entity.custom;

import net.bubba.bubbasbees.ritual.RitualManager;

import net.bubba.bubbasbees.bees.BeeSpec;
import net.bubba.bubbasbees.blockentities.AdvancedBeehiveBlockEntity;
import net.bubba.bubbasbees.entity.goals.BeeAnchorHomingGoal;
import net.bubba.bubbasbees.entity.goals.BeeDaftWanderGoal;
import net.bubba.bubbasbees.entity.goals.ModBeePollinateGoal;
import net.bubba.bubbasbees.mixin.BeeEntityAccessor;
import net.bubba.bubbasbees.bees.QueenData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;



public class BubbasBee extends Bee implements GeoEntity {

    public BlockPos homeAnchorPos;
    // Work mode of the bee's hive (see AdvancedBeehiveBlockEntity); kept for bees that are away from it or have none
    private boolean busy = false;
    private int nextModeCheckTick = 0;
    private static final int MODE_CHECK_INTERVAL = 20;

    // Lava filler: the cauldron under the bee last check, so one fly-over rolls once
    private @Nullable BlockPos lastCauldron;
    private static final float LAVA_FILL_CHANCE = 0.25F;
    // Royal guard: keeps stinging (it survives it) until the anger it got from a mugging runs out
    private int mugUntilTick;
    // A mugging stings once every 2.5 s (once a second was lethal); threats get the normal rate
    private static final int MUG_STING_INTERVAL = 50;
    /** A royal guard's sting: 1.5 hearts (other bees keep vanilla's 1 heart). */
    private static final double GUARD_STING_DAMAGE = 3.0D;
    private @Nullable java.util.UUID mugTarget;
    private int nextMugStingTick;

    // Ore bees come in a stone and a deepslate look; each bee rolls its own when it's created and
    // keeps it in its save data (through hives and reloads). Synced so the client picks the texture.
    private static final EntityDataAccessor<Boolean> DEEPSLATE =
            SynchedEntityData.defineId(BubbasBee.class, EntityDataSerializers.BOOLEAN);
    // The look-at HUD's copies: only the server knows the bee's hive and work mode
    private static final EntityDataAccessor<Boolean> BUSY_SHOWN =
            SynchedEntityData.defineId(BubbasBee.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<java.util.Optional<BlockPos>> HOME_SHOWN =
            SynchedEntityData.defineId(BubbasBee.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    // Ticks until a baby grows up, for the HUD timer (the client only knows baby yes/no); refreshed every second
    private static final EntityDataAccessor<Integer> GROW_TICKS_SHOWN =
            SynchedEntityData.defineId(BubbasBee.class, EntityDataSerializers.INT);

    /** The dev-only base bee (diamond look, no spec, makes nothing). */
    public BubbasBee(EntityType<? extends Bee> bubbasBeeEntityType, Level level) {
        this(bubbasBeeEntityType, level, null);
    }

    protected BubbasBee(EntityType<? extends Bee> type, Level level, @Nullable BeeSpec spec) {
        super(type, level);
        if (!level.isClientSide && (spec == null || spec.deepslateLook())) {
            setDeepslate(this.random.nextBoolean());
        }
    }

    /** This bee's roster data; null only for the dev base bee. */
    public @Nullable BeeSpec getSpec() {
        return null;
    }

    /** Whether the bee pollinates this block (its flower tag). */
    public boolean isFlowerState(BlockState state) {
        BeeSpec spec = getSpec();
        return spec != null && state.is(spec.flowerTag());
    }

    /** The comb this trip leaves in the hive, or EMPTY on a failed roll. */
    public ItemStack rollComb() {
        BeeSpec spec = getSpec();
        return spec == null ? ItemStack.EMPTY : spec.rollComb(this.random);
    }

    public boolean has(BeeSpec.Trait trait) {
        BeeSpec spec = getSpec();
        return spec != null && spec.has(trait);
    }

    /** Weather and time allow work where the bee is now (see BeeSpec.canWorkAt; the sky rule applies to hive and flower). */
    public boolean canWorkNow() {
        BeeSpec spec = getSpec();
        if (spec == null) return !this.level().isRaining() && !this.level().isNight();
        return spec.canWorkAt(this.level(), this.blockPosition(), false, RitualManager.isRitualHive(this.level(), this.getHivePos()));
    }

    /** Nocturnal bees only use flowers under open sky. */
    public boolean canUseFlowerAt(BlockPos flower) {
        // A flower holding water (waterlogged, or kelp/seagrass) is for water-safe bees only; the pollinate goal
        // already needs air above it, so they hover over the water surface, never in it
        FluidState fluid = this.level().getFluidState(flower);
        if (!fluid.isEmpty() && !(fluid.is(FluidTags.WATER) && has(BeeSpec.Trait.WATER_SAFE))) return false;
        return !has(BeeSpec.Trait.NOCTURNAL) || BeeSpec.openSky(this.level(), flower.above());
    }

    /** Takes no damage from water. */
    public boolean isWaterProof() {
        return has(BeeSpec.Trait.WATER_SAFE) || has(BeeSpec.Trait.NO_WATER_DAMAGE);
    }

    /// Getting out of water
    // Vanilla starts drowning a bee after 20 ticks in water (then 1 damage every half second) and its pathfinder
    // never swims, so a bee knocked into a lake stayed there. Bees that can drown now rise instead: a minimum
    // upward speed while in water, and a sideways push toward the hive while something overhead (ice, a cave
    // roof) stops the rise. Only reads the in-water flag vanilla already keeps, so it costs nothing out of water
    private static final double WATER_ESCAPE_RISE = 0.2D;
    private static final double WATER_ESCAPE_SIDEWAYS = 0.1D;

    @Override
    protected void customServerAiStep() {
        if (this.isInWaterOrBubble() && !isWaterProof()) escapeWater();
        super.customServerAiStep();
    }

    private void escapeWater() {
        Vec3 motion = this.getDeltaMovement();
        double x = motion.x;
        double z = motion.z;
        // FIX: 1.21.1 mappings have Entity.onGround(), not isOnGround()
        if (this.verticalCollision && !this.onGround()) {
            BlockPos hive = this.getHivePos();
            Vec3 way = hive != null ? Vec3.atCenterOf(hive).subtract(this.position()) : this.getLookAngle();
            way = new Vec3(way.x, 0.0D, way.z);
            if (way.lengthSqr() > 1.0E-4D) {
                way = way.normalize().scale(WATER_ESCAPE_SIDEWAYS);
                x = way.x;
                z = way.z;
            }
        }
        this.setDeltaMovement(x, Math.max(motion.y, WATER_ESCAPE_RISE), z);
    }

    /// Traits that change damage
    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        // DEVIATION: uses damage-type checks instead of damageSources() identity checks; same behaviour in 1.20.1.
        if (source.is(DamageTypes.SWEET_BERRY_BUSH)) return false;   // vanilla exempts only its own bee type
        if (source.is(DamageTypeTags.IS_EXPLOSION) && has(BeeSpec.Trait.BLAST_PROOF)) return false;
        if (source.is(DamageTypes.DROWN) && (has(BeeSpec.Trait.WATER_SAFE) || has(BeeSpec.Trait.NO_WATER_DAMAGE))) return false;
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && this.isAlive() && !source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            if (has(BeeSpec.Trait.SHORT_TELEPORT)) {
                BeeTraits.shortTeleport(this);
            }
        }
        return hurt;
    }

    @Override
    public void makeStuckInBlock(@NotNull BlockState state, @NotNull Vec3 multiplier) {
        if (state.is(Blocks.SWEET_BERRY_BUSH)) return;
        super.makeStuckInBlock(state, multiplier);
    }

    @Override
    public boolean canFreeze() {
        return !has(BeeSpec.Trait.SNOWPROOF) && super.canFreeze();
    }

    // Silent wings: no flap, so no FLAP vibration for sculk sensors and shriekers
    @Override
    public boolean isFlapping() {
        return !has(BeeSpec.Trait.SILENT_WINGS) && super.isFlapping();
    }

    private void syncHudInfo() {
        this.entityData.set(BUSY_SHOWN, isBusy());
        this.entityData.set(HOME_SHOWN, java.util.Optional.ofNullable(this.getHivePos()));
    }

    /** Client-safe copy of {@link #isBusy()}, for the look-at HUD. */
    public boolean isBusyShown() {
        return this.entityData.get(BUSY_SHOWN);
    }

    /** Client-safe copy of the home hive position, for the look-at HUD; null if the bee has none. */
    public @Nullable BlockPos getHomeShown() {
        return this.entityData.get(HOME_SHOWN).orElse(null);
    }

    /** Client-safe ticks until this baby grows up, for the look-at HUD; 0 if not known yet or not a baby. */
    public int getGrowTicksShown() {
        return this.entityData.get(GROW_TICKS_SHOWN);
    }

    /// Royal guard
    public boolean isRoyalGuard() {
        return has(BeeSpec.Trait.ROYAL_GUARD);
    }

    /** Someone hit this guard, or a bee near it: sting them over and over until the anger runs out. */
    public void mug(LivingEntity attacker) {
        mugTarget = attacker.getUUID();
        this.setTarget(attacker);
        this.setPersistentAngerTarget(attacker.getUUID());
        this.startPersistentAngerTimer();
    }

    @Override
    public void startPersistentAngerTimer() {
        super.startPersistentAngerTimer();
        mugUntilTick = this.tickCount + this.getRemainingPersistentAngerTime();
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        boolean mugging = isRoyalGuard() && target.getUUID().equals(mugTarget);
        if (mugging && this.tickCount < nextMugStingTick) return false;
        if (isRoyalGuard()) {
            // set here, not at spawn, so guards saved before this change sting harder too
            var damage = this.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damage != null) damage.setBaseValue(GUARD_STING_DAMAGE);
        }
        boolean hit = super.doHurtTarget(target);
        if (hit && mugging) nextMugStingTick = this.tickCount + MUG_STING_INTERVAL;
        if (hit && isRoyalGuard()) {
            // Vanilla bees die some time after stinging; the guard doesn't, and goes on while still angry
            ((BeeEntityAccessor) this).callSetHasStung(false);
            if (target instanceof LivingEntity living && living.isAlive() && this.tickCount < mugUntilTick) {
                this.setTarget(living);
                this.setPersistentAngerTarget(living.getUUID());
                this.setRemainingPersistentAngerTime(mugUntilTick - this.tickCount);
            }
        }
        return hit;
    }

    /// Royal queen
    public boolean isQueen() {
        return has(BeeSpec.Trait.ROYAL_QUEEN);
    }

    @Override
    public void die(@NotNull DamageSource source) {
        super.die(source);
        if (isQueen() && !this.level().isClientSide && this.getServer() != null) {
            QueenData data = QueenData.get(this.level());
            if (data != null) data.died(this.getUUID());
            this.getServer().getPlayerList().broadcastSystemMessage(
                    Component.translatable("message.bubbasbees.queen_fallen").withStyle(ChatFormatting.GOLD), false);
        }
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        // Into a bee jar (or a hive, which records her position right after): no known position meanwhile
        if (isQueen() && reason == RemovalReason.DISCARDED && !this.level().isClientSide) {
            QueenData data = QueenData.get(this.level());
            if (data != null) data.clearPosition(this.getUUID());
        }
        super.remove(reason);
    }

    /// Per-tick trait work, spread out so no trait scans every tick
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && this.isAlive()) BeeParticles.clientTick(this);
        if (this.level().isClientSide || !this.isAlive()) return;
        if ((this.tickCount + this.getId()) % MODE_CHECK_INTERVAL == 0) syncHudInfo();
        int phase = this.tickCount + this.getId();
        if (phase % 20 == 0) this.entityData.set(GROW_TICKS_SHOWN, this.isBaby() ? -this.getAge() : 0);
        if (has(BeeSpec.Trait.GLOW) && phase % 4 == 0) BeeTraits.placeLight(this);
        if (has(BeeSpec.Trait.LAVA_FILLER) && phase % 10 == 0) {
            BlockPos cauldron = BeeTraits.cauldronBelow(this);
            if (cauldron != null && !cauldron.equals(lastCauldron) && this.random.nextFloat() < LAVA_FILL_CHANCE) {
                BeeTraits.addLava(this, cauldron);
            }
            lastCauldron = cauldron;
        }
        if (isRoyalGuard() && phase % 200 == 0) BeeTraits.waxNearbyCopper(this);
        if (isQueen()) {
            if (!this.isPersistenceRequired()) this.setPersistenceRequired();
            if (phase % 20 == 0) {
                QueenData data = QueenData.get(this.level());
                if (data != null) data.setPosition(this.getUUID(), this.level().dimension(), this.blockPosition());
            }
            if (phase % 100 == 0) BeeTraits.healBees(this);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DEEPSLATE, false);
        builder.define(BUSY_SHOWN, false);
        builder.define(HOME_SHOWN, java.util.Optional.empty());
        builder.define(GROW_TICKS_SHOWN, 0);
    }

    public boolean isDeepslate() {
        return this.entityData.get(DEEPSLATE);
    }

    public void setDeepslate(boolean deepslate) {
        this.entityData.set(DEEPSLATE, deepslate);
    }

    public BlockPos getHomeAnchorPos() {
        return homeAnchorPos;
    }

    public void setHomeAnchorPos(BlockPos pos) {
        this.homeAnchorPos = pos;
    }

    /** Follows the hive's mode within a second of it being switched, also for bees already out flying. */
    public boolean isBusy() {
        if (this.tickCount >= nextModeCheckTick) {
            nextModeCheckTick = this.tickCount + MODE_CHECK_INTERVAL;
            BlockPos hive = this.getHivePos();
            if (hive != null && this.level().isLoaded(hive)
                    && this.level().getBlockEntity(hive) instanceof AdvancedBeehiveBlockEntity advancedHive) {
                busy = advancedHive.isBusy();
            }
        }
        return busy;
    }

    public String getVariant() {
        BeeSpec spec = getSpec();
        return spec == null ? "none" : spec.id();
    }

    // Spawn eggs and breeding produce a baby of this bee's own type instead of a vanilla bee
    @Override
    public Bee getBreedOffspring(@NotNull ServerLevel level, @NotNull AgeableMob other) {
        return this.getType().create(level) instanceof Bee baby ? baby : null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (homeAnchorPos != null) {
            tag.put("BubbasHomeAnchor", NbtUtils.writeBlockPos(homeAnchorPos));
        }
        tag.putBoolean("BubbasDeepslate", isDeepslate());
        tag.putBoolean(AdvancedBeehiveBlockEntity.BUSY_TAG, busy);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("BubbasHomeAnchor")) {
            // Pre-1.20.5 saves used an {X,Y,Z} compound the int-array reader rejects; fall back like a missing key
            homeAnchorPos = NbtUtils.readBlockPos(tag, "BubbasHomeAnchor")
                    .orElseGet(() -> this.getHivePos() != null ? this.getHivePos() : this.blockPosition());
        } else {
            BlockPos hive = this.getHivePos();
            homeAnchorPos = hive != null ? hive : this.blockPosition();
        }
        if (tag.contains("BubbasDeepslate")) {
            setDeepslate(tag.getBoolean("BubbasDeepslate"));
        }
        busy = tag.getBoolean(AdvancedBeehiveBlockEntity.BUSY_TAG);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(3, new BeeAnchorHomingGoal(this));
        Bee.BeePollinateGoal oldGoal = ((BeeEntityAccessor)this).getBeePollinateGoal();
        if (oldGoal != null) {
            this.goalSelector.removeGoal(oldGoal);
            ModBeePollinateGoal modGoal = new ModBeePollinateGoal(this);
            this.goalSelector.addGoal(4, modGoal);
            ((BeeEntityAccessor)this).setBeePollinateGoal(modGoal);
            // Outranks vanilla's wander (8) so a dawdling bee hops slowly without pausing
            this.goalSelector.addGoal(7, new BeeDaftWanderGoal(this));
        }
    }

    // Vanilla's bee navigation pauses for the whole pollinate goal because its hover drives the move control
    // directly. ModBeePollinateGoal also approaches from farther away and needs the pathfinder when a wall blocks
    // the straight line, so pause only while it is hovering over the flower
    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level) {
            @Override
            public boolean isStableDestination(@NotNull BlockPos pos) {
                return !this.level.getBlockState(pos.below()).isAir();
            }

            @Override
            public void tick() {
                Bee.BeePollinateGoal goal = ((BeeEntityAccessor) BubbasBee.this).getBeePollinateGoal();
                boolean paused = goal instanceof ModBeePollinateGoal modGoal
                        ? modGoal.isHoveringAtFlower()
                        : goal != null && goal.isPollinating();
                if (!paused) {
                    super.tick();
                }
            }
        };
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(false);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "bee_controller", 0, state ->
            state.setAndContinue(RawAnimation.begin()
                    .thenLoop("animation.bee.fly")
                    .thenLoop("animation.bee.fly.bobbing"))
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static AttributeSupplier setAttributes() {
        return setAttributes(10.0D);
    }

    public static AttributeSupplier setAttributes(double maxHealth) {
        return BubbasBee.createMobAttributes()
                .add(Attributes.MAX_HEALTH, maxHealth)
                .add(Attributes.FLYING_SPEED, (double)0.6F)
                .add(Attributes.MOVEMENT_SPEED, (double)0.3F)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D).build();
    }

    @Override
    public boolean isFood(ItemStack p_27895_) {
        return false;
    }

    @Override
    public boolean isFlowerValid(@NotNull BlockPos blockPos) {
        return this.level().isLoaded(blockPos) && isFlowerState(this.level().getBlockState(blockPos));
    }
}