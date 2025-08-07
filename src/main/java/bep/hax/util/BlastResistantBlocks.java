package bep.hax.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Original PVP blast resistant blocks utility
 * Critical for 2b2t bedrock and unbreakable block detection
 */
public class BlastResistantBlocks {

    /**
     * Checks if a block is unbreakable (bedrock, barrier, etc.)
     * @param pos Block position to check
     * @return true if block is unbreakable
     */
    public static boolean isUnbreakable(BlockPos pos) {
        if (mc.world == null) return false;

        Block block = mc.world.getBlockState(pos).getBlock();
        return isUnbreakable(block);
    }

    /**
     * Checks if a block type is unbreakable
     * @param block Block to check
     * @return true if block is unbreakable
     */
    public static boolean isUnbreakable(Block block) {
        return block == Blocks.BEDROCK ||
               block == Blocks.BARRIER ||
               block == Blocks.COMMAND_BLOCK ||
               block == Blocks.CHAIN_COMMAND_BLOCK ||
               block == Blocks.REPEATING_COMMAND_BLOCK ||
               block == Blocks.STRUCTURE_BLOCK ||
               block == Blocks.JIGSAW ||
               block == Blocks.MOVING_PISTON ||
               block == Blocks.VOID_AIR;
    }

    /**
     * Checks if a block is blast resistant
     * @param pos Block position to check
     * @return true if block is blast resistant
     */
    public static boolean isBlastResistant(BlockPos pos) {
        if (mc.world == null) return false;

        Block block = mc.world.getBlockState(pos).getBlock();
        return isBlastResistant(block);
    }

    /**
     * Checks if a block type is blast resistant
     * @param block Block to check
     * @return true if block is blast resistant
     */
    public static boolean isBlastResistant(Block block) {
        return block == Blocks.OBSIDIAN ||
               block == Blocks.CRYING_OBSIDIAN ||
               block == Blocks.ANVIL ||
               block == Blocks.CHIPPED_ANVIL ||
               block == Blocks.DAMAGED_ANVIL ||
               block == Blocks.ENCHANTING_TABLE ||
               block == Blocks.ENDER_CHEST ||
               block == Blocks.RESPAWN_ANCHOR ||
               isUnbreakable(block);
    }
}
