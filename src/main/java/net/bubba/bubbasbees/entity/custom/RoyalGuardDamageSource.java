package net.bubba.bubbasbees.entity.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.entity.LivingEntity;

/** A royal guard's sting: the same damage as a bee's sting, with its own death messages. */
public class RoyalGuardDamageSource extends EntityDamageSource {
    private static final String KEY = "death.attack.bubbasbees.royal_guard.";
    private static final int MESSAGES = 11;

    public RoyalGuardDamageSource(BubbasBee guard) {
        super("sting", guard);
    }

    /** One of the royal death messages at random; %2$s in the lang text is the guard's name. */
    @Override
    public Component getLocalizedDeathMessage(LivingEntity killed) {
        return Component.translatable(KEY + killed.getRandom().nextInt(MESSAGES), killed.getDisplayName(), this.getEntity().getDisplayName());
    }
}
