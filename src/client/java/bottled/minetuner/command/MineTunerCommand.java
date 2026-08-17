package bottled.minetuner.command;

import bottled.minetuner.config.cloth.MineTunerClothConfigScreen;
import bottled.minetuner.gui.BenchmarkGuiScreen;
import bottled.minetuner.gui.MineTunerGuiScreen;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class MineTunerCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("minetuner")
                // Opens the custom HUD editor.
                .then(literal("gui").executes(ctx -> {
                    Minecraft.getInstance().schedule(() ->
                            Minecraft.getInstance().gui.setScreen(new MineTunerGuiScreen()));
                    return 1;
                }))
                // Opens the Cloth Config screen
                .then(literal("config").executes(ctx -> {
                    Minecraft.getInstance().schedule(() -> {
                        var mc = Minecraft.getInstance();
                        mc.gui.setScreen(MineTunerClothConfigScreen.build(mc.gui.screen()));
                    });
                    return 1;
                }))
                // Opens the dedicated Benchmark GUI
                .then(literal("benchmark").executes(ctx -> {
                    Minecraft.getInstance().schedule(() ->
                            Minecraft.getInstance().gui.setScreen(new BenchmarkGuiScreen()));
                    return 1;
                }))
        );
    }
}

