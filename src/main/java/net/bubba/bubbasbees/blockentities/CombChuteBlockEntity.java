package net.bubba.bubbasbees.blockentities;

import net.bubba.bubbasbees.blocks.custom.CombChuteBlock;
import net.bubba.bubbasbees.registries.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The comb chute's storage: 5 output slots (filled only by the chute) and 1 bottle slot (glass bottles only).
 * Every 8 ticks, like a hopper, it pushes one item down and checks the hive above.
 */
public class CombChuteBlockEntity extends BlockEntity {
    public static final int OUTPUT_SLOTS = 5;
    public static final int BOTTLE_SLOT = OUTPUT_SLOTS;
    private static final int COOLDOWN = 8;
    /** Vanilla hives and nests give this many honeycombs when sheared. */
    private static final int VANILLA_SHEAR_COMBS = 3;
    private int cooldown;

    private final ItemStackHandler items = new ItemStackHandler(OUTPUT_SLOTS + 1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot != BOTTLE_SLOT || stack.is(Items.GLASS_BOTTLE);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    };

    /** What hoppers and pipes see: they can pull from the output slots and put glass bottles in. */
    private final IItemHandler external = new IItemHandler() {
        @Override
        public int getSlots() {
            return OUTPUT_SLOTS + 1;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return slot == BOTTLE_SLOT ? items.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == BOTTLE_SLOT ? ItemStack.EMPTY : items.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == BOTTLE_SLOT && stack.is(Items.GLASS_BOTTLE);
        }
    };
    private LazyOptional<IItemHandler> externalCap = LazyOptional.of(() -> external);

    public CombChuteBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMB_CHUTE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CombChuteBlockEntity chute) {
        if (--chute.cooldown > 0) return;
        chute.cooldown = COOLDOWN;
        chute.pushDown(level, pos);
        chute.harvestAbove(level, pos, state);
    }

    /// Output: one item per 8 ticks into whatever is below, as a hopper does
    private void pushDown(Level level, BlockPos pos) {
        BlockEntity below = level.getBlockEntity(pos.below());
        if (below == null) return;
        below.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).ifPresent(target -> {
            for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
                ItemStack one = items.extractItem(slot, 1, true);
                if (one.isEmpty()) continue;
                if (ItemHandlerHelper.insertItemStacked(target, one, true).isEmpty()) {
                    ItemHandlerHelper.insertItemStacked(target, items.extractItem(slot, 1, false), false);
                    return;
                }
            }
        });
    }

    /// Harvest: only a full hive (honey level 5), everything at once, without angering the bees
    private void harvestAbove(Level level, BlockPos pos, BlockState state) {
        BlockPos hivePos = pos.above();
        BlockState hiveState = level.getBlockState(hivePos);
        if (!(hiveState.getBlock() instanceof BeehiveBlock hiveBlock) || hiveState.getValue(BeehiveBlock.HONEY_LEVEL) < 5) return;
        BlockEntity hiveEntity = level.getBlockEntity(hivePos);
        if (!(hiveEntity instanceof BeehiveBlockEntity)) return;
        if (state.getValue(CombChuteBlock.HONEY)) {
            ItemStack bottle = items.getStackInSlot(BOTTLE_SLOT);
            ItemStack honey = new ItemStack(Items.HONEY_BOTTLE);
            if (bottle.isEmpty() || !fits(List.of(honey))) return;
            items.extractItem(BOTTLE_SLOT, 1, false);
            store(honey);
            if (hiveEntity instanceof AdvancedBeehiveBlockEntity advanced) advanced.emptyHoney();
            else hiveBlock.resetHoneyLevel(level, hiveState, hivePos);
            level.playSound(null, hivePos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PICKUP, hivePos);
        } else {
            List<ItemStack> combs;
            if (hiveEntity instanceof AdvancedBeehiveBlockEntity advanced) {
                combs = new ArrayList<>(advanced.getHoneycombList());
            } else {
                combs = List.of(new ItemStack(Items.HONEYCOMB, VANILLA_SHEAR_COMBS));
            }
            if (!fits(combs)) return;   // waits until there is room for the whole harvest
            if (hiveEntity instanceof AdvancedBeehiveBlockEntity advanced) {
                combs = advanced.takeAllCombs();
            } else {
                hiveBlock.resetHoneyLevel(level, hiveState, hivePos);
            }
            for (ItemStack comb : combs) store(comb.copy());
            level.playSound(null, hivePos, SoundEvents.BEEHIVE_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.SHEAR, hivePos);
        }
    }

    /** Whether all these stacks fit into the output slots together. */
    private boolean fits(List<ItemStack> stacks) {
        ItemStackHandler copy = new ItemStackHandler(OUTPUT_SLOTS);
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) copy.setStackInSlot(slot, items.getStackInSlot(slot).copy());
        for (ItemStack stack : stacks) {
            if (!ItemHandlerHelper.insertItemStacked(copy, stack.copy(), false).isEmpty()) return false;
        }
        return true;
    }

    private void store(ItemStack stack) {
        ItemStack left = stack;
        for (int slot = 0; slot < OUTPUT_SLOTS && !left.isEmpty(); slot++) left = items.insertItem(slot, left, false);
    }

    /** Puts glass bottles in the bottle slot; returns what didn't fit. */
    public ItemStack insertBottles(ItemStack bottles) {
        return items.insertItem(BOTTLE_SLOT, bottles, false);
    }

    public int bottleCount() {
        return items.getStackInSlot(BOTTLE_SLOT).getCount();
    }

    /** Items currently in the output slots, in slot order, for the HUD and Jade. */
    public List<ItemStack> outputStacks() {
        List<ItemStack> list = new ArrayList<>();
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) list.add(stack.copy());
        }
        return list;
    }

    /** Everything inside, for dropping when the chute is broken. */
    public List<ItemStack> contents() {
        List<ItemStack> list = new ArrayList<>();
        for (int slot = 0; slot < items.getSlots(); slot++) {
            if (!items.getStackInSlot(slot).isEmpty()) list.add(items.getStackInSlot(slot).copy());
        }
        return list;
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Items"));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return externalCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        externalCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        externalCap = LazyOptional.of(() -> external);
    }
}
