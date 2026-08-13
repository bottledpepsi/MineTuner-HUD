package bottled.minetuner.command;

import bottled.minetuner.config.cloth.MineTunerClothConfigScreen;
import bottled.minetuner.gui.MineTunerGuiScreen;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class MineTunerCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("minetuner")
                // Opens the custom in-game editor (drag lists, right-click to configure).
                .then(literal("gui").executes(ctx -> {
                    Minecraft.getInstance().schedule(() ->
                            Minecraft.getInstance().gui.setScreen(new MineTunerGuiScreen()));
                    return 1;
                }))
                // Opens the Cloth Config screen — a text-field-based
                // alternative covering every minetuner.json field at once, including
                // ones with no control anywhere in the custom GUI (hardware
                // sensor settings, GUI panel sizing/tuning). Also reachable from
                // ModMenu's mod list if the player has it installed (see
                // bottled.minetuner.config.cloth.MineTunerModMenuIntegration), but this
                // command works whether or not ModMenu is present.
                .then(literal("config").executes(ctx -> {
                    Minecraft.getInstance().schedule(() -> {
                        var mc = Minecraft.getInstance();
                        mc.gui.setScreen(MineTunerClothConfigScreen.build(mc.gui.screen()));
                    });
                    return 1;
                }))
        );
    }
}

