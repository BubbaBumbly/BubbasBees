package net.bubba.bubbasbees.entity.custom;

import net.bubba.bubbasbees.blocks.custom.PartialLavaCauldronBlock;
import net.bubba.bubbasbees.registries.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Server-side trait actions for mod bees (see BeeSpec.Trait); BubbasBee decides when they run. */
final class BeeTraits {
    private BeeTraits() {}

    /// Burrower
    static final int BURROW_RANGE = 3;

    /** A sand, red sand or gravel block near the bee with air above it, or null. */
    static @Nullable BlockPos findBurrow(BubbasBee bee) {
        BlockPos origin = bee.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-BURROW_RANGE, -BURROW_RANGE, -BURROW_RANGE),
                origin.offset(BURROW_RANGE, BURROW_RANGE, BURROW_RANGE))) {
            BlockState state = bee.level.getBlockState(pos);
            if (!(state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL)) || !bee.level.getBlockState(pos.above()).isAir()) continue;
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }
        return best;
    }

    static void burrowParticles(BubbasBee bee, BlockPos block) {
        if (!(bee.level instanceof ServerLevel server)) return;
        BlockState state = server.getBlockState(block);
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state.isAir() ? Blocks.SAND.defaultBlockState() : state),
                block.getX() + 0.5D, block.getY() + 1.0D, block.getZ() + 0.5D, 24, 0.3D, 0.15D, 0.3D, 0.1D);
        server.playSound(null, block, state.getSoundType().getBreakSound(), SoundSource.NEUTRAL, 0.8F, 1.2F);
    }

    /// Short teleport (ender bee): like an enderman's, but short and only through the air
    static void shortTeleport(BubbasBee bee) {
        Level level = bee.level;
        for (int attempt = 0; attempt < 16; attempt++) {
            double x = bee.getX() + (bee.getRandom().nextDouble() - 0.5D) * 16.0D;
            double y = bee.getY() + (bee.getRandom().nextDouble() - 0.5D) * 8.0D;
            double z = bee.getZ() + (bee.getRandom().nextDouble() - 0.5D) * 16.0D;
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.isLoaded(pos) || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()
                    || !level.getFluidState(pos).isEmpty()) continue;
            double fromX = bee.getX(), fromY = bee.getY(), fromZ = bee.getZ();
            bee.teleportTo(pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D);
            bee.getNavigation().stop();
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.PORTAL, fromX, fromY + 0.3D, fromZ, 16, 0.2D, 0.2D, 0.2D, 0.3D);
                server.sendParticles(ParticleTypes.PORTAL, bee.getX(), bee.getY() + 0.3D, bee.getZ(), 16, 0.2D, 0.2D, 0.2D, 0.3D);
            }
            level.playSound(null, fromX, fromY, fromZ, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.6F, 1.5F);
            level.gameEvent(GameEvent.TELEPORT, bee.position(), GameEvent.Context.of(bee));
            return;
        }
    }

    /// Lava filler (magma bee): the first cauldron found within 3 blocks below the bee
    static @Nullable BlockPos cauldronBelow(BubbasBee bee) {
        BlockPos pos = bee.blockPosition();
        for (int down = 0; down <= 3; down++) {
            BlockState state = bee.level.getBlockState(pos.below(down));
            if (state.is(Blocks.CAULDRON) || state.is(ModBlocks.PARTIAL_LAVA_CAULDRON.get())) return pos.below(down);
            if (!state.isAir()) return null;
        }
        return null;
    }

    /** Adds a quarter of lava; four quarters make the vanilla lava cauldron. */
    static void addLava(BubbasBee bee, BlockPos cauldron) {
        BlockState state = bee.level.getBlockState(cauldron);
        BlockState partial = ModBlocks.PARTIAL_LAVA_CAULDRON.get().defaultBlockState();
        int level = state.is(ModBlocks.PARTIAL_LAVA_CAULDRON.get()) ? state.getValue(PartialLavaCauldronBlock.LEVEL) : 0;
        bee.level.setBlockAndUpdate(cauldron, PartialLavaCauldronBlock.stateFor(partial, level + 1));
        bee.level.playSound(null, cauldron, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 0.6F, 1.2F);
        bee.level.gameEvent(GameEvent.FLUID_PLACE, cauldron, GameEvent.Context.of(bee));
    }

    /// Glow: a self-removing light block where the bee flies through the dark
    static final int LIGHT_BELOW = 8;

    static void placeLight(BubbasBee bee) {
        BlockPos pos = bee.blockPosition();
        BlockState state = bee.level.getBlockState(pos);
        if (!state.is(Blocks.AIR) && !state.is(Blocks.CAVE_AIR)) return;
        if (bee.level.getBrightness(LightLayer.BLOCK, pos) >= LIGHT_BELOW) return;
        if (bee.level.getBrightness(LightLayer.SKY, pos) - bee.level.getSkyDarken() >= LIGHT_BELOW) return;
        bee.level.setBlock(pos, ModBlocks.BEE_LIGHT.get().defaultBlockState(), 3);
    }

    /// Royal guard: waxes one unwaxed copper block within 4 blocks
    static final int WAX_RANGE = 4;

    static void waxNearbyCopper(BubbasBee bee) {
        BlockPos origin = bee.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-WAX_RANGE, -WAX_RANGE, -WAX_RANGE),
                origin.offset(WAX_RANGE, WAX_RANGE, WAX_RANGE))) {
            BlockState state = bee.level.getBlockState(pos);
            Optional<BlockState> waxed = HoneycombItem.getWaxed(state);
            if (waxed.isEmpty()) continue;
            bee.level.setBlockAndUpdate(pos, waxed.get());
            bee.level.levelEvent(3003, pos, 0);   // wax-on particles + sound
            return;
        }
    }

    /// Royal queen: half a heart to every bee within 8 blocks, herself included
    static final double HEAL_RANGE = 8.0D;

    static void healBees(BubbasBee queen) {
        for (Bee bee : queen.level.getEntitiesOfClass(Bee.class, queen.getBoundingBox().inflate(HEAL_RANGE))) {
            if (bee.isAlive() && bee.getHealth() < bee.getMaxHealth()) {
                bee.heal(1.0F);
                if (queen.level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.HEART, bee.getX(), bee.getY() + 0.6D, bee.getZ(), 1, 0.1D, 0.1D, 0.1D, 0.0D);
                }
            }
        }
    }
}
