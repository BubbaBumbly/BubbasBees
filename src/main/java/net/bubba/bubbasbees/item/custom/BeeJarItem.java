package net.bubba.bubbasbees.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BeeJarItem extends Item {

    private static final String TAG_BEE_DATA = "BeeData";

    public BeeJarItem(Properties properties) {
        super(properties);
    }

    public static boolean isFilled(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_BEE_DATA);
    }

    public static float getFillState(ItemStack stack) {
        if (!isFilled(stack)) return 0.0F;
        CompoundTag tag = stack.getTag();
        if (tag == null) return 1.0F;
        CompoundTag beeData = tag.getCompound(TAG_BEE_DATA);
        String entityTypeStr = beeData.getString("EntityType");
        if (entityTypeStr.isEmpty()) return 1.0F;

        String path = entityTypeStr.contains(":") ? entityTypeStr.substring(entityTypeStr.indexOf(':') + 1) : entityTypeStr;

        return switch (path) {
            case "copper_bee" -> 2.0F;
            case "ender_bee" -> 3.0F;
            case "gold_bee" -> 4.0F;
            case "iron_bee" -> 5.0F;
            case "nature_bee" -> 6.0F;
            case "redstone_bee" -> 7.0F;
            case "sandy_bee" -> 8.0F;
            case "stone_bee" -> 9.0F;
            default -> 1.0F;
        };
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull LivingEntity target, @NotNull InteractionHand hand) {
        if (!isFilled(stack) && target instanceof Bee bee) {
            if (!player.level.isClientSide) {
                CompoundTag beeData = new CompoundTag();
                bee.saveWithoutId(beeData);
                beeData.putString("EntityType", EntityType.getKey(bee.getType()).toString());

                if (player.isCreative()) {
                    ItemStack filledJar = new ItemStack(this, 1);
                    filledJar.getOrCreateTag().put(TAG_BEE_DATA, beeData);
                    if (!player.addItem(filledJar)) {
                        player.drop(filledJar, false);
                    }
                } else if (stack.getCount() > 1) {
                    ItemStack filledJar = new ItemStack(this, 1);
                    filledJar.getOrCreateTag().put(TAG_BEE_DATA, beeData);
                    stack.shrink(1);
                    if (!player.addItem(filledJar)) {
                        player.drop(filledJar, false);
                    }
                } else {
                    stack.getOrCreateTag().put(TAG_BEE_DATA, beeData);
                }

                bee.remove(Entity.RemovalReason.DISCARDED);

                player.level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.NEUTRAL, 1.0F, 1.0F);
                player.gameEvent(GameEvent.ENTITY_INTERACT);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player != null && isFilled(stack)) {
            if (!level.isClientSide) {
                BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
                CompoundTag beeData = stack.getTag().getCompound(TAG_BEE_DATA);
                String entityTypeStr = beeData.getString("EntityType");
                EntityType<?> entityType = EntityType.byString(entityTypeStr).orElse(null);

                if (entityType != null) {
                    Entity entity = entityType.create(level);
                    if (entity != null) {
                        beeData.remove("Pos");
                        beeData.remove("Motion");
                        beeData.remove("Rotation");
                        beeData.remove("HivePos");
                        beeData.remove("BubbasHomeAnchor");
                        beeData.remove("FlowerPos");
                        entity.load(beeData);
                        entity.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);

                        if (entity instanceof Bee bee) {
                            bee.setStayOutOfHiveCountdown(400);
                        }

                        level.addFreshEntity(entity);

                        stack.getTag().remove(TAG_BEE_DATA);
                        if (stack.getTag().isEmpty()) {
                            stack.setTag(null);
                        }

                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.BOTTLE_EMPTY, SoundSource.NEUTRAL, 1.0F, 1.0F);
                        player.gameEvent(GameEvent.ENTITY_PLACE);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        if (isFilled(stack)) {
            CompoundTag beeData = stack.getTag().getCompound(TAG_BEE_DATA);
            String entityTypeStr = beeData.getString("EntityType");
            EntityType<?> entityType = EntityType.byString(entityTypeStr).orElse(null);
            if (entityType != null) {
                if (EntityType.getKey(entityType).getNamespace().equals("minecraft")) {
                    return Component.translatable("item.bubbasbees.bee_jar.filled");
                } else {
                    return Component.translatable("item.bubbasbees.bee_jar.filled_custom", entityType.getDescription());
                }
            }
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        if (isFilled(stack)) {
            CompoundTag beeData = stack.getTag().getCompound(TAG_BEE_DATA);
            String entityTypeStr = beeData.getString("EntityType");
            EntityType<?> entityType = EntityType.byString(entityTypeStr).orElse(null);
            if (entityType != null) {
                boolean hasNectar = beeData.getBoolean("HasNectar");
                tooltip.add(entityType.getDescription().copy().withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.translatable("item.bubbasbees.bee_jar.pollinated",
                        Component.translatable("item.bubbasbees.bee_jar." + (hasNectar ? "yes" : "no")))
                        .withStyle(ChatFormatting.GOLD));
            }
        }
    }
}
