package bottled.minetuner.sample.sources;

import bottled.minetuner.MineTunerDataHolder;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.sample.Cadence;
import bottled.minetuner.sample.SamplingContext;
import bottled.minetuner.sample.StatSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.Set;

/** Player vital stats. */
public final class PlayerVitalsSource implements StatSource {
    /** Lowercase name for a {@link GameType}, matching vanilla's own lowercase
     *  naming convention for game modes (as used by e.g. the /gamemode command). */
    private static String gameTypeName(GameType type) {
        return type.name().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Converting the hover-name Component to a String allocates, so it is cached by
     * (slot, item) and refreshed every {@link #NAME_REFRESH_TICKS} ticks to pick up a rename.
     */
    private static final int NAME_REFRESH_TICKS = 20;
    private int cachedSlot = -1;
    private Object cachedItem;
    private String cachedName = "";
    private int ticksSinceNameRefresh = 0;

    private String heldItemName(ItemStack held, int slot) {
        if (held.isEmpty()) {
            cachedItem = null;
            cachedName = "";
            return "";
        }
        ticksSinceNameRefresh++;
        if (slot != cachedSlot || held.getItem() != cachedItem || ticksSinceNameRefresh >= NAME_REFRESH_TICKS) {
            cachedSlot = slot;
            cachedItem = held.getItem();
            cachedName = held.getHoverName().getString();
            ticksSinceNameRefresh = 0;
        }
        return cachedName;
    }

    @Override
    public String id() {
        return "player_vitals";
    }

    @Override
    public Set<MineTunerConfig.Stat> feeds() {
        return Set.of(MineTunerConfig.Stat.HEALTH, MineTunerConfig.Stat.HUNGER, MineTunerConfig.Stat.SATURATION, MineTunerConfig.Stat.ARMOR, MineTunerConfig.Stat.AIR, MineTunerConfig.Stat.XP_LEVEL, MineTunerConfig.Stat.XP_PROGRESS, MineTunerConfig.Stat.GAME_MODE, MineTunerConfig.Stat.SELECTED_SLOT, MineTunerConfig.Stat.HELD_ITEM, MineTunerConfig.Stat.VERTICAL_SPEED);
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
        MineTunerDataHolder.heldItemName = heldItemName(held, player.getInventory().getSelectedSlot());

        // Vertical speed lives here (not in PlayerPositionSource, which computes
        // horizontal-only speed from dx/dz) since it's grouped with the other
        // per-frame player-vitals reads rather than the position/facing block.
        MineTunerDataHolder.verticalSpeedBps = (float) (player.getDeltaMovement().y * 20.0);
    }
}
