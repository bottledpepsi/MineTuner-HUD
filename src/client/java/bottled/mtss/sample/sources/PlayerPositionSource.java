package bottled.mtss.sample.sources;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.sample.Cadence;
import bottled.mtss.sample.SamplingContext;
import bottled.mtss.sample.StatSource;

/**
 * Player position, facing, and horizontal speed. Batched into one source
 * rather than split per-field: all three share the {@code ctx.hasPlayer()}
 * precondition and are always read together in the original inline block.
 */
public final class PlayerPositionSource implements StatSource {
    @Override public String id() { return "player_position"; }
    @Override public Cadence cadence() { return Cadence.PER_FRAME; }
    @Override public boolean isAvailable(SamplingContext ctx) { return ctx.hasPlayer(); }

    @Override public void sample(SamplingContext ctx) {
        var player = ctx.player();
        MtssDataHolder.playerX = player.getX();
        MtssDataHolder.playerY = player.getY();
        MtssDataHolder.playerZ = player.getZ();

        // Facing: NORTH/SOUTH/EAST/WEST + intercardinals from yaw
        float yaw = ((player.getYRot() % 360) + 360) % 360;
        if      (yaw <  22.5f)  MtssDataHolder.facingName = "S";
        else if (yaw <  67.5f)  MtssDataHolder.facingName = "SW";
        else if (yaw < 112.5f)  MtssDataHolder.facingName = "W";
        else if (yaw < 157.5f)  MtssDataHolder.facingName = "NW";
        else if (yaw < 202.5f)  MtssDataHolder.facingName = "N";
        else if (yaw < 247.5f)  MtssDataHolder.facingName = "NE";
        else if (yaw < 292.5f)  MtssDataHolder.facingName = "E";
        else if (yaw < 337.5f)  MtssDataHolder.facingName = "SE";
        else                     MtssDataHolder.facingName = "S";

        // Horizontal speed: per-tick delta movement × 20 ticks/sec = blocks/sec
        double dx = player.getDeltaMovement().x;
        double dz = player.getDeltaMovement().z;
        MtssDataHolder.speedBps = (float) (Math.sqrt(dx * dx + dz * dz) * 20.0);
    }
}
