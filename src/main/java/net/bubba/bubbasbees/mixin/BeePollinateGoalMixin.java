package net.bubba.bubbasbees.mixin;


import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.bubba.bubbasbees.entity.custom.CopperBee;
import net.bubba.bubbasbees.util.ListUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(Bee.BeePollinateGoal.class)
public class BeePollinateGoalMixin {
    @Final
    @Mutable
    @Shadow(aliases = {"field_20367", "f_27970_"})
    private Bee this$0;

    @Shadow
    public Predicate<BlockState> VALID_POLLINATION_BLOCKS;

    @Shadow
    public Vec3 hoverPos;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/animal/Bee;)V", at = @At(value = "RETURN"))
    private void init(Bee beeEntity, CallbackInfo ci) {
        this.this$0 = beeEntity;
        bubbasBees$BeePollinateGoalMixinActivate();
    }

    @Unique
    public void bubbasBees$BeePollinateGoalMixinActivate() {
        if (this.this$0 instanceof CopperBee bubbasBee){
            this.VALID_POLLINATION_BLOCKS = (state) -> {
                if (state.hasProperty(BlockStateProperties.WATERLOGGED) && (Boolean) state.getValue(BlockStateProperties.WATERLOGGED)) {
                    return false;
                } else if (state.is(ListUtils.withinBlockListReturnBlock(bubbasBee.validFlowerList, state.getBlock()))) {
                    return true;
                } else {
                    return false;
                }
            };
        }
    }
    @Inject(method = "stop", at = @At("HEAD"))
    private void stop (CallbackInfo callback){
        if (this$0 instanceof CopperBee bee) {
            bubbasBees$CopperAgingMech(bee);
        }
    }
    @Inject(method = "setWantedPos", at = @At("TAIL"))
    public void setWantedPos (CallbackInfo callbackInfo) {
        if (this$0 instanceof BubbasBee bee) {
            bee.getMoveControl().setWantedPosition(hoverPos.x(), hoverPos.y() + 0.5, hoverPos.z(), 0.3499999940395355);
        }
    }
    @Unique
    public void bubbasBees$CopperAgingMech(BubbasBee bee) {
        BlockPos blockpos = bee.blockPosition().below(1);
        BlockState blockstate = bee.level.getBlockState(blockpos);
        if (blockstate.is(Blocks.COPPER_BLOCK)){
            bee.level.setBlock(blockpos, Blocks.EXPOSED_COPPER.defaultBlockState(),2);
        } else if (blockstate.is(Blocks.EXPOSED_COPPER)) {
            bee.level.setBlock(blockpos, Blocks.WEATHERED_COPPER.defaultBlockState(),2);
        } else if (blockstate.is(Blocks.WEATHERED_COPPER)) {
            bee.level.setBlock(blockpos, Blocks.OXIDIZED_COPPER.defaultBlockState(),2);
        }
    }
}
