package dev.msmr.fabric;

import dev.msmr.FilterPayload;
import dev.msmr.client.ClientCapabilities;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public final class FabricClientNetworking {
    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(FilterPayload.TYPE,(payload,context) -> context.client().execute(() -> { if(context.client().player==context.player()) ClientCapabilities.receive(payload); }));
        ClientCapabilities.transport=new ClientCapabilities.Transport() {
            public boolean canSend() { return Minecraft.getInstance().getConnection()!=null && ClientPlayNetworking.canSend(FilterPayload.TYPE); }
            public void send(FilterPayload payload) { ClientPlayNetworking.send(payload); }
        };
    }
}
