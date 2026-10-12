package dev.msmr.neoforge;

import dev.msmr.*;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;

@Mod("msmr")
public final class MsmrCommon {
    public MsmrCommon(IEventBus bus) { bus.addListener(MsmrCommon::register); }
    private static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2").optional().playBidirectional(FilterPayload.TYPE,FilterPayload.CODEC,(payload,context) -> context.enqueueWork(() -> {
            if(context.flow()==PacketFlow.SERVERBOUND && context.player() instanceof ServerPlayer player)
                ServerPickupRules.receive(player,payload,context::reply);
            else if(context.flow()==PacketFlow.CLIENTBOUND) NeoClientNetworking.receive(payload,context.player());
        }));
    }
}
