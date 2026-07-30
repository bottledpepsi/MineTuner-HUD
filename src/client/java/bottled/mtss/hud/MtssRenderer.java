package bottled.mtss.hud;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.ArrayList;
import java.util.List;

public class MtssRenderer {

    // ── Per-frame line cache ──────────────────────────────────────────────────
    // buildLines is called both here (render) and in the GUI (drawList + getListBounds).
    // The cache avoids redundant string building within the same frame.

    public record LineCache(List<String> lines, List<Integer> colors) {
        public int boxW(net.minecraft.client.gui.Font font) {
            return lines.stream().mapToInt(font::width).max().orElse(0) + 4;
        }
        public int boxH(net.minecraft.client.gui.Font font) {
            return (font.lineHeight + 1) * lines.size() + 3;
        }
    }

    private static final java.util.Map<Integer, LineCache> FRAME_CACHE = new java.util.HashMap<>();
    /**
     * Incremented by {@link #tickCache()} at the start of each render call.
     * The GUI screen calls tickCache() too so both share the same frame budget.
     */
    private static long cacheGeneration = 0;
    private static long lastCacheGeneration = -1;

    /** Advance the cache generation — call once per render frame. */
    public static void tickCache() {
        if (cacheGeneration != lastCacheGeneration) {
            FRAME_CACHE.clear();
            lastCacheGeneration = cacheGeneration;
        }
        cacheGeneration++;
    }

    /** Returns cached lines for this list, building them if needed this frame. */
    public static LineCache getCachedLines(MtssConfig.StatListConfig cfg) {
        return FRAME_CACHE.computeIfAbsent(cfg.id, id -> {
            List<String>  lines  = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();
            buildLines(cfg, lines, colors);
            return new LineCache(lines, colors);
        });
    }

    // ── Position ──────────────────────────────────────────────────────────────

    public static int[] getPosition(MtssConfig.StatListConfig cfg,
                                    int screenW, int screenH, int boxW, int boxH) {
        int x, y;
        switch (cfg.anchorCorner) {
            case TOP_RIGHT    -> { x = screenW - boxW - cfg.anchorDx; y = cfg.anchorDy; }
            case BOTTOM_LEFT  -> { x = cfg.anchorDx;                  y = screenH - boxH - cfg.anchorDy; }
            case BOTTOM_RIGHT -> { x = screenW - boxW - cfg.anchorDx; y = screenH - boxH - cfg.anchorDy; }
            default           -> { x = cfg.anchorDx;                  y = cfg.anchorDy; } // TOP_LEFT
        }
        // Snap overrides beat the corner anchor on the snapped axis
        int cx = screenW / 2;
        int cy = screenH / 2;
        x = switch (cfg.snapX) {
            case LEFT_ON_CENTER   -> cx;
            case CENTER_ON_CENTER -> cx - boxW / 2;
            case RIGHT_ON_CENTER  -> cx - boxW;
            default -> x;
        };
        y = switch (cfg.snapY) {
            case TOP_ON_CENTER    -> cy;
            case CENTER_ON_CENTER -> cy - boxH / 2;
            case BOTTOM_ON_CENTER -> cy - boxH;
            default -> y;
        };
        x = Math.max(0, Math.min(screenW - boxW, x));
        y = Math.max(0, Math.min(screenH - boxH, y));
        return new int[]{x, y};
    }

    // ── Line building ─────────────────────────────────────────────────────────

    public static void buildLines(MtssConfig.StatListConfig cfg,
                                  List<String> lines, List<Integer> colors) {
        for (MtssConfig.Stat stat : cfg.getVisibleStats()) {
            String text  = null;
            int    color = 0xFFFFFFFF;
            int    decimals = cfg.getStatSettings(stat).decimals;
            switch (stat) {
                case TPS   -> { text = MtssDataHolder.getFormattedTps(decimals);   color = MtssDataHolder.getTpsColor(); }
                case MSPT  -> { text = MtssDataHolder.getFormattedMspt(decimals); /* empty on remote servers */ }
                case FPS   -> { text = MtssDataHolder.getFormattedFps();      color = MtssDataHolder.getFpsColor(); }
                case PING  -> { text = MtssDataHolder.getFormattedPing();     color = MtssDataHolder.getPingColor(); }
                case MEMORY-> { text = MtssDataHolder.getFormattedMem();      color = MtssDataHolder.getMemColor(); }
                case CPU   -> { text = MtssDataHolder.getFormattedCpu(decimals);   color = MtssDataHolder.getCpuColor(); }
                case ENTITIES         -> { text = MtssDataHolder.getFormattedEntities(); }
                case CHUNKS           -> { text = MtssDataHolder.getFormattedChunks(); }
                case RENDERED_SECTIONS-> { text = MtssDataHolder.getFormattedRendered(); }
                case COORDS           -> { text = MtssDataHolder.getFormattedCoords(); }
                case FACING           -> { text = MtssDataHolder.getFormattedFacing(); }
                case SPEED            -> { text = MtssDataHolder.getFormattedSpeed(decimals); color = MtssDataHolder.getSpeedColor(); }
                case GC_TIME          -> { text = MtssDataHolder.getFormattedGcTime(); }
                case BIOME            -> { text = MtssDataHolder.getFormattedBiome(); }
                case LIGHT_LEVEL      -> { text = MtssDataHolder.getFormattedLight(); }
                case DIMENSION        -> { text = MtssDataHolder.getFormattedDimension(); }
            }
            if (text == null || text.isEmpty()) continue;
            // Strip prefix ("Label: ") when showPrefix is disabled for this stat
            if (!cfg.getStatSettings(stat).showPrefix) {
                int sep = text.indexOf(": ");
                if (sep >= 0) text = text.substring(sep + 2);
            }
            // Per-list color override replaces threshold coloring entirely
            if (cfg.useCustomColor) color = cfg.overrideColor;
            lines.add(text);
            colors.add(color);
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.getDebugOverlay().showDebugScreen()) return;
        if (mc.getConnection() == null) return;
        if (mc.gui.screen() instanceof bottled.mtss.gui.MtssGuiScreen) return;

        // Advance frame cache so getCachedLines() is fresh this frame
        tickCache();

        // ── Data collection ──────────────────────────────────────────────────
        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            MtssDataHolder.mspt =
                    mc.getSingleplayerServer().getAverageTickTimeNanos() / 1_000_000.0f;
        } else {
            MtssDataHolder.mspt = -1f; // not available on remote servers
        }
        MtssDataHolder.fps = mc.getFps();

        ClientPacketListener conn = mc.getConnection();
        if (mc.player != null && conn != null) {
            PlayerInfo info = conn.getPlayerInfo(mc.player.getUUID());
            MtssDataHolder.ping = info != null ? info.getLatency() : -1;
        }

        if (mc.level != null) {
            MtssDataHolder.entityCount  = mc.level.getEntityCount();
            MtssDataHolder.loadedChunks = mc.level.getChunkSource().getLoadedChunksCount();
            MtssDataHolder.dimensionName = mc.level.dimension().identifier().getPath();
        }
        if (mc.player != null) {
            MtssDataHolder.playerX = mc.player.getX();
            MtssDataHolder.playerY = mc.player.getY();
            MtssDataHolder.playerZ = mc.player.getZ();
            if (mc.level != null) {
                net.minecraft.core.BlockPos pos = mc.player.blockPosition();
                MtssDataHolder.lightLevel = mc.level.getMaxLocalRawBrightness(pos);
                var biomeHolder = mc.level.getBiome(pos);
                MtssDataHolder.biomeName = biomeHolder.unwrapKey()
                        .map(key -> key.identifier().getPath())
                        .orElse("?");
            }
            // Direction enum: NORTH/SOUTH/EAST/WEST + intercardinals from yaw
            float yaw = ((mc.player.getYRot() % 360) + 360) % 360;
            if      (yaw <  22.5f)  MtssDataHolder.facingName = "S";
            else if (yaw <  67.5f)  MtssDataHolder.facingName = "SW";
            else if (yaw < 112.5f)  MtssDataHolder.facingName = "W";
            else if (yaw < 157.5f)  MtssDataHolder.facingName = "NW";
            else if (yaw < 202.5f)  MtssDataHolder.facingName = "N";
            else if (yaw < 247.5f)  MtssDataHolder.facingName = "NE";
            else if (yaw < 292.5f)  MtssDataHolder.facingName = "E";
            else if (yaw < 337.5f)  MtssDataHolder.facingName = "SE";
            else                     MtssDataHolder.facingName = "S";
            // Horizontal speed: delta movement is per-tick, × 20 = blocks/sec
            double dx = mc.player.getDeltaMovement().x;
            double dz = mc.player.getDeltaMovement().z;
            MtssDataHolder.speedBps = (float)(Math.sqrt(dx * dx + dz * dz) * 20.0);
        }
        if (mc.levelRenderer != null) {
            MtssDataHolder.renderedSections = mc.levelExtractor.countRenderedSections();
        }

        MtssDataHolder.updateFastMetrics();
        MtssDataHolder.updateSlowMetrics();

        // ── Render each stat list (uses frame cache) ─────────────────────────
        MtssConfig root = MtssConfig.getInstance();
        var font = mc.font;

        for (MtssConfig.StatListConfig listCfg : root.lists) {
            LineCache cache = getCachedLines(listCfg);
            List<String>  lines  = cache.lines();
            List<Integer> colors = cache.colors();
            if (lines.isEmpty()) continue;

            float scale = listCfg.textScale <= 0f ? 1f : listCfg.textScale;
            int unscaledW = cache.boxW(font);
            int unscaledH = cache.boxH(font);
            int lineH = font.lineHeight + 1;

            // Position math happens in screen-pixel space, so use the scaled box size
            // for layout — otherwise a scaled-up list could overlap the screen edge or
            // other lists at its anchor point.
            int boxW = Math.round(unscaledW * scale);
            int boxH = Math.round(unscaledH * scale);

            int[] pos = getPosition(listCfg, graphics.guiWidth(), graphics.guiHeight(), boxW, boxH);
            int x = pos[0], y = pos[1];

            if (listCfg.showBackground) {
                graphics.fill(x - 1, y - 1, x + boxW + 1, y + boxH + 1, 0x90000000);
            }
            boolean shadow = listCfg.textShadow;

            if (scale == 1f) {
                for (int i = 0; i < lines.size(); i++) {
                    graphics.text(font, lines.get(i), x + 2, y + 2 + i * lineH, colors.get(i), shadow);
                }
            } else {
                // Scale around the box's top-left corner: translate to (x, y) in screen space,
                // scale, then draw at the unscaled local offsets.
                var matrices = graphics.pose();
                matrices.pushMatrix();
                matrices.translate(x, y);
                matrices.scale(scale, scale);
                for (int i = 0; i < lines.size(); i++) {
                    graphics.text(font, lines.get(i), 2, 2 + i * lineH, colors.get(i), shadow);
                }
                matrices.popMatrix();
            }
        }
    }
}
