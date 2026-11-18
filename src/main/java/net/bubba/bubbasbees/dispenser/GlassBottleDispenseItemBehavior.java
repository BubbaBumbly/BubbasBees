package net.bubba.bubbasbees.dispenser;

import net.bubba.bubbasbees.blockentities.AdvancedBeehiveBlockEntity;
import net.bubba.bubbasbees.blocks.custom.AdvancedBeehiveBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class GlassBottleDispenseItemBehavior extends OptionalDispenseItemBehavior {
    private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();
    private ItemStack takeLiquid(BlockSource p_123447_, ItemStack p_123448_, ItemStack p_123449_) {
        p_123448_.shrink(1);
        if (p_123448_.isEmpty()) {
            p_123447_.getLevel().gameEvent((Entity)null, GameEvent.FLUID_PICKUP, p_123447_.getPos());
            return p_123449_.copy();
        } else {
            if (((DispenserBlockEntity)p_123447_.getEntity()).addItem(p_123449_.copy()) < 0) {
                this.defaultDispenseItemBehavior.dispense(p_123447_, p_123449_.copy());
            }

            return p_123448_;
        }
    }
    @Override
    public ItemStack execute(BlockSource blockSource, ItemStack itemStack) {
        this.setSuccess(false);
        ServerLevel serverlevel = blockSource.getLevel();
        BlockPos blockpos = blockSource.getPos().relative((Direction)blockSource.getBlockState().getValue(DispenserBlock.FACING));
        BlockState blockstate = serverlevel.getBlockState(blockpos);
        if (blockstate.is(BlockTags.BEEHIVES, (p_123442_) -> p_123442_.hasProperty(BeehiveBlock.HONEY_LEVEL) &&
                p_123442_.getBlock() instanceof BeehiveBlock) && (Integer)blockstate.getValue(BeehiveBlock.HONEY_LEVEL) >= 5 &&
                !(blockstate.getBlock() instanceof AdvancedBeehiveBlock beehiveBlock2)) {
            ((BeehiveBlock) blockstate.getBlock()).releaseBeesAndResetHoneyLevel(serverlevel, blockstate, blockpos, (Player) null, BeehiveBlockEntity.BeeReleaseStatus.BEE_RELEASED);
            this.setSuccess(true);
            return this.takeLiquid(blockSource, itemStack, new ItemStack(Items.HONEY_BOTTLE));
        } else if (blockstate.is(BlockTags.BEEHIVES, (p_123442_) -> p_123442_.hasProperty(BeehiveBlock.HONEY_LEVEL) &&
                p_123442_.getBlock() instanceof BeehiveBlock) && (Integer)blockstate.getValue(BeehiveBlock.HONEY_LEVEL) >= 5 &&
                (blockstate.getBlock() instanceof AdvancedBeehiveBlock beehiveBlock2)) {
            AdvancedBeehiveBlockEntity blockEntity = (AdvancedBeehiveBlockEntity) serverlevel.getBlockEntity(blockpos);
            serverlevel.setBlockAndUpdate(blockpos, blockstate.setValue(BeehiveBlock.HONEY_LEVEL, 0));
            blockEntity.clearCombList();
            this.setSuccess(true);
            return this.takeLiquid(blockSource, itemStack, new ItemStack(Items.HONEY_BOTTLE));
        } else if (serverlevel.getFluidState(blockpos).is(FluidTags.WATER)) {
            this.setSuccess(true);
            return this.takeLiquid(blockSource, itemStack, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER));
        } else {
            return super.execute(blockSource, itemStack);
        }
    }
}
