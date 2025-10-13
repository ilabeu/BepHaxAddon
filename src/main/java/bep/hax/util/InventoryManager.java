package bep.hax.util;
import bep.hax.mixin.accessor.UpdateSelectedSlotS2CPacketAccessor;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import static meteordevelopment.meteorclient.MeteorClient.mc;
public class InventoryManager {
    private static InventoryManager INSTANCE;
    private final List<PreSwapData> swapData = new CopyOnWriteArrayList<>();
    private int serverSlot = -1;
    private boolean sendingPacket = false;
    private boolean isEating = false;
    private InventoryManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }
    public static InventoryManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new InventoryManager();
        }
        return INSTANCE;
    }
    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (sendingPacket) return;
        if (event.packet instanceof UpdateSelectedSlotC2SPacket packet) {
            final int packetSlot = packet.getSelectedSlot();
            if (!PlayerInventory.isValidHotbarIndex(packetSlot) || serverSlot == packetSlot) {
                event.cancel();
                return;
            }
            serverSlot = packetSlot;
        }
    }
    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof UpdateSelectedSlotS2CPacket packet) {
            serverSlot = ((UpdateSelectedSlotS2CPacketAccessor) (Object) packet).getSlot();
        }
    }
    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player != null && serverSlot == -1) {
            serverSlot = mc.player.getInventory().selectedSlot;
        }
        swapData.removeIf(PreSwapData::isExpired);
    }
    public void setSlot(final int barSlot) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (isEating) return;
        if (serverSlot == -1) {
            serverSlot = mc.player.getInventory().selectedSlot;
        }
        if (serverSlot != barSlot && PlayerInventory.isValidHotbarIndex(barSlot)) {
            setSlotForced(barSlot);
            final ItemStack[] hotbarCopy = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                hotbarCopy[i] = mc.player.getInventory().getStack(i);
            }
            swapData.add(new PreSwapData(hotbarCopy, serverSlot, barSlot));
        }
    }
    public void setClientSlot(final int barSlot) {
        if (mc.player == null) return;
        if (isEating) return;
        if (mc.player.getInventory().selectedSlot != barSlot && PlayerInventory.isValidHotbarIndex(barSlot)) {
            mc.player.getInventory().selectedSlot = barSlot;
            setSlotForced(barSlot);
        }
    }
    public void setSlotForced(final int barSlot) {
        if (mc.getNetworkHandler() == null) return;
        sendingPacket = true;
        try {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(barSlot));
            serverSlot = barSlot;
        } finally {
            sendingPacket = false;
        }
    }
    public void syncToClient() {
        if (mc.player == null) return;
        if (isDesynced()) {
            setSlotForced(mc.player.getInventory().selectedSlot);
            for (PreSwapData data : swapData) {
                data.beginClear();
            }
        }
    }
    public boolean isDesynced() {
        if (mc.player == null) return false;
        return mc.player.getInventory().selectedSlot != serverSlot;
    }
    public int getServerSlot() {
        if (mc.player == null) return -1;
        return serverSlot == -1 ? mc.player.getInventory().selectedSlot : serverSlot;
    }
    public int getClientSlot() {
        if (mc.player == null) return -1;
        return mc.player.getInventory().selectedSlot;
    }
    public ItemStack getServerItem() {
        if (mc.player != null && getServerSlot() != -1) {
            return mc.player.getInventory().getStack(getServerSlot());
        }
        return ItemStack.EMPTY;
    }
    public void setEating(boolean eating) {
        this.isEating = eating;
    }
    public boolean isEating() {
        return isEating;
    }
    public static int getBestWeaponSlot() {
        float bestDamage = 0.0f;
        int bestSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            float damage = getWeaponDamage(stack);
            if (damage > bestDamage) {
                bestDamage = damage;
                bestSlot = i;
            }
        }
        return bestSlot;
    }
    public static float getWeaponDamage(ItemStack stack) {
        if (stack.isEmpty()) return 0.0f;
        Item item = stack.getItem();
        float baseDamage = 0.0f;
        if (item instanceof SwordItem sword) {
            baseDamage = 4.0f;
        } else if (item instanceof AxeItem axe) {
            baseDamage = 5.0f;
        } else if (item instanceof TridentItem) {
            baseDamage = TridentItem.ATTACK_DAMAGE;
        } else if (item instanceof MaceItem) {
            baseDamage = 5.0f;
        } else {
            return 0.0f;
        }
        int sharpnessLevel = meteordevelopment.meteorclient.utils.Utils.getEnchantmentLevel(stack, Enchantments.SHARPNESS);
        float sharpnessDamage = sharpnessLevel * 0.5f + 0.5f;
        return baseDamage + sharpnessDamage;
    }
    public static int getBestBreachMaceSlot() {
        int bestSlot = -1;
        int bestBreachLevel = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!(stack.getItem() instanceof MaceItem)) continue;
            int breachLevel = meteordevelopment.meteorclient.utils.Utils.getEnchantmentLevel(stack, Enchantments.BREACH);
            if (breachLevel > bestBreachLevel) {
                bestBreachLevel = breachLevel;
                bestSlot = i;
            }
        }
        return bestSlot;
    }
    public static boolean isHoldingWeapon() {
        ItemStack mainHand = mc.player.getMainHandStack();
        Item item = mainHand.getItem();
        return item instanceof SwordItem ||
               item instanceof AxeItem ||
               item instanceof TridentItem ||
               item instanceof MaceItem;
    }
    public static boolean isHoldingWeaponType(Class<? extends Item> weaponType) {
        return weaponType.isInstance(mc.player.getMainHandStack().getItem());
    }
    public static ItemStack getCurrentWeapon() {
        ItemStack mainHand = mc.player.getMainHandStack();
        return isHoldingWeapon() ? mainHand : ItemStack.EMPTY;
    }
    public static void swapToSlot(int slot) {
        if (slot >= 0 && slot < 9) {
            mc.player.getInventory().selectedSlot = slot;
        }
    }
    public static double getAttackSpeed(ItemStack weapon) {
        if (weapon.isEmpty()) return 4.0;
        Item item = weapon.getItem();
        if (item instanceof SwordItem) return 1.6;
        if (item instanceof AxeItem) return 0.8;
        if (item instanceof TridentItem) return 1.1;
        if (item instanceof MaceItem) return 0.6;
        return 4.0;
    }
    public static int getAttackCooldownTicks(ItemStack weapon) {
        double attackSpeed = getAttackSpeed(weapon);
        return (int) Math.ceil(20.0 / attackSpeed);
    }
    public static boolean isHolding32k() {
        if (mc.player == null) return false;
        ItemStack mainHand = mc.player.getMainHandStack();
        ItemStack offHand = mc.player.getOffHandStack();
        return is32kWeapon(mainHand) || is32kWeapon(offHand);
    }
    private static boolean is32kWeapon(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof MiningToolItem || stack.getItem() instanceof SwordItem)) {
            return false;
        }
        return meteordevelopment.meteorclient.utils.Utils.getEnchantmentLevel(stack, Enchantments.SHARPNESS) > 1000 ||
               meteordevelopment.meteorclient.utils.Utils.getEnchantmentLevel(stack, Enchantments.SMITE) > 1000 ||
               meteordevelopment.meteorclient.utils.Utils.getEnchantmentLevel(stack, Enchantments.BANE_OF_ARTHROPODS) > 1000;
    }
    public static boolean isMovingInput() {
        if (mc.player == null) return false;
        return mc.player.input.movementForward != 0.0f ||
               mc.player.input.movementSideways != 0.0f ||
               mc.options.jumpKey.isPressed() ||
               mc.options.sneakKey.isPressed();
    }
    public static class PreSwapData {
        private final ItemStack[] preHotbar;
        private final int starting;
        private final int swapTo;
        private long clearTime = -1;
        public PreSwapData(ItemStack[] preHotbar, int start, int swapTo) {
            this.preHotbar = preHotbar;
            this.starting = start;
            this.swapTo = swapTo;
        }
        public void beginClear() {
            clearTime = System.currentTimeMillis();
        }
        public boolean isExpired() {
            return clearTime != -1 && System.currentTimeMillis() - clearTime > 300;
        }
        public ItemStack getPreHolding(int i) {
            return preHotbar[i];
        }
        public int getStarting() {
            return starting;
        }
        public int getSlot() {
            return swapTo;
        }
    }
    public enum VelocityMode {
        NORMAL,
        WALLS,
        GRIM,
        GRIM_V3
    }
    public enum SwapMode {
        Normal,
        Silent
    }
    public interface IPlayerInteractEntityC2SPacket {
        boolean isAttackPacket();
        int getTargetEntityId();
    }
}