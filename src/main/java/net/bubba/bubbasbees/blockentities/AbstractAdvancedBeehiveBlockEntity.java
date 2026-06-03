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
    1. Create a guide for the mod, maybe use Patchouli?
    2. Github duhhh
    3. recipe fixes
    4. quark recipes?
    5. Test bee AI, see if reducing explore timer to very small value stops exploration or if something else
    is in charge of the bees flying everywhere when they come out of the hive. also check the bee history lists to
    see if they are being populated.



     */
}