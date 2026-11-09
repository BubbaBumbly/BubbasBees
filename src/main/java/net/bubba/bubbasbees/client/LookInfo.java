package net.bubba.bubbasbees.client;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.bees.BeeSpec;
import net.bubba.bubbasbees.blockentities.CombChuteBlockEntity;
import net.bubba.bubbasbees.blockentities.PedestalBlockEntity;
import net.bubba.bubbasbees.blocks.custom.*;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.bubba.bubbasbees.entity.custom.DarkBee;
import net.bubba.bubbasbees.registries.ModBlocks;
import net.bubba.bubbasbees.util.HiveInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Look-at HUD lines for the mod's non-hive blocks and bees (including vanilla bees and dark bees).
 * Client-only: it reads {@link Minecraft#level} and {@link Minecraft#hitResult}.
 */
public final class LookInfo {
    private LookInfo() {}

    @NotNull
    public static List<Component> lines(Minecraft minecraft, HitResult hit) {
        if (hit == null || hit.getType() == HitResult.Type.MISS || minecraft.level == null) return List.of();
        if (hit instanceof BlockHitResult blockHit) return blockLines(minecraft.level, blockHit.getBlockPos());
        if (hit instanceof EntityHitResult entityHit) return entityLines(entityHit.getEntity());
        return List.of();
    }

    @NotNull
    private static List<Component> blockLines(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (!isOurBlock(block) || HiveInfo.isHive(state)) return List.of();

        List<Component> lines = new ArrayList<>();
        lines.add(block.getName());
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (block instanceof CombChuteBlock) {
            boolean honey = state.getValue(CombChuteBlock.HONEY);
            lines.add(Component.translatable(honey
                    ? "hud.bubbasbees.comb_chute.honey_mode" : "hud.bubbasbees.comb_chute.comb_mode"));
            if (blockEntity instanceof CombChuteBlockEntity chute) {
                lines.add(Component.translatable("hud.bubbasbees.comb_chute.bottles", chute.bottleCount()));
                for (ItemStack stack : chute.outputStacks()) {
                    lines.add(Component.translatable("hud.bubbasbees.comb_chute.output_item",
                            stack.getHoverName(), stack.getCount()));
                }
            }
            return lines;
        }

        if (block instanceof PedestalBlock) {
            if (blockEntity instanceof PedestalBlockEntity pedestal && !pedestal.getItem().isEmpty()) {
                lines.add(Component.translatable("hud.bubbasbees.pedestal.holding", pedestal.getItem().getHoverName()));
            } else {
                lines.add(Component.translatable("hud.bubbasbees.pedestal.empty"));
            }
            return lines;
        }

        if (block instanceof PartialLavaCauldronBlock) {
            int mb = state.getValue(PartialLavaCauldronBlock.LEVEL) * PartialLavaCauldronBlock.MB_PER_LEVEL;
            lines.add(Component.translatable("hud.bubbasbees.lava_cauldron", mb));
            return lines;
        }

        if (block instanceof StardustBlock) {
            lines.add(Component.translatable("hud.bubbasbees.stardust"));
            return lines;
        }

        if (block instanceof RoyalJellyBlock) {
            lines.add(Component.translatable("hud.bubbasbees.royal_jelly_block"));
            return lines;
        }

        if (block instanceof PinkPistonHeadBlock) {
            lines.add(Component.translatable("hud.bubbasbees.pink_piston_head"));
            return lines;
        }

        if (block == ModBlocks.PINK_STICKY_PISTON.get()) {
            lines.add(Component.translatable("hud.bubbasbees.pink_sticky_piston"));
            return lines;
        }

        return List.of();
    }

    @NotNull
    private static List<Component> entityLines(Entity entity) {
        List<Component> lines = new ArrayList<>();

        if (entity instanceof BubbasBee bee) {
            lines.add(entity.getName());
            BeeSpec spec = bee.getSpec();
            BeeSpec.Tier tier = spec != null ? spec.tier() : BeeSpec.Tier.NONE;
            lines.add(Component.translatable(tierKey(tier)));
            if (spec != null && !spec.traits().isEmpty()) {
                lines.add(traitsLine(spec.traits()));
            }
            if (bee.isAngry()) lines.add(Component.translatable("hud.bubbasbees.bee.angry"));
            if (bee.hasNectar()) lines.add(Component.translatable("hud.bubbasbees.bee.pollen"));
            if (bee.isBaby()) lines.add(Component.translatable("hud.bubbasbees.bee.baby"));
            return lines;
        }

        if (entity instanceof Bee bee) {
            lines.add(entity.getName());
            if (bee.isAngry()) lines.add(Component.translatable("hud.bubbasbees.bee.angry"));
            if (bee.hasNectar()) lines.add(Component.translatable("hud.bubbasbees.bee.pollen"));
            return lines;
        }

        if (entity instanceof DarkBee) {
            lines.add(entity.getName());
            return lines;
        }

        return List.of();
    }

    private static boolean isOurBlock(Block block) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
        return key != null && BubbasBees.MOD_ID.equals(key.getNamespace());
    }

    private static String tierKey(BeeSpec.Tier tier) {
        return "hud.bubbasbees.tier." + tier.name().toLowerCase(Locale.ROOT);
    }

    private static String traitKey(BeeSpec.Trait trait) {
        return "hud.bubbasbees.trait." + trait.name().toLowerCase(Locale.ROOT);
    }

    @NotNull
    private static Component traitsLine(Iterable<BeeSpec.Trait> traits) {
        MutableComponent line = Component.empty();
        boolean first = true;
        for (BeeSpec.Trait trait : traits) {
            if (!first) line.append(", ");
            first = false;
            line.append(Component.translatable(traitKey(trait)));
        }
        return line;
    }
}
