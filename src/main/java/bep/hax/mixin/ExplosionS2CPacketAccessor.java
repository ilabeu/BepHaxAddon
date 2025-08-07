package bep.hax.mixin;

import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin for ExplosionS2CPacket
 * For Minecraft 1.21.4 - using invoker for getKnockback method
 */
@Mixin(ExplosionS2CPacket.class)
public interface ExplosionS2CPacketAccessor {
    // In 1.21.4, ExplosionS2CPacket has a getKnockback() method that returns Optional<Vec3d>
    // We'll use the public method directly instead of accessing private fields
}