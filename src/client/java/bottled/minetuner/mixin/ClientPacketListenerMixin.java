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

    @Inject(method = "handleTickingState", at = @At("TAIL"))
    private void minetuner$onTickingState(ClientboundTickingStatePacket packet, CallbackInfo ci) {
        MineTunerDataHolder.tickRate = packet.tickRate();
    }
}
