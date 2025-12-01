package net.bubba.bubbasbees.entity.custom;

import net.bubba.bubbasbees.registries.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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


public class BubbasBee extends Bee implements IAnimatable {

    public ArrayList<Block> validFlowerList;
    public ItemStack validHoneycombOutput;

    public BubbasBee(EntityType<? extends Bee> bubbasBeeEntityType, Level level) {
        super(bubbasBeeEntityType, level);
    }

    public @NotNull EntityType<? extends Bee> getType(){
        return ModEntityTypes.BUBBAS_BEE.get();
    }

    public ItemStack getHoneycombOutput (){
        return this.validHoneycombOutput;
    }
    ///Start Geckolib
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
    ///End Geckolib
    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }
    public static AttributeSupplier setAttributes() {
        return BubbasBee.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.FLYING_SPEED, (double)0.6F)
                .add(Attributes.MOVEMENT_SPEED, (double)0.3F)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D).build();
    }

    public boolean isFood(ItemStack p_27895_) {
        return p_27895_.is(ItemTags.ANVIL);
    }

    @Override
    public boolean isFlowerValid(@NotNull BlockPos blockPos) {
        boolean output = this.level.getBlockState(blockPos).is(Blocks.COPPER_BLOCK);
        System.out.println("isFlowerValidInvoke --> within BubbasBee Class --> " + "iscopper?: " + output);
        return this.level.getBlockState(blockPos).is(Blocks.COPPER_BLOCK);
    }
    ///New DataStruct for BubbasBee
    /*public static class ModBeeData {
        public ArrayList<Item> honeycombItems;
        public ArrayList<Block> validFlowerBlocks;

        public ModBeeData(ArrayList<Item> items, ArrayList<Block> blocks) {
            this.honeycombItems = new ArrayList<Item>();
            honeycombItems.addAll(items);
            this.validFlowerBlocks = new ArrayList<Block>();
            validFlowerBlocks.addAll(blocks);
        }
    }*/
}