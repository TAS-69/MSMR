package dev.msmr.client;

import dev.msmr.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.*;

/** Retries reuse the same request ID, so a delayed reply cannot perform a transfer twice. */
public final class ClientOperations {
    private static AbstractContainerMenu menu;
    private static String operation;
    private static int source,retries,conflicts;
    private static long started,sentAt;
    private static FilterPayload pending;
    private ClientOperations() {}
    public static boolean busy() { return operation!=null; }
    public static void reset() { menu=null; operation=null; pending=null; retries=0; conflicts=0; }
    private static void message(String value) { var p=Minecraft.getInstance().player; if(p!=null) p.displayClientMessage(Component.literal(value),true); }
    public static boolean start(String action,int slot) {
        if(!ClientCapabilities.available()) return false;
        if(busy()) return true;
        var mc=Minecraft.getInstance(); menu=mc.player.containerMenu; operation=action; source=slot; started=System.nanoTime(); pending=null; retries=0; conflicts=0;
        message("Waiting for server confirmation..."); return true;
    }
    public static void tick() {
        if(!busy()) return;
        var mc=Minecraft.getInstance(); long now=System.nanoTime();
        if(mc.player==null || mc.player.containerMenu!=menu) { reset(); return; }
        if(pending==null) {
            if(!(mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)) { reset(); message("Move cancelled before it was sent."); return; }
            if(now-started>10_000_000_000L) { reset(); message("Server sync timed out. Nothing was requested; reopen the inventory."); return; }
            if(!ClientCapabilities.synced() || menu instanceof ChestMenu && !ClientChestSync.ready()) return;
            if(!menu.getCarried().isEmpty()) { reset(); message("Empty your cursor before moving items."); return; }
            pending=FilterPayload.encode(FilterPayload.ACTION,ClientCapabilities.nextRequest(),new StorageProtocol.Action(menu.containerId,menu.getStateId(),ClientCapabilities.revision(),menu instanceof ChestMenu?ClientChestSync.token():0,operation,source));
            sentAt=now; ClientCapabilities.send(pending);
        } else if(now-sentAt>2_000_000_000L) {
            sentAt=now;
            if(++retries<=3) ClientCapabilities.send(pending);
            else { reset(); message("Server reply timed out. Check your inventory before retrying."); }
        }
    }
    public static void receive(FilterPayload packet) {
        if(pending==null || packet.revision()!=pending.revision()) return;
        try {
            var result=packet.read(StorageProtocol.Result.class);
            if(!result.success() && result.message().startsWith("Inventory changed") && conflicts++==0) { pending=null; retries=0; started=System.nanoTime(); return; }
            reset(); message(result.message());
        } catch(RuntimeException ex) { reset(); message("Invalid server reply. Reopen your inventory before retrying."); }
    }
}
