package net.bubba.bubbasbees.mixin;

import net.bubba.bubbasbees.blockentities.AdvancedBeehiveBlockEntity;
import net.bubba.bubbasbees.entity.custom.*;
import net.bubba.bubbasbees.registries.ModEntityTypes;
import net.bubba.bubbasbees.registries.ModItems;
import net.bubba.bubbasbees.util.ListUtils;
import net.bubba.bubbasbees.util.ModConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;


@Mixin(Bee.class)
public abstract class BeeMixin extends Animal {

    protected BeeMixin(EntityType<? extends Bee> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow
    BlockPos hivePos;

    @Shadow
    public boolean hasHive() {
        return this.hivePos != null;
    }

    @Shadow
    public abstract boolean hasNectar();

    @Shadow
    public abstract void dropOffNectar();

    @Shadow
    public abstract void setStayOutOfHiveCountdown(int stayOutOfHiveCountdown);

    @Shadow @Final public static String TAG_CANNOT_ENTER_HIVE_TICKS;

    @Shadow @Nullable public abstract BlockPos getHivePos();

    @Unique
    public void bubbasBees$setHivePos(BlockPos hivePos) {
        this.hivePos = hivePos;
    }

    /// Bee conversion functions below (use conversion gems to convert Bee to BubbasBee)
    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (itemstack.is(Items.STICK)){
            BlockPos hivePos = this.getHivePos();
            String typeStr = this.getType().toString();
            System.out.println("Type: " + typeStr);
            if (hivePos != null && !level.isClientSide){
                    System.out.println("HivePos: " + hivePos.toString());
                } else if (!level.isClientSide){
                    System.out.println("No HivePos");
                }
        }
        if (ListUtils.withinItemList(ModConstants.CONVERSION_ITEMS, itemstack.getItem()) && this.getType() == EntityType.BEE) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
            if (!this.level.isClientSide) {
                this.bubbasBees$beeConversion(player, itemstack);
            }

            return InteractionResult.SUCCESS;
        } else {
            return super.mobInteract(player, hand);
        }
    }
    @Unique
    public void bubbasBees$beeConversion(Player player, ItemStack itemStack) {
        if (!this.level.isClientSide) {
            if (itemStack.is(ModItems.COPPER_CONVERSION_GEM.get())){
                Bee newbie = new CopperBee(ModEntityTypes.COPPER_BEE.get(), this.level);
                bubbasBees$spawnThat(newbie);
            } else if (itemStack.is(ModItems.SANDY_CONVERSION_GEM.get())){
                Bee newbie = new SandyBee(ModEntityTypes.SANDY_BEE.get(), this.level);
                bubbasBees$spawnThat(newbie);
            } else if (itemStack.is(ModItems.ENDER_CONVERSION_GEM.get())){
                Bee newbie = new EnderBee(ModEntityTypes.ENDER_BEE.get(), this.level);
                bubbasBees$spawnThat(newbie);
            } else if (itemStack.is(ModItems.GOLD_CONVERSION_GEM.get())){
                Bee newbie = new GoldBee(ModEntityTypes.GOLD_BEE.get(), this.level);
                bubbasBees$spawnThat(newbie);
            } else if (itemStack.is(ModItems.IRON_CONVERSION_GEM.get())){
                Bee newbie = new IronBee(ModEntityTypes.IRON_BEE.get(), this.level);
                bubbasBees$spawnThat(newbie);
            } else if (itemStack.is(ModItems.NATURE_CONVERSION_GEM.get())){
                Bee newbie = new NatureBee(ModEntityTypes.NATURE_BEE.get(), this.level);
                bubbasBees$spawnThat(newbie);
            } else if (itemStack.is(ModItems.REDSTONE_CONVERSION_GEM.get())){
                Bee newbie = new RedstoneBee(ModEntityTypes.REDSTONE_BEE.get(), this.level);
                bubbasBees$spawnThat(newbie);
            } else if (itemStack.is(ModItems.STONE_CONVERSION_GEM.get())){
                Bee newbie = new StoneBee(ModEntityTypes.STONE_BEE.get(), this.level);
                bubbasBees$spawnThat(newbie);
            }
        }
    }
    @Unique
    public void bubbasBees$spawnThat (Bee newbie){
        newbie.moveTo(this.getX(), this.getY(), this.getZ(),
                this.random.nextFloat() * 180.0F, this.random.nextFloat()* 180.F);
        this.level.addFreshEntity(newbie);
        this.remove(RemovalReason.DISCARDED);
    }

    @Inject(at = @At("HEAD"), method = "doesHiveHaveSpace", cancellable = true)
    private void doesHiveHaveSpace(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        BlockEntity blockEntity = this.level.getBlockEntity(pos);
        if ((blockEntity instanceof AdvancedBeehiveBlockEntity advanced_hive && !advanced_hive.isFull())) {
            callback.setReturnValue(true);
        }
    }
    /// Checking hive validity
    @Inject(at = @At("HEAD"), method = "isHiveValid", cancellable = true)
    public void isHiveValid(CallbackInfoReturnable<Boolean> callback) {
        if (this.hasHive()) {
            BlockPos pos = this.hivePos;
            if (pos != null) {
                BlockEntity blockEntity = this.level.getBlockEntity(this.hivePos);
                if ((blockEntity instanceof AdvancedBeehiveBlockEntity advanced_hive && advanced_hive.isAllowedBee())) {
                    callback.setReturnValue(true);
                }
            }
        }
    }
    /// Goal editing section
    @Inject(method = "registerGoals", at = @At("HEAD"))
    protected void registerGoalsMixin (CallbackInfo ci) {
        if (this.getType() == EntityType.BEE) {
            this.goalSelector.addGoal(3, new TemptGoal(
                    this, 1.25D, Ingredient.of(ModItems.COPPER_CONVERSION_GEM.get()), false));
            this.goalSelector.addGoal(3, new TemptGoal(
                    this, 1.25D, Ingredient.of(ModItems.SANDY_CONVERSION_GEM.get()), false));
            this.goalSelector.addGoal(3, new TemptGoal(
                    this, 1.25D, Ingredient.of(ModItems.ENDER_CONVERSION_GEM.get()), false));
            this.goalSelector.addGoal(3, new TemptGoal(
                    this, 1.25D, Ingredient.of(ModItems.GOLD_CONVERSION_GEM.get()), false));
            this.goalSelector.addGoal(3, new TemptGoal(
                    this, 1.25D, Ingredient.of(ModItems.IRON_CONVERSION_GEM.get()), false));
            this.goalSelector.addGoal(3, new TemptGoal(
                    this, 1.25D, Ingredient.of(ModItems.NATURE_CONVERSION_GEM.get()), false));
            this.goalSelector.addGoal(3, new TemptGoal(
                    this, 1.25D, Ingredient.of(ModItems.REDSTONE_CONVERSION_GEM.get()), false));
            this.goalSelector.addGoal(3, new TemptGoal(
                    this, 1.25D, Ingredient.of(ModItems.STONE_CONVERSION_GEM.get()), false));
        }
    }
    /// End goal editing section
}
