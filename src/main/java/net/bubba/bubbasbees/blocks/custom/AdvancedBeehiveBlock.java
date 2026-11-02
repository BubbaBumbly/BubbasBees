package net.bubba.bubbasbees.blocks.custom;


import net.bubba.bubbasbees.blockentities.AdvancedBeehiveBlockEntity;
import net.bubba.bubbasbees.registries.ModBlockEntities;
import net.bubba.bubbasbees.registries.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.datafix.fixes.BlockStateData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ToolActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AdvancedBeehiveBlock extends BeehiveBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public AdvancedBeehiveBlock(BlockBehaviour.Properties properties){
        super(properties);
        this.registerDefaultState(this.
                stateDefinition.any().setValue(HONEY_LEVEL, 0).setValue(FACING, Direction.NORTH));
    }

    @Override
    public @NotNull InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult result) {
        ItemStack held_Item = player.getItemInHand(hand);
        // A Bubba Star switches the hive between calm (vanilla-like) and busy bees; the star is not used up
        if (held_Item.is(ModItems.BUBBA_STAR.get())) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof AdvancedBeehiveBlockEntity hive) {
                hive.toggleBusy();
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, hive.isBusy() ? 1.4F : 0.8F);
                player.displayClientMessage(Component.translatable(
                        hive.isBusy() ? "message.bubbasbees.hive_mode.busy" : "message.bubbasbees.hive_mode.calm"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // Comb chute: right-click to fit one; sneak + right-click with shears takes it back out (sneak + empty hand is
        // Carry On's pick-up gesture in packs that have it)
        if (level.getBlockEntity(pos) instanceof AdvancedBeehiveBlockEntity chuteHive) {
            if (held_Item.is(ModItems.COMB_CHUTE.get()) && !chuteHive.hasCombChute()) {
                if (!level.isClientSide) {
                    chuteHive.setCombChute(true);
                    if (!player.getAbilities().instabuild) held_Item.shrink(1);
                    level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 1.2F);
                    player.displayClientMessage(Component.translatable("message.bubbasbees.comb_chute.fitted"), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (player.isShiftKeyDown() && held_Item.canPerformAction(ToolActions.SHEARS_HARVEST) && chuteHive.hasCombChute()) {
                if (!level.isClientSide) {
                    chuteHive.setCombChute(false);
                    ItemStack chute = new ItemStack(ModItems.COMB_CHUTE.get());
                    if (!player.getInventory().add(chute)) player.drop(chute, false);
                    level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.2F);
                    player.displayClientMessage(Component.translatable("message.bubbasbees.comb_chute.removed"), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        int i = (Integer) state.getValue(HONEY_LEVEL);
        boolean flag = false;
        if (i>=5){
            Item item = held_Item.getItem();
            if (held_Item.canPerformAction(ToolActions.SHEARS_HARVEST)) {
                level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BEEHIVE_SHEAR, SoundSource.NEUTRAL, 1.0F, 1.0F);
                dropModHoneycomb(level, pos);
                held_Item.hurtAndBreak(1, player, (p_49571_) -> {
                    p_49571_.broadcastBreakEvent(hand);
                });
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

            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return super.use(state, level, pos, player, hand, result);
        }
    }
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getPlayer() != null &&
                context.getPlayer().isShiftKeyDown() ?
                context.getHorizontalDirection() : context.getHorizontalDirection().getOpposite();
        return this.defaultBlockState().setValue(FACING, direction);
    }
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AdvancedBeehiveBlockEntity hive) {
                // 1. Read NBT from the placed item
                CompoundTag tag = stack.getTagElement("BlockEntityTag");
                if (tag != null) {
                    // 2. Copy whatever you care about
                    if (tag.contains("ModBeehiveCombList")) {
                        ListTag combListTag = tag.getList("ModBeehiveCombList", Tag.TAG_COMPOUND);
                        ArrayList<ItemStack> combList = new ArrayList<>();
                        // Convert each CompoundTag in combListTag back to ItemStack
                        for (Tag entry : combListTag) {
                            CompoundTag itemTag = (CompoundTag) entry;
                            ItemStack comb = ItemStack.of(itemTag);
                            combList.add(comb);
                        }
                        hive.setCombList(combList);
                    }
                    hive.setChanged();
                }
            }
        }
    }

    // A fitted comb chute drops as an item when the hive goes (it is not kept in the hive item)
    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AdvancedBeehiveBlockEntity hive
                && hive.hasCombChute()) {
            popResource(level, pos, new ItemStack(ModItems.COMB_CHUTE.get()));
        }
        super.onRemove(state, level, pos, newState, moved);
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
        blockStateBuilder.add(new Property[]{HONEY_LEVEL, FACING});
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


