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
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.block.Blocks;
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
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;



public class BubbasBee extends Bee implements IAnimatable {

    public BlockPos homeAnchorPos;
    // Work mode of the bee's hive (see AdvancedBeehiveBlockEntity); kept for bees that are away from it or have none
    private boolean busy = false;
    private int nextModeCheckTick = 0;
    private static final int MODE_CHECK_INTERVAL = 20;

    // Burrower: hiding in the block below burrowSpot until burrowTicksLeft runs out (invisible, can't be hurt)
    private @Nullable BlockPos burrowBlock;
    private int burrowTicksLeft;
    private static final int BURROW_MIN_TICKS = 100;
    private static final int BURROW_MAX_TICKS = 200;
    // Lava filler: the cauldron under the bee last check, so one fly-over rolls once
    private @Nullable BlockPos lastCauldron;
    private static final float LAVA_FILL_CHANCE = 0.25F;
    // Royal guard: keeps stinging (it survives it) until the anger it got from a mugging runs out
    private int mugUntilTick;
    // A mugging stings once every 3 s (user, 2026-11-02: once a second was lethal); threats get the normal rate
    private static final int MUG_STING_INTERVAL = 60;
    private @Nullable java.util.UUID mugTarget;
    private int nextMugStingTick;

    // Ore bees come in a stone and a deepslate look; each bee rolls its own when it's created and
    // keeps it in its save data (through hives and reloads). Synced so the client picks the texture.
    private static final EntityDataAccessor<Boolean> DEEPSLATE =
            SynchedEntityData.defineId(BubbasBee.class, EntityDataSerializers.BOOLEAN);

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
        if (spec == null) return !this.level.isRaining() && !this.level.isNight();
        return spec.canWorkAt(this.level, this.blockPosition(), false, RitualManager.isRitualHive(this.level, this.getHivePos()));
    }

    /** Nocturnal bees only use flowers under open sky. */
    public boolean canUseFlowerAt(BlockPos flower) {
        return !has(BeeSpec.Trait.NOCTURNAL) || this.level.canSeeSky(flower.above());
    }

    public boolean isBurrowed() {
        return burrowBlock != null;
    }

    /// Traits that change damage
    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (source == DamageSource.SWEET_BERRY_BUSH) return false;   // vanilla exempts only its own bee type
        if (isBurrowed() && source != DamageSource.OUT_OF_WORLD) return false;
        if (source.isExplosion() && has(BeeSpec.Trait.BLAST_PROOF)) return false;
        if (source == DamageSource.DROWN && (has(BeeSpec.Trait.WATER_SAFE) || has(BeeSpec.Trait.NO_WATER_DAMAGE))) return false;
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level.isClientSide && this.isAlive() && source != DamageSource.OUT_OF_WORLD) {
            if (has(BeeSpec.Trait.BURROWER)) {
                startBurrow();
            } else if (has(BeeSpec.Trait.SHORT_TELEPORT)) {
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

    /// Burrower
    private void startBurrow() {
        if (isBurrowed()) return;
        BlockPos block = BeeTraits.findBurrow(this);
        if (block == null) return;
        burrowBlock = block;
        burrowTicksLeft = Mth.nextInt(this.random, BURROW_MIN_TICKS, BURROW_MAX_TICKS);
        BeeTraits.burrowParticles(this, block);
        this.setInvisible(true);
        this.getNavigation().stop();
        this.setTarget(null);
        this.moveTo(block.getX() + 0.5D, block.getY() + 1.0D, block.getZ() + 0.5D, getYRot(), getXRot());
    }

    private void burrowTick() {
        BlockPos block = burrowBlock;
        if (block == null) return;
        this.setDeltaMovement(Vec3.ZERO);
        this.setInvisible(true);   // LivingEntity resets invisibility from potion effects, e.g. after a reload
        this.setPos(block.getX() + 0.5D, block.getY() + 1.0D, block.getZ() + 0.5D);
        // Pops out when the time is up, or at once if its block is dug away
        BlockState state = this.level.getBlockState(block);
        if (--burrowTicksLeft <= 0 || !(state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL))) {
            BeeTraits.burrowParticles(this, block);
            burrowBlock = null;
            this.setInvisible(false);
            this.setDeltaMovement(0.0D, 0.2D, 0.0D);
        }
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
        if (isQueen() && !this.level.isClientSide && this.getServer() != null) {
            QueenData data = QueenData.get(this.level);
            if (data != null) data.died(this.getUUID());
            this.getServer().getPlayerList().broadcastSystemMessage(
                    Component.translatable("message.bubbasbees.queen_fallen").withStyle(ChatFormatting.GOLD), false);
        }
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        // Into a bee jar (or a hive, which records her position right after): no known position meanwhile
        if (isQueen() && reason == RemovalReason.DISCARDED && !this.level.isClientSide) {
            QueenData data = QueenData.get(this.level);
            if (data != null) data.clearPosition(this.getUUID());
        }
        super.remove(reason);
    }

    /// Per-tick trait work, spread out so no trait scans every tick
    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide && this.isAlive()) BeeParticles.clientTick(this);
        if (this.level.isClientSide || !this.isAlive()) return;
        if (isBurrowed()) {
            burrowTick();
            return;
        }
        int phase = this.tickCount + this.getId();
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
                QueenData data = QueenData.get(this.level);
                if (data != null) data.setPosition(this.getUUID(), this.level.dimension(), this.blockPosition());
            }
            if (phase % 100 == 0) BeeTraits.healBees(this);
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DEEPSLATE, false);
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
            if (hive != null && this.level.isLoaded(hive)
                    && this.level.getBlockEntity(hive) instanceof AdvancedBeehiveBlockEntity advancedHive) {
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
        if (burrowBlock != null) {
            tag.put("BubbasBurrow", NbtUtils.writeBlockPos(burrowBlock));
            tag.putInt("BubbasBurrowTicks", burrowTicksLeft);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("BubbasHomeAnchor")) {
            homeAnchorPos = NbtUtils.readBlockPos(tag.getCompound("BubbasHomeAnchor"));
        } else {
            BlockPos hive = this.getHivePos();
            homeAnchorPos = hive != null ? hive : this.blockPosition();
        }
        if (tag.contains("BubbasDeepslate")) {
            setDeepslate(tag.getBoolean("BubbasDeepslate"));
        }
        busy = tag.getBoolean(AdvancedBeehiveBlockEntity.BUSY_TAG);
        if (tag.contains("BubbasBurrow")) {
            burrowBlock = NbtUtils.readBlockPos(tag.getCompound("BubbasBurrow"));
            burrowTicksLeft = tag.getInt("BubbasBurrowTicks");
        }
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

    ///Start Geckolib
    private final AnimationFactory factory = GeckoLibUtil.createFactory(this);

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        if (event.isMoving()) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.bubbasbee.moving"));
            return PlayState.CONTINUE;
        }

        event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.bubbasbee.idle"));
        return PlayState.CONTINUE;
    }
    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "bee_controller", 0, event -> {
            event.getController().setAnimation(new AnimationBuilder()
                    .addAnimation("animation.bee.fly", ILoopType.EDefaultLoopTypes.LOOP)
                    .addAnimation("animation.bee.fly.bobbing", ILoopType.EDefaultLoopTypes.LOOP)
            );
            return PlayState.CONTINUE;
        }));
    }
    ///End Geckolib
    @Override
    public AnimationFactory getFactory() {
        return this.factory;
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
        return this.level.isLoaded(blockPos) && isFlowerState(this.level.getBlockState(blockPos));
    }
}