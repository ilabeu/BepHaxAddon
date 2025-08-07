package bep.hax.util;

import meteordevelopment.meteorclient.systems.friends.Friends;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Utilities for entity targeting and prioritization in combat scenarios.
 */
public class TargetUtils {

    public enum Priority {
        HEALTH,
        DISTANCE,
        ARMOR
    }

    public enum TargetMode {
        SWITCH,
        SINGLE
    }

    /**
     * Gets all valid attackable entities within the specified range.
     */
    public static List<Entity> getTargets(Vec3d fromPos, double range, boolean players, boolean monsters,
                                         boolean neutrals, boolean animals, boolean invisibles) {
        List<Entity> targets = new ArrayList<>();

        for (Entity entity : mc.world.getEntities()) {
            if (!isValidTarget(entity, fromPos, range, players, monsters, neutrals, animals, invisibles)) {
                continue;
            }
            targets.add(entity);
        }

        return targets;
    }

    /**
     * Gets the best target based on the specified priority.
     */
    public static Entity getBestTarget(Vec3d fromPos, double range, Priority priority, boolean players,
                                     boolean monsters, boolean neutrals, boolean animals, boolean invisibles) {
        List<Entity> targets = getTargets(fromPos, range, players, monsters, neutrals, animals, invisibles);

        if (targets.isEmpty()) return null;

        return switch (priority) {
            case DISTANCE -> targets.stream()
                    .min(Comparator.comparing(e -> fromPos.distanceTo(e.getPos())))
                    .orElse(null);
            case HEALTH -> targets.stream()
                    .filter(e -> e instanceof LivingEntity)
                    .min(Comparator.comparing(e -> {
                        LivingEntity living = (LivingEntity) e;
                        return living.getHealth() + living.getAbsorptionAmount();
                    }))
                    .orElse(null);
            case ARMOR -> targets.stream()
                    .filter(e -> e instanceof LivingEntity)
                    .min(Comparator.comparing(e -> getArmorDurability((LivingEntity) e)))
                    .orElse(null);
        };
    }

    /**
     * Checks if an entity is a valid target for combat.
     */
    public static boolean isValidTarget(Entity entity, Vec3d fromPos, double range, boolean players,
                                       boolean monsters, boolean neutrals, boolean animals, boolean invisibles) {
        if (entity == null || entity == mc.player || !entity.isAlive()) {
            return false;
        }

        // Check range
        double distance = fromPos.distanceTo(entity.getPos());
        if (distance > range) {
            return false;
        }

        // Skip certain entity types
        if (entity instanceof EndCrystalEntity || entity instanceof ItemEntity ||
            entity instanceof ArrowEntity || entity instanceof ExperienceBottleEntity) {
            return false;
        }

        // Check friends (only for players)
        if (entity instanceof PlayerEntity && Friends.get().isFriend((PlayerEntity) entity)) {
            return false;
        }

        // Check invisibility
        if (entity.isInvisible() && !invisibles) {
            return false;
        }

        // Check entity type targeting
        return isTargetableType(entity, players, monsters, neutrals, animals);
    }

    /**
     * Checks if entity type should be targeted based on settings.
     */
    public static boolean isTargetableType(Entity entity, boolean players, boolean monsters,
                                          boolean neutrals, boolean animals) {
        if (entity instanceof PlayerEntity) {
            return players;
        }

        return EntityUtil.isMonster(entity) && monsters ||
               EntityUtil.isNeutral(entity) && neutrals ||
               EntityUtil.isPassive(entity) && animals;
    }

    /**
     * Gets the total armor durability percentage of a living entity.
     */
    public static float getArmorDurability(LivingEntity entity) {
        float totalDamage = 0.0f;
        float totalMax = 0.0f;

        for (ItemStack armor : entity.getArmorItems()) {
            if (armor != null && !armor.isEmpty()) {
                totalDamage += armor.getDamage();
                totalMax += armor.getMaxDamage();
            }
        }

        if (totalMax == 0) return 0.0f;
        return 100.0f - (totalDamage / totalMax * 100.0f);
    }

    /**
     * Checks if an entity has any armor equipped.
     */
    public static boolean hasArmor(LivingEntity entity) {
        return entity.getArmorItems().iterator().hasNext();
    }
}
