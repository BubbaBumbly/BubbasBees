package net.bubba.bubbasbees.entity.custom;

import net.bubba.bubbasbees.bees.BeeSpec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Minimal ambient particles for some bees (user 2026-11-08, "P1 very rare"): about one every 4 seconds per bee,
 * client side only. Ender portal specks, magma small flame, soul, deep dark sculk soul, frost
 * snowflakes (corrupt nature: none, user 2026-11-08), starlight end-rod sparkle at night, queen wax sparkle, water bees bubbles while in water, mushroom
 * spores.
 */
public final class BeeParticles {
    /** One in this many client ticks: ~4 s on average. */
    public static final int CHANCE = 80;

    private BeeParticles() {}

    public static void clientTick(BubbasBee bee) {
        BeeSpec spec = bee.getSpec();
        if (spec == null || bee.isInvisible()) return;
        RandomSource random = bee.getRandom();
        if (random.nextInt(CHANCE) != 0) return;
        Level level = bee.level;
        ParticleOptions particle = switch (spec.id()) {
            case "ender" -> ParticleTypes.PORTAL;
            case "magma" -> ParticleTypes.SMALL_FLAME;
            case "soul" -> ParticleTypes.SOUL;
            case "deep_dark" -> ParticleTypes.SCULK_SOUL;
            case "frost" -> ParticleTypes.SNOWFLAKE;
            case "starlight" -> level.isNight() ? ParticleTypes.END_ROD : null;
            case "royal_queen" -> ParticleTypes.WAX_ON;
            case "sea", "coral", "prismarine" -> bee.isInWater() ? ParticleTypes.BUBBLE : null;
            case "mushroom_fields" -> ParticleTypes.MYCELIUM;
            default -> null;
        };
        if (particle == null) return;
        double x = bee.getRandomX(0.5D), y = bee.getRandomY(), z = bee.getRandomZ(0.5D);
        if (particle == ParticleTypes.PORTAL) {   // portal particles drift in from their motion, like an enderman's
            level.addParticle(particle, x, y, z, (random.nextDouble() - 0.5D) * 0.5D, -random.nextDouble() * 0.3D,
                    (random.nextDouble() - 0.5D) * 0.5D);
        } else {
            level.addParticle(particle, x, y, z, 0.0D, 0.01D, 0.0D);
        }
    }
}
