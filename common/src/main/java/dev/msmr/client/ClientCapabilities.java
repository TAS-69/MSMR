package dev.msmr.client;

import dev.msmr.*;
import java.util.Map;
import net.minecraft.client.Minecraft;

/** No API classes are linked here. Each optional loader adapter supplies a transport. */
public final class ClientCapabilities {
    public interface Transport { boolean canSend(); void send(FilterPayload payload); }
    public static Transport transport;
    private static Object connection;
    private static boolean confirmed,probed;
    private static long revision,ack,pendingSince;
    private static FilterSnapshot sent,failed;
    private static String error="";
    private ClientCapabilities() {}
    public static boolean pickupsAvailable() { return Minecraft.getInstance().getSingleplayerServer()!=null || confirmed; }
    public static String error() { return error; }
    public static String pickupExplanation() {
        if(Minecraft.getInstance().getSingleplayerServer()!=null) return "Block ground pickups in this local world. Rejected drops stay on the ground.";
        return confirmed?"The server supports MSMR pickup filters.":"Requires MSMR on the server and compatible networking APIs on both ends. Inventory moves still work.";
    }
    public static void receive(FilterPayload payload) {
        if(payload.kind()==FilterPayload.HELLO && payload.revision()==FilterPayload.VERSION) { confirmed=true; sent=null; }
        else if(confirmed && payload.kind()==FilterPayload.ACK && payload.revision()<=revision) { ack=Math.max(ack,payload.revision()); if(ack==revision && failed==null) error=""; }
    }
    public static void update(Rule wide,Map<Integer,Rule> slots) {
        var mc=Minecraft.getInstance(); Object next=mc.getConnection();
        if(next!=connection) { connection=next; confirmed=false; probed=false; sent=null; failed=null; revision=0; ack=0; pendingSince=0; error=""; }
        if(next==null || mc.getSingleplayerServer()!=null || transport==null || !transport.canSend()) return;
        if(!probed) { probed=true; transport.send(FilterPayload.signal(FilterPayload.PROBE,FilterPayload.VERSION)); }
        if(!confirmed) return;
        if(ack<revision && pendingSince!=0 && System.nanoTime()-pendingSince>5_000_000_000L) error="Server has not confirmed pickup filters.";
        FilterSnapshot value=new FilterSnapshot(wide,slots);
        if(value.equals(sent) || value.equals(failed)) return;
        try { FilterPayload payload=FilterPayload.rules(revision+1,value); transport.send(payload); revision++; sent=value; failed=null; pendingSince=System.nanoTime(); error=""; }
        catch(RuntimeException ex) { failed=value; error="Server sync failed: filter is too large."; }
    }
    public static boolean synced() { return confirmed && sent!=null && ack==revision; }
}
