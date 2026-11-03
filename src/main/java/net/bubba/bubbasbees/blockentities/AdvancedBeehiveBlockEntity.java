package net.bubba.bubbasbees.blockentities;

import net.bubba.bubbasbees.ritual.RitualManager;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.logging.LogUtils;

import net.bubba.bubbasbees.bees.BeeSpec;
import net.bubba.bubbasbees.bees.BeeSpecs;
import net.bubba.bubbasbees.bees.QueenData;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import net.bubba.bubbasbees.item.custom.SmokerItem;
import net.bubba.bubbasbees.blocks.custom.AdvancedBeehiveBlock;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.bubba.bubbasbees.mixin.BeehiveBeeDataAccessor;
import net.bubba.bubbasbees.mixin.BeehiveEntityAccessor;
import net.bubba.bubbasbees.registries.ModBlockEntities;
import net.bubba.bubbasbees.util.MathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;


public class AdvancedBeehiveBlockEntity extends BeehiveBlockEntity {

    private BlockPos savedFlowerPos;
    private BlockEntityType<?> tileEntityType;
    private ArrayList<ItemStack> honeycombList = Lists.newArrayList();
    protected boolean isSmoked = false;
    protected int ticksSmoked = -1;
    /** How long the current smoke lasts; set by the smoker that was used (copper 15 s, iron 30 s). */
    protected int smokeDuration = SmokerItem.IRON_SMOKE_TICKS;
    /** Busy hives send out the mod's fast pollinators; calm hives (the default) keep their bees close to vanilla. */
    protected boolean busy = false;
    public static final String BUSY_TAG = "BubbasBusy";
    public static final int MAX_COMBS = 8;
    /** Dev-only balance measurement: -Dbubbasbees.measure=true logs every trip's comb roll (step 6). */
    private static final boolean MEASURE = Boolean.getBoolean("bubbasbees.measure");
    /** Stored-bee flag: this stay is 20% shorter (queen within 16 blocks, or the bee's home biome). */
    public static final String BOOST_TAG = "BubbasBoost";
    /// Herder (savanna bee): animals within 8 blocks grow up / lose breeding cooldown 25% faster
    private static final int HERDER_INTERVAL = 200;
    private static final int HERDER_RANGE = 8;
    private static final int HERDER_BONUS_TICKS = HERDER_INTERVAL / 4;
    // A savanna bee counts while inside, and for a while after it left (out on a trip)
    private static final int HERDER_MEMORY_TICKS = 2400;
    private static final String HERDED_TAG = "BubbasHerdedAt";
    private long lastHerderSeen = Long.MIN_VALUE / 2;
    public static final int STAY_TICKS = 600;
    public static final int BUSY_STAY_TICKS_WITH_NECTAR = 1200;
    // Vanilla's MIN_OCCUPATION_TICKS_NECTAR
    public static final int CALM_STAY_TICKS_WITH_NECTAR = 2400;

    public AdvancedBeehiveBlockEntity(BlockEntityType<?> tileEntityType, BlockPos pos, BlockState state) {
        super(pos, state);
        this.tileEntityType = tileEntityType;
    }
    public AdvancedBeehiveBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.ADVANCED_BEEHIVE.get(), pos, state);
    }
    @Nonnull
    @Override
    public BlockEntityType<?> getType() {
        return this.tileEntityType == null ? super.getType() : this.tileEntityType;
    }
    public ArrayList<ItemStack> getHoneycombList () {
        return this.honeycombList;
    }
    public void clearCombList (){
        this.honeycombList.clear();
    }

    public static void serverSideTick(Level level, BlockPos pos, BlockState state, AdvancedBeehiveBlockEntity hive) {
        if (hive.isSmoked) {
            if (MathUtils.inRangeInclusive(0, hive.smokeDuration, hive.ticksSmoked)) {
                hive.ticksSmoked++;
            } else {
                hive.isSmoked = false;
                hive.ticksSmoked = -1;
            }
        }
        tickOccupants(hive, state, hive.getBees());
        if ((level.getGameTime() + pos.asLong()) % HERDER_INTERVAL == 0 && hive.isHerding()) hive.herd();
        ((HiveOccupantView) hive).bubbasBees$syncOccupantsIfChanged();
        DebugPackets.sendHiveInfo(level, pos, state, hive);
    }

    /// this function ticks over every bee within the hive.
    private static void tickOccupants(AdvancedBeehiveBlockEntity hive, BlockState state, List<BeeData> bees) {
        BeehiveBlockEntity.BeeData beeData;
        boolean flag = false;
        for (Iterator<BeeData> iterator = bees.iterator(); iterator.hasNext(); increaseTicksInHive(beeData, 1)) {
            beeData = iterator.next();
            CompoundTag beeNbt = ((BeehiveBeeDataAccessor) beeData).getEntityData();
            boolean hasNectar = beeNbt.getBoolean("HasNectar");
            if (((BeehiveBeeDataAccessor) beeData).getTicksInHive() > hive.stayTicks(beeNbt)) {
                BeeReleaseStatus status = hasNectar
                        ? BeeReleaseStatus.HONEY_DELIVERED : BeeReleaseStatus.BEE_RELEASED;
                if(releaseBee(hive, state, beeData, null, status)){
                    iterator.remove();
                    flag = true;
                }
            }
        }
        if (flag) {
            setChanged(hive.level, hive.worldPosition, state);
        }
    }
    /// Begin Loading/Saving
    @Override
    public void load(@NotNull CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains("ModBeehiveCombList")) this.honeycombList = getHoneycombs(nbt);
        if (nbt.contains("Smoked")) this.isSmoked = nbt.getBoolean("Smoked");
        this.ticksSmoked = nbt.getInt("TicksSmoked");
        this.smokeDuration = nbt.contains("SmokeDuration") ? nbt.getInt("SmokeDuration") : SmokerItem.IRON_SMOKE_TICKS;
        this.busy = nbt.getBoolean(BUSY_TAG);
    }
    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        if (!honeycombList.isEmpty()) tag.put("ModBeehiveCombList", writeCombList());
        tag.putBoolean("Smoked", isSmoked);
        tag.putInt("TicksSmoked", ticksSmoked);
        tag.putInt("SmokeDuration", smokeDuration);
        tag.putBoolean(BUSY_TAG, busy);
    }
    public ArrayList<ItemStack> getHoneycombs(CompoundTag nbt) {
        return nbt.getList("ModBeehiveCombList", Tag.TAG_COMPOUND)
                .stream()
                .map(CompoundTag.class::cast)
                .map(ItemStack::of)
                .collect(Collectors.toCollection(ArrayList::new));
    }
    private ListTag writeCombList (){
        ListTag nbtTagList = new ListTag();
        for (ItemStack honeycomb : honeycombList) nbtTagList.add(honeycomb.save(new CompoundTag()));
        return nbtTagList;
    }
    public void setCombList (ArrayList<ItemStack> combList) {
        this.honeycombList.clear();
        this.honeycombList = combList;
    }

    /// End Loading/Saving
    private boolean hasSavedFlowerPos() {
        return this.savedFlowerPos != null;
    }
    @Override
    public void addOccupantWithPresetTicks(Entity entity, boolean flag, int ticksInHive) {
        if (this.getOccupantCount() < 5) {
            entity.stopRiding();
            entity.ejectPassengers();
            CompoundTag compoundtag = new CompoundTag();
            entity.save(compoundtag);
            if (this.level != null && !this.level.isClientSide) {
                // Decided once on entry (no searching while inside): 20% shorter stay near the queen or in the bee's
                // home biome; the two never stack
                QueenData queen = QueenData.get(this.level);
                boolean boosted = (queen != null && queen.boosts(this.level, this.worldPosition))
                        || (entity instanceof BubbasBee bee && bee.getSpec() != null
                            && bee.getSpec().isHomeBiome(this.level, this.worldPosition));
                compoundtag.putBoolean(BOOST_TAG, boosted);
                if (entity instanceof BubbasBee bee && bee.has(BeeSpec.Trait.HERDER)) lastHerderSeen = this.level.getGameTime();
            }
            this.storeBee(compoundtag, ticksInHive, flag);
            if (this.level != null) {
                if (entity instanceof Bee bee) {
                    if (bee.hasSavedFlowerPos() && (!this.hasSavedFlowerPos() || this.level.random.nextBoolean())) {
                        this.savedFlowerPos = bee.getSavedFlowerPos();
                    }
                }

                BlockPos blockpos = this.getBlockPos();
                this.level.playSound((Player)null, (double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ(), SoundEvents.BEEHIVE_ENTER, SoundSource.BLOCKS, 1.0F, 1.0F);
                this.level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(entity, this.getBlockState()));
            }

            entity.discard();
            // After discard (which clears her position, as for a bee jar): the queen is now in this hive
            if (entity instanceof BubbasBee queen && queen.isQueen() && this.level != null) {
                QueenData data = QueenData.get(this.level);
                if (data != null) data.setPosition(queen.getUUID(), this.level.dimension(), this.worldPosition);
            }
            super.setChanged();
            if (this.level != null) {
                recalculateHoneyLevel(this, this.getBlockState());
            }
        }
    }

    private static void recalculateHoneyLevel(AdvancedBeehiveBlockEntity hive, BlockState state) {
        float combsInHive = hive.honeycombList.size();
        int honeyLevel = Mth.clamp(Mth.floor(0.625 * combsInHive), 0, 5);
        if (hive.level != null) {
            hive.level.setBlockAndUpdate(hive.worldPosition, hive.getBlockState().setValue(BeehiveBlock.HONEY_LEVEL, honeyLevel));
        }
    }
    /// Various releasing bee methods
    // Release one bee after enough ticks in hive
    private static boolean releaseBee(AdvancedBeehiveBlockEntity hive, @NotNull BlockState state, @NotNull BeehiveBlockEntity.BeeData tileBee, @Nullable List<Entity> entities, @NotNull BeeReleaseStatus beehiveState) {
        CompoundTag nbt = ((BeehiveBeeDataAccessor) tileBee).getEntityData();
        if (hive.mustStay(nbt, beehiveState)) {
            return false;
        } else {
            BeehiveEntityAccessor.callRemoveIgnoredBeeTags(nbt);
            nbt.put("HivePos", NbtUtils.writeBlockPos(hive.getBlockPos()));
            nbt.put("BubbasHomeAnchor", NbtUtils.writeBlockPos(hive.getBlockPos()));
            nbt.putBoolean("NoGravity", true);
            nbt.putBoolean(BUSY_TAG, hive.busy);
            Direction direction = state.getValue(BeehiveBlock.FACING);
            BlockPos relative = hive.worldPosition.relative(direction);
            if (hive.level == null) return false;
            if (!hive.level.getBlockState(relative).getCollisionShape(hive.level, relative).isEmpty() && beehiveState != BeeReleaseStatus.EMERGENCY) {
                return false;
            }
            Entity entity = EntityType.loadEntityRecursive(nbt, hive.level, entity1 -> entity1);
            if (entity != null) {
                if (entity instanceof BubbasBee released && released.has(BeeSpec.Trait.HERDER)) {
                    hive.lastHerderSeen = hive.level.getGameTime();
                }
                BlockPos blockpos = hive.getBlockPos();
                EntityDimensions size = entity.getDimensions(Pose.STANDING);
                double d0 = 0.65D + size.width / 2.0F;
                double d1 = blockpos.getX() + 0.5D + d0 * direction.getStepX();
                double d2 = blockpos.getY() + Math.max(0.5D - (size.height / 2.0F), 0);
                double d3 = blockpos.getZ() + 0.5D + d0 * direction.getStepZ();
                entity.moveTo(d1, d2, d3, entity.getYRot(), entity.getXRot());
                if (beehiveState == BeeReleaseStatus.HONEY_DELIVERED) {
                    if (entity instanceof Bee bee){
                        bee.dropOffNectar();
                        // Mod bees roll their comb table (a failed roll leaves nothing); vanilla bees give honeycomb.
                        // A full hive (8 combs) loses the roll. No combs inside a running ritual circle.
                        ItemStack comb = RitualManager.isRitualHive(hive.level, hive.worldPosition) ? ItemStack.EMPTY
                                : bee instanceof BubbasBee bubbasBee
                                ? bubbasBee.rollComb() : new ItemStack(Items.HONEYCOMB);
                        if (MEASURE) LogUtils.getLogger().info("[BBMEASURE] t={} dim={} hive={} bee={} comb={} full={}",
                                hive.level.getGameTime(), hive.level.dimension().location(), hive.worldPosition.toShortString(),
                                nbt.getString("id"), comb.isEmpty() ? "none" : ForgeRegistries.ITEMS.getKey(comb.getItem()),
                                hive.honeycombList.size() >= MAX_COMBS);
                        if (!comb.isEmpty() && hive.honeycombList.size() < MAX_COMBS) hive.honeycombList.add(comb);
                    }
                    if (getHoneyLevel(state) < 5) {
                        recalculateHoneyLevel(hive,state);
                    }
                }
                hive.level.playSound(null, hive.worldPosition.getX(), hive.worldPosition.getY(), hive.worldPosition.getZ(), SoundEvents.BEEHIVE_EXIT, SoundSource.BLOCKS, 1.0F, 1.0F);
                hive.level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(entity, hive.level.getBlockState(blockpos)));
                return hive.level.addFreshEntity(entity);
            }
            return true;
        }
    }
    /** Per-bee release rule: vanilla bees wait out night and rain; mod bees follow BeeSpec.canWorkAt at the hive. */
    private boolean mustStay(CompoundTag beeNbt, BeeReleaseStatus status) {
        if (status == BeeReleaseStatus.EMERGENCY || this.level == null) return false;
        BeeSpec spec = BeeSpecs.byEntity(ResourceLocation.tryParse(beeNbt.getString("id")));
        if (spec == null) return this.level.isNight() || this.level.isRaining();
        return !spec.canWorkAt(this.level, this.worldPosition.above(), true, RitualManager.isRitualHive(this.level, this.worldPosition));
    }

    public static boolean shouldStayInHive(Level level, BeeReleaseStatus beehiveState) {
        return (level != null && (level.isNight() || level.isRaining())) && beehiveState != BeeReleaseStatus.EMERGENCY;
    }
    private static void increaseTicksInHive(BeeData beeData, int amount) {
        ((BeehiveBeeDataAccessor) beeData).setTicksInHive(((BeehiveBeeDataAccessor) beeData).getTicksInHive() + amount);
    }
    // Release all bees and they're pissed
    @Override
    public void emptyAllLivingFromHive(@Nullable Player player, @NotNull BlockState state, @NotNull BeeReleaseStatus status) {
        if (player == null) return;
        this.releaseAllBees(state, status)
                .stream()
                .filter(e -> e.position().distanceToSqr(player.position()) <= 16.0D)
                .forEach(entity -> {
                    if (!this.isSedated()) {
                        if (entity instanceof Mob mob) {
                            mob.setTarget(player);
                        }
                    }
                });
    }
    // Release all bees regularly
    private List<Entity> releaseAllBees(BlockState state, BeeReleaseStatus status) {
        List<Entity> list = com.google.common.collect.Lists.newArrayList();
        getBees().removeIf(beeData -> releaseBee(this, state, beeData, list, status));
        return list;
    }
    /// End release section

    public List<BeeData> getBees() {
        return ((BeehiveEntityAccessor) this).getStored();
    }

    public boolean isAllowedBee() {
        return getBlockState().getBlock() instanceof AdvancedBeehiveBlock;
    }

    public boolean isFull() {
        return getOccupantCount() >= 5;
    }

    private AdvancedBeehiveBlock getBlock() {
        return (AdvancedBeehiveBlock) this.getBlockState().getBlock();
    }

    /** How long a bee stays inside before it is released. */
    public int stayTicks(boolean hasNectar) {
        if (!hasNectar) return STAY_TICKS;
        return busy ? BUSY_STAY_TICKS_WITH_NECTAR : CALM_STAY_TICKS_WITH_NECTAR;
    }

    /** Stay for a stored bee: 20% shorter when boosted (queen or home biome, see addOccupant); Nether and End bees stay twice as long outside their home dimension. */
    public int stayTicks(CompoundTag beeNbt) {
        int ticks = stayTicks(beeNbt.getBoolean("HasNectar"));
        if (beeNbt.getBoolean(BOOST_TAG)) ticks = ticks * 4 / 5;
        BeeSpec spec = BeeSpecs.byEntity(ResourceLocation.tryParse(beeNbt.getString("id")));
        return spec != null && this.level != null && spec.isAwayFromHome(this.level) ? ticks * 2 : ticks;
    }

    public boolean isBusy() {
        return busy;
    }

    public void toggleBusy() {
        busy = !busy;
        setChanged();
    }

    private boolean isHerding() {
        if (this.level == null) return false;
        if (this.level.getGameTime() - lastHerderSeen < HERDER_MEMORY_TICKS) return true;
        for (BeeData data : getBees()) {
            BeeSpec spec = BeeSpecs.byEntity(ResourceLocation.tryParse(((BeehiveBeeDataAccessor) data).getEntityData().getString("id")));
            if (spec != null && spec.has(BeeSpec.Trait.HERDER)) return true;
        }
        return false;
    }

    /** One box query per 10 s; an animal near several herder hives still only gets one bonus per 10 s. */
    private void herd() {
        if (this.level == null) return;
        long now = this.level.getGameTime();
        for (Animal animal : this.level.getEntitiesOfClass(Animal.class, new AABB(this.worldPosition).inflate(HERDER_RANGE))) {
            CompoundTag data = animal.getPersistentData();
            if (data.contains(HERDED_TAG) && now - data.getLong(HERDED_TAG) < HERDER_INTERVAL) continue;
            int age = animal.getAge();
            if (age == 0) continue;
            data.putLong(HERDED_TAG, now);
            animal.setAge(age < 0 ? Math.min(0, age + HERDER_BONUS_TICKS) : Math.max(0, age - HERDER_BONUS_TICKS));
        }
    }

    /// Harvest by a comb chute below: takes every comb out and empties the hive's honey (as if sheared, but the
    /// bees stay calm). Returns the combs.
    public List<ItemStack> takeAllCombs() {
        List<ItemStack> combs = new ArrayList<>(honeycombList);
        honeycombList.clear();
        emptyHoney();
        return combs;
    }

    /** Honey level back to 0 and the combs gone (the chute's honey-bottle harvest). */
    public void emptyHoney() {
        honeycombList.clear();
        if (this.level != null) {
            this.level.setBlockAndUpdate(this.worldPosition, this.getBlockState().setValue(BeehiveBlock.HONEY_LEVEL, 0));
        }
        setChanged();
    }

    /// Smoker implementation
    public void smokeHive(int durationTicks) {
        this.isSmoked = true;
        ticksSmoked = 0;
        smokeDuration = durationTicks;
    }

    /** Game time the smoke wears off, or HiveOccupantView.NOT_SMOKED; constant while the smoke lasts. */
    public long smokeEndGameTime(long gameTime) {
        return isSmoked ? gameTime + Math.max(0, smokeDuration - ticksSmoked) : HiveOccupantView.NOT_SMOKED;
    }

    public int getTicksSmoked() {
        return ticksSmoked;
    }

    @Override
    public boolean isSedated() {
        return isSmoked || super.isSedated();
    }
}
