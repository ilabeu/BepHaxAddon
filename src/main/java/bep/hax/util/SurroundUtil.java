package bep.hax.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.List;

/**
 * Utility for surround block detection
 */
public class SurroundUtil {

    /**
     * Gets surround positions around a player (excluding down position)
     */
    public static List<BlockPos> getSurroundNoDown(PlayerEntity player) {
        BlockPos playerPos = player.getBlockPos();

        return Arrays.asList(
            playerPos.north(),  // North
            playerPos.south(),  // South
            playerPos.east(),   // East
            playerPos.west()    // West
        );
    }

    /**
     * Gets all surround positions around a player (including down)
     */
    public static List<BlockPos> getSurround(PlayerEntity player) {
        BlockPos playerPos = player.getBlockPos();

        return Arrays.asList(
            playerPos.north(),  // North
            playerPos.south(),  // South
            playerPos.east(),   // East
            playerPos.west(),   // West
            playerPos.down()    // Down
        );
    }
}
