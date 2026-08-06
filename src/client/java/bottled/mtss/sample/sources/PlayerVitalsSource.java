package bottled.mtss.sample.sources;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.sample.Cadence;
import bottled.mtss.sample.SamplingContext;
import bottled.mtss.sample.StatSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/**
 * Player vital stats: health, hunger, saturation, armor, air, XP, game
 * mode, and hotbar/held-item state. Batched into one source since they all
 * share the {@code ctx.hasPlayer()} precondition and are all cheap direct
 * reads off {@code LocalPlayer}/{@code Abilities} — no per-field source is
 * worth the extra registration overhead here.
 */
public final class PlayerVitalsSource implements StatSource {
    @Override public String id() { return "player_vitals"; }
    @Override public Cadence cadence() { return Cadence.PER_FRAME; }
    @Override public boolean isAvailable(SamplingContext ctx) { return ctx.hasPlayer(); }

    @Override public void sample(SamplingContext ctx) {
        var player = ctx.player();

        MtssDataHolder.health    = player.getHealth();
        MtssDataHolder.maxHealth = player.getMaxHealth();
        MtssDataHolder.hunger    = player.getFoodData().getFoodLevel();
        MtssDataHolder.saturation = player.getFoodData().getSaturationLevel();
        MtssDataHolder.armor     = player.getArmorValue();
        MtssDataHolder.air       = player.getAirSupply();
        MtssDataHolder.maxAir    = player.getMaxAirSupply();

        MtssDataHolder.xpLevel    = player.experienceLevel;
        MtssDataHolder.xpProgress = player.experienceProgress;

        MtssDataHolder.gameMode = ctx.mc().gameMode != null && ctx.mc().gameMode.getPlayerMode() != null
                ? gameTypeName(ctx.mc().gameMode.getPlayerMode())
                : "";

        MtssDataHolder.selectedSlot = player.getInventory().getSelectedSlot();
        ItemStack held = player.getMainHandItem();
        MtssDataHolder.heldItemName = held.isEmpty() ? "" : held.getHoverName().getString();

        // Vertical speed: signed blocks/sec, unlike PlayerPositionSource's
        // horizontal-only speed. Positive = rising, negative = falling.
        MtssDataHolder.verticalSpeedBps = (float) (player.getDeltaMovement().y * 20.0);
    }

    /**
     * Lowercase name for a {@link GameType}, matching vanilla's own
     * lowercase game-mode identifiers (survival/creative/adventure/
     * spectator) rather than the enum constant's own SCREAMING_CASE name.
     */
    private static String gameTypeName(GameType type) {
        return type.name().toLowerCase(java.util.Locale.ROOT);
    }
}
