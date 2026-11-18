package net.bubba.bubbasbees.registries;

import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;

/** Royal guards mug anyone who hurts a bee near them (the queen and the guards themselves included). */
public final class BeeEvents {
    private static final double GUARD_RANGE = 16.0D;

    private BeeEvents() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!entity.level().isClientSide && entity instanceof Bee) {
                if (source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof Bee)) {
                    if (!(attacker instanceof Player player && (player.isCreative() || player.isSpectator()))) {
                        for (BubbasBee guard : entity.level().getEntitiesOfClass(BubbasBee.class,
                                entity.getBoundingBox().inflate(GUARD_RANGE),
                                bee -> bee.isRoyalGuard() && bee.isAlive())) {
                            guard.mug(attacker);
                        }
                    }
                }
            }
            return true;
        });
    }
}
