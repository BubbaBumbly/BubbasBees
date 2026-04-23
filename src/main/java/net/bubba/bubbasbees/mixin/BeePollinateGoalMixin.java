package net.bubba.bubbasbees.mixin;


import net.bubba.bubbasbees.entity.custom.BubbasBee;
import net.bubba.bubbasbees.entity.custom.CopperBee;
import net.bubba.bubbasbees.util.ListUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
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
        if (this.this$0 instanceof BubbasBee bubbasBee){
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
        BlockPos particles_pos = blockpos.above(1);
        double xpos = particles_pos.getX();
        double ypos = particles_pos.getY();
        double zpos = particles_pos.getZ();
        if (blockstate.is(Blocks.COPPER_BLOCK)){
            bee.level.setBlockAndUpdate(blockpos, Blocks.EXPOSED_COPPER.defaultBlockState());
            if (bee.level instanceof ServerLevel serverLevel){
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, xpos + 0.5, ypos + 0.2, zpos + 0.5,
                        5, 0.2, 0.2, 0.2, 0);
            }
        } else if (blockstate.is(Blocks.EXPOSED_COPPER)) {
            bee.level.setBlockAndUpdate(blockpos, Blocks.WEATHERED_COPPER.defaultBlockState());
            if (bee.level instanceof ServerLevel serverLevel){
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, xpos + 0.5, ypos + 0.2, zpos + 0.5,
                        5, 0.2, 0.2, 0.2, 0);
            }
        } else if (blockstate.is(Blocks.WEATHERED_COPPER)) {
            bee.level.setBlockAndUpdate(blockpos, Blocks.OXIDIZED_COPPER.defaultBlockState());
            if (bee.level instanceof ServerLevel serverLevel){
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, xpos + 0.5, ypos + 0.2, zpos + 0.5,
                        5, 0.2, 0.2, 0.2, 0);
            }
        }
    }
}
