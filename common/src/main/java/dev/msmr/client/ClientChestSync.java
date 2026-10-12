package dev.msmr.client;

import dev.msmr.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.*;

/** Server state wins conflicts. Rapid local edits coalesce without overwriting another owner's rules. */
public final class ClientChestSync {
    private static AbstractContainerMenu menu;
    private static StorageProtocol.State state;
    private static List<ChestProfile> desired;
    private static FilterPayload edit;
    private static long latest,pollAt,editAt;
    private static int retries;
    private static String notice="";
    private static final Set<Long> reads=new HashSet<>();
    private static boolean closing;
    private static long closeAt;
    private ClientChestSync() {}
    public static void reset() { menu=null; state=null; desired=null; edit=null; latest=0; pollAt=0; notice=""; closing=false; reads.clear(); }
    public static boolean closing() { return closing; }
    public static boolean deferClose() {
        var player=Minecraft.getInstance().player;
        if(!ClientCapabilities.available() || player==null || player.containerMenu!=menu || !(menu instanceof ChestMenu) || edit==null && desired==null) return false;
        if(!closing) { closing=true; closeAt=System.nanoTime(); } return true;
    }
    public static boolean ready() { return state!=null && edit==null && desired==null; }
    public static long token() { return state==null?0:state.token(); }
    public static boolean canEdit() { return !ClientCapabilities.available() || state!=null && state.editable(); }
    public static String description() { return !ClientCapabilities.available()?"Local only; automation needs server support.":state==null?"Loading shared chest rules...":!notice.isEmpty()?notice:!state.editable()?"Read only. Claim/chest permission required.":edit!=null || desired!=null?"Syncing shared rules...":"Shared rules; server protection permissions apply."; }
    public static void edited() { if(ClientCapabilities.available()) { desired=ClientRules.chestProfiles(); notice=""; } }
    public static void update() {
        var mc=Minecraft.getInstance(); var next=mc.player.containerMenu;
        if(next!=menu) { reset(); menu=next; }
        if(!(menu instanceof ChestMenu)) return;
        long now=System.nanoTime();
        if(closing && (edit==null && desired==null || now-closeAt>10_000_000_000L)) {
            boolean timedOut=edit!=null || desired!=null; desired=null; edit=null; closing=false;
            if(timedOut) mc.player.displayClientMessage(Component.literal("Chest closed before sync completed. Reopen it to check the server rules."),true);
            mc.player.closeContainer(); return;
        }
        if(edit!=null) {
            if(now-editAt>2_000_000_000L) { editAt=now; if(++retries<=3) ClientCapabilities.send(edit); else { notice="Sync delayed. Reopen the chest to reload shared rules."; mc.player.displayClientMessage(Component.literal(notice),true); desired=null; edit=null; state=null; } }
            return;
        }
        if(desired!=null && state!=null) {
            if(!state.editable()) { desired=null; ClientRules.acceptChestProfiles(state.profiles()); notice="Claim/chest protection or filter ownership prevents editing."; return; }
            if(desired.equals(state.profiles())) { desired=null; return; }
            try { edit=FilterPayload.encode(FilterPayload.CHEST_SET,ClientCapabilities.nextRequest(),new StorageProtocol.Edit(menu.containerId,state.token(),state.versions(),desired)); desired=null; editAt=now; retries=0; ClientCapabilities.send(edit); }
            catch(RuntimeException ex) { desired=null; edit=null; ClientRules.acceptChestProfiles(state.profiles()); notice="Rules exceed the server sync limit. The saved server rules were restored."; mc.player.displayClientMessage(Component.literal(notice),true); }
            return;
        }
        if(now-pollAt>1_000_000_000L) { pollAt=now; long id=ClientCapabilities.nextRequest(); reads.removeIf(old -> old<id-8); reads.add(id); ClientCapabilities.send(FilterPayload.encode(FilterPayload.CHEST_GET,id,new StorageProtocol.Get(menu.containerId))); }
    }
    public static void receive(FilterPayload packet) {
        try {
            if((edit==null || packet.revision()!=edit.revision()) && !reads.remove(packet.revision())) return;
            var incoming=packet.read(StorageProtocol.State.class); var mc=Minecraft.getInstance();
            if(menu==null || mc.player==null || mc.player.containerMenu!=menu || incoming.menu()!=menu.containerId || packet.revision()<latest) return;
            latest=packet.revision();
            if(incoming.profiles().isEmpty()) { state=incoming; desired=null; edit=null; notice=incoming.message().isEmpty()?"Server could not identify this storage block.":incoming.message(); return; }
            boolean first=state==null; List<ChestProfile> local=ClientRules.chestProfiles(); state=incoming;
            if(edit!=null && packet.revision()==edit.revision()) edit=null;
            if(!incoming.message().isEmpty()) { desired=null; notice=incoming.message(); mc.player.displayClientMessage(Component.literal(notice),true); }
            if(first && desired==null && incoming.editable() && incoming.versions().stream().allMatch(v -> v==0) && ClientRules.mayImportChestProfiles() && local.stream().anyMatch(p -> !p.empty())) desired=local;
            ClientRules.markSharedChest();
            if(desired==null && edit==null) ClientRules.acceptChestProfiles(incoming.profiles());
        } catch(RuntimeException ex) { notice="Invalid server chest response; rules were not changed."; }
    }
}
