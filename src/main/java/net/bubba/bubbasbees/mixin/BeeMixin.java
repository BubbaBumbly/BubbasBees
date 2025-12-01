package net.bubba.bubbasbees.mixin;

import net.bubba.bubbasbees.blockentities.AdvancedBeehiveBlockEntity;
import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.bubba.bubbasbees.registries.ModEntityTypes;
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

    /// Bee conversion functions below (use nether star to convert Bee to BubbasBee)
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
            if (this.getType().is(EntityTypeTags.BEEHIVE_INHABITORS)) {
                System.out.println("I am beehive inhabitor");

            } else {
                System.out.println("Somehow I am not beehive inhabitor");
            }
        }
        if (itemstack.is(Items.NETHER_STAR) && this.getType() != ModEntityTypes.BUBBAS_BEE.get()) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
            if (!this.level.isClientSide) {
                this.bubbasBees$beeConversion(player);
            }

            return InteractionResult.SUCCESS;
        } else {
            return super.mobInteract(player, hand);
        }
    }
    @Unique
    public void bubbasBees$beeConversion(Player player) {
        if (!this.level.isClientSide) {
            Bee newbie = new BubbasBee(ModEntityTypes.BUBBAS_BEE.get(), this.level);
            newbie.moveTo(this.getX(), this.getY(), this.getZ(),
                    this.random.nextFloat() * 180.0F, this.random.nextFloat()* 180.F);
            this.level.addFreshEntity(newbie);
            this.remove(RemovalReason.DISCARDED);
            System.out.println("ConversionInvoke!");
        }
        else {
            player.sendSystemMessage(Component.literal("REEE"));
        }
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
        if (this.getType() != ModEntityTypes.BUBBAS_BEE.get()) {
            this.goalSelector.addGoal(3, new TemptGoal(
                    this, 1.25D, Ingredient.of(Items.NETHER_STAR), false));
        }
    }
    /// End goal editing section
    /// Goal Appropriation section

    /// End Goal Appropriation section
}
