package bottled.minetuner;

import bottled.minetuner.command.MineTunerCommand;
import bottled.minetuner.config.MineTunerConfig;
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
        });
    }
}
