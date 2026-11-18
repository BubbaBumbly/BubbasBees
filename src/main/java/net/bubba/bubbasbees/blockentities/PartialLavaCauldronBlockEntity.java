package net.bubba.bubbasbees.blockentities;

import net.bubba.bubbasbees.blocks.custom.PartialLavaCauldronBlock;
import net.bubba.bubbasbees.registries.ModBlockEntities;
import net.bubba.bubbasbees.registries.ModBlocks;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

/**
 * The partial lava cauldron exposes its lava through this block entity: one storage of 1000 mB that holds
 * 250/500/750 mB. Fill and drain go in whole quarters (250 mB); filling to 1000 mB turns it into the vanilla lava
 * cauldron, draining it all leaves an empty cauldron.
 */
public class PartialLavaCauldronBlockEntity extends BlockEntity {
    private final LavaTank handler = new LavaTank();

    public PartialLavaCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PARTIAL_LAVA_CAULDRON.get(), pos, state);
    }

    /** The fluid storage registered with the Fabric Transfer API for pipes and buckets. */
    public Storage<FluidVariant> getFluidStorage() {
        return handler;
    }

    private int levelNow() {
        BlockState state = getBlockState();
        return state.is(ModBlocks.PARTIAL_LAVA_CAULDRON.get()) ? state.getValue(PartialLavaCauldronBlock.LEVEL) : 0;
    }

    private void setLevelTo(int quarters) {
        if (level == null) return;
        level.setBlockAndUpdate(worldPosition, PartialLavaCauldronBlock.stateFor(getBlockState(), quarters));
    }

    private final class LavaTank extends SnapshotParticipant<Integer> implements SingleSlotStorage<FluidVariant> {
        // The Transfer API counts in droplets (81000 per bucket), not mB
        private static final long Q = FluidConstants.BUCKET / 4;

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0 || !resource.isOf(Fluids.LAVA)) return 0;
            int quarters = levelNow();
            long add = Math.min(maxAmount / Q, 4 - quarters);
            if (add <= 0) return 0;
            updateSnapshots(transaction);
            setLevelTo(quarters + (int) add);
            return add * Q;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0 || !resource.isOf(Fluids.LAVA)) return 0;
            int quarters = levelNow();
            long take = Math.min(maxAmount / Q, quarters);
            if (take <= 0) return 0;
            updateSnapshots(transaction);
            setLevelTo(quarters - (int) take);
            return take * Q;
        }

        @Override
        protected Integer createSnapshot() {
            return levelNow();
        }

        @Override
        protected void readSnapshot(Integer snapshot) {
            setLevelTo(snapshot);
        }

        @Override
        public boolean isResourceBlank() {
            return levelNow() == 0;
        }

        @Override
        public @NotNull FluidVariant getResource() {
            return levelNow() == 0 ? FluidVariant.blank() : FluidVariant.of(Fluids.LAVA);
        }

        @Override
        public long getAmount() {
            return (long) levelNow() * Q;
        }

        @Override
        public long getCapacity() {
            return 4L * Q;
        }
    }
}
