package bep.hax.modules;

import bep.hax.Bep;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.*;  
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class Replenish extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgItems = settings.createGroup("Items");
    private final SettingGroup sgAdvanced = settings.createGroup("Advanced");
    
    private final Setting<Integer> threshold = sgGeneral.add(new IntSetting.Builder()
        .name("threshold")
        .description("Refill when stack reaches this amount.")
        .defaultValue(8)
        .min(1)
        .max(63)
        .sliderMin(1)
        .sliderMax(63)
        .build());
    
    private final Setting<Integer> tickDelay = sgGeneral.add(new IntSetting.Builder()
        .name("tick-delay")
        .description("Delay in ticks between refill operations.")
        .defaultValue(1)
        .min(0)
        .max(10)
        .sliderMin(0)
        .sliderMax(10)
        .build());
    
    private final Setting<Boolean> pauseOnUse = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-on-use")
        .description("Pause refilling while using items.")
        .defaultValue(true)
        .build());
    
    private final Setting<Boolean> smartRefill = sgGeneral.add(new BoolSetting.Builder()
        .name("smart-refill")
        .description("Only refill when you're about to run out completely.")
        .defaultValue(false)
        .build());
    
    private final Setting<Boolean> preferFullStacks = sgGeneral.add(new BoolSetting.Builder()
        .name("prefer-full-stacks")
        .description("Prefer taking from fuller stacks in inventory.")
        .defaultValue(true)
        .build());
    
    // Item specific settings
    private final Setting<Boolean> refillBlocks = sgItems.add(new BoolSetting.Builder()
        .name("blocks")
        .description("Refill building blocks.")
        .defaultValue(true)
        .build());
    
    private final Setting<Boolean> refillFood = sgItems.add(new BoolSetting.Builder()
        .name("food")
        .description("Refill food items.")
        .defaultValue(true)
        .build());
    
    private final Setting<Boolean> refillTools = sgItems.add(new BoolSetting.Builder()
        .name("tools")
        .description("Refill tools (pickaxe, axe, shovel, etc).")
        .defaultValue(false)
        .build());
    
    private final Setting<Boolean> refillWeapons = sgItems.add(new BoolSetting.Builder()
        .name("weapons")
        .description("Refill weapons (sword, bow, crossbow).")
        .defaultValue(false)
        .build());
    
    private final Setting<Boolean> refillProjectiles = sgItems.add(new BoolSetting.Builder()
        .name("projectiles")
        .description("Refill projectiles (arrows, fireworks).")
        .defaultValue(true)
        .build());
    
    private final Setting<Boolean> refillPearls = sgItems.add(new BoolSetting.Builder()
        .name("ender-pearls")
        .description("Refill ender pearls.")
        .defaultValue(true)
        .build());
    
    private final Setting<Boolean> refillPotions = sgItems.add(new BoolSetting.Builder()
        .name("potions")
        .description("Refill potions.")
        .defaultValue(true)
        .build());
    
    private final Setting<Boolean> refillTotems = sgItems.add(new BoolSetting.Builder()
        .name("totems")
        .description("Refill totems of undying.")
        .defaultValue(true)
        .build());
    
    private final Setting<Boolean> refillGaps = sgItems.add(new BoolSetting.Builder()
        .name("golden-apples")
        .description("Refill golden apples.")
        .defaultValue(true)
        .build());
    
    private final Setting<Boolean> refillFireworks = sgItems.add(new BoolSetting.Builder()
        .name("fireworks")
        .description("Refill firework rockets.")
        .defaultValue(true)
        .build());
    
    // Advanced settings
    private final Setting<Boolean> useShiftClick = sgAdvanced.add(new BoolSetting.Builder()
        .name("use-shift-click")
        .description("Use shift-click packets for faster refilling.")
        .defaultValue(true)
        .build());
    
    private final Setting<Boolean> silentRefill = sgAdvanced.add(new BoolSetting.Builder()
        .name("silent-refill")
        .description("Refill without opening inventory (packet-based).")
        .defaultValue(true)
        .build());
    
    private final Setting<Integer> maxRefillsPerTick = sgAdvanced.add(new IntSetting.Builder()
        .name("max-refills-per-tick")
        .description("Maximum refill operations per tick.")
        .defaultValue(1)
        .min(1)
        .max(5)
        .sliderMin(1)
        .sliderMax(5)
        .build());
    
    private final Setting<Boolean> maintainTool = sgAdvanced.add(new BoolSetting.Builder()
        .name("maintain-tool-type")
        .description("Only replace tools with the same type and material.")
        .defaultValue(true)
        .build());
    
    private int delayTicks = 0;
    private final Map<Integer, Integer> lastStackSizes = new HashMap<>();
    private final List<RefillOperation> pendingRefills = new ArrayList<>();
    
    public Replenish() {
        super(Bep.CATEGORY, "replenish", "Advanced auto replenish using shift-click packets.");
    }
    
    @Override
    public void onActivate() {
        delayTicks = 0;
        lastStackSizes.clear();
        pendingRefills.clear();
    }
    
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        
        // Handle delay
        if (delayTicks > 0) {
            delayTicks--;
            return;
        }
        
        // Pause if using items
        if (pauseOnUse.get() && mc.player.isUsingItem()) {
            return;
        }
        
        // Process pending refills
        if (!pendingRefills.isEmpty()) {
            processPendingRefills();
            return;
        }
        
        // Check hotbar for items that need refilling
        checkHotbar();
    }
    
    private void checkHotbar() {
        int refillsThisTick = 0;
        
        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            if (refillsThisTick >= maxRefillsPerTick.get()) break;
            
            ItemStack hotbarStack = mc.player.getInventory().getStack(hotbarSlot);
            
            // Skip empty slots
            if (hotbarStack.isEmpty()) continue;
            
            // Check if this item type should be refilled
            if (!shouldRefillItem(hotbarStack)) continue;
            
            // Check if stack needs refilling
            int currentSize = hotbarStack.getCount();
            int maxSize = hotbarStack.getMaxCount();
            
            // Smart refill logic
            if (smartRefill.get()) {
                Integer lastSize = lastStackSizes.get(hotbarSlot);
                if (lastSize != null && lastSize > currentSize && currentSize <= 1) {
                    // About to run out, refill now
                    if (attemptRefill(hotbarSlot, hotbarStack)) {
                        refillsThisTick++;
                    }
                }
                lastStackSizes.put(hotbarSlot, currentSize);
            } else {
                // Normal threshold-based refill
                if (currentSize <= threshold.get() && currentSize < maxSize) {
                    if (attemptRefill(hotbarSlot, hotbarStack)) {
                        refillsThisTick++;
                    }
                }
            }
        }
        
        if (refillsThisTick > 0) {
            delayTicks = tickDelay.get();
        }
    }
    
    private boolean shouldRefillItem(ItemStack stack) {
        Item item = stack.getItem();
        
        // Check specific item types
        if (refillTotems.get() && item == Items.TOTEM_OF_UNDYING) return true;
        if (refillPearls.get() && item == Items.ENDER_PEARL) return true;
        if (refillGaps.get() && (item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE)) return true;
        if (refillFireworks.get() && item == Items.FIREWORK_ROCKET) return true;
        
        // Check categories
        if (refillBlocks.get() && item instanceof BlockItem) return true;
        if (refillFood.get() && item.getComponents().contains(net.minecraft.component.DataComponentTypes.FOOD)) return true;
        if (refillTools.get() && item instanceof MiningToolItem) return true;
        if (refillWeapons.get() && (item instanceof SwordItem || item instanceof BowItem || item instanceof CrossbowItem)) return true;
        if (refillProjectiles.get() && (item instanceof ArrowItem || item == Items.FIREWORK_ROCKET)) return true;
        if (refillPotions.get() && item instanceof PotionItem) return true;
        
        return false;
    }
    
    private boolean attemptRefill(int hotbarSlot, ItemStack hotbarStack) {
        // Find matching item in inventory
        int sourceSlot = findSourceSlot(hotbarStack);
        if (sourceSlot == -1) return false;
        
        // Create refill operation
        RefillOperation operation = new RefillOperation(sourceSlot, hotbarSlot + 36, hotbarStack.getItem());
        
        if (useShiftClick.get()) {
            performShiftClickRefill(operation);
        } else {
            performNormalRefill(operation);
        }
        
        return true;
    }
    
    private int findSourceSlot(ItemStack targetStack) {
        int bestSlot = -1;
        int bestCount = 0;
        
        // Search main inventory (slots 9-35)
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            
            if (stack.isEmpty()) continue;
            
            // Check if items match
            if (!canStack(targetStack, stack)) continue;
            
            // For tools, check if we should maintain type
            if (maintainTool.get() && targetStack.getItem() instanceof MiningToolItem) {
                if (stack.getItem().getClass() != targetStack.getItem().getClass()) continue;
            }
            
            // Prefer fuller stacks if configured
            if (preferFullStacks.get()) {
                if (stack.getCount() > bestCount) {
                    bestCount = stack.getCount();
                    bestSlot = i;
                }
            } else {
                // Just return first matching slot
                return i;
            }
        }
        
        return bestSlot;
    }
    
    private boolean canStack(ItemStack stack1, ItemStack stack2) {
        if (stack1.getItem() != stack2.getItem()) return false;
        
        // For stackable items, check if they can be stacked
        if (stack1.getMaxCount() > 1) {
            return ItemStack.areItemsAndComponentsEqual(stack1, stack2);
        }
        
        // For non-stackable items (tools, weapons), just check if same type
        return stack1.getItem() == stack2.getItem();
    }
    
    private void performShiftClickRefill(RefillOperation operation) {
        if (silentRefill.get()) {
            // Send shift-click packet directly without opening inventory
            sendShiftClickPacket(operation.sourceSlot);
        } else {
            // Queue the operation for when inventory is open
            pendingRefills.add(operation);
        }
    }
    
    private void performNormalRefill(RefillOperation operation) {
        // Move item from inventory to hotbar
        InvUtils.move().from(operation.sourceSlot).to(operation.targetSlot - 36);
    }
    
    private void sendShiftClickPacket(int slot) {
        // Create shift-click packet
        int syncId = mc.player.currentScreenHandler.syncId;
        int revision = mc.player.currentScreenHandler.getRevision();
        
        // Create changed slots map for the packet
        Int2ObjectMap<ItemStack> changedSlots = new Int2ObjectOpenHashMap<>();
        
        // Shift-click action
        mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(
            syncId,
            revision,
            slot,
            0, // button (0 for left click)
            SlotActionType.QUICK_MOVE,
            ItemStack.EMPTY,
            changedSlots
        ));
    }
    
    private void processPendingRefills() {
        if (mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
            // Inventory is closed, can't process
            return;
        }
        
        int processed = 0;
        while (!pendingRefills.isEmpty() && processed < maxRefillsPerTick.get()) {
            RefillOperation operation = pendingRefills.remove(0);
            sendShiftClickPacket(operation.sourceSlot);
            processed++;
        }
        
        if (processed > 0) {
            delayTicks = tickDelay.get();
        }
    }
    
    private static class RefillOperation {
        final int sourceSlot;
        final int targetSlot;
        final Item item;
        
        RefillOperation(int sourceSlot, int targetSlot, Item item) {
            this.sourceSlot = sourceSlot;
            this.targetSlot = targetSlot;
            this.item = item;
        }
    }
    
    @Override
    public String getInfoString() {
        if (!pendingRefills.isEmpty()) {
            return "Refilling (" + pendingRefills.size() + ")";
        }
        return null;
    }
}