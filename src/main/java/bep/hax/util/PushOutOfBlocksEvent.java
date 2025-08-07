package bep.hax.util;

/**
 * Event fired when player is about to be pushed out of blocks
 */
public class PushOutOfBlocksEvent {
    private boolean canceled = false;

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
