package bottled.mtss.sample.sources;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.sample.Cadence;
import bottled.mtss.sample.SamplingContext;
import bottled.mtss.sample.StatSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Player vital stats. */
public final class PlayerVitalsSource implements StatSource {
    /** Lowercase name for a { GameType}, matching vanilla's own lowercase game-mode. */
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

        MtssDataHolder.health = player.getHealth();
        MtssDataHolder.maxHealth = player.getMaxHealth();
        MtssDataHolder.hunger = player.getFoodData().getFoodLevel();
        MtssDataHolder.saturation = player.getFoodData().getSaturationLevel();
        MtssDataHolder.armor = player.getArmorValue();
        MtssDataHolder.air = player.getAirSupply();
        MtssDataHolder.maxAir = player.getMaxAirSupply();

        MtssDataHolder.xpLevel = player.experienceLevel;
        MtssDataHolder.xpProgress = player.experienceProgress;

        MtssDataHolder.gameMode = ctx.mc().gameMode != null && ctx.mc().gameMode.getPlayerMode() != null
                ? gameTypeName(ctx.mc().gameMode.getPlayerMode())
                : "";

        MtssDataHolder.selectedSlot = player.getInventory().getSelectedSlot();
        ItemStack held = player.getMainHandItem();
        MtssDataHolder.heldItemName = held.isEmpty() ? "" : held.getHoverName().getString();

        // Vertical speed.
        // horizontal-only speed.
        MtssDataHolder.verticalSpeedBps = (float) (player.getDeltaMovement().y * 20.0);
    }
}
