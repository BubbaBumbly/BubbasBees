package net.bubba.bubbasbees.mixin;

import net.bubba.bubbasbees.blockentities.AdvancedBeehiveBlockEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Bee.BeeEnterHiveGoal.class)
public abstract class BeeEnterHiveGoalMixin {

    // FIX: the synthetic outer field this$0 is not visible under Fabric's mappings; keep our own reference to the bee
    @Unique
    private Bee bubbasBees$bee;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/animal/Bee;)V", at = @At(value = "RETURN"))
    private void init(Bee beeEntity, CallbackInfo ci) {
        this.bubbasBees$bee = beeEntity;
    }

    @Inject(method = "canBeeUse", at = @At(value = "INVOKE_ASSIGN", target =
            "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)" +
                    "Lnet/minecraft/world/level/block/entity/BlockEntity;"), cancellable = true)
    public void canBeeUse(CallbackInfoReturnable<Boolean> callback) {
        BlockEntity blockEntity = bubbasBees$bee.level().getBlockEntity(bubbasBees$bee.getHivePos());
        if ((blockEntity instanceof AdvancedBeehiveBlockEntity advanced_hive)) {
            if (!advanced_hive.isFull()) {
                callback.setReturnValue(true);
            } else {
                ((BeeEntityAccessor) bubbasBees$bee).setHivePos(null);
            }
        }
    }

}
