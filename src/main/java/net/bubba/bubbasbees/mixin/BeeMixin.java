package net.bubba.bubbasbees.mixin;

import net.bubba.bubbasbees.bees.BeeSpec;
import net.bubba.bubbasbees.blockentities.AdvancedBeehiveBlockEntity;
import net.bubba.bubbasbees.entity.custom.*;
import net.bubba.bubbasbees.registries.ModEntityTypes;
import net.bubba.bubbasbees.registries.ModItems;
import net.bubba.bubbasbees.ritual.RitualManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @Shadow @Nullable public abstract BlockPos getSavedFlowerPos();

    @Unique
    public void bubbasBees$setHivePos(BlockPos hivePos) {
        this.hivePos = hivePos;
    }

    /// Bee conversion functions below (use conversion gems to convert Bee to BubbasBee)
    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        BeeSpec gemSpec = ModItems.specForGem(itemstack);
        if (gemSpec != null && this.getType() == EntityType.BEE) {
            if (!this.level.isClientSide) {
                Bee newbie = ModEntityTypes.bee(gemSpec.id()).get().create(this.level);
                if (newbie != null) {
                    bubbasBees$conversionParticles(itemstack);
                    bubbasBees$spawnThat(newbie);
                }
            }
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            return InteractionResult.SUCCESS;
        } else {
            return super.mobInteract(player, hand);
        }
    }
    /// One burst of the gem's own crumbs plus a small sparkle, so every bee's conversion matches its gem
    @Unique
    private void bubbasBees$conversionParticles(ItemStack gem) {
        if (!(this.level instanceof net.minecraft.server.level.ServerLevel server)) return;
        double x = this.getX(), y = this.getY() + 0.3D, z = this.getZ();
        server.sendParticles(new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM, gem),
                x, y, z, 24, 0.2D, 0.2D, 0.2D, 0.08D);
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, x, y, z, 6, 0.25D, 0.25D, 0.25D, 0.02D);
    }

    @Unique
    public void bubbasBees$spawnThat (Bee newbie){
        BlockPos oldHive = this.getHivePos();
        newbie.moveTo(this.getX(), this.getY(), this.getZ(),
                this.random.nextFloat() * 180.0F, this.random.nextFloat()* 180.F);
        if (oldHive != null) {
            ((BeeEntityAccessor)newbie).setHivePos(oldHive);
        }
        if (newbie instanceof BubbasBee bubba) {
            if (oldHive != null) {
                bubba.setHomeAnchorPos(oldHive);
            }
        }
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
    /// Work rules per bee (BeeSpec.canWorkAt): a mod bee heads home when its own weather/time rule says stop
    @Redirect(method = "wantsToEnterHive",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;isRaining()Z"))
    private boolean bubbasBees$stopsForWeather(Level level) {
        if ((Object) this instanceof BubbasBee bee && bee.getSpec() != null) return !bee.canWorkNow();
        return level.isRaining();
    }

    @Redirect(method = "wantsToEnterHive",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;isNight()Z"))
    private boolean bubbasBees$stopsForNight(Level level) {
        if ((Object) this instanceof BubbasBee bee && bee.getSpec() != null) return false;   // covered above
        // vanilla bees work through the night while their hive is in a running ritual circle
        return level.isNight() && !RitualManager.isRitualHive(level, this.hivePos);
    }

    /// A vanilla bee finished a flower visit: vanilla bees are a kind of their own in the Celestial Ritual.
    /// tickCount is 0 while a bee is being read from saved data, which also sets the nectar flag.
    @Inject(method = "setHasNectar", at = @At("HEAD"))
    private void bubbasBees$vanillaPollinated(boolean hasNectar, CallbackInfo ci) {
        if (hasNectar && !this.hasNectar() && this.tickCount > 0 && this.getType() == EntityType.BEE) {
            RitualManager.onPollinated((Bee) (Object) this, "vanilla", this.getSavedFlowerPos());
        }
    }

    /// Goal editing section
    @Inject(method = "registerGoals", at = @At("HEAD"))
    protected void registerGoalsMixin (CallbackInfo ci) {
        if (this.getType() == EntityType.BEE) {
            this.goalSelector.addGoal(3, new TemptGoal(this, 1.25D, Ingredient.of(ModItems.allGems()), false));
        }
    }
    /// End goal editing section

    // Royal guards sting with their own damage source, for their death messages
    @ModifyArg(method = "doHurtTarget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), index = 0)
    private DamageSource bubbasBees$guardSting(DamageSource source) {
        return (Object) this instanceof BubbasBee bee && bee.isRoyalGuard() ? new RoyalGuardDamageSource(bee) : source;
    }
}
