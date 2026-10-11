package dev.msmr.neoforge;
import dev.msmr.client.ClientRules;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
@Mod(value="msmr", dist=Dist.CLIENT)
public final class MsmrNeoForge {
    public MsmrNeoForge() { ClientRules.init(); NeoClientNetworking.init(); }
}
