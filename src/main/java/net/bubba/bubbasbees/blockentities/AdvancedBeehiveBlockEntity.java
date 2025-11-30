package net.bubba.bubbasbees.blockentities;

import net.bubba.bubbasbees.blocks.custom.AdvancedBeehiveBlock;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.bubba.bubbasbees.mixin.BeehiveBeeDataAccessor;
import net.bubba.bubbasbees.mixin.BeehiveEntityAccessor;
import net.bubba.bubbasbees.registries.ModBlockEntities;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
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


    public static void serverSideTick(Level level, BlockPos pos, BlockState state, AdvancedBeehiveBlockEntity hive) {
        tickOccupants(hive, state, hive.getBees());
        DebugPackets.sendHiveInfo(level, pos, state, hive);
    }

    /// this function ticks over every bee within the hive. Every gametick, this is what happens
    /// to the bees within the hive.
    private static void tickOccupants(AdvancedBeehiveBlockEntity hive, BlockState state, List<BeeData> bees) {
        BeehiveBlockEntity.BeeData beeData;
        for (Iterator<BeeData> iterator = bees.iterator(); iterator.hasNext(); increaseTicksInHive(beeData, 1)) {
            beeData = iterator.next();
            if (((BeehiveBeeDataAccessor) beeData).getTicksInHive() > ((BeehiveBeeDataAccessor) beeData).getMinOccupationTicks()) {
                System.out.println("Ticks spent in hive: " + ((BeehiveBeeDataAccessor) beeData).getTicksInHive());
                BeeReleaseStatus status = ((BeehiveBeeDataAccessor) beeData).getEntityData().getBoolean("HasNectar")
                        ? BeeReleaseStatus.HONEY_DELIVERED : BeeReleaseStatus.BEE_RELEASED;
                System.out.println("Ready to release!");
                if(releaseBee(hive, state, beeData, null, status)){
                    iterator.remove();
                }
            }
        }
    }
    /// Begin Loading/Saving
    @Override
    public void load(@NotNull CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains("ModBeehiveCombList")) honeycombList = getHoneycombs(nbt);
    }
    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        //tag.put("ModBeehiveCombList", writeCombList());
        if (!honeycombList.isEmpty()) tag.put("ModBeehiveCombList", writeCombList());
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
    /// End Loading/Saving
    private boolean hasSavedFlowerPos() {
        return this.savedFlowerPos != null;
    }
    @Override
    public void addOccupantWithPresetTicks(Entity entity, boolean flag, int ticksInHive) {
        boolean newFlag = false;
        if (this.getOccupantCount() < 5) {
            entity.stopRiding();
            entity.ejectPassengers();
            CompoundTag compoundtag = new CompoundTag();
            entity.save(compoundtag);
            this.storeBee(compoundtag, ticksInHive, newFlag);
            if (this.level != null) {
                if (entity instanceof Bee bee) {
                    if (bee.hasSavedFlowerPos() && (!this.hasSavedFlowerPos() || this.level.random.nextBoolean())) {
                        this.savedFlowerPos = bee.getSavedFlowerPos();
                    }
                    if (entity instanceof BubbasBee bubbasBee){
                        if (!(honeycombList.size() >= 8)) honeycombList.add(bubbasBee.getHoneycombOutput());
                    } else {
                        if (!(honeycombList.size() >= 8)) honeycombList.add(Items.HONEYCOMB.getDefaultInstance());
                    }
                }

                BlockPos blockpos = this.getBlockPos();
                this.level.playSound((Player)null, (double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ(), SoundEvents.BEEHIVE_ENTER, SoundSource.BLOCKS, 1.0F, 1.0F);
                this.level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(entity, this.getBlockState()));
            }

            entity.discard();
            super.setChanged();
        }
    }

    private static void recalculateHoneyLevel(AdvancedBeehiveBlockEntity hive, BlockState state) {
        int honeyLevel = getHoneyLevel(state);
        float combsInHive = hive.honeycombList.size();
        double stateConverter;
        if (honeyLevel < 5){
            stateConverter = 0.625 * combsInHive;
            honeyLevel = Mth.floor(stateConverter);
        }
        if (hive.level != null) {
            hive.level.setBlockAndUpdate(hive.worldPosition, hive.getBlockState().setValue(BeehiveBlock.HONEY_LEVEL, honeyLevel));
        }
    }

    private static boolean releaseBee(AdvancedBeehiveBlockEntity hive, @NotNull BlockState state, @NotNull BeehiveBlockEntity.BeeData tileBee, @Nullable List<Entity> entities, @NotNull BeeReleaseStatus beehiveState) {
        if (shouldStayInHive(hive.level, beehiveState)) {
            return false;
        } else {
            CompoundTag nbt = ((BeehiveBeeDataAccessor) tileBee).getEntityData();
            BeehiveEntityAccessor.callRemoveIgnoredBeeTags(nbt);
            nbt.put("HivePos", NbtUtils.writeBlockPos(hive.getBlockPos()));
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
                    }
                    if (getHoneyLevel(state) < 5) {
                        recalculateHoneyLevel(hive,state);
                    }
                    if (entities != null) entities.add(entity);
                }
                hive.level.playSound(null, hive.worldPosition.getX(), hive.worldPosition.getY(), hive.worldPosition.getZ(), SoundEvents.BEEHIVE_EXIT, SoundSource.BLOCKS, 1.0F, 1.0F);
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
}
