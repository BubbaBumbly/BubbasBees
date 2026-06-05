package net.bubba.bubbasbees.blockentities;

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
            if (MathUtils.inRangeInclusive(0,600, hive.ticksSmoked)) {
                hive.ticksSmoked++;
            } else {
                hive.isSmoked = false;
                hive.ticksSmoked = -1;
            }
        }
        tickOccupants(hive, state, hive.getBees());
        DebugPackets.sendHiveInfo(level, pos, state, hive);
    }

    /// this function ticks over every bee within the hive.
    private static void tickOccupants(AdvancedBeehiveBlockEntity hive, BlockState state, List<BeeData> bees) {
        BeehiveBlockEntity.BeeData beeData;
        boolean flag = false;
        for (Iterator<BeeData> iterator = bees.iterator(); iterator.hasNext(); increaseTicksInHive(beeData, 1)) {
            beeData = iterator.next();
            boolean hasNectar = ((BeehiveBeeDataAccessor) beeData).getEntityData().getBoolean("HasNectar");
            if (((BeehiveBeeDataAccessor) beeData).getTicksInHive() > (hasNectar ? 1200 : 600)) {
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
    }
    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        if (!honeycombList.isEmpty()) tag.put("ModBeehiveCombList", writeCombList());
        tag.putBoolean("Smoked", isSmoked);
        tag.putInt("TicksSmoked", ticksSmoked);
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
        if (shouldStayInHive(hive.level, beehiveState)) {
            return false;
        } else {
            CompoundTag nbt = ((BeehiveBeeDataAccessor) tileBee).getEntityData();
            BeehiveEntityAccessor.callRemoveIgnoredBeeTags(nbt);
            nbt.put("HivePos", NbtUtils.writeBlockPos(hive.getBlockPos()));
            nbt.put("BubbasHomeAnchor", NbtUtils.writeBlockPos(hive.getBlockPos()));
            nbt.putBoolean("NoGravity", true);
            Direction direction = state.getValue(BeehiveBlock.FACING);
            BlockPos relative = hive.worldPosition.relative(direction);
            if (hive.level == null) return false;
            if (!hive.level.getBlockState(relative).getCollisionShape(hive.level, relative).isEmpty() && beehiveState != BeeReleaseStatus.EMERGENCY) {
                return false;
            }
            Entity entity = EntityType.loadEntityRecursive(nbt, hive.level, entity1 -> entity1);
            if (entity != null) {
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
                        if (bee instanceof BubbasBee bubbasBee){
                            if (!(hive.honeycombList.size() >= 8)) hive.honeycombList.add(bubbasBee.getHoneycombOutput());
                        } else {
                            if (!(hive.honeycombList.size() >= 8)) hive.honeycombList.add(Items.HONEYCOMB.getDefaultInstance());
                        }
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

    /// Smoker implementation
    public void smokeHive() {
        this.isSmoked = true;
        ticksSmoked = 0;
    }

    public int getTicksSmoked() {
        return ticksSmoked;
    }

    @Override
    public boolean isSedated() {
        return isSmoked || super.isSedated();
    }
}
