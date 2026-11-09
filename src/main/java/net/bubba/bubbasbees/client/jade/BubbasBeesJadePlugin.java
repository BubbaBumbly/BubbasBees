package net.bubba.bubbasbees.client.jade;

import net.bubba.bubbasbees.entity.custom.DarkBee;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade integration, loaded by Jade only (it finds this class by its annotation), so Jade stays optional.
 * While Jade is installed the bee list goes into Jade's tooltip and HiveInfoOverlay stays hidden.
 * LookInfo providers add the same non-hive block/entity lines Jade shows in its own tooltip.
 */
@WailaPlugin
public class BubbasBeesJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(HiveBeesProvider.INSTANCE, BeehiveBlock.class);
        registration.registerBlockComponent(LookInfoBlockProvider.INSTANCE, Block.class);
        registration.registerEntityComponent(LookInfoEntityProvider.INSTANCE, Bee.class);
        registration.registerEntityComponent(LookInfoEntityProvider.INSTANCE, DarkBee.class);
    }
}
