package bep.hax.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import bep.hax.util.BlastResistantBlocks;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Original PVP PositionUtil - exact implementation for 2b2t compatibility
 * Preserves all original math and functionality that Meteor Client lacks
 */
public class PositionUtil {

    /**
     * Creates an enclosing bounding box for a list of block positions
     * @param posList List of block positions
     * @return Bounding box that contains all positions
     */
    public static Box enclosingBox(List<BlockPos> posList) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (BlockPos blockPos : posList) {
            if (blockPos.getX() < minX) {
                minX = blockPos.getX();
            }
            if (blockPos.getY() < minY) {
                minY = blockPos.getY();
            }
            if (blockPos.getZ() < minZ) {
                minZ = blockPos.getZ();
            }
            if (blockPos.getX() > maxX) {
                maxX = blockPos.getX();
            }
            if (blockPos.getY() > maxY) {
                maxY = blockPos.getY();
            }
            if (blockPos.getZ() > maxZ) {
                maxZ = blockPos.getZ();
            }
        }

        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Gets a rounded BlockPos from double coordinates
     * Uses PVP's specific rounding logic for precision
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return Rounded BlockPos
     */
    public static BlockPos getRoundedBlockPos(final double x, final double y, final double z) {
        final int flooredX = MathHelper.floor(x);
        final int flooredY = (int) Math.round(y);
        final int flooredZ = MathHelper.floor(z);
        return new BlockPos(flooredX, flooredY, flooredZ);
    }

    /**
     * Checks if any position in the box relative to player position contains bedrock
     * @param box Bounding box to check
     * @param pos Player position
     * @return true if bedrock is found
     */
    public static boolean isBedrock(Box box, BlockPos pos) {
        return getAllInBox(box, pos).stream().anyMatch(BlastResistantBlocks::isUnbreakable);
    }

    /**
     * Returns a List of all BlockPos positions in the given Box that match the player position Y level
     * Critical for PVP's surround and combat calculations
     * @param box Bounding box
     * @param pos The player position
     * @return List of positions at player Y level
     */
    public static List<BlockPos> getAllInBox(Box box, BlockPos pos) {
        final List<BlockPos> intersections = new ArrayList<>();
        for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                intersections.add(new BlockPos(x, pos.getY(), z));
            }
        }
        return intersections;
    }

    /**
     * Returns a List of all BlockPos positions in the given Box for all Y levels
     * Used for comprehensive block scanning
     * @param box Bounding box
     * @return List of all positions in the box
     */
    public static List<BlockPos> getAllInBox(Box box) {
        final List<BlockPos> intersections = new ArrayList<>();
        for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                    intersections.add(new BlockPos(x, y, z));
                }
            }
        }
        return intersections;
    }

    /**
     * Checks if player is currently phasing through blocks
     * Using PVP's specific phasing detection logic
     */
    public static boolean isPhasing() {
        if (mc.player == null || mc.world == null) return false;

        return getAllInBox(mc.player.getBoundingBox()).stream()
                .anyMatch(blockPos -> mc.world.getBlockState(blockPos).blocksMovement());
    }
}
