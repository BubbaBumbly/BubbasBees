package net.bubba.bubbasbees.mixin;

import net.bubba.bubbasbees.blockentities.AdvancedBeehiveBlockEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Bee.BeeEnterHiveGoal.class)
public abstract class BeeEnterHiveGoalMixin {

    @Final
    @Mutable
    @Shadow(aliases = {"field_20367", "f_27970_"})
    private Bee this$0;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/animal/Bee;)V", at = @At(value = "RETURN"))
    private void init(Bee beeEntity, CallbackInfo ci) {
        this.this$0 = beeEntity;
    }

    @Inject(method = "canBeeUse", at = @At(value = "INVOKE_ASSIGN", target =
            "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)" +
                    "Lnet/minecraft/world/level/block/entity/BlockEntity;"), cancellable = true)
    public void canBeeUse(CallbackInfoReturnable<Boolean> callback) {
        //BlockEntity blockentity = Bee.this.level.getBlockEntity(Bee.this.hivePos);
        BlockEntity blockEntity = this$0.level.getBlockEntity(this$0.getHivePos());
        if ((blockEntity instanceof AdvancedBeehiveBlockEntity advanced_hive)) {
            if (!advanced_hive.isFull()) {
                System.out.println("canBeeUseInvoke --> inside advanced_hive block!");
                callback.setReturnValue(true);
            } else {
                ((BeeEntityAccessor) this$0).setHivePos(null);
            }
        }
    }
    @Inject(method = "start", at = @At(value = "TAIL"))
    public void onStart(CallbackInfo ci) {
        BlockEntity blockEntity = this$0.level.getBlockEntity(this$0.getHivePos());
        if (blockEntity instanceof AdvancedBeehiveBlockEntity holder) {

        }
    }
}
