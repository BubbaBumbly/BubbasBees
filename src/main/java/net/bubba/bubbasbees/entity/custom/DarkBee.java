package net.bubba.bubbasbees.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.mojang.math.Vector3f;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

import java.util.EnumSet;

/**
 * Night-ritual pest (finish-spec.md "Dark bee"): flies to a lit candle, snuffs it and vanishes in a puff. One hit
 * kills it; royal guards attack it on sight. Not a real bee (no hive, no comb), never saved with the world.
 * The ritual (step 5) picks its candle with setCandle; on its own it goes for the nearest lit candle.
 */
public class DarkBee extends PathfinderMob implements FlyingAnimal, IAnimatable {
    private static final int SEARCH_RADIUS = 12;
    private static final int GIVE_UP_TICKS = 200;
    private static final int MAX_LIFE_TICKS = 1200;
    private static final DustParticleOptions TRAIL = new DustParticleOptions(new Vector3f(0.45F, 0.2F, 0.65F), 0.7F);
    private @Nullable BlockPos candle;
    private int ticksWithoutCandle;

    public DarkBee(EntityType<? extends DarkBee> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.xpReward = 0;
    }

    public static AttributeSupplier setAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.FLYING_SPEED, 0.5D)      // bees: 0.6
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 24.0D).build();
    }

    public void setCandle(@Nullable BlockPos candle) {
        this.candle = candle == null ? null : candle.immutable();
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(false);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SnuffCandleGoal());
    }

    @Override
    public boolean isFlying() {
        return !this.onGround;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, @NotNull DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, @NotNull BlockState state, @NotNull BlockPos pos) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level.isClientSide) {
            if (this.tickCount % 2 == 0) {
                this.level.addParticle(TRAIL, this.getX(), this.getY() + 0.3D, this.getZ(), 0.0D, 0.0D, 0.0D);
            }
        } else if (this.tickCount > MAX_LIFE_TICKS) {
            poof();
        }
    }

    /** Vanishes with a quick puff of smoke. */
    public void poof() {
        if (this.level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.3D, getZ(), 8, 0.2D, 0.2D, 0.2D, 0.02D);
            server.sendParticles(ParticleTypes.WITCH, getX(), getY() + 0.3D, getZ(), 6, 0.2D, 0.2D, 0.2D, 0.0D);
        }
        this.playSound(SoundEvents.FIRE_EXTINGUISH, 0.4F, 1.6F);
        this.discard();
    }

    private static boolean isLitCandle(BlockState state) {
        return state.getBlock() instanceof AbstractCandleBlock && state.getValue(AbstractCandleBlock.LIT);
    }

    private @Nullable BlockPos findCandle() {
        BlockPos origin = this.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-SEARCH_RADIUS, -4, -SEARCH_RADIUS),
                origin.offset(SEARCH_RADIUS, 4, SEARCH_RADIUS))) {
            if (!isLitCandle(this.level.getBlockState(pos))) continue;
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }
        return best;
    }

    private class SnuffCandleGoal extends Goal {
        SnuffCandleGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (candle == null || !isLitCandle(level.getBlockState(candle))) {
                candle = null;
                if (tickCount % 20 == 0) candle = findCandle();
                if (candle == null) {
                    if (++ticksWithoutCandle > GIVE_UP_TICKS) poof();
                    return;
                }
            }
            ticksWithoutCandle = 0;
            Vec3 target = Vec3.atBottomCenterOf(candle).add(0.0D, 0.6D, 0.0D);
            getLookControl().setLookAt(target.x, target.y, target.z);
            if (position().distanceToSqr(target) < 0.8D) {
                BlockState state = level.getBlockState(candle);
                AbstractCandleBlock.extinguish(null, state, level, candle);
                poof();
                return;
            }
            getMoveControl().setWantedPosition(target.x, target.y, target.z, 1.0D);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS;   // the whisper
    }

    @Override
    public int getAmbientSoundInterval() {
        return 60;
    }

    @Override
    protected float getSoundVolume() {
        return 0.35F;
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.BEE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BEE_DEATH;
    }

    private final AnimationFactory factory = GeckoLibUtil.createFactory(this);

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "dark_bee_controller", 0, event -> {
            event.getController().setAnimation(new AnimationBuilder()
                    .addAnimation("animation.bee.fly", ILoopType.EDefaultLoopTypes.LOOP)
                    .addAnimation("animation.bee.fly.bobbing", ILoopType.EDefaultLoopTypes.LOOP));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }
}
