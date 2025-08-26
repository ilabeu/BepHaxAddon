package bep.hax.modules;

import bep.hax.Bep;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class ElytraSwap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> durabilityThreshold = sgGeneral.add(new IntSetting.Builder()
        .name("Durability Threshold")
        .description("Swap elytra when durability drops below this value.")
        .defaultValue(10)
        .min(1)
        .max(100)
        .sliderRange(1, 50)
        .build()
    );

    private final Setting<Boolean> onlyWhileFlying = sgGeneral.add(new BoolSetting.Builder()
        .name("Only While Flying")
        .description("Only swap elytras while actively flying.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> pauseInInventory = sgGeneral.add(new BoolSetting.Builder()
        .name("Pause In Inventory")
        .description("Don't swap while inventory is open to prevent desync.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> swapCooldown = sgGeneral.add(new IntSetting.Builder()
        .name("Swap Cooldown")
        .description("Ticks to wait after swapping before checking again.")
        .defaultValue(100)
        .min(20)
        .max(200)
        .sliderRange(20, 200)
        .build()
    );

    private final Setting<Boolean> notifySwap = sgGeneral.add(new BoolSetting.Builder()
        .name("Notify Swap")
        .description("Send a chat message when swapping elytras.")
        .defaultValue(true)
        .build()
    );

    private int cooldownTimer = 0;
    private boolean needsSwap = false;
    private int swapStage = 0;
    private int stageTimer = 0;
    private int targetSlot = -1;
    
    private int newElytraOriginalSlot = -1; // Where the new elytra came from
    private int hotbarSlotUsed = -1; // Which hotbar slot we're using for swaps
    private ItemStack hotbarOriginalItem = ItemStack.EMPTY; // What was originally in hotbar

    public ElytraSwap() {
        super(
            Bep.CATEGORY,
            "ElytraSwap",
            "Automatically swaps elytras when they reach low durability."
        );
    }

    @Override
    public void onActivate() {
        resetSwapState();
    }

    @Override
    public void onDeactivate() {
        resetSwapState();
    }

    private void resetSwapState() {
        cooldownTimer = 0;
        needsSwap = false;
        swapStage = 0;
        stageTimer = 0;
        targetSlot = -1;
        newElytraOriginalSlot = -1;
        hotbarSlotUsed = -1;
        hotbarOriginalItem = ItemStack.EMPTY;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // Handle cooldown
        if (cooldownTimer > 0) {
            cooldownTimer--;
            return;
        }

        // Safety check - don't swap with inventory open
        if (pauseInInventory.get() && mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
            resetSwapState();
            return;
        }

        // Check if wearing elytra
        ItemStack chestItem = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (!chestItem.getItem().equals(Items.ELYTRA)) {
            return;
        }

        // Check if we should only swap while flying
        if (onlyWhileFlying.get() && !mc.player.isGliding()) {
            return;
        }

        // Process swap stages
        if (needsSwap) {
            processSwapStages();
            return;
        }

        // Check durability
        int currentDurability = chestItem.getMaxDamage() - chestItem.getDamage();
        if (currentDurability <= durabilityThreshold.get()) {
            initiateSwap();
        }
    }

    private void initiateSwap() {
        // Find best elytra in inventory
        int bestSlot = -1;
        int bestDurability = durabilityThreshold.get();

        // Check entire inventory (0-35)
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem().equals(Items.ELYTRA)) {
                int durability = stack.getMaxDamage() - stack.getDamage();
                if (durability > bestDurability) {
                    bestDurability = durability;
                    bestSlot = i;
                }
            }
        }

        if (bestSlot == -1) {
            return;
        }

        targetSlot = bestSlot;
        needsSwap = true;
        swapStage = 1;
        stageTimer = 0;
    }

    private void processSwapStages() {
        stageTimer++;

        // Add delay between stages to prevent desync
        if (stageTimer < 5) return;

        switch (swapStage) {
            case 1 -> {
                // Stage 1: Remember where the new elytra came from and move it to hotbar
                newElytraOriginalSlot = targetSlot; // Remember where the new elytra came from
                
                // Move elytra to hotbar if it's in main inventory
                if (targetSlot >= 9) {
                    // Need to find a hotbar slot to use
                    int hotbarSlot = -1;
                    
                    // Try to use a consistent hotbar slot
                    if (hotbarSlotUsed != -1 && hotbarSlotUsed < 9) {
                        // Use the previously used hotbar slot
                        hotbarSlot = hotbarSlotUsed;
                    } else {
                        // Find empty slot or slot with non-essential item
                        for (int i = 0; i < 9; i++) {
                            ItemStack stack = mc.player.getInventory().getStack(i);
                            if (stack.isEmpty() || !isEssentialItem(stack)) {
                                hotbarSlot = i;
                                break;
                            }
                        }
                        
                        if (hotbarSlot == -1) {
                            hotbarSlot = 0; // Use first slot if no good option
                        }
                    }
                    
                    // Remember what was originally in this hotbar slot
                    hotbarOriginalItem = mc.player.getInventory().getStack(hotbarSlot).copy();
                    hotbarSlotUsed = hotbarSlot;
                    
                    // Move new elytra to hotbar
                    InvUtils.move().from(targetSlot).toHotbar(hotbarSlot);
                    targetSlot = hotbarSlot; // Update target to the hotbar slot
                    swapStage = 2;
                    stageTimer = 0;
                } else {
                    // Already in hotbar - just use it directly
                    hotbarSlotUsed = targetSlot;
                    hotbarOriginalItem = ItemStack.EMPTY; // Nothing to restore
                    swapStage = 2;
                    stageTimer = 0;
                }
            }
            case 2 -> {
                // Stage 2: Equip the new elytra from hotbar
                // Swap to the elytra slot temporarily
                InvUtils.swap(targetSlot, false);

                // Use right-click to equip it
                mc.interactionManager.interactItem(mc.player, net.minecraft.util.Hand.MAIN_HAND);

                // Swap back to previous slot
                InvUtils.swapBack();

                swapStage = 3;
                stageTimer = 0;
            }
            case 3 -> {
                // Stage 3: Move broken elytra to where new one came from
                // The broken elytra is now in the hotbar at targetSlot
                // We need to move it to where the new elytra originally was
                
                if (newElytraOriginalSlot >= 9) {
                    // New elytra came from inventory, move broken one there
                    InvUtils.move().fromHotbar(targetSlot).to(newElytraOriginalSlot);
                    
                    // Now restore the original hotbar item if there was one
                    if (!hotbarOriginalItem.isEmpty()) {
                        // Need to wait a tick before restoring
                        swapStage = 4;
                        stageTimer = 0;
                        return;
                    }
                }
                // If new elytra was already in hotbar, broken one stays there (correct behavior)
                
                // Notify
                if (notifySwap.get()) {
                    ItemStack newChest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
                    if (newChest.getItem().equals(Items.ELYTRA)) {
                        int newDurability = newChest.getMaxDamage() - newChest.getDamage();
                        info("Swapped to elytra with " + newDurability + " durability");
                    }
                }

                // Complete the swap
                needsSwap = false;
                swapStage = 0;
                stageTimer = 0;
                targetSlot = -1;
                cooldownTimer = swapCooldown.get();
            }
            case 4 -> {
                // Stage 4: Restore original hotbar item
                if (stageTimer < 3) {
                    stageTimer++;
                    return;
                }
                
                // Look for the original hotbar item in inventory and restore it
                for (int i = 9; i < 36; i++) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (ItemStack.areItemsEqual(stack, hotbarOriginalItem)) {
                        InvUtils.move().from(i).toHotbar(hotbarSlotUsed);
                        break;
                    }
                }
                
                // Complete the swap
                needsSwap = false;
                swapStage = 0;
                stageTimer = 0;
                targetSlot = -1;
                cooldownTimer = swapCooldown.get();
            }
        }
    }
    
    private boolean isEssentialItem(ItemStack stack) {
        // Consider items like tools, weapons, food as essential
        return stack.getItem().equals(Items.TOTEM_OF_UNDYING) ||
               stack.getItem().equals(Items.GOLDEN_APPLE) ||
               stack.getItem().equals(Items.ENCHANTED_GOLDEN_APPLE) ||
               stack.getItem().equals(Items.ENDER_PEARL) ||
               stack.getItem().equals(Items.CHORUS_FRUIT);
    }
}
