package bottled.minetuner.sample.sources;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.sample.Cadence;
import bottled.minetuner.sample.SamplingContext;
import bottled.minetuner.sample.StatSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Player vital stats. */
public final class PlayerVitalsSource implements StatSource {
    /** Lowercase name for a {@link GameType}, matching vanilla's own lowercase
     *  naming convention for game modes (as used by e.g. the /gamemode command). */
    private static String gameTypeName(GameType type) {
        return type.name().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public String id() {
        return "player_vitals";
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

        MineTunerDataHolder.health = player.getHealth();
        MineTunerDataHolder.maxHealth = player.getMaxHealth();
        MineTunerDataHolder.hunger = player.getFoodData().getFoodLevel();
        MineTunerDataHolder.saturation = player.getFoodData().getSaturationLevel();
        MineTunerDataHolder.armor = player.getArmorValue();
        MineTunerDataHolder.air = player.getAirSupply();
        MineTunerDataHolder.maxAir = player.getMaxAirSupply();

        MineTunerDataHolder.xpLevel = player.experienceLevel;
        MineTunerDataHolder.xpProgress = player.experienceProgress;

        MineTunerDataHolder.gameMode = ctx.mc().gameMode != null && ctx.mc().gameMode.getPlayerMode() != null
                ? gameTypeName(ctx.mc().gameMode.getPlayerMode())
                : "";

        MineTunerDataHolder.selectedSlot = player.getInventory().getSelectedSlot();
        ItemStack held = player.getMainHandItem();
        MineTunerDataHolder.heldItemName = held.isEmpty() ? "" : held.getHoverName().getString();

        // Vertical speed lives here (not in PlayerPositionSource, which computes
        // horizontal-only speed from dx/dz) since it's grouped with the other
        // per-frame player-vitals reads rather than the position/facing block.
        MineTunerDataHolder.verticalSpeedBps = (float) (player.getDeltaMovement().y * 20.0);
    }
}
