package dev.msmr.neoforge;

import dev.msmr.FilterPayload;
import dev.msmr.client.ClientCapabilities;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NeoClientNetworking {
    public static void receive(FilterPayload payload,net.minecraft.world.entity.player.Player origin) { if(Minecraft.getInstance().player==origin) ClientCapabilities.receive(payload); }
    public static void init() {
        ClientCapabilities.transport=new ClientCapabilities.Transport() {
            public boolean canSend() { var connection=Minecraft.getInstance().getConnection(); return connection!=null && connection.hasChannel(FilterPayload.TYPE.id()); }
            public void send(FilterPayload payload) { PacketDistributor.sendToServer(payload); }
        };
    }
}
