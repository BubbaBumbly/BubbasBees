package net.bubba.bubbasbees.blockentities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AbstractAdvancedBeehiveBlockEntity extends BeehiveBlockEntity {

    public AbstractAdvancedBeehiveBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    /*
    List of shit to accomplish
    1. Make sure bees get mad and their angered behavior is correct ///DONE mostly
    2. Smoker item and figure out a way to make that happen ///DONE
    3. Other Bubbasbees (stone[general], plant, end, iron, gold, redstone)
    3-1. Create different items to convert different bees
    4. Make certain dispenser logic is functional
    5. Create a guide for the mod, maybe use Patchouli?
    6. Create bee jar item

    recipe idea for bubba's star: --> staged recipe? echo shard x4, nether star x5, netherite x1,
    TODO: 12/19/2025 --> Create all new bee types, items and json shit

     */
}