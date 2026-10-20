package bottled.minetuner.sample.sources;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.sample.Cadence;
import bottled.minetuner.sample.SamplingContext;
import bottled.minetuner.sample.StatSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Set;

/** What's under the crosshair (block or entity), plus whether the player is. */
public final class TargetingSource implements StatSource {
    /** Below this speed, floating-point jitter from slope/edge physics can make a. */
    private static final double MOVING_EPSILON = 0.01;

    private Object cachedBlockState;
    private String cachedBlockName = "";
    private int cachedEntityId = Integer.MIN_VALUE;
    private String cachedEntityName = "";

    @Override
    public String id() {
        return "targeting";
    }

    @Override
    public Set<MineTunerConfig.Stat> feeds() {
        return Set.of(MineTunerConfig.Stat.LOOKING_AT, MineTunerConfig.Stat.MOVING);
    }

    @Override
    public Cadence cadence() {
        return Cadence.PER_TICK;
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
            MineTunerDataHolder.lookingAtKind = "block";
            if (state == null) {
                MineTunerDataHolder.lookingAtName = "";
            } else {
                // BlockStates are interned, so identity is a sound cache key for the block name.
                if (state != cachedBlockState) {
                    cachedBlockState = state;
                    cachedBlockName = state.getBlock().getName().getString();
                }
                MineTunerDataHolder.lookingAtName = cachedBlockName;
            }
        } else if (hit instanceof EntityHitResult entityHit && hit.getType() == HitResult.Type.ENTITY) {
            Entity target = entityHit.getEntity();
            MineTunerDataHolder.lookingAtKind = "entity";
            // Cached per targeted entity; a rename of the same entity shows once the
            // crosshair leaves and re-enters it.
            if (target.getId() != cachedEntityId) {
                cachedEntityId = target.getId();
                cachedEntityName = target.getName().getString();
            }
            MineTunerDataHolder.lookingAtName = cachedEntityName;
        } else {
            MineTunerDataHolder.lookingAtKind = "";
            MineTunerDataHolder.lookingAtName = "";
            cachedEntityId = Integer.MIN_VALUE;
        }

        var player = ctx.player();
        double dx = player.getDeltaMovement().x;
        double dz = player.getDeltaMovement().z;
        MineTunerDataHolder.isMoving = Math.sqrt(dx * dx + dz * dz) * 20.0 > MOVING_EPSILON;
    }
}
