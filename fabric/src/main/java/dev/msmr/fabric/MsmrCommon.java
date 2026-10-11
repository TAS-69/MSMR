package dev.msmr.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class MsmrCommon implements ModInitializer {
    @Override public void onInitialize() {
        if(FabricLoader.getInstance().isModLoaded("fabric-networking-api-v1")) FabricNetworking.init();
        else org.slf4j.LoggerFactory.getLogger("MSMR").info("Optional networking API absent: local features available; remote pickup synchronization disabled.");
    }
}
