package net.bubba.bubbasbees.blockentities;

import net.bubba.bubbasbees.registries.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/** The one item on a pedestal or the Celestial Altar, synced to clients for the floating display. */
public class PedestalBlockEntity extends BlockEntity {
    private static final String ITEM_TAG = "Item";
    private static final String RITUAL_TAG = "RitualActive";
    private ItemStack item = ItemStack.EMPTY;
    /** Set by a running ritual: the altar spins its item faster. */
    private boolean ritualActive = false;

    public PedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PEDESTAL.get(), pos, state);
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack stack) {
        this.item = stack;
        changed();
    }

    public boolean isRitualActive() {
        return ritualActive;
    }

    public void setRitualActive(boolean active) {
        if (active == ritualActive) return;
        ritualActive = active;
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        item = tag.contains(ITEM_TAG) ? ItemStack.parseOptional(registries, tag.getCompound(ITEM_TAG)) : ItemStack.EMPTY;
        ritualActive = tag.getBoolean(RITUAL_TAG);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!item.isEmpty()) tag.put(ITEM_TAG, item.save(registries));
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(@NotNull HolderLookup.Provider registries) {
        CompoundTag tag = saveWithoutMetadata(registries);
        // Always written, so the tag is never empty: an empty tag is dropped from the update packet, and the client
        // would keep drawing an item that was taken off. Client only: rituals live in memory (RitualManager).
        tag.putBoolean(RITUAL_TAG, ritualActive);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
