package net.bubba.bubbasbees.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;

public class ListUtils {
    public static boolean withinBlockList(ArrayList<Block> blockList, Block block) {
        for (Block eachBlock : blockList){
            if (eachBlock == block){
                return true;
            }
        }
        return false;
    }
    public static Block withinBlockListReturnBlock(ArrayList<Block> blockList, Block block){
        for (Block eachBlock : blockList){
            if (eachBlock == block){
                return block;
            }
        }
        return Blocks.STONE;
    }
    public static boolean withinCombList(ArrayList<Item> itemList, Item item) {
        for (Item eachItem : itemList){
            if (eachItem == item){
                return true;
            }
        }
        return false;
    }
}
