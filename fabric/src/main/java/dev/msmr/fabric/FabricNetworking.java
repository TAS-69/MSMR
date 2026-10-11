package dev.msmr.fabric;

import dev.msmr.*;
import net.fabricmc.fabric.api.networking.v1.*;

/** Loaded only when Fabric's optional networking API is present. */
public final class FabricNetworking {
    public static void init() {
        PayloadTypeRegistry.playC2S().register(FilterPayload.TYPE,FilterPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FilterPayload.TYPE,FilterPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(FilterPayload.TYPE,(payload,context) ->
            ServerPickupRules.receive(context.player(),payload,response -> ServerPlayNetworking.send(context.player(),response)));
    }
}
