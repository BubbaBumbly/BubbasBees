package net.bubba.bubbasbees.registries;

import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Royal guards mug anyone who hurts a bee near them (the queen and the guards themselves included). */
@Mod.EventBusSubscriber(modid = BubbasBees.MOD_ID)
public class BeeEvents {
    private static final double GUARD_RANGE = 16.0D;

    @SubscribeEvent
    public static void onBeeHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level.isClientSide || !(victim instanceof Bee)) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker instanceof Bee) return;
        if (attacker instanceof Player player && (player.isCreative() || player.isSpectator())) return;
        for (BubbasBee guard : victim.level.getEntitiesOfClass(BubbasBee.class, victim.getBoundingBox().inflate(GUARD_RANGE),
                bee -> bee.isRoyalGuard() && bee.isAlive())) {
            guard.mug(attacker);
        }
    }
}
