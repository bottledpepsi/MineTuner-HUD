package bottled.mtss.sample;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;

/** Immutable per-frame snapshot of the handles a {@link StatSource} might need. */
public record SamplingContext(
        Minecraft mc,
        LocalPlayer player,        // null if not in a world.
        ClientLevel level,         // null if not in a world.
        ClientPacketListener conn  // null if not connected.
) {
    static SamplingContext capture() {
        Minecraft mc = Minecraft.getInstance();
        return new SamplingContext(mc, mc.player, mc.level, mc.getConnection());
    }

    // Public, not package-private: individual StatSource implementations live one
    // package down in bottled.mtss.sample.sources (per the design's package layout),
    // so these need to be visible there.
    public boolean hasPlayer() {
        return player != null;
    }

    public boolean hasLevel() {
        return level != null;
    }
}
