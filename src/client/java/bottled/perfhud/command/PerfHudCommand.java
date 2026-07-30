package bottled.perfhud.command;

import bottled.perfhud.gui.PerfHudGuiScreen;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class PerfHudCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("perfhud")
            // /perfhud gui — open the GUI
            .then(literal("gui").executes(ctx -> {
                Minecraft.getInstance().schedule(() ->
                        Minecraft.getInstance().gui.setScreen(new PerfHudGuiScreen()));
                return 1;
            }))
        );
    }
}
