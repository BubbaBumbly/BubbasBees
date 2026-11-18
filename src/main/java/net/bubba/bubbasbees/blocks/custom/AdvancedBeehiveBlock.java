package net.bubba.bubbasbees.blocks.custom;


import net.bubba.bubbasbees.blockentities.AdvancedBeehiveBlockEntity;
import net.bubba.bubbasbees.registries.ModBlockEntities;
import net.bubba.bubbasbees.registries.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AdvancedBeehiveBlock extends BeehiveBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Combs inside, 0-8: what the panel shows, what the side window fills one cell
     *  per comb for, and the comparator output. Vanilla's HONEY_LEVEL stays alongside as the "full" flag (5 = 8 combs)
     *  that vanilla code and other mods check; see {@link #vanillaLevel}. */
    public static final IntegerProperty COMBS = IntegerProperty.create("combs", 0, 8);

    /** Vanilla honey level for a comb count: 0 only when empty (so a reset to 0 means "harvested"), 5 only when full. */
    public static int vanillaLevel(int combs) {
        return combs <= 0 ? 0 : combs >= 8 ? 5 : Math.min(4, (combs + 1) / 2);
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
        return state.getValue(COMBS);
    }

    public AdvancedBeehiveBlock(BlockBehaviour.Properties properties){
        super(properties);
        this.registerDefaultState(this.
                stateDefinition.any().setValue(HONEY_LEVEL, 0).setValue(COMBS, 0).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack held_Item, BlockState state, Level level, BlockPos pos, Player player,
                                                       InteractionHand hand, BlockHitResult result) {
        // A Bubba Star switches the hive between calm (vanilla-like) and busy bees; the star is not used up
        if (held_Item.is(ModItems.LESSER_BUBBA_STAR.get()) || held_Item.is(ModItems.BUBBA_STAR.get())
                || held_Item.is(ModItems.CELESTIAL_BUBBA_STAR.get())) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof AdvancedBeehiveBlockEntity hive) {
                hive.toggleBusy();
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, hive.isBusy() ? 1.4F : 0.8F);
                player.displayClientMessage(Component.translatable(
                        hive.isBusy() ? "message.bubbasbees.hive_mode.busy" : "message.bubbasbees.hive_mode.calm"), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        int i = (Integer) state.getValue(HONEY_LEVEL);
        boolean flag = false;
        if (i>=5){
            Item item = held_Item.getItem();
            if (held_Item.getItem() instanceof ShearsItem || held_Item.is(ConventionalItemTags.SHEAR_TOOLS)) {
                level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BEEHIVE_SHEAR, SoundSource.NEUTRAL, 1.0F, 1.0F);
                dropModHoneycomb(level, pos);
                held_Item.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                flag = true;
                level.gameEvent(player, GameEvent.SHEAR, pos);
            } else if (held_Item.is(Items.GLASS_BOTTLE)) {
                held_Item.shrink(1);
                level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
                if (held_Item.isEmpty()) {
                    player.setItemInHand(hand, new ItemStack(Items.HONEY_BOTTLE, 1));
                } else if (!player.getInventory().add(1, new ItemStack(Items.HONEY_BOTTLE))) {
                    player.drop(new ItemStack(Items.HONEY_BOTTLE), false);
                }
                flag = true;
                level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
            }

            if (!level.isClientSide() && flag) {
                player.awardStat(Stats.ITEM_USED.get(item));
            }
        }

        if (flag) {
            BlockEntity be = level.getBlockEntity(pos);
            boolean isSedated = be instanceof AdvancedBeehiveBlockEntity hive && hive.isSedated();
            if (!isSedated && !player.isCreative()) {
                if (this.hiveContainsBees(level, pos)) {
                    if (be instanceof AdvancedBeehiveBlockEntity hive){
                        hive.emptyAllLivingFromHive(player,state, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
                    }
                    this.angerNearbyBees(level, pos);
                }
                this.releaseBeesAndResetHoneyLevel(level, state, pos, player, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
            } else {
                this.resetHoneyLevel(level, state, pos);
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return super.useItemOn(held_Item, state, level, pos, player, hand, result);
        }
    }
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getPlayer() != null &&
                context.getPlayer().isShiftKeyDown() ?
                context.getHorizontalDirection() : context.getHorizontalDirection().getOpposite();
        return this.defaultBlockState().setValue(FACING, direction);
    }
    private boolean hiveContainsBees(Level level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        DirectionProperty direction = FACING;
        BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity instanceof BeehiveBlockEntity beehiveblockentity) {
            return !beehiveblockentity.isEmpty();
        } else {
            return false;
        }
    }
    private void angerNearbyBees(Level level, BlockPos pos) {
        List<Bee> list = level.getEntitiesOfClass(Bee.class, (new AABB(pos)).inflate(8.0, 6.0, 8.0));
        if (!list.isEmpty()) {
            List<Player> list1 = level.getEntitiesOfClass(Player.class, (new AABB(pos)).inflate(8.0, 6.0, 8.0));
            if (list1.isEmpty()) {
                return;
            }

            int i = list1.size();
            Iterator var6 = list.iterator();

            while (var6.hasNext()) {
                Bee bee = (Bee) var6.next();
                if (bee.getTarget() == null) {
                    bee.setTarget((LivingEntity) list1.get(level.random.nextInt(i)));
                }
            }
        }

    }
    public void dropModHoneycomb(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof AdvancedBeehiveBlockEntity hive) {
            ArrayList<ItemStack> combList = hive.getHoneycombList();
            BlockState blockState = level.getBlockState(pos);
            Direction direction = blockState.getValue(FACING);
            for (ItemStack comb : combList){
                popResourceFromFace(level,pos,direction,comb);
            }
            combList.clear();
        }
    }
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> blockStateBuilder) {
        blockStateBuilder.add(new Property[]{HONEY_LEVEL, COMBS, FACING});
    }
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedBeehiveBlockEntity(pos, state);
    }

        @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            @NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTickerHelper(
                blockEntityType, ModBlockEntities.ADVANCED_BEEHIVE.get(), AdvancedBeehiveBlockEntity::serverSideTick);
    }
}
