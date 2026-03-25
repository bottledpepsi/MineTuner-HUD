package bottled.perfhud;

import bottled.perfhud.command.PerfHudCommand;
import bottled.perfhud.config.PerfHudConfig;
import bottled.perfhud.hud.PerfHudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class PerfHudClient implements ClientModInitializer {

    private static final PerfHudRenderer RENDERER = new PerfHudRenderer();

    @Override
    public void onInitializeClient() {
        PerfHudConfig.getInstance();

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("perfhud", "overlay"),
                RENDERER::render
        );

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                PerfHudCommand.register(dispatcher));
    }
}
