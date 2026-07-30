package bottled.mtss;

import bottled.mtss.command.MtssCommand;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.gui.MtssGuiScreen;
import bottled.mtss.hud.MtssRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class MtssClient implements ClientModInitializer {

    private static final MtssRenderer RENDERER = new MtssRenderer();

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MtssMod.MOD_ID, "main"));

    /** Default keybind to open the MineTuner Statistics Server editor — no default GLFW key, bind it in Controls. */
    private static final KeyMapping OPEN_GUI_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.mtss.open_gui",
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_H,
                    CATEGORY
            ));

    @Override
    public void onInitializeClient() {
        MtssConfig.getInstance();

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("mtss", "overlay"),
                RENDERER::render
        );

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                MtssCommand.register(dispatcher));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_GUI_KEY.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new MtssGuiScreen());
                }
            }
        });
    }
}
