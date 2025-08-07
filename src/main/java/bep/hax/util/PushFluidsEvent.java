package bep.hax.util;

import net.minecraft.fluid.FluidState;

/**
 * Event fired when player is about to be pushed by fluids
 */
public class PushFluidsEvent {
    private final FluidState fluidState;
    private boolean canceled = false;

    public PushFluidsEvent(FluidState fluidState) {
        this.fluidState = fluidState;
    }

    public FluidState getFluidState() {
        return fluidState;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void cancel() {
        this.canceled = true;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}
