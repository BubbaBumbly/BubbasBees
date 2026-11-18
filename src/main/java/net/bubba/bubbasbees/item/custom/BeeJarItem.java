package net.bubba.bubbasbees.item.custom;

import net.bubba.bubbasbees.registries.ModDataComponents;
import net.bubba.bubbasbees.BubbasBees;
import net.bubba.bubbasbees.bees.BeeSpec;
import net.bubba.bubbasbees.bees.BeeSpecs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BeeJarItem extends Item {

    private static final String TAG_ENTITY_TYPE = "EntityType";

    public BeeJarItem(Properties properties) {
        super(properties);
    }

    public static boolean isFilled(ItemStack stack) {
        return stack.has(ModDataComponents.BEE_JAR_CONTENTS.get());
    }

    /** Copy of the stored bee's NBT, or null when the jar is empty. */
    @Nullable
    private static CompoundTag beeData(ItemStack stack) {
        CustomData data = stack.get(ModDataComponents.BEE_JAR_CONTENTS.get());
        return data == null ? null : data.copyTag();
    }

    public static float getFillState(ItemStack stack) {
        CompoundTag beeData = beeData(stack);
        if (beeData == null) return 0.0F;
        String entityTypeStr = beeData.getString(TAG_ENTITY_TYPE);
        if (entityTypeStr.isEmpty()) return 1.0F;

        // 2 + the bee's place in the roster; tools/gen_bee_data.py writes bee_jar.json's overrides in the same order
        ResourceLocation id = ResourceLocation.tryParse(entityTypeStr);
        if (id != null && id.equals(ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "sandy_bee"))) {
            id = ResourceLocation.fromNamespaceAndPath(BubbasBees.MOD_ID, "desert_bee");   // the desert bee's id before the rename
        }
        BeeSpec spec = BeeSpecs.byEntity(id);
        if (spec == null) return 1.0F;
        // Ore bees jarred in their deepslate look (BubbasBee's BubbasDeepslate roll, kept in the jar) get the next half step
        boolean deepslate = spec.deepslateLook() && beeData.getBoolean("BubbasDeepslate");
        return 2.0F + BeeSpecs.ALL.indexOf(spec) + (deepslate ? 0.5F : 0.0F);
    }

    private static void fill(ItemStack jar, CompoundTag beeData) {
        jar.set(ModDataComponents.BEE_JAR_CONTENTS.get(), CustomData.of(beeData));
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull LivingEntity target, @NotNull InteractionHand hand) {
        if (!isFilled(stack) && target instanceof Bee bee) {
            if (!player.level().isClientSide) {
                CompoundTag beeData = new CompoundTag();
                bee.saveWithoutId(beeData);
                beeData.putString(TAG_ENTITY_TYPE, EntityType.getKey(bee.getType()).toString());

                if (player.isCreative()) {
                    ItemStack filledJar = new ItemStack(this, 1);
                    fill(filledJar, beeData);
                    if (!player.addItem(filledJar)) {
                        player.drop(filledJar, false);
                    }
                } else if (stack.getCount() > 1) {
                    ItemStack filledJar = new ItemStack(this, 1);
                    fill(filledJar, beeData);
                    stack.shrink(1);
                    if (!player.addItem(filledJar)) {
                        player.drop(filledJar, false);
                    }
                } else {
                    fill(stack, beeData);
                }

                bee.remove(Entity.RemovalReason.DISCARDED);

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
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
                CompoundTag beeData = beeData(stack);
                String entityTypeStr = beeData.getString(TAG_ENTITY_TYPE);
                EntityType<?> entityType = EntityType.byString(entityTypeStr).orElse(null);

                if (entityType != null) {
                    Entity entity = entityType.create(level);
                    if (entity != null) {
                        beeData.remove("Pos");
                        beeData.remove("Motion");
                        beeData.remove("Rotation");
                        beeData.remove(Bee.TAG_HIVE_POS);
                        beeData.remove("BubbasHomeAnchor");
                        beeData.remove(Bee.TAG_FLOWER_POS);
                        entity.load(beeData);
                        entity.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);

                        if (entity instanceof Bee bee) {
                            bee.setStayOutOfHiveCountdown(400);
                        }

                        level.addFreshEntity(entity);

                        stack.remove(ModDataComponents.BEE_JAR_CONTENTS.get());

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

    @Nullable
    private static EntityType<?> storedType(ItemStack stack) {
        CompoundTag beeData = beeData(stack);
        if (beeData == null) return null;
        return EntityType.byString(beeData.getString(TAG_ENTITY_TYPE)).orElse(null);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        EntityType<?> entityType = storedType(stack);
        if (entityType != null) {
            if (EntityType.getKey(entityType).getNamespace().equals("minecraft")) {
                return Component.translatable("item.bubbasbees.bee_jar.filled");
            } else {
                return Component.translatable("item.bubbasbees.bee_jar.filled_custom", entityType.getDescription());
            }
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        CompoundTag beeData = beeData(stack);
        EntityType<?> entityType = storedType(stack);
        if (beeData != null && entityType != null) {
            boolean hasNectar = beeData.getBoolean(Bee.TAG_HAS_NECTAR);
            tooltip.add(entityType.getDescription().copy().withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("item.bubbasbees.bee_jar.pollinated",
                    Component.translatable("item.bubbasbees.bee_jar." + (hasNectar ? "yes" : "no")))
                    .withStyle(ChatFormatting.GOLD));
            // A baby's age is kept in the jar and does not run there (jars must show babies)
            int age = beeData.getInt("Age");
            if (age < 0) {
                int s = (-age + 19) / 20;
                tooltip.add(Component.translatable("item.bubbasbees.bee_jar.baby", String.format("%d:%02d", s / 60, s % 60))
                        .withStyle(ChatFormatting.GOLD));
            }
        }
    }
}
