package dev.msmr.client;

import dev.msmr.*;
import java.util.Map;
import net.minecraft.client.Minecraft;

/** Optional transport with acknowledged rules, bounded retries and connection-scoped state. */
public final class ClientCapabilities {
    public interface Transport { boolean canSend(); void send(FilterPayload payload); }
    public static Transport transport;
    private static Object connection;
    private static boolean confirmed;
    private static long revision,ack,lastProbe,pendingSince;
    private static FilterSnapshot sent,failed;
    private static FilterPayload pending;
    private static String error="";
    private static int retries;
    private static long request;
    private ClientCapabilities() {}
    public static boolean available() { return confirmed; }
    public static boolean pickupsAvailable() { return Minecraft.getInstance().getSingleplayerServer()!=null || confirmed; }
    public static long revision() { return revision; }
    public static long nextRequest() { return ++request; }
    public static String error() { return error; }
    public static String pickupExplanation() {
        if(Minecraft.getInstance().getSingleplayerServer()!=null) return "Block ground pickups in this local world. Rejected drops stay on the ground.";
        return confirmed?"The server supports MSMR pickup filters.":"Requires MSMR on the server and compatible networking APIs on both ends. Inventory moves still work.";
    }
    public static void send(FilterPayload payload) {
        var mc=Minecraft.getInstance(); var server=mc.getSingleplayerServer();
        if(server!=null && mc.player!=null) {
            var id=mc.player.getUUID(); Object origin=mc.getConnection();
            server.execute(() -> { var player=server.getPlayerList().getPlayer(id); if(player!=null) ServerPickupRules.receive(player,payload,response -> mc.execute(() -> { if(mc.getConnection()==origin) receive(response); })); });
        } else if(transport!=null && transport.canSend()) transport.send(payload);
    }
    public static void receive(FilterPayload payload) {
        if(payload.kind()==FilterPayload.HELLO && payload.revision()==FilterPayload.VERSION) { if(!confirmed) { confirmed=true; sent=null; } }
        else if(confirmed && payload.kind()==FilterPayload.ACK && payload.revision()<=revision) { ack=Math.max(ack,payload.revision()); if(ack==revision) { pending=null; if(failed==null) error=""; } }
        else if(confirmed && payload.kind()==FilterPayload.CHEST_STATE) ClientChestSync.receive(payload);
        else if(confirmed && payload.kind()==FilterPayload.RESULT) ClientOperations.receive(payload);
    }
    public static void update(Rule wide,Map<Integer,Rule> slots) {
        var mc=Minecraft.getInstance(); Object next=mc.getConnection(); long now=System.nanoTime();
        if(next!=connection) { connection=next; confirmed=false; sent=null; failed=null; pending=null; revision=0; ack=0; pendingSince=0; lastProbe=0; error=""; request=0; ClientChestSync.reset(); ClientOperations.reset(); }
        if(next==null || mc.getSingleplayerServer()==null && (transport==null || !transport.canSend())) return;
        if(!confirmed && now-lastProbe>2_000_000_000L) { lastProbe=now; send(FilterPayload.signal(FilterPayload.PROBE,FilterPayload.VERSION)); }
        if(!confirmed) return;
        FilterSnapshot value=new FilterSnapshot(wide,slots);
        if(!value.equals(sent) && !value.equals(failed)) try {
            pending=FilterPayload.rules(++revision,value); sent=value; failed=null; retries=0; pendingSince=now; send(pending); error="";
        } catch(RuntimeException ex) { failed=value; pending=null; error="Server sync failed: filter is too large."; }
        if(pending!=null && now-pendingSince>2_000_000_000L) { pendingSince=now; if(++retries<=3) send(pending); else error="Server has not confirmed inventory filters. Waiting safely."; }
        ClientChestSync.update(); ClientOperations.tick();
    }
    public static boolean synced() { return confirmed && sent!=null && ack==revision && failed==null; }
}
