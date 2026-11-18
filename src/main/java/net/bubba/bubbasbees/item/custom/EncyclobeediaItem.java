package net.bubba.bubbasbees.item.custom;

// DEVIATION: NeoForge 1.21 removed DistExecutor; the client check uses FMLEnvironment.dist and only loads GuideScreen on a client.
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/** The guide book: right-click opens the Encyclobeedia screen (client only; the server just says it worked). */
public class EncyclobeediaItem extends Item {
    public EncyclobeediaItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide && FMLEnvironment.dist == Dist.CLIENT) {
            net.bubba.bubbasbees.client.guide.GuideScreen.open();
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
