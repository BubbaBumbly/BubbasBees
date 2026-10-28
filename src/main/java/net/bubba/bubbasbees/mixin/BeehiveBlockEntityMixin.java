package net.bubba.bubbasbees.mixin;

import net.bubba.bubbasbees.blockentities.AdvancedBeehiveBlockEntity;
import net.bubba.bubbasbees.blockentities.HiveOccupant;
import net.bubba.bubbasbees.blockentities.HiveOccupantView;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
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

/** Syncs the stored bees (type + release time), the smoke timer and the work mode to the client for the hive info HUD. */
@Mixin(BeehiveBlockEntity.class)
public abstract class BeehiveBlockEntityMixin extends BlockEntity implements HiveOccupantView {

    @Shadow @Final private List<BeehiveBlockEntity.BeeData> stored;

    @Unique private final List<HiveOccupant> bubbasBees$clientOccupants = new ArrayList<>();
    @Unique private long bubbasBees$clientSmokeEnd = NOT_SMOKED;
    @Unique private boolean bubbasBees$clientBusy = false;
    @Unique private String bubbasBees$lastSyncedSignature = null;

    protected BeehiveBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // The advanced hive releases on its own thresholds (see stayTicks); vanilla uses each bee's minOccupationTicks
    @Unique
    private List<HiveOccupant> bubbasBees$serverOccupants() {
        long now = this.level == null ? 0L : this.level.getGameTime();
        AdvancedBeehiveBlockEntity advanced = (Object) this instanceof AdvancedBeehiveBlockEntity hive ? hive : null;
        List<HiveOccupant> list = new ArrayList<>(this.stored.size());
        for (BeehiveBlockEntity.BeeData beeData : this.stored) {
            BeehiveBeeDataAccessor data = (BeehiveBeeDataAccessor) beeData;
            CompoundTag entityData = data.getEntityData();
            ResourceLocation id = ResourceLocation.tryParse(entityData.getString("id"));
            if (id == null) continue;
            int stayTicks = advanced != null
                    ? advanced.stayTicks(entityData.getBoolean("HasNectar"))
                    : data.getMinOccupationTicks();
            int remaining = stayTicks - data.getTicksInHive();
            // An overdue bee (held in by night or rain) gets a fixed marker, not a due time that moves every tick
            long due = remaining > 0 ? now + remaining : HiveOccupant.READY;
            list.add(new HiveOccupant(id, due));
        }
        return list;
    }

    // Only the advanced hive has a timed smoke; a campfire under a vanilla hive is not timed
    @Unique
    private long bubbasBees$serverSmokeEnd() {
        if (!((Object) this instanceof AdvancedBeehiveBlockEntity hive) || this.level == null) return NOT_SMOKED;
        return hive.smokeEndGameTime(this.level.getGameTime());
    }

    @Unique
    private boolean bubbasBees$serverBusy() {
        return (Object) this instanceof AdvancedBeehiveBlockEntity hive && hive.isBusy();
    }

    @Unique
    private static String bubbasBees$signature(List<HiveOccupant> occupants, long smokeEnd, boolean busy) {
        StringBuilder builder = new StringBuilder("smoke@").append(smokeEnd).append(";busy@").append(busy).append(';');
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
    public long bubbasBees$getClientSmokeEnd() {
        return this.bubbasBees$clientSmokeEnd;
    }

    @Override
    public boolean bubbasBees$isClientBusy() {
        return this.bubbasBees$clientBusy;
    }

    @Override
    public void bubbasBees$syncOccupantsIfChanged() {
        Level level = this.level;
        if (level == null || level.isClientSide) return;
        String signature = bubbasBees$signature(bubbasBees$serverOccupants(), bubbasBees$serverSmokeEnd(), bubbasBees$serverBusy());
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

    // Merged overrides of BlockEntity: only ids, release times, the smoke end and the mode travel to the client, never full bee NBT
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
        tag.putLong(SMOKE_END_TAG, bubbasBees$serverSmokeEnd());
        tag.putBoolean(BUSY_TAG, bubbasBees$serverBusy());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // Forge's default onDataPacket calls load(tag) directly, bypassing handleUpdateTag; route it here
    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) this.handleUpdateTag(tag);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        if (!tag.contains(OCCUPANTS_TAG, Tag.TAG_LIST)) return;
        this.bubbasBees$clientSmokeEnd = tag.contains(SMOKE_END_TAG) ? tag.getLong(SMOKE_END_TAG) : NOT_SMOKED;
        this.bubbasBees$clientBusy = tag.getBoolean(BUSY_TAG);
        this.bubbasBees$clientOccupants.clear();
        ListTag list = tag.getList(OCCUPANTS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(entry.getString(ID_TAG));
            if (id != null) this.bubbasBees$clientOccupants.add(new HiveOccupant(id, entry.getLong(DUE_TAG)));
        }
    }
}
