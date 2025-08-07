package bep.hax.util;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Event fired when a player attacks/starts mining a block
 * Exact PVP implementation for module compatibility
 */
public class AttackBlockEvent {
    private final BlockPos pos;
    private final Direction direction;
    private final BlockState state;
    private boolean cancelled = false;

    public AttackBlockEvent(BlockPos pos, Direction direction, BlockState state) {
        this.pos = pos;
        this.direction = direction;
        this.state = state;
    }

    public BlockPos getPos() {
        return pos;
    }

    public Direction getDirection() {
        return direction;
    }

    public BlockState getState() {
        return state;
    }

    public void cancel() {
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
