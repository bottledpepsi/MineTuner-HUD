package bottled.minetuner.mixin;

import bottled.minetuner.MineTunerDataHolder;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundTickingStatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    // require = 0: the tick-rate feature is non-essential, so if a Minecraft update renames
    // handleTickingState the injector is skipped instead of crashing the launch.
    @Inject(method = "handleTickingState", at = @At("TAIL"), require = 0)
    private void minetuner$onTickingState(ClientboundTickingStatePacket packet, CallbackInfo ci) {
        MineTunerDataHolder.tickRate = packet.tickRate();
    }
}
