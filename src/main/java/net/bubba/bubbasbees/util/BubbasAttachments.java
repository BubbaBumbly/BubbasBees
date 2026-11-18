package net.bubba.bubbasbees.util;

// DEVIATION: NeoForge's Entity.getPersistentData() is replaced with a Fabric per-entity data attachment.
import net.bubba.bubbasbees.BubbasBees;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** Per-entity NBT (the old NeoForge getPersistentData()) via Fabric's data attachment API. */
public final class BubbasAttachments {
    private static final AttachmentType<CompoundTag> ENTITY_DATA = AttachmentRegistry.createPersistent(
            ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "entity_data"), CompoundTag.CODEC);

    private BubbasAttachments() {
    }

    /** Forces registration during mod initialization (attachments cannot be registered later). */
    public static void init() {
    }

    public static CompoundTag entityData(Entity entity) {
        return ((AttachmentTarget) entity).getAttachedOrCreate(ENTITY_DATA, CompoundTag::new);
    }
}
