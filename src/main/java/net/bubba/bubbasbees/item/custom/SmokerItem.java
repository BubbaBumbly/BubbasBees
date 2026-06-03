package net.bubba.bubbasbees.item.custom;

import net.bubba.bubbasbees.blockentities.AdvancedBeehiveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class SmokerItem extends Item {

    public SmokerItem(Properties properties) {
        super(properties.durability(500));
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide && context.getItemInHand().getDamageValue() < context.getItemInHand().getMaxDamage()) {
            BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
            if (blockEntity instanceof AdvancedBeehiveBlockEntity hive) {
                hive.smokeHive();
                context.getItemInHand().hurtAndBreak(1, context.getPlayer(), player -> player.broadcastBreakEvent(context.getHand()));
                if (context.getLevel() instanceof ServerLevel serverLevel) {
                    BlockPos pos = context.getClickedPos();
                    serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 20, 0.2D, 0.2D, 0.2D, 0.01D);
                }
                return InteractionResult.SUCCESS;
            } else if (blockEntity instanceof BeehiveBlockEntity && context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.literal("This feature is not yet implemented."), true);
                return InteractionResult.SUCCESS;
            }
        }
        return super.useOn(context);
    }
}
