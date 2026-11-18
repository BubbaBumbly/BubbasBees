package net.bubba.bubbasbees.blockentities;

import net.bubba.bubbasbees.blocks.custom.CombChuteBlock;
import net.bubba.bubbasbees.registries.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The comb chute's storage: 5 output slots (filled only by the chute) and 1 bottle slot (glass bottles only).
 * Every 8 ticks, like a hopper, it pushes one item out of its spout and checks the hive behind it (FACING = spout).
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
            if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
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

    public CombChuteBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMB_CHUTE.get(), pos, state);
    }

    /** The item handler registered with NeoForge for hopper/pipe access. */
    public IItemHandler getExternalHandler() {
        return external;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CombChuteBlockEntity chute) {
        if (--chute.cooldown > 0) return;
        chute.cooldown = COOLDOWN;
        Direction spout = state.getValue(CombChuteBlock.FACING);
        chute.pushOut(level, pos, spout);
        chute.harvestHive(level, pos.relative(spout.getOpposite()), state);
    }

    /// Output: one item per 8 ticks into whatever the spout points at, as a hopper does
    private void pushOut(Level level, BlockPos pos, Direction spout) {
        // DEVIATION: 1.21.1 has no BlockEntity.getCapability; NeoForge exposes block capabilities through the level
        IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(spout), spout.getOpposite());
        if (target == null) return;
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            ItemStack one = items.extractItem(slot, 1, true);
            if (one.isEmpty()) continue;
            if (ItemHandlerHelper.insertItemStacked(target, one, true).isEmpty()) {
                ItemHandlerHelper.insertItemStacked(target, items.extractItem(slot, 1, false), false);
                return;
            }
        }
    }

    /// Harvest: only a full hive (honey level 5), everything at once, without angering the bees
    private void harvestHive(Level level, BlockPos hivePos, BlockState state) {
        BlockState hiveState = level.getBlockState(hivePos);
        if (!(hiveState.getBlock() instanceof BeehiveBlock hiveBlock) || hiveState.getValue(BeehiveBlock.HONEY_LEVEL) < 5) return;
        BlockEntity hiveEntity = level.getBlockEntity(hivePos);
        if (!(hiveEntity instanceof BeehiveBlockEntity)) return;
        if (state.getValue(CombChuteBlock.HONEY)) {
            ItemStack bottle = items.getStackInSlot(BOTTLE_SLOT);
            // An Advanced Beehive's honey fills two bottles; a vanilla beehive, or a chute down to its last bottle, one
            int count = Math.min(bottle.getCount(), hiveEntity instanceof AdvancedBeehiveBlockEntity ? 2 : 1);
            ItemStack honey = new ItemStack(Items.HONEY_BOTTLE, count);
            if (bottle.isEmpty() || !fits(List.of(honey))) return;
            items.extractItem(BOTTLE_SLOT, count, false);
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

    /** Everything inside, for dropping when the chute is broken. */
    public List<ItemStack> contents() {
        List<ItemStack> list = new ArrayList<>();
        for (int slot = 0; slot < items.getSlots(); slot++) {
            if (!items.getStackInSlot(slot).isEmpty()) list.add(items.getStackInSlot(slot).copy());
        }
        return list;
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
    }

    /** Items currently in the output slots, in slot order, for the HUD and Jade. */
    public java.util.List<net.minecraft.world.item.ItemStack> outputStacks() {
        java.util.List<net.minecraft.world.item.ItemStack> list = new java.util.ArrayList<>();
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            net.minecraft.world.item.ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) list.add(stack.copy());
        }
        return list;
    }

    @Override
    public @NotNull net.minecraft.nbt.CompoundTag getUpdateTag(@NotNull net.minecraft.core.HolderLookup.Provider registries) {
        net.minecraft.nbt.CompoundTag tag = saveWithoutMetadata(registries);
        tag.putBoolean("BubbasSync", true);   // an empty tag would be dropped from the update packet
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
