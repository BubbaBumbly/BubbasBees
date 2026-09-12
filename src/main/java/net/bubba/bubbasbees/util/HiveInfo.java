package net.bubba.bubbasbees.util;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.blockentities.HiveOccupant;
import net.bubba.bubbasbees.blockentities.HiveOccupantView;
import net.bubba.bubbasbees.blocks.custom.AdvancedBeehiveBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Shared, side-neutral helpers for reading what a hive holds (from a block entity or an item). */
public final class HiveInfo {
    public static final int VANILLA_MAX_OCCUPANTS = 3;
    public static final int ADVANCED_MAX_OCCUPANTS = 5;
    public static final int MAX_HONEY = 5;
    // Face of the 7x7x10 bee body in the 64x64 entity texture (box UV north face)
    public static final int FACE_U = 10;
    public static final int FACE_V = 10;
    public static final int FACE_SIZE = 7;
    public static final int TEXTURE_SIZE = 64;
    private static final ResourceLocation VANILLA_BEE_TEXTURE = new ResourceLocation("minecraft", "textures/entity/bee/bee.png");

    private HiveInfo() {}

    public static boolean isHive(BlockState state) {
        return state.is(BlockTags.BEEHIVES);
    }

    public static boolean isHiveItem(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && isHive(blockItem.getBlock().defaultBlockState());
    }

    public static int maxOccupants(Block block) {
        return block instanceof AdvancedBeehiveBlock ? ADVANCED_MAX_OCCUPANTS : VANILLA_MAX_OCCUPANTS;
    }

    public static int honeyLevel(BlockState state) {
        return state.hasProperty(BeehiveBlock.HONEY_LEVEL) ? state.getValue(BeehiveBlock.HONEY_LEVEL) : 0;
    }

    /** Occupants of a client-side hive block entity, or an empty list. */
    public static List<HiveOccupant> occupants(@Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof HiveOccupantView view) {
            return view.bubbasBees$getClientOccupants();
        }
        return List.of();
    }

    /** Occupants stored in a picked-up hive item's BlockEntityTag (no release timers). */
    public static List<HiveOccupant> occupants(ItemStack stack) {
        List<HiveOccupant> occupants = new ArrayList<>();
        CompoundTag blockEntityTag = stack.getTagElement("BlockEntityTag");
        if (blockEntityTag == null) return occupants;
        ListTag bees = blockEntityTag.getList("Bees", Tag.TAG_COMPOUND);
        for (int i = 0; i < bees.size(); i++) {
            CompoundTag entityData = bees.getCompound(i).getCompound("EntityData");
            ResourceLocation id = ResourceLocation.tryParse(entityData.getString("id"));
            if (id != null) occupants.add(new HiveOccupant(id));
        }
        return occupants;
    }

    /** Honey level copied into a hive item's BlockStateTag, or 0 if absent. */
    public static int honeyLevel(ItemStack stack) {
        CompoundTag stateTag = stack.getTagElement("BlockStateTag");
        if (stateTag == null || !stateTag.contains("honey_level")) return 0;
        try {
            return Integer.parseInt(stateTag.getString("honey_level"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static Component beeName(ResourceLocation entityId) {
        return EntityType.byString(entityId.toString())
                .map(EntityType::getDescription)
                .orElse(Component.literal(entityId.toString()));
    }

    /** Entity texture whose face region is drawn as the bee icon, or null when unknown. */
    @Nullable
    public static ResourceLocation iconTexture(ResourceLocation entityId) {
        if (entityId.getNamespace().equals("minecraft") && entityId.getPath().equals("bee")) {
            return VANILLA_BEE_TEXTURE;
        }
        if (!entityId.getNamespace().equals(BubbasBees.MOD_ID)) return null;
        String path = entityId.getPath();
        String texture = path.equals("bubbas_bee") ? "diamond_bee" : path;
        return new ResourceLocation(BubbasBees.MOD_ID, "textures/entity/" + texture + ".png");
    }

    /** Countdown text for a stored bee: m:ss until release, or "waiting" when held past its time. */
    public static Component timerText(HiveOccupant occupant, long gameTime) {
        long remaining = occupant.remainingTicks(gameTime);
        if (remaining <= 0L) return Component.translatable("gui.bubbasbees.hive_info.waiting");
        long seconds = (remaining + 19L) / 20L;
        return Component.literal(String.format("%d:%02d", seconds / 60L, seconds % 60L));
    }

    public static Component headerText(Component hiveName, int honey, int beeCount, int maxBees) {
        return Component.translatable("gui.bubbasbees.hive_info.header", hiveName, honey, MAX_HONEY, beeCount, maxBees);
    }

    public static Component honeyText(int honey) {
        return Component.translatable("gui.bubbasbees.hive_info.honey", honey, MAX_HONEY);
    }

    public static Component beesText(int count, int max) {
        return Component.translatable("gui.bubbasbees.hive_info.bees", count, max);
    }

    public static Component emptyText() {
        return Component.translatable("gui.bubbasbees.hive_info.empty");
    }
}
