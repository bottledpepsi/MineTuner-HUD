package bottled.minetuner.sample.sources;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Small shared math/helpers for the stat sources. */
final class SourceMath {

    private SourceMath() {
    }

    /** The delta movement to use for speed/moving readings: the VEHICLE's when the
     *  player is riding, else the player's own. A rider's client-side delta movement
     *  doesn't track vehicle motion, so without this fallback a boat/minecart/horse
     *  passenger moving at full speed reads ~0 bps and "not moving". */
    static Vec3 effectiveDeltaMovement(Entity player) {
        Entity vehicle = player.getVehicle();
        return vehicle != null ? vehicle.getDeltaMovement() : player.getDeltaMovement();
    }
}
