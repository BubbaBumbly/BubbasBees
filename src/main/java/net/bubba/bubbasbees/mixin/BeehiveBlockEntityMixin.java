package net.bubba.bubbasbees.mixin;

import net.bubba.bubbasbees.blockentities.AdvancedBeehiveBlockEntity;
import net.bubba.bubbasbees.blockentities.HiveOccupant;
import net.bubba.bubbasbees.blockentities.HiveOccupantView;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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

/** Syncs the stored bees (type + release time) to the client so the hive info HUD can show them. */
@Mixin(BeehiveBlockEntity.class)
public abstract class BeehiveBlockEntityMixin extends BlockEntity implements HiveOccupantView {

    @Shadow @Final private List<BeehiveBlockEntity.BeeData> stored;

    @Unique private static final int ADVANCED_STAY_TICKS = 600;
    @Unique private static final int ADVANCED_STAY_TICKS_WITH_NECTAR = 1200;
    @Unique private final List<HiveOccupant> bubbasBees$clientOccupants = new ArrayList<>();
    @Unique private String bubbasBees$lastSyncedSignature = null;

    protected BeehiveBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // The advanced hive releases on its own fixed thresholds; vanilla uses each bee's minOccupationTicks
    @Unique
    private List<HiveOccupant> bubbasBees$serverOccupants() {
        long now = this.level == null ? 0L : this.level.getGameTime();
        boolean advanced = (Object) this instanceof AdvancedBeehiveBlockEntity;
        List<HiveOccupant> list = new ArrayList<>(this.stored.size());
        for (BeehiveBlockEntity.BeeData beeData : this.stored) {
            BeehiveBeeDataAccessor data = (BeehiveBeeDataAccessor) beeData;
            CompoundTag entityData = data.getEntityData();
            ResourceLocation id = ResourceLocation.tryParse(entityData.getString("id"));
            if (id == null) continue;
            int stayTicks = advanced
                    ? (entityData.getBoolean("HasNectar") ? ADVANCED_STAY_TICKS_WITH_NECTAR : ADVANCED_STAY_TICKS)
                    : data.getMinOccupationTicks();
            long due = now + Math.max(0, stayTicks - data.getTicksInHive());
            list.add(new HiveOccupant(id, due));
        }
        return list;
    }

    @Unique
    private static String bubbasBees$signature(List<HiveOccupant> occupants) {
        StringBuilder builder = new StringBuilder();
        for (HiveOccupant occupant : occupants) {
            builder.append(occupant.id()).append('@').append(occupant.dueGameTime()).append(';');
        }
        return builder.toString();
    }

    @Override
    public List<HiveOccupant> bubbasBees$getClientOccupants() {
        return this.bubbasBees$clientOccupants;
    }

    @Override
    public void bubbasBees$syncOccupantsIfChanged() {
        Level level = this.level;
        if (level == null || level.isClientSide) return;
        String signature = bubbasBees$signature(bubbasBees$serverOccupants());
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

    // Merged overrides of BlockEntity: only ids and release times travel to the client, never full bee NBT
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (HiveOccupant occupant : bubbasBees$serverOccupants()) {
            CompoundTag entry = new CompoundTag();
            entry.putString(ID_TAG, occupant.id().toString());
            entry.putLong(DUE_TAG, occupant.dueGameTime());
            list.add(entry);
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
        if (!tag.contains(OCCUPANTS_TAG, Tag.TAG_LIST)) return;
        this.bubbasBees$clientOccupants.clear();
        ListTag list = tag.getList(OCCUPANTS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(entry.getString(ID_TAG));
            if (id != null) this.bubbasBees$clientOccupants.add(new HiveOccupant(id, entry.getLong(DUE_TAG)));
        }
    }
}
