package net.bubba.bubbasbees.mixin;

import net.bubba.bubbasbees.blockentities.AdvancedBeehiveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(BeehiveBlock.class)
public abstract class BeehiveBlockMixin extends BaseEntityBlock {
    @Shadow @Final public static IntegerProperty HONEY_LEVEL;

    protected BeehiveBlockMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = "use", at = @At(value = "HEAD"), cancellable = true)
    public void useMixin(BlockState state, Level level, BlockPos pos, Player player,
                         InteractionHand hand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack held_Item = player.getItemInHand(hand);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BeehiveBlockEntity beehiveBlockEntity &&
                !(blockEntity instanceof AdvancedBeehiveBlockEntity) &&
                        held_Item.is(Items.STICK) && !level.isClientSide()) {
            boolean hiveEmpty = beehiveBlockEntity.isEmpty();
            boolean hasSpace = beehiveBlockEntity.isFull();
            int occupantCount = beehiveBlockEntity.getOccupantCount();
            int honeyLevel = state.getValue(HONEY_LEVEL);
            List<BeehiveBlockEntity.BeeData> beesInHive = ((BeehiveEntityAccessor) beehiveBlockEntity).getStored();
            System.out.println("This is a: " + blockEntity.toString());
            System.out.println("Hive Empty?: " + String.valueOf(hiveEmpty));
            System.out.println("Has Space?: " + String.valueOf(!hasSpace));
            System.out.println("Occupant Count: " + occupantCount);
            System.out.println("Honey Level: " + honeyLevel);
            System.out.println();
            cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
            if (!beesInHive.isEmpty()) {
                for (BeehiveBlockEntity.BeeData bee : beesInHive){
                    System.out.println("Bee: " + bee);
                    System.out.println("BeeTicksInHive: " + ((BeehiveBeeDataAccessor) bee).getTicksInHive());
                    System.out.println("BeeMinOccTicks: " + ((BeehiveBeeDataAccessor) bee).getMinOccupationTicks());
                }
            } else {
                System.out.println("No bees STORED in Hive :(");
            }
        }
        if (blockEntity instanceof AdvancedBeehiveBlockEntity advancedBeehiveBlockEntity &&
                held_Item.is(Items.STICK) && !level.isClientSide()) {
            boolean hiveEmpty = advancedBeehiveBlockEntity.isEmpty();
            boolean hasSpace = advancedBeehiveBlockEntity.isFull();
            int occupantCount = advancedBeehiveBlockEntity.getOccupantCount();
            int honeyLevel = state.getValue(HONEY_LEVEL);
            List<BeehiveBlockEntity.BeeData> beeList = advancedBeehiveBlockEntity.getBees();
            System.out.println("This is a: " + blockEntity.toString());
            System.out.println("Hive Empty?: " + String.valueOf(hiveEmpty));
            System.out.println("Has Space?: " + String.valueOf(hasSpace));
            System.out.println("Occupant Count: " + occupantCount);
            System.out.println("Honey Level: " + honeyLevel);
            if (!advancedBeehiveBlockEntity.getHoneycombList().isEmpty()){
                System.out.println(advancedBeehiveBlockEntity.getHoneycombList());
            }
            if (!beeList.isEmpty()) {
                System.out.println("Bees STORED in hive: ");
                for (BeehiveBlockEntity.BeeData bee : beeList) {
                    System.out.print(String.valueOf(bee) + ", ");
                }
                System.out.print("\n");
            } else {
                System.out.println("No bees STORED in Hive :(");
            }
            cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
        }
    }
}
        /*int i = state.getValue(HONEY_LEVEL);
        String isBlocktagBeehive = String.valueOf(state.is(BlockTags.BEEHIVES));
        if (!level.isClientSide){
            //.out.println("Honey Level: " + i);
           // System.out.println("Handheld Item: " + itemstack.toString());
           // System.out.println("Has Beehive BlockTag: " + isBlocktagBeehive);

        }
    } */
