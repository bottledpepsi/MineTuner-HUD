package bottled.mtss.mixin;

import bottled.mtss.MtssDataHolder;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundTickingStatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Inject(method = "handleTickingState", at = @At("TAIL"))
    private void mtss$onTickingState(ClientboundTickingStatePacket packet, CallbackInfo ci) {
        MtssDataHolder.tickRate = packet.tickRate();
    }
}
