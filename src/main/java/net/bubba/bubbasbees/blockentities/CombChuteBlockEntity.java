package net.bubba.bubbasbees.blockentities;

import net.bubba.bubbasbees.blocks.custom.CombChuteBlock;
import net.bubba.bubbasbees.registries.ModBlockEntities;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
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

    private final SimpleContainer items = new SimpleContainer(OUTPUT_SLOTS + 1) {
        @Override
        public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
            return slot != BOTTLE_SLOT || stack.is(Items.GLASS_BOTTLE);
        }
    };

    private final List<SlotStorage> views = new ArrayList<>();
    {
        items.addListener(container -> {
            setChanged();
            if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        });
        for (int slot = 0; slot <= OUTPUT_SLOTS; slot++) views.add(new SlotStorage(slot));
    }

    /** What hoppers and pipes see: they can pull from the output slots and put glass bottles in. */
    private final Storage<ItemVariant> external = new Storage<>() {
        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            return views.get(BOTTLE_SLOT).insert(resource, maxAmount, transaction);
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            long total = 0;
            for (int slot = 0; slot < OUTPUT_SLOTS && total < maxAmount; slot++) {
                total += views.get(slot).extract(resource, maxAmount - total, transaction);
            }
            return total;
        }

        @Override
        public Iterator<StorageView<ItemVariant>> iterator() {
            return views.stream().map(view -> (StorageView<ItemVariant>) view).iterator();
        }
    };

    public CombChuteBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMB_CHUTE.get(), pos, state);
    }

    /** The item storage registered with the Fabric Transfer API for hopper/pipe access. */
    public Storage<ItemVariant> getExternalStorage() {
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
        Storage<ItemVariant> target = ItemStorage.SIDED.find(level, pos.relative(spout), spout.getOpposite());
        if (target == null) return;
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            ItemStack one = items.getItem(slot);
            if (one.isEmpty()) continue;
            long moved = 0;
            try (Transaction transaction = Transaction.openOuter()) {
                moved = target.insert(ItemVariant.of(one), 1, transaction);
                if (moved > 0) transaction.commit();
            }
            if (moved > 0) {
                items.removeItem(slot, (int) moved);
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
            ItemStack bottle = items.getItem(BOTTLE_SLOT);
            // An Advanced Beehive's honey fills two bottles; a vanilla beehive, or a chute down to its last bottle, one
            int count = Math.min(bottle.getCount(), hiveEntity instanceof AdvancedBeehiveBlockEntity ? 2 : 1);
            ItemStack honey = new ItemStack(Items.HONEY_BOTTLE, count);
            if (bottle.isEmpty() || !fits(List.of(honey))) return;
            items.removeItem(BOTTLE_SLOT, count);
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
        SimpleContainer copy = new SimpleContainer(OUTPUT_SLOTS);
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) copy.setItem(slot, items.getItem(slot).copy());
        for (ItemStack stack : stacks) {
            if (!insertInto(copy, stack.copy(), OUTPUT_SLOTS)) return false;
        }
        return true;
    }

    private void store(ItemStack stack) {
        insertInto(items, stack, OUTPUT_SLOTS);
    }

    /** Stacks the items into the first slots that hold the same item or are empty; returns whether all fitted. */
    private static boolean insertInto(Container target, ItemStack stack, int slotCount) {
        ItemStack left = stack;
        for (int slot = 0; slot < slotCount && !left.isEmpty(); slot++) {
            ItemStack current = target.getItem(slot);
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, left)) continue;
            int space = current.isEmpty() ? 64 : current.getMaxStackSize() - current.getCount();
            int move = Math.min(space, left.getCount());
            if (move <= 0) continue;
            if (current.isEmpty()) target.setItem(slot, left.copyWithCount(move));
            else {
                current.grow(move);
                target.setChanged();
            }
            left.shrink(move);
        }
        return left.isEmpty();
    }

    /** Puts glass bottles in the bottle slot; returns what didn't fit. */
    public ItemStack insertBottles(ItemStack bottles) {
        if (bottles.isEmpty() || !bottles.is(Items.GLASS_BOTTLE)) return bottles;
        ItemStack current = items.getItem(BOTTLE_SLOT);
        if (!current.isEmpty() && !current.is(Items.GLASS_BOTTLE)) return bottles;
        int space = 64 - current.getCount();
        int move = Math.min(space, bottles.getCount());
        if (move <= 0) return bottles;
        if (current.isEmpty()) items.setItem(BOTTLE_SLOT, bottles.copyWithCount(move));
        else {
            current.grow(move);
            items.setChanged();
        }
        ItemStack left = bottles.copy();
        left.shrink(move);
        return left;
    }

    public int bottleCount() {
        return items.getItem(BOTTLE_SLOT).getCount();
    }

    /** Everything inside, for dropping when the chute is broken. */
    public List<ItemStack> contents() {
        List<ItemStack> list = new ArrayList<>();
        for (int slot = 0; slot < items.getContainerSize(); slot++) {
            if (!items.getItem(slot).isEmpty()) list.add(items.getItem(slot).copy());
        }
        return list;
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.getItems().clear();   // the client gets this on every sync: slots missing from the tag must empty
        ContainerHelper.loadAllItems(tag, items.getItems(), registries);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items.getItems(), registries);
    }

    /** One slot of the external storage: bottles go into the bottle slot, output items come out of the other five. */
    private final class SlotStorage extends SnapshotParticipant<ItemStack> implements SingleSlotStorage<ItemVariant> {
        private final int slot;

        private SlotStorage(int slot) {
            this.slot = slot;
        }

        private ItemStack stack() {
            return items.getItem(slot);
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0 || slot != BOTTLE_SLOT || !resource.isOf(Items.GLASS_BOTTLE)) return 0;
            ItemStack current = stack();
            // SimpleContainer allows 99 per slot on 1.21; a slot never holds more than the item's own stack size
            int limit = Math.min(items.getMaxStackSize(), resource.toStack().getMaxStackSize());
            long space = current.isEmpty() ? limit
                    : ItemVariant.of(current).equals(resource) ? limit - current.getCount() : 0;
            if (space <= 0) return 0;
            long inserted = Math.min(maxAmount, space);
            updateSnapshots(transaction);
            if (current.isEmpty()) items.setItem(slot, resource.toStack((int) inserted));
            else {
                current.grow((int) inserted);
                items.setChanged();
            }
            return inserted;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0 || slot == BOTTLE_SLOT) return 0;
            ItemStack current = stack();
            if (current.isEmpty() || !resource.matches(current)) return 0;
            long extracted = Math.min(maxAmount, current.getCount());
            updateSnapshots(transaction);
            current.shrink((int) extracted);
            if (current.isEmpty()) items.setItem(slot, ItemStack.EMPTY);
            else items.setChanged();
            return extracted;
        }

        @Override
        protected ItemStack createSnapshot() {
            return stack().copy();
        }

        @Override
        protected void readSnapshot(ItemStack snapshot) {
            items.setItem(slot, snapshot);
        }

        @Override
        protected void onFinalCommit() {
            setChanged();
        }

        @Override
        public boolean isResourceBlank() {
            return stack().isEmpty();
        }

        @Override
        public ItemVariant getResource() {
            return ItemVariant.of(stack());
        }

        @Override
        public long getAmount() {
            return stack().getCount();
        }

        @Override
        public long getCapacity() {
            return stack().isEmpty() ? 64 : stack().getMaxStackSize();
        }
    }

    /** Items currently in the output slots, in slot order, for the HUD and Jade. */
    public java.util.List<net.minecraft.world.item.ItemStack> outputStacks() {
        java.util.List<net.minecraft.world.item.ItemStack> list = new java.util.ArrayList<>();
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            net.minecraft.world.item.ItemStack stack = items.getItem(slot);
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
