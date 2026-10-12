package dev.msmr;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Player pickup rules and versioned requests; shared storage validates ownership separately. */
public final class ServerPickupRules {
    private record Local(MinecraftServer server,UUID player,FilterSnapshot rules) {}
    private record Remote(FilterSnapshot rules,long revision) {}
    private static volatile Local local;
    private static final Map<ServerPlayer,Remote> remote=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<ServerPlayer,RequestBudget> budget=new WeakHashMap<>();
    private ServerPickupRules() {}
    public static void clearLocal() { local=null; }
    public static void publishLocal(MinecraftServer server,UUID player,Rule wide,Map<Integer,Rule> slots) {
        if(server==null) { clearLocal(); return; }
        Local old=local;
        if(old!=null && old.server==server && old.player.equals(player) && old.rules.inventory().equals(wide) && old.rules.slots().equals(slots)) return;
        local=new Local(server,player,new FilterSnapshot(wide,slots));
    }
    public static void receive(ServerPlayer player,FilterPayload payload,Consumer<FilterPayload> reply) {
        long now=System.nanoTime(); var count=budget.computeIfAbsent(player,p -> new RequestBudget(now));
        if(!count.allow(payload.data().length,now)) return;
        if(payload.kind()>=FilterPayload.CHEST_GET) { ServerStorage.receive(player,payload,reply); return; }
        if(payload.kind()==FilterPayload.PROBE && payload.revision()==FilterPayload.VERSION) { reply.accept(FilterPayload.signal(FilterPayload.HELLO,FilterPayload.VERSION)); return; }
        if(payload.kind()!=FilterPayload.RULES || payload.revision()<1) return;
        Remote old=remote.get(player);
        if(old!=null && payload.revision()<=old.revision) { reply.accept(FilterPayload.signal(FilterPayload.ACK,old.revision)); return; }
        try {
            FilterSnapshot snapshot=payload.snapshot();
            remote.put(player,new Remote(snapshot,payload.revision()));
            reply.accept(FilterPayload.signal(FilterPayload.ACK,payload.revision()));
        } catch(RuntimeException ex) {
            invalid(player,"filters");
        }
    }
    public static void invalid(ServerPlayer player,String kind) {
        var count=budget.get(player);
        if(count!=null && count.warn(System.nanoTime())) org.slf4j.LoggerFactory.getLogger("MSMR").warn("Ignored invalid {} request from {}",kind,player.getUUID());
    }
    public static long revision(ServerPlayer player) { var value=remote.get(player); return value==null?0:value.revision; }
    /** Respawn replaces ServerPlayer while preserving the network connection and client rule revision. */
    public static void respawn(ServerPlayer original,ServerPlayer replacement) {
        if(original==replacement || !original.getUUID().equals(replacement.getUUID()) || original.getServer()!=replacement.getServer()) return;
        Remote rules=remote.remove(original);
        if(rules!=null) remote.put(replacement,rules);
        budget.remove(original);
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
