package net.bubba.bubbasbees.entity.custom;

import net.bubba.bubbasbees.entity.goals.BeeAnchorHomingGoal;
import net.bubba.bubbasbees.entity.goals.ModBeePollinateGoal;
import net.bubba.bubbasbees.mixin.BeeEntityAccessor;
import net.bubba.bubbasbees.registries.ModEntityTypes;
import net.bubba.bubbasbees.registries.ModItems;
import net.bubba.bubbasbees.util.ListUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    public String variant = "none";
    public Item conversionItem;
    public BlockPos homeAnchorPos;

    // Ore bees come in a stone and a deepslate look; each bee rolls its own when it's created and
    // keeps it in its save data (through hives and reloads). Synced so the client picks the texture.
    private static final EntityDataAccessor<Boolean> DEEPSLATE =
            SynchedEntityData.defineId(BubbasBee.class, EntityDataSerializers.BOOLEAN);

    public BubbasBee(EntityType<? extends Bee> bubbasBeeEntityType, Level level) {
        super(bubbasBeeEntityType, level);
        if (!level.isClientSide) {
            setDeepslate(this.random.nextBoolean());
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DEEPSLATE, false);
    }

    public boolean isDeepslate() {
        return this.entityData.get(DEEPSLATE);
    }

    public void setDeepslate(boolean deepslate) {
        this.entityData.set(DEEPSLATE, deepslate);
    }

    public BlockPos getHomeAnchorPos() {
        return homeAnchorPos;
    }

    public void setHomeAnchorPos(BlockPos pos) {
        this.homeAnchorPos = pos;
    }

    public @NotNull EntityType<? extends Bee> getType(){
        return ModEntityTypes.BUBBAS_BEE.get();
    }

    public String getVariant () {return this.variant;}

    public ItemStack getHoneycombOutput (){
        return this.validHoneycombOutput;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (itemstack.is(Items.STICK) && !this.level.isClientSide) {
            System.out.println("=== BubbasBee Debug: " + this.getName().getString() + " ===");
            System.out.println("  Variant: " + variant);
            System.out.println("  Tick: " + tickCount);
            System.out.println("  HasNectar: " + hasNectar());
            System.out.println("  SavedFlowerPos: " + (savedFlowerPos != null ? savedFlowerPos.toShortString() : "null"));
            System.out.println("  ValidFlowerList: " + (validFlowerList == null ? "null!" : validFlowerList.size() + " blocks"));
            System.out.println("  Has hive: " + hasHive());
            System.out.println("  HivePos: " + (getHivePos() != null ? getHivePos().toShortString() : "null"));
            System.out.println("  HomeAnchor: " + (homeAnchorPos != null ? homeAnchorPos.toShortString() : "null"));
            System.out.println("======================================");
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    // Spawn eggs and breeding produce a baby of this bee's own type instead of a vanilla bee
    @Override
    public Bee getBreedOffspring(@NotNull ServerLevel level, @NotNull AgeableMob other) {
        return this.getType().create(level);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (homeAnchorPos != null) {
            tag.put("BubbasHomeAnchor", NbtUtils.writeBlockPos(homeAnchorPos));
        }
        tag.putBoolean("BubbasDeepslate", isDeepslate());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("BubbasHomeAnchor")) {
            homeAnchorPos = NbtUtils.readBlockPos(tag.getCompound("BubbasHomeAnchor"));
        } else {
            BlockPos hive = this.getHivePos();
            homeAnchorPos = hive != null ? hive : this.blockPosition();
        }
        if (tag.contains("BubbasDeepslate")) {
            setDeepslate(tag.getBoolean("BubbasDeepslate"));
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(3, new BeeAnchorHomingGoal(this));
        Bee.BeePollinateGoal oldGoal = ((BeeEntityAccessor)this).getBeePollinateGoal();
        if (oldGoal != null) {
            this.goalSelector.removeGoal(oldGoal);
            ModBeePollinateGoal modGoal = new ModBeePollinateGoal(this);
            this.goalSelector.addGoal(4, modGoal);
            ((BeeEntityAccessor)this).setBeePollinateGoal(modGoal);
        }
    }

    // Vanilla's bee navigation pauses for the whole pollinate goal because its hover drives the move control
    // directly. ModBeePollinateGoal also approaches from farther away and needs the pathfinder when a wall blocks
    // the straight line, so pause only while it is hovering over the flower
    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level) {
            @Override
            public boolean isStableDestination(@NotNull BlockPos pos) {
                return !this.level.getBlockState(pos.below()).isAir();
            }

            @Override
            public void tick() {
                Bee.BeePollinateGoal goal = ((BeeEntityAccessor) BubbasBee.this).getBeePollinateGoal();
                boolean paused = goal instanceof ModBeePollinateGoal modGoal
                        ? modGoal.isHoveringAtFlower()
                        : goal != null && goal.isPollinating();
                if (!paused) {
                    super.tick();
                }
            }
        };
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(false);
        navigation.setCanPassDoors(true);
        return navigation;
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
        return false;
    }

    @Override
    public boolean isFlowerValid(@NotNull BlockPos blockPos) {
        return this.level.isLoaded(blockPos) && ListUtils.withinBlockList(validFlowerList, this.level.getBlockState(blockPos).getBlock());
    }
}