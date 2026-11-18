package net.bubba.bubbasbees.blockentities;

import net.bubba.bubbasbees.blocks.custom.PartialLavaCauldronBlock;
import net.bubba.bubbasbees.registries.ModBlockEntities;
import net.bubba.bubbasbees.registries.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Forge 1.19.2 has no cauldron fluid hook, so the partial lava cauldron exposes its lava through this block entity:
 * one tank of 1000 mB that holds 250/500/750 mB. Fill and drain go in whole quarters (250 mB); filling to 1000 mB turns
 * it into the vanilla lava cauldron, draining it all leaves an empty cauldron.
 */
public class PartialLavaCauldronBlockEntity extends BlockEntity {
    private final LazyOptional<IFluidHandler> handler = LazyOptional.of(Tank::new);

    public PartialLavaCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PARTIAL_LAVA_CAULDRON.get(), pos, state);
    }

    private int levelNow() {
        BlockState state = getBlockState();
        return state.is(ModBlocks.PARTIAL_LAVA_CAULDRON.get()) ? state.getValue(PartialLavaCauldronBlock.LEVEL) : 0;
    }

    private void setLevelTo(int quarters) {
        if (level == null) return;
        level.setBlockAndUpdate(worldPosition, PartialLavaCauldronBlock.stateFor(getBlockState(), quarters));
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) return handler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handler.invalidate();
    }

    private class Tank implements IFluidHandler {
        private static final int Q = PartialLavaCauldronBlock.MB_PER_LEVEL;

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            int quarters = levelNow();
            return quarters == 0 ? FluidStack.EMPTY : new FluidStack(Fluids.LAVA, quarters * Q);
        }

        @Override
        public int getTankCapacity(int tank) {
            return 4 * Q;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return stack.getFluid().isSame(Fluids.LAVA);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
            int quarters = levelNow();
            int add = Math.min(resource.getAmount() / Q, 4 - quarters);
            if (add <= 0) return 0;
            if (action.execute()) setLevelTo(quarters + add);
            return add * Q;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !isFluidValid(0, resource)) return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            int quarters = levelNow();
            int take = Math.min(maxDrain / Q, quarters);
            if (take <= 0) return FluidStack.EMPTY;
            if (action.execute()) setLevelTo(quarters - take);
            return new FluidStack(Fluids.LAVA, take * Q);
        }
    }
}
