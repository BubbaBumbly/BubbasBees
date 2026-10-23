package net.bubba.bubbasbees.client.jade;

import net.minecraft.world.level.block.BeehiveBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade integration, loaded by Jade only (it finds this class by its annotation), so Jade stays optional.
 * While Jade is installed the bee list goes into Jade's tooltip and HiveInfoOverlay stays hidden.
 */
@WailaPlugin
public class BubbasBeesJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(HiveBeesProvider.INSTANCE, BeehiveBlock.class);
    }
}
