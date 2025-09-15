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

    private final SettingGroup sgCombat = settings.createGroup("Combat Protection");

    private final Setting<Boolean> swapOnHit = sgCombat.add(new BoolSetting.Builder()
        .name("Swap On Hit")
        .description("Automatically swap elytra to chestplate when hit by an entity.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> hitProtectionDuration = sgCombat.add(new IntSetting.Builder()
        .name("Protection Duration")
        .description("Ticks to keep chestplate equipped after being hit.")
        .defaultValue(60)
        .min(20)
        .max(200)
        .sliderRange(20, 200)
        .visible(swapOnHit::get)
        .build()
    );

    private final Setting<Boolean> autoSwapBack = sgCombat.add(new BoolSetting.Builder()
        .name("Auto Swap Back")
        .description("Automatically swap back to elytra after protection duration.")
        .defaultValue(true)
        .visible(swapOnHit::get)
        .build()
    );

    private final Setting<Boolean> prioritizeNetherite = sgCombat.add(new BoolSetting.Builder()
        .name("Prioritize Netherite")
        .description("Prioritize netherite chestplates over diamond.")
        .defaultValue(true)
        .visible(swapOnHit::get)
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
    
    // Combat protection state
    private boolean protectionActive = false;
    private int protectionTimer = 0;
    private int lastHurtTime = 0;
    private boolean needsChestplateSwap = false;
    private int chestplateSwapStage = 0;
    private int chestplateSlot = -1;
    private ItemStack storedElytra = ItemStack.EMPTY;

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
        protectionActive = false;
        protectionTimer = 0;
        lastHurtTime = 0;
        needsChestplateSwap = false;
        chestplateSwapStage = 0;
        chestplateSlot = -1;
        storedElytra = ItemStack.EMPTY;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // Handle combat protection
        if (swapOnHit.get()) {
            handleCombatProtection();
        }

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
        
        // Skip durability check if we're in protection mode
        if (protectionActive) {
            return;
        }
        
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
                // First verify we have an elytra in the target slot
                ItemStack toEquip = mc.player.getInventory().getStack(targetSlot);
                if (!toEquip.getItem().equals(Items.ELYTRA)) {
                    // Something went wrong, abort
                    resetSwapState();
                    return;
                }
                
                // Also verify we're not about to equip pants (safety check)
                if (toEquip.getItem().equals(Items.LEATHER_LEGGINGS) ||
                    toEquip.getItem().equals(Items.CHAINMAIL_LEGGINGS) ||
                    toEquip.getItem().equals(Items.IRON_LEGGINGS) ||
                    toEquip.getItem().equals(Items.GOLDEN_LEGGINGS) ||
                    toEquip.getItem().equals(Items.DIAMOND_LEGGINGS) ||
                    toEquip.getItem().equals(Items.NETHERITE_LEGGINGS)) {
                    // Somehow we have leggings, abort to prevent pants removal
                    resetSwapState();
                    return;
                }
                
                // Swap to the elytra slot temporarily
                InvUtils.swap(targetSlot, false);

                // Use right-click to equip it (this maintains smooth movement)
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
    
    private void handleCombatProtection() {
        if (mc.player == null) return;
        
        // Check if player was recently hurt
        if (mc.player.hurtTime > 0 && mc.player.hurtTime > lastHurtTime) {
            // Player was just hit
            lastHurtTime = mc.player.hurtTime;
            
            // Check if wearing elytra and not already in protection mode
            ItemStack chestItem = mc.player.getEquippedStack(EquipmentSlot.CHEST);
            if (chestItem.getItem().equals(Items.ELYTRA) && !protectionActive) {
                // Find a chestplate
                int bestChestplate = findBestChestplate();
                if (bestChestplate != -1) {
                    storedElytra = chestItem.copy();
                    chestplateSlot = bestChestplate;
                    needsChestplateSwap = true;
                    chestplateSwapStage = 1;
                    stageTimer = 0;
                    protectionActive = true;
                    protectionTimer = hitProtectionDuration.get();
                    
                    if (notifySwap.get()) {
                        info("Swapping to chestplate for protection!");
                    }
                }
            } else if (protectionActive) {
                // Reset protection timer if hit again while protected
                protectionTimer = hitProtectionDuration.get();
            }
        }
        
        // Update hurt time tracker
        if (mc.player.hurtTime < lastHurtTime) {
            lastHurtTime = mc.player.hurtTime;
        }
        
        // Process chestplate swap
        if (needsChestplateSwap) {
            processChestplateSwap();
            return;
        }
        
        // Handle protection timer and swap back
        if (protectionActive && !needsChestplateSwap) {
            protectionTimer--;
            
            if (protectionTimer <= 0 && autoSwapBack.get()) {
                // Time to swap back to elytra
                ItemStack chestItem = mc.player.getEquippedStack(EquipmentSlot.CHEST);
                if (!chestItem.getItem().equals(Items.ELYTRA) && !storedElytra.isEmpty()) {
                    // Find where the elytra is now
                    int elytraSlot = findStoredElytra();
                    if (elytraSlot != -1) {
                        chestplateSlot = elytraSlot;
                        needsChestplateSwap = true;
                        chestplateSwapStage = 1;
                        stageTimer = 0;
                        
                        if (notifySwap.get()) {
                            info("Protection period ended, swapping back to elytra.");
                        }
                    } else {
                        // Couldn't find elytra, end protection mode
                        protectionActive = false;
                        storedElytra = ItemStack.EMPTY;
                    }
                } else {
                    // Already wearing elytra or no stored elytra
                    protectionActive = false;
                    storedElytra = ItemStack.EMPTY;
                }
            }
        }
    }
    
    private void processChestplateSwap() {
        stageTimer++;
        
        // Add delay between stages
        if (stageTimer < 3) return;
        
        switch (chestplateSwapStage) {
            case 1 -> {
                // Move chestplate to hotbar if needed
                if (chestplateSlot >= 9) {
                    // Find a hotbar slot
                    int hotbarSlot = 0;
                    for (int i = 0; i < 9; i++) {
                        ItemStack stack = mc.player.getInventory().getStack(i);
                        if (stack.isEmpty() || !isEssentialItem(stack)) {
                            hotbarSlot = i;
                            break;
                        }
                    }
                    
                    InvUtils.move().from(chestplateSlot).toHotbar(hotbarSlot);
                    chestplateSlot = hotbarSlot;
                }
                
                chestplateSwapStage = 2;
                stageTimer = 0;
            }
            case 2 -> {
                // Equip the chestplate/elytra
                ItemStack toEquip = mc.player.getInventory().getStack(chestplateSlot);
                
                // Verify it's a chestplate or elytra
                if (!isChestplateItem(toEquip)) {
                    // Not a valid chest item, abort
                    needsChestplateSwap = false;
                    chestplateSwapStage = 0;
                    return;
                }
                
                // Safety check - ensure we're not equipping leggings
                if (toEquip.getItem().equals(Items.LEATHER_LEGGINGS) ||
                    toEquip.getItem().equals(Items.CHAINMAIL_LEGGINGS) ||
                    toEquip.getItem().equals(Items.IRON_LEGGINGS) ||
                    toEquip.getItem().equals(Items.GOLDEN_LEGGINGS) ||
                    toEquip.getItem().equals(Items.DIAMOND_LEGGINGS) ||
                    toEquip.getItem().equals(Items.NETHERITE_LEGGINGS)) {
                    // Abort if somehow we have leggings
                    needsChestplateSwap = false;
                    chestplateSwapStage = 0;
                    return;
                }
                
                // Use the key swap method for smooth movement
                InvUtils.swap(chestplateSlot, false);
                mc.interactionManager.interactItem(mc.player, net.minecraft.util.Hand.MAIN_HAND);
                InvUtils.swapBack();
                
                chestplateSwapStage = 3;
                stageTimer = 0;
            }
            case 3 -> {
                // Complete the swap
                needsChestplateSwap = false;
                chestplateSwapStage = 0;
                stageTimer = 0;
                chestplateSlot = -1;
                
                // If swapping back to elytra, end protection mode
                ItemStack chestItem = mc.player.getEquippedStack(EquipmentSlot.CHEST);
                if (chestItem.getItem().equals(Items.ELYTRA)) {
                    protectionActive = false;
                    storedElytra = ItemStack.EMPTY;
                }
            }
        }
    }
    
    private int findBestChestplate() {
        int bestSlot = -1;
        int bestValue = 0;
        
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            
            int value = getChestplateValue(stack);
            if (value > bestValue) {
                bestValue = value;
                bestSlot = i;
            }
        }
        
        return bestSlot;
    }
    
    private int getChestplateValue(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        
        // Check for different chestplate types
        if (stack.getItem().equals(Items.NETHERITE_CHESTPLATE)) {
            if (prioritizeNetherite.get()) {
                return 1000 + (stack.getMaxDamage() - stack.getDamage());
            }
            return 400 + (stack.getMaxDamage() - stack.getDamage());
        } else if (stack.getItem().equals(Items.DIAMOND_CHESTPLATE)) {
            return 300 + (stack.getMaxDamage() - stack.getDamage());
        } else if (stack.getItem().equals(Items.IRON_CHESTPLATE)) {
            return 200 + (stack.getMaxDamage() - stack.getDamage());
        } else if (stack.getItem().equals(Items.GOLDEN_CHESTPLATE)) {
            return 100 + (stack.getMaxDamage() - stack.getDamage());
        } else if (stack.getItem().equals(Items.CHAINMAIL_CHESTPLATE)) {
            return 150 + (stack.getMaxDamage() - stack.getDamage());
        } else if (stack.getItem().equals(Items.LEATHER_CHESTPLATE)) {
            return 50 + (stack.getMaxDamage() - stack.getDamage());
        }
        
        return 0;
    }
    
    private boolean isChestplateItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem().equals(Items.ELYTRA) ||
               stack.getItem().equals(Items.NETHERITE_CHESTPLATE) ||
               stack.getItem().equals(Items.DIAMOND_CHESTPLATE) ||
               stack.getItem().equals(Items.IRON_CHESTPLATE) ||
               stack.getItem().equals(Items.GOLDEN_CHESTPLATE) ||
               stack.getItem().equals(Items.CHAINMAIL_CHESTPLATE) ||
               stack.getItem().equals(Items.LEATHER_CHESTPLATE);
    }
    
    private int findStoredElytra() {
        // Look for the elytra we stored earlier
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem().equals(Items.ELYTRA)) {
                // Check if it's roughly the same elytra (by damage)
                if (Math.abs(stack.getDamage() - storedElytra.getDamage()) <= 5) {
                    return i;
                }
            }
        }
        
        // If we can't find the exact one, just find any elytra
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem().equals(Items.ELYTRA)) {
                return i;
            }
        }
        
        return -1;
    }
}
