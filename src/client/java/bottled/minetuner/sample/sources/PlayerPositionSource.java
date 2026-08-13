package bottled.minetuner.sample.sources;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.sample.Cadence;
import bottled.minetuner.sample.SamplingContext;
import bottled.minetuner.sample.StatSource;

/** Player position, facing, orientation, and horizontal speed. */
public final class PlayerPositionSource implements StatSource {
    @Override
    public String id() {
        return "player_position";
    }

    @Override
    public Cadence cadence() {
        return Cadence.PER_FRAME;
    }

    @Override
    public boolean isAvailable(SamplingContext ctx) {
        return ctx.hasPlayer();
    }

    @Override
    public void sample(SamplingContext ctx) {
        var player = ctx.player();
        MineTunerDataHolder.playerX = player.getX();
        MineTunerDataHolder.playerY = player.getY();
        MineTunerDataHolder.playerZ = player.getZ();

        // Yaw normalized to [0, 360).
        float yaw = ((player.getYRot() % 360) + 360) % 360;
        MineTunerDataHolder.playerYaw = yaw;
        MineTunerDataHolder.playerPitch = player.getXRot();

        // Facing.
        if (yaw < 22.5f) MineTunerDataHolder.facingName = "S";
        else if (yaw < 67.5f) MineTunerDataHolder.facingName = "SW";
        else if (yaw < 112.5f) MineTunerDataHolder.facingName = "W";
        else if (yaw < 157.5f) MineTunerDataHolder.facingName = "NW";
        else if (yaw < 202.5f) MineTunerDataHolder.facingName = "N";
        else if (yaw < 247.5f) MineTunerDataHolder.facingName = "NE";
        else if (yaw < 292.5f) MineTunerDataHolder.facingName = "E";
        else if (yaw < 337.5f) MineTunerDataHolder.facingName = "SE";
        else MineTunerDataHolder.facingName = "S";

        // Horizontal speed.
        double dx = player.getDeltaMovement().x;
        double dz = player.getDeltaMovement().z;
        MineTunerDataHolder.speedBps = (float) (Math.sqrt(dx * dx + dz * dz) * 20.0);
    }
}
