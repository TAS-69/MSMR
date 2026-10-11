package dev.msmr.fabric;

import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;

/** Only link optional transfer adapters when the API is installed. */
public final class FabricTransferPlugin implements IMixinConfigPlugin {
    public void onLoad(String pkg) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String target,String mixin) { return FabricLoader.getInstance().isModLoaded("fabric-transfer-api-v1"); }
    public void acceptTargets(Set<String> mine,Set<String> others) {}
    public List<String> getMixins() { return null; }
    public void preApply(String target,ClassNode node,String mixin,IMixinInfo info) {}
    public void postApply(String target,ClassNode node,String mixin,IMixinInfo info) {}
}
