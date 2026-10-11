package dev.msmr;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Only server-authoritative rules live here. Optional clients never affect other players. */
public final class ServerPickupRules {
    private record Local(MinecraftServer server,UUID player,FilterSnapshot rules) {}
    private record Remote(FilterSnapshot rules,long revision) {}
    private static volatile Local local;
    private static final Map<ServerPlayer,Remote> remote=Collections.synchronizedMap(new WeakHashMap<>());
    private ServerPickupRules() {}
    public static void clearLocal() { local=null; }
    public static void publishLocal(MinecraftServer server,UUID player,Rule wide,Map<Integer,Rule> slots) {
        if(server==null) { clearLocal(); return; }
        Local old=local;
        if(old!=null && old.server==server && old.player.equals(player) && old.rules.inventory().equals(wide) && old.rules.slots().equals(slots)) return;
        local=new Local(server,player,new FilterSnapshot(wide,slots));
    }
    public static void receive(ServerPlayer player,FilterPayload payload,Consumer<FilterPayload> reply) {
        if(payload.kind()==FilterPayload.PROBE && payload.revision()==FilterPayload.VERSION) { reply.accept(FilterPayload.signal(FilterPayload.HELLO,FilterPayload.VERSION)); return; }
        if(payload.kind()!=FilterPayload.RULES || payload.revision()<1) return;
        Remote old=remote.get(player);
        if(old!=null && payload.revision()<=old.revision) { reply.accept(FilterPayload.signal(FilterPayload.ACK,old.revision)); return; }
        try {
            FilterSnapshot snapshot=payload.snapshot();
            remote.put(player,new Remote(snapshot,payload.revision()));
            reply.accept(FilterPayload.signal(FilterPayload.ACK,payload.revision()));
        } catch(RuntimeException ex) {
            org.slf4j.LoggerFactory.getLogger("MSMR").warn("Ignored invalid filters from {}",player.getUUID());
        }
    }
    public static boolean add(Inventory inventory,ItemStack stack) {
        FilterSnapshot rules=forPlayer(inventory.player);
        return rules==null?inventory.add(stack):PickupFilters.insert(inventory,stack,rules.inventory(),rules.slots());
    }
    public static FilterSnapshot forPlayer(net.minecraft.world.entity.player.Player entity) {
        if(!(entity instanceof ServerPlayer player)) return null;
        Local current=local;
        FilterSnapshot rules=current!=null && player.getServer()==current.server && player.getUUID().equals(current.player)?current.rules:null;
        if(rules==null) { Remote value=remote.get(player); if(value!=null) rules=value.rules; }
        return rules;
    }
}
