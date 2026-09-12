package net.bubba.bubbasbees.mixin;

import net.bubba.bubbasbees.blockentities.HiveOccupantView;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** Syncs the entity types of stored bees to the client so the hive info HUD can show them. */
@Mixin(BeehiveBlockEntity.class)
public abstract class BeehiveBlockEntityMixin extends BlockEntity implements HiveOccupantView {

    @Shadow @Final private List<BeehiveBlockEntity.BeeData> stored;

    @Unique private final List<ResourceLocation> bubbasBees$clientOccupants = new ArrayList<>();
    @Unique private String bubbasBees$lastSyncedSignature = null;

    protected BeehiveBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Unique
    private List<String> bubbasBees$storedIds() {
        List<String> ids = new ArrayList<>(this.stored.size());
        for (BeehiveBlockEntity.BeeData beeData : this.stored) {
            ids.add(((BeehiveBeeDataAccessor) beeData).getEntityData().getString("id"));
        }
        return ids;
    }

    @Override
    public List<ResourceLocation> bubbasBees$getClientOccupants() {
        return this.bubbasBees$clientOccupants;
    }

    @Override
    public void bubbasBees$syncOccupantsIfChanged() {
        Level level = this.level;
        if (level == null || level.isClientSide) return;
        String signature = String.join(",", bubbasBees$storedIds());
        if (!signature.equals(this.bubbasBees$lastSyncedSignature)) {
            this.bubbasBees$lastSyncedSignature = signature;
            BlockState state = this.getBlockState();
            level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    // Vanilla hives tick here; the advanced hive calls the sync from its own ticker
    @Inject(method = "serverTick", at = @At("TAIL"))
    private static void bubbasBees$afterServerTick(Level level, BlockPos pos, BlockState state, BeehiveBlockEntity hive, CallbackInfo ci) {
        ((HiveOccupantView) hive).bubbasBees$syncOccupantsIfChanged();
    }

    // Merged overrides of BlockEntity: only the occupant ids travel to the client, never full bee NBT
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (String id : bubbasBees$storedIds()) {
            list.add(StringTag.valueOf(id));
        }
        tag.put(OCCUPANTS_TAG, list);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        this.bubbasBees$clientOccupants.clear();
        ListTag list = tag.getList(OCCUPANTS_TAG, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) this.bubbasBees$clientOccupants.add(id);
        }
    }
}
