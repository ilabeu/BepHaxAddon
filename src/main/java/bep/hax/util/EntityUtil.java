package bep.hax.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.*;

/**
 * Utilities for entity classification and analysis.
 */
public class EntityUtil {

    /**
     * Checks if an entity is a hostile monster.
     */
    public static boolean isMonster(Entity entity) {
        return entity instanceof HostileEntity ||
            entity instanceof SlimeEntity ||
            entity instanceof GhastEntity ||
            entity instanceof PhantomEntity ||
            entity instanceof ShulkerEntity;
    }

    /**
     * Checks if an entity is a neutral mob that can become hostile.
     */
    public static boolean isNeutral(Entity entity) {
        return entity instanceof ZombifiedPiglinEntity ||
            entity instanceof PiglinEntity ||
            entity instanceof EndermanEntity ||
            entity instanceof WolfEntity ||
            entity instanceof LlamaEntity ||
            entity instanceof TraderLlamaEntity ||
            entity instanceof BeeEntity ||
            entity instanceof SpiderEntity ||
            entity instanceof CaveSpiderEntity ||
            entity instanceof PolarBearEntity ||
            entity instanceof PandaEntity ||
            entity instanceof DolphinEntity ||
            entity instanceof IronGolemEntity;
    }

    /**
     * Checks if a neutral mob is currently aggressive/attacking.
     * This helps distinguish between peaceful and aggressive states.
     */
    public static boolean isAggressive(Entity entity) {
        if (entity instanceof EndermanEntity enderman) {
            return enderman.isAngry();
        }
        if (entity instanceof ZombifiedPiglinEntity zombifiedPiglin) {
            return zombifiedPiglin.isAttacking();
        }
        if (entity instanceof WolfEntity wolf) {
            return wolf.isAttacking();
        }
        if (entity instanceof PiglinEntity piglin) {
            return piglin.isAttacking();
        }
        if (entity instanceof BeeEntity bee) {
            return bee.hasAngerTime();
        }
        if (entity instanceof PolarBearEntity polarBear) {
            return polarBear.isAttacking();
        }
        if (entity instanceof LlamaEntity llama) {
            return llama.isAttacking();
        }
        if (entity instanceof IronGolemEntity ironGolem) {
            return ironGolem.isAttacking();
        }
        // Spiders are hostile at night or in darkness
        if (entity instanceof SpiderEntity || entity instanceof CaveSpiderEntity) {
            return entity.getWorld().getAmbientDarkness() >= 0.5f;
        }
        return false;
    }

    /**
     * Checks if an entity is a passive animal.
     */
    public static boolean isPassive(Entity entity) {
        return entity instanceof AnimalEntity ||
            entity instanceof AmbientEntity ||
            entity instanceof WaterCreatureEntity ||
            entity instanceof VillagerEntity ||
            entity instanceof SquidEntity ||
            entity instanceof BatEntity;
    }

    /**
     * Checks if an entity is a player.
     */
    public static boolean isPlayer(Entity entity) {
        return entity instanceof PlayerEntity;
    }

    /**
     * Gets a general category for the entity type.
     */
    public static EntityCategory getEntityCategory(Entity entity) {
        if (isPlayer(entity)) return EntityCategory.PLAYER;
        if (isMonster(entity)) return EntityCategory.MONSTER;
        if (isNeutral(entity)) return EntityCategory.NEUTRAL;
        if (isPassive(entity)) return EntityCategory.PASSIVE;
        return EntityCategory.OTHER;
    }

    /**
     * Checks if an entity type should be considered for targeting.
     */
    public static boolean isLivingTarget(Entity entity) {
        return entity instanceof PlayerEntity ||
            entity instanceof MobEntity;
    }

    /**
     * Gets the entity's display name or type name.
     */
    public static String getEntityName(Entity entity) {
        if (entity instanceof PlayerEntity player) {
            return player.getGameProfile().getName();
        }
        return EntityType.getId(entity.getType()).getPath();
    }

    /**
     * Checks if entity is undead (affected by smite enchantment).
     */
    public static boolean isUndead(Entity entity) {
        return entity instanceof ZombieEntity ||
            entity instanceof SkeletonEntity ||
            entity instanceof WitherSkeletonEntity ||
            entity instanceof StrayEntity ||
            entity instanceof HuskEntity ||
            entity instanceof DrownedEntity ||
            entity instanceof ZombieVillagerEntity ||
            entity instanceof ZombifiedPiglinEntity ||
            entity instanceof net.minecraft.entity.boss.WitherEntity ||
            entity instanceof PhantomEntity;
    }

    /**
     * Checks if entity is arthropod (affected by bane of arthropods).
     */
    public static boolean isArthropod(Entity entity) {
        return entity instanceof SpiderEntity ||
            entity instanceof CaveSpiderEntity ||
            entity instanceof SilverfishEntity ||
            entity instanceof EndermiteEntity ||
            entity instanceof BeeEntity;
    }

    /**
     * Checks if an entity is a vehicle (boat, minecart, etc.)
     * Exact implementation from PVP EntityUtil.isVehicle()
     * @param entity Entity to check
     * @return true if entity is a vehicle
     */
    public static boolean isVehicle(Entity entity) {
        return entity instanceof BoatEntity ||
            entity instanceof MinecartEntity ||
            entity instanceof FurnaceMinecartEntity ||
            entity instanceof ChestMinecartEntity;
    }

    public enum EntityCategory {
        PLAYER,
        MONSTER,
        NEUTRAL,
        PASSIVE,
        OTHER
    }
}
