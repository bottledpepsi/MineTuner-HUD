package bottled.minetuner;

import bottled.minetuner.benchmark.BenchmarkSession;
import bottled.minetuner.command.MineTunerCommand;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.gui.BenchmarkGuiScreen;
import bottled.minetuner.gui.MineTunerGuiScreen;
import bottled.minetuner.hud.MineTunerRenderer;
import bottled.minetuner.sample.HardwareSensorPoller;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class MineTunerClient implements ClientModInitializer {

    private static final MineTunerRenderer RENDERER = new MineTunerRenderer();

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MineTunerMod.MOD_ID, "main"));

    /** Opens the editor. */
    private static final KeyMapping OPEN_GUI_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.minetuner.open_gui",
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_H,
                    CATEGORY
            ));

    /** Shows/hides the overlay without opening the editor. */
    private static final KeyMapping TOGGLE_OVERLAY_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.minetuner.toggle_overlay",
                    InputConstants.Type.KEYSYM,
                    InputConstants.UNKNOWN.getValue(),
                    CATEGORY
            ));

    /** Starts a fresh benchmark run, discarding any previous run's results — RTSS's F9
     *  "start/reset" semantics. Unbound by default, matching TOGGLE_OVERLAY_KEY's own
     *  precedent for an opt-in, power-user action that shouldn't risk surprising a user
     *  who hasn't deliberately bound it (see BenchmarkGuiScreen/BenchmarkSession docs). */
    private static final KeyMapping BENCHMARK_START_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.minetuner.benchmark_start",
                    InputConstants.Type.KEYSYM,
                    InputConstants.UNKNOWN.getValue(),
                    CATEGORY
            ));

    /** Stops the active benchmark and freezes its results — RTSS's Shift+F9
     *  "stop/freeze" semantics. Unbound by default, same reasoning as BENCHMARK_START_KEY. */
    private static final KeyMapping BENCHMARK_STOP_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.minetuner.benchmark_stop",
                    InputConstants.Type.KEYSYM,
                    InputConstants.UNKNOWN.getValue(),
                    CATEGORY
            ));

    /** Opens the dedicated Benchmark GUI (control + results panel). Unbound by default,
     *  same reasoning as BENCHMARK_START_KEY/BENCHMARK_STOP_KEY — /minetuner benchmark
     *  remains available as an alternative entry point either way (see MineTunerCommand). */
    private static final KeyMapping OPEN_BENCHMARK_GUI_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.minetuner.open_benchmark_gui",
                    InputConstants.Type.KEYSYM,
                    InputConstants.UNKNOWN.getValue(),
                    CATEGORY
            ));

    @Override
    public void onInitializeClient() {
        MineTunerConfig.getInstance();

        // Opt-in only.
        // and is a no-op (doesn't even create the thread) when it's false,.
        // so a user who hasn't turned this on sees zero behavior change.
        // no thread, no startup cost, no network activity.
        HardwareSensorPoller.startIfEnabled();

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("minetuner", "overlay"),
                RENDERER::render
        );

        // Frametime is sampled here
        LevelRenderEvents.START_MAIN.register(context -> MineTunerDataHolder.recordFrametime(System.nanoTime()));

        // Session Avg/Min/Max FPS aggregates reset on every world join/disconnect, so a HUD
        // reading like "Min FPS: 12" from one world/server doesn't silently carry over into a
        // completely different one you've since joined. Both fire for both singleplayer world
        // entry/exit and multiplayer server connect/disconnect — there's no separate "world"
        // vs "server" distinction to make here.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            MineTunerDataHolder.resetSessionFpsStats();
            MineTunerDataHolder.resetPercentileLowFps();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            MineTunerDataHolder.resetSessionFpsStats();
            MineTunerDataHolder.resetPercentileLowFps();
        });

        // Deliberately NOT resetting BenchmarkSession here, unlike the session FPS/
        // percentile-low stats just above. Those two exist specifically to describe "this
        // world/server, right now" and would mislead if they silently carried over into a
        // different one. A benchmark result is different: it's a frozen, user-triggered
        // snapshot the player explicitly asked to keep ("Results must remain frozen after
        // Stop until another benchmark is started/reset" — see BenchmarkSession's own
        // doc), and a world/server disconnect (including an unintentional one, e.g. a
        // brief connection drop) is not itself a user request to discard it. Starting a
        // new benchmark (BENCHMARK_START_KEY) remains the one action that discards a
        // previous run's results.

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                MineTunerCommand.register(dispatcher));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_GUI_KEY.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new MineTunerGuiScreen());
                }
            }
            while (TOGGLE_OVERLAY_KEY.consumeClick()) {
                MineTunerConfig cfg = MineTunerConfig.getInstance();
                cfg.overlayEnabled = !cfg.overlayEnabled;
                cfg.save();
                String key = cfg.overlayEnabled ? "minetuner.toggle.on" : "minetuner.toggle.off";
                if (client.gui != null) {
                    client.gui.hud.setOverlayMessage(Component.translatable(key), false);
                }
            }
            while (BENCHMARK_START_KEY.consumeClick()) {
                BenchmarkSession.start();
                if (client.gui != null) {
                    client.gui.hud.setOverlayMessage(
                            Component.translatable("minetuner.benchmark.started"), false);
                }
            }
            while (BENCHMARK_STOP_KEY.consumeClick()) {
                // No-op inside BenchmarkSession#stop() if nothing was RECORDING (e.g. a
                // stray Stop press while IDLE/already STOPPED) — only show the "stopped"
                // confirmation if this press actually froze a run, so an accidental Stop
                // press with nothing running doesn't claim a benchmark just finished.
                boolean wasRecording = BenchmarkSession.isRecording();
                BenchmarkSession.stop();
                if (wasRecording && client.gui != null) {
                    String avgFps = String.format("%.1f", BenchmarkSession.finalAvgFps());
                    client.gui.hud.setOverlayMessage(
                            Component.translatable("minetuner.benchmark.stopped", avgFps), false);
                }
            }
            while (OPEN_BENCHMARK_GUI_KEY.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new BenchmarkGuiScreen());
                }
            }
        });
    }
}
