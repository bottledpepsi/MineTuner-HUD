package bottled.mtss.sample.sources;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.sample.Cadence;
import bottled.mtss.sample.SamplingContext;
import bottled.mtss.sample.StatSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** What's under the crosshair (block or entity), plus whether the player is. */
public final class TargetingSource implements StatSource {
    /** Below this speed, floating-point jitter from slope/edge physics can make a. */
    private static final double MOVING_EPSILON = 0.01;

    @Override
    public String id() {
        return "targeting";
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
        HitResult hit = ctx.mc().hitResult;
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            var state = ctx.hasLevel() ? ctx.level().getBlockState(blockHit.getBlockPos()) : null;
            MtssDataHolder.lookingAtKind = "block";
            MtssDataHolder.lookingAtName = state != null
                    ? state.getBlock().getName().getString()
                    : "";
        } else if (hit instanceof EntityHitResult entityHit && hit.getType() == HitResult.Type.ENTITY) {
            Entity target = entityHit.getEntity();
            MtssDataHolder.lookingAtKind = "entity";
            MtssDataHolder.lookingAtName = target.getName().getString();
        } else {
            MtssDataHolder.lookingAtKind = "";
            MtssDataHolder.lookingAtName = "";
        }

        var player = ctx.player();
        double dx = player.getDeltaMovement().x;
        double dz = player.getDeltaMovement().z;
        MtssDataHolder.isMoving = Math.sqrt(dx * dx + dz * dz) * 20.0 > MOVING_EPSILON;
    }
}
