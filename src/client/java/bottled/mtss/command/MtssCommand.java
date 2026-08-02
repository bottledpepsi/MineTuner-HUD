package bottled.mtss.command;

import bottled.mtss.gui.MtssGuiScreen;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class MtssCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("mtss")
            // Opens the GUI
            .then(literal("gui").executes(ctx -> {
                Minecraft.getInstance().schedule(() ->
                        Minecraft.getInstance().gui.setScreen(new MtssGuiScreen()));
                return 1;
            }))
        );
    }
}
