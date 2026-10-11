package dev.msmr.fabric;

import dev.msmr.*;
import dev.msmr.client.ClientRules;
import net.fabricmc.api.ClientModInitializer;

public final class MsmrClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientRules.init();
        if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("fabric-networking-api-v1")) FabricClientNetworking.init();
    }
}
