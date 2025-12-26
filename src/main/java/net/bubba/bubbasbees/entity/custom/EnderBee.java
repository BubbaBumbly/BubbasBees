package net.bubba.bubbasbees.entity.custom;

import net.bubba.bubbasbees.registries.ModEntityTypes;
import net.bubba.bubbasbees.registries.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

public class EnderBee extends BubbasBee {
    public EnderBee(EntityType<? extends Bee> bubbasBeeEntityType, Level level) {
        super(bubbasBeeEntityType, level);
        this.validFlowerList = new ArrayList<>(List.of(Blocks.END_STONE));
        this.validHoneycombOutput = (ModItems.ENDER_HONEYCOMB.get().getDefaultInstance());
        this.variant = "ender";
        //this.conversionItem = ModItems.
    }
    public @NotNull EntityType<? extends Bee> getType(){
        return ModEntityTypes.ENDER_BEE.get();
    }
    ///Geckolib below
    private final AnimationFactory factory = GeckoLibUtil.createFactory(this);

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        if (event.isMoving()) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.bubbasbee.moving"));
            return PlayState.CONTINUE;
        }

        event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.bubbasbee.idle"));
        return PlayState.CONTINUE;
    }
    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "bee_controller", 0, event -> {
            event.getController().setAnimation(new AnimationBuilder()
                    .addAnimation("animation.bee.fly", ILoopType.EDefaultLoopTypes.LOOP)
                    .addAnimation("animation.bee.fly.bobbing", ILoopType.EDefaultLoopTypes.LOOP)
            );
            return PlayState.CONTINUE;
        }));
    }
    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }
    ///End Geckolib
    public static AttributeSupplier setAttributes() {
        return CopperBee.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.FLYING_SPEED, (double)0.6F)
                .add(Attributes.MOVEMENT_SPEED, (double)0.3F)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D).build();
    }
}
