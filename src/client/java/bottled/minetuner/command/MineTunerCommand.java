package bottled.minetuner.command;

import bottled.minetuner.benchmark.BenchmarkSession;
import bottled.minetuner.config.cloth.MineTunerClothConfigScreen;
import bottled.minetuner.gui.BenchmarkGuiScreen;
import bottled.minetuner.gui.MineTunerGuiScreen;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class MineTunerCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("minetuner")
                // Root with no subcommand: print the subcommand list instead of the
                // default bare "unknown command" silence a typo'd invocation got.
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(
                            Component.translatable("minetuner.cmd.help"));
                    return 1;
                })
                // Opens the custom HUD editor. The screen itself is the feedback.
                .then(literal("gui").executes(ctx -> {
                    Minecraft.getInstance().schedule(() ->
                            Minecraft.getInstance().gui.setScreen(new MineTunerGuiScreen()));
                    return 1;
                }))
                // Opens the Cloth Config screen. No mod-loaded guard needed:
                // cloth-config is a hard "depends" in fabric.mod.json, so the mod
                // (and this command) can't be loaded without it in the first place.
                .then(literal("config").executes(ctx -> {
                    Minecraft.getInstance().schedule(() -> {
                        var mc = Minecraft.getInstance();
                        mc.gui.setScreen(MineTunerClothConfigScreen.build(mc.gui.screen()));
                    });
                    return 1;
                }))
                // Opens the dedicated Benchmark GUI.
                .then(literal("benchmark").executes(ctx -> {
                    Minecraft.getInstance().schedule(() ->
                            Minecraft.getInstance().gui.setScreen(new BenchmarkGuiScreen()));
                    return 1;
                })
                // Scriptable benchmark control, mirroring the keybinds' overlay-message
                // feedback (including the "only say Stopped if this stopped something"
                // rule from MineTunerClient's Stop handler).
                .then(literal("start").executes(ctx -> {
                    BenchmarkSession.start();
                    ctx.getSource().sendFeedback(
                            Component.translatable("minetuner.benchmark.started"));
                    return 1;
                }))
                .then(literal("stop").executes(ctx -> {
                    boolean wasRecording = BenchmarkSession.isRecording();
                    BenchmarkSession.stop();
                    if (wasRecording) {
                        String avgFps = String.format(java.util.Locale.ROOT, "%.1f",
                                BenchmarkSession.finalAvgFps());
                        ctx.getSource().sendFeedback(
                                Component.translatable("minetuner.benchmark.stopped", avgFps));
                    }
                    return 1;
                }))
                .then(literal("save").executes(ctx -> {
                    if (!BenchmarkSession.hasFrozenResult()) {
                        ctx.getSource().sendError(
                                Component.translatable("minetuner.cmd.benchmark.nothing_to_save"));
                        return 0;
                    }
                    if (BenchmarkSession.saveState() == BenchmarkSession.SaveState.SAVING) {
                        ctx.getSource().sendFeedback(
                                Component.translatable("minetuner.benchmark.saving"));
                        return 1;
                    }
                    BenchmarkSession.saveToFolderAsync();
                    ctx.getSource().sendFeedback(
                            Component.translatable("minetuner.benchmark.saving"));
                    return 1;
                })))
        );
    }
}
