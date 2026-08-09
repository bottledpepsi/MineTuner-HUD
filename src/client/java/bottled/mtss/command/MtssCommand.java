package bottled.mtss.command;

import bottled.mtss.config.cloth.MtssClothConfigScreen;
import bottled.mtss.gui.MtssGuiScreen;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class MtssCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("mtss")
                // Opens the custom in-game editor (drag lists, right-click to configure).
                .then(literal("gui").executes(ctx -> {
                    Minecraft.getInstance().schedule(() ->
                            Minecraft.getInstance().gui.setScreen(new MtssGuiScreen()));
                    return 1;
                }))
                // Opens the Cloth Config screen.
                // alternative covering every mtss.json field at once, including.
                // ones with no control anywhere in the custom GUI (hardware.
                // sensor settings, GUI panel sizing/tuning).
                // ModMenu's mod list if the player has it installed.
                // bottled.mtss.config.cloth.MtssModMenuIntegration.
                // command works whether or not ModMenu is present.
                .then(literal("config").executes(ctx -> {
                    Minecraft.getInstance().schedule(() -> {
                        var mc = Minecraft.getInstance();
                        mc.gui.setScreen(MtssClothConfigScreen.build(mc.gui.screen()));
                    });
                    return 1;
                }))
        );
    }
}

