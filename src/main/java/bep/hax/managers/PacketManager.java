package bep.hax.managers;
import net.minecraft.item.ItemStack;
import bep.hax.config.StardustConfig;
import net.minecraft.screen.ScreenHandler;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.MeteorClient;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
public class PacketManager {
    public PacketManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!Utils.canUpdate()) return;
        if (!StardustConfig.ignoreOverlayMessages.get()) return;
        if (!(event.packet instanceof OverlayMessageS2CPacket packet)) return;
        if (StardustConfig.overlayMessageFilter.get().isEmpty()
            || StardustConfig.overlayMessageFilter.get().stream().allMatch(String::isBlank)) return;
        for (String filter : StardustConfig.overlayMessageFilter.get()) {
            if (filter.isBlank()) continue;
            if (packet.text().getString().equalsIgnoreCase(filter)) {
                event.cancel();
            }
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null) return;
        if (!StardustConfig.antiInventoryPacketKick.get()) return;
        if (!(event.packet instanceof ClickSlotC2SPacket packet)) return;
        if (!packet.getActionType().equals(SlotActionType.QUICK_MOVE)) return;
        int origin = packet.getSlot();
        ScreenHandler handler = mc.player.currentScreenHandler;
        if (origin < 0 || origin >= handler.slots.size()) return;
        ItemStack toMove = handler.getSlot(origin).getStack();
        if (toMove.isEmpty()) {
            return;
        }
        int start;
        int until;
        if (handler instanceof PlayerScreenHandler) {
            if (origin < 9) {
                start = 9;
                until = 44;
            } else if (origin < 36) {
                start = 36;
                until = 45;
            } else {
                start = 9;
                until = 36;
            }
        } else {
            if (handler.slots.size() > 63) {
                if (origin >= 54) {
                    start = 0;
                    until = 54;
                } else {
                    start = 54;
                    until = handler.slots.size();
                }
            } else {
                if (origin >= 27) {
                    start = 0;
                    until = 27;
                } else {
                    start = 27;
                    until = handler.slots.size();
                }
            }
        }
        boolean foundValidSlot = false;
        for (int n = start; n < until; n++) {
            ItemStack stack = handler.getSlot(n).getStack();
            if (stack.isEmpty() || (ItemStack.areItemsAndComponentsEqual(toMove, stack) && stack.getCount() < stack.getMaxCount())) {
                foundValidSlot = true;
                break;
            }
        }
        if (!foundValidSlot) {
            event.cancel();
        }
    }
}
