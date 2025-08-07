package bep.hax.util;

import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Utilities for managing inventory operations and weapon selection.
 */
public class InventoryUtils {

    /**
     * Gets the slot of the best weapon in the hotbar.
     */
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

    /**
     * Gets the damage value of a weapon including enchantments.
     */
    public static float getWeaponDamage(ItemStack stack) {
        if (stack.isEmpty()) return 0.0f;

        Item item = stack.getItem();
        float baseDamage = 0.0f;

        if (item instanceof SwordItem sword) {
            baseDamage = 4.0f; // Base sword damage in 1.21.4
        } else if (item instanceof AxeItem axe) {
            baseDamage = 5.0f; // Base axe damage in 1.21.4
        } else if (item instanceof TridentItem) {
            baseDamage = TridentItem.ATTACK_DAMAGE;
        } else if (item instanceof MaceItem) {
            baseDamage = 5.0f; // Base mace damage
        } else {
            return 0.0f;
        }

        // Add sharpness damage
        int sharpnessLevel = meteordevelopment.meteorclient.utils.Utils.getEnchantmentLevel(stack, Enchantments.SHARPNESS);
        float sharpnessDamage = sharpnessLevel * 0.5f + 0.5f;

        return baseDamage + sharpnessDamage;
    }

    /**
     * Gets the slot of the best mace with breach enchantment.
     */
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

    /**
     * Checks if the player is holding a weapon in their main hand.
     */
    public static boolean isHoldingWeapon() {
        ItemStack mainHand = mc.player.getMainHandStack();
        Item item = mainHand.getItem();

        return item instanceof SwordItem ||
               item instanceof AxeItem ||
               item instanceof TridentItem ||
               item instanceof MaceItem;
    }

    /**
     * Checks if the player is holding a specific type of weapon.
     */
    public static boolean isHoldingWeaponType(Class<? extends Item> weaponType) {
        return weaponType.isInstance(mc.player.getMainHandStack().getItem());
    }

    /**
     * Gets the current weapon in the main hand.
     */
    public static ItemStack getCurrentWeapon() {
        ItemStack mainHand = mc.player.getMainHandStack();
        return isHoldingWeapon() ? mainHand : ItemStack.EMPTY;
    }

    /**
     * Swaps to the specified hotbar slot.
     */
    public static void swapToSlot(int slot) {
        if (slot >= 0 && slot < 9) {
            mc.player.getInventory().selectedSlot = slot;
        }
    }

    /**
     * Gets the attack speed of the current weapon.
     */
    public static double getAttackSpeed(ItemStack weapon) {
        if (weapon.isEmpty()) return 4.0; // Default attack speed

        // This would need to be implemented by reading the weapon's attribute modifiers
        // For now, return reasonable defaults
        Item item = weapon.getItem();
        if (item instanceof SwordItem) return 1.6;
        if (item instanceof AxeItem) return 0.8;
        if (item instanceof TridentItem) return 1.1;
        if (item instanceof MaceItem) return 0.6;

        return 4.0;
    }

    /**
     * Calculates the attack cooldown in ticks for the current weapon.
     */
    public static int getAttackCooldownTicks(ItemStack weapon) {
        double attackSpeed = getAttackSpeed(weapon);
        return (int) Math.ceil(20.0 / attackSpeed);
    }

    /**
     * Checks if the player is holding a 32k weapon (overpowered enchanted weapon)
     * @return true if holding a weapon with high enchantment levels
     */
    public static boolean isHolding32k() {
        if (mc.player == null) return false;

        ItemStack mainHand = mc.player.getMainHandStack();
        ItemStack offHand = mc.player.getOffHandStack();

        return is32kWeapon(mainHand) || is32kWeapon(offHand);
    }

    /**
     * Checks if an item stack is a 32k weapon
     * @param stack Item stack to check
     * @return true if it's a 32k weapon
     */
    private static boolean is32kWeapon(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof MiningToolItem || stack.getItem() instanceof SwordItem)) {
            return false;
        }

        // Check for abnormally high enchantment levels (32k weapons typically have level 32767)
        return meteordevelopment.meteorclient.utils.Utils.getEnchantmentLevel(stack, Enchantments.SHARPNESS) > 1000 ||
               meteordevelopment.meteorclient.utils.Utils.getEnchantmentLevel(stack, Enchantments.SMITE) > 1000 ||
               meteordevelopment.meteorclient.utils.Utils.getEnchantmentLevel(stack, Enchantments.BANE_OF_ARTHROPODS) > 1000;
    }

    /**
     * Checks if the player is currently providing movement input
     * @return true if player is trying to move
     */
    public static boolean isMovingInput() {
        if (mc.player == null) return false;

        return mc.player.input.movementForward != 0.0f ||
               mc.player.input.movementSideways != 0.0f ||
               mc.options.jumpKey.isPressed() ||
               mc.options.sneakKey.isPressed();
    }
}
