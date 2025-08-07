package bep.hax.util;

import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Utility class for block placement operations shared across multiple modules
 * Consolidated from PVP's placement systems for optimal performance
 */
public class PlacementUtils {
    // Explosion-resistant blocks for defensive placements
    private static final List<Block> RESISTANT_BLOCKS = Arrays.asList(
        Blocks.OBSIDIAN,
        Blocks.CRYING_OBSIDIAN,
        Blocks.ENDER_CHEST,
        Blocks.RESPAWN_ANCHOR,
        Blocks.ENCHANTING_TABLE,
        Blocks.ANVIL
    );

    /**
     * Finds the best resistant block in inventory
     * @return FindItemResult with slot info, or empty if none found
     */
    public static FindItemResult findResistantBlock() {
        for (Block block : RESISTANT_BLOCKS) {
            FindItemResult result = InvUtils.findInHotbar(block.asItem());
            if (result.found()) return result;
        }
        return InvUtils.findInHotbar(itemStack -> false);
    }

    /**
     * Places a block at the specified position with proper rotation and validation
     * @param pos Block position to place at
     * @param rotate Whether to rotate to face the placement
     * @param swing Whether to swing hand after placement
     * @param strictDirection Whether to use strict direction checking
     * @return true if placement was successful
     */
    public static boolean placeBlock(BlockPos pos, boolean rotate, boolean swing, boolean strictDirection) {
        FindItemResult block = findResistantBlock();
        if (!block.found()) return false;

        return placeBlock(pos, block, rotate, swing, strictDirection);
    }

    /**
     * Places a specific block item at the specified position
     * @param pos Block position to place at
     * @param block The block item to place
     * @param rotate Whether to rotate to face the placement
     * @param swing Whether to swing hand after placement
     * @param strictDirection Whether to use strict direction checking
     * @return true if placement was successful
     */
    public static boolean placeBlock(BlockPos pos, FindItemResult block, boolean rotate, boolean swing, boolean strictDirection) {
        if (!block.found() || !canPlace(pos, strictDirection)) return false;

        // Find best direction to place from
        Direction side = getPlaceSide(pos);
        if (side == null) return false;

        BlockPos neighbor = pos.offset(side);
        Direction opposite = side.getOpposite();

        // Calculate rotation if needed
        Vec3d hitPos = Vec3d.ofCenter(neighbor).add(Vec3d.of(opposite.getVector()).multiply(0.5));
        if (rotate) {
            Rotations.rotate(Rotations.getYaw(hitPos), Rotations.getPitch(hitPos));
        }

        // Switch to block if needed
        if (block.getHand() == null && !InvUtils.swap(block.slot(), false)) return false;

        // Create and send placement packet
        BlockHitResult hitResult = new BlockHitResult(hitPos, opposite, neighbor, false);
        Hand hand = block.getHand() != null ? block.getHand() : Hand.MAIN_HAND;

        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, hitResult, 0));

        if (swing) {
            if (hand == Hand.MAIN_HAND) {
                mc.player.swingHand(Hand.MAIN_HAND);
            } else {
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
            }
        }

        return true;
    }

    /**
     * Checks if a block can be placed at the specified position
     * @param pos Position to check
     * @param strictDirection Whether to require strict direction validation
     * @return true if placement is valid
     */
    public static boolean canPlace(BlockPos pos, boolean strictDirection) {
        if (!mc.world.getBlockState(pos).isReplaceable()) return false;
        if (!mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), pos, net.minecraft.block.ShapeContext.absent())) return false;

        // Check for entities in the way
        Box checkBox = Box.from(Vec3d.ofCenter(pos));
        List<net.minecraft.entity.Entity> entities = mc.world.getOtherEntities(null, checkBox);
        for (net.minecraft.entity.Entity entity : entities) {
            if (!entity.isSpectator() && entity.isAlive()) {
                return false;
            }
        }

        // Ensure there's a valid side to place against
        return !strictDirection || getPlaceSide(pos) != null;
    }

    /**
     * Gets the best side to place a block from
     * @param pos Block position
     * @return Direction to place from, or null if none available
     */
    public static Direction getPlaceSide(BlockPos pos) {
        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.offset(side);
            if (!mc.world.getBlockState(neighbor).isReplaceable()) {
                return side;
            }
        }
        return null;
    }

    /**
     * Calculates the optimal placement position relative to player facing
     * Used for directional placement modules like SelfFill
     * @param yaw Player yaw angle
     * @param basePos Base position to offset from
     * @return Calculated placement position
     */
    public static BlockPos getDirectionalPlacement(float yaw, BlockPos basePos) {
        float normalizedYaw = yaw % 360.0f;
        if (normalizedYaw < 0.0f) normalizedYaw += 360.0f;

        if (normalizedYaw >= 22.5 && normalizedYaw < 67.5) return basePos.south().west();
        else if (normalizedYaw >= 67.5 && normalizedYaw < 112.5) return basePos.west();
        else if (normalizedYaw >= 112.5 && normalizedYaw < 157.5) return basePos.north().west();
        else if (normalizedYaw >= 157.5 && normalizedYaw < 202.5) return basePos.north();
        else if (normalizedYaw >= 202.5 && normalizedYaw < 247.5) return basePos.north().east();
        else if (normalizedYaw >= 247.5 && normalizedYaw < 292.5) return basePos.east();
        else if (normalizedYaw >= 292.5 && normalizedYaw < 337.5) return basePos.south().east();
        else return basePos.south();
    }

    /**
     * Checks if the player is currently phasing through blocks
     * @return true if player bounding box intersects with solid blocks
     */
    public static boolean isPhasing() {
        if (mc.player == null) return false;

        net.minecraft.util.math.Box bb = mc.player.getBoundingBox();
        int minX = net.minecraft.util.math.MathHelper.floor(bb.minX);
        int maxX = net.minecraft.util.math.MathHelper.floor(bb.maxX) + 1;
        int minY = net.minecraft.util.math.MathHelper.floor(bb.minY);
        int maxY = net.minecraft.util.math.MathHelper.floor(bb.maxY) + 1;
        int minZ = net.minecraft.util.math.MathHelper.floor(bb.minZ);
        int maxZ = net.minecraft.util.math.MathHelper.floor(bb.maxZ) + 1;

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (mc.world.getBlockState(pos).blocksMovement()) {
                        net.minecraft.util.math.Box blockBox = new net.minecraft.util.math.Box(x, y, z, x + 1.0, y + 1.0, z + 1.0);
                        if (bb.intersects(blockBox)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Gets inventory slot containing an ender pearl
     * @return Slot number or -1 if none found
     */
    public static int getEnderPearlSlot() {
        if (mc.player == null) return -1;

        for (int i = 0; i < 45; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == net.minecraft.item.Items.ENDER_PEARL) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Performs inventory click operations for item swapping
     * @param slot Slot to interact with
     * @param actionType Type of slot action
     */
    public static void clickSlot(int slot, net.minecraft.screen.slot.SlotActionType actionType) {
        if (mc.interactionManager != null && mc.player != null) {
            mc.interactionManager.clickSlot(0, slot, 0, actionType, mc.player);
        }
    }

    /**
     * Alias for isPhasing() - checks if player is phased (inside a block)
     * @return true if player is inside any solid block
     */
    public static boolean isPhased() {
        return isPhasing();
    }

    /**
     * Checks if player is double phased (inside blocks at both head and feet level)
     * @return true if player is inside solid blocks at multiple Y levels
     */
    public static boolean isDoublePhased() {
        if (mc.player == null || mc.world == null) return false;

        Box playerBox = mc.player.getBoundingBox();
        boolean feetBlocked = false;
        boolean headBlocked = false;

        for (int x = (int) Math.floor(playerBox.minX); x <= Math.floor(playerBox.maxX); x++) {
            for (int z = (int) Math.floor(playerBox.minZ); z <= Math.floor(playerBox.maxZ); z++) {
                // Check feet level
                BlockPos feetPos = new BlockPos(x, (int) Math.floor(playerBox.minY), z);
                if (mc.world.getBlockState(feetPos).blocksMovement()) {
                    feetBlocked = true;
                }

                // Check head level
                BlockPos headPos = new BlockPos(x, (int) Math.floor(playerBox.maxY), z);
                if (mc.world.getBlockState(headPos).blocksMovement()) {
                    headBlocked = true;
                }

                if (feetBlocked && headBlocked) {
                    return true;
                }
            }
        }
        return false;
    }
}
