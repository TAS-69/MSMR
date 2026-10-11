package dev.msmr.client;

import java.util.*;
import dev.msmr.Rule;
import dev.msmr.InventorySort;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Uses acknowledged server operations when available, otherwise guarded vanilla clicks. */
public final class ClientTransfers {
    private record Action(int slot,int button,ItemStack cursor,ItemStack before) {}
    private static final Deque<Action> pending=new ArrayDeque<>();
    private static final Deque<Integer> fillSources=new ArrayDeque<>();
    private static AbstractContainerMenu active;
    private static AbstractContainerMenu sortRequested;
    private enum Batch { RESERVED, TO_CHEST, FROM_CHEST }
    private static Batch batch=Batch.RESERVED;
    public static boolean emitting;
    public static boolean busy() { return !pending.isEmpty() || !fillSources.isEmpty() || ClientOperations.busy() || ClientChestSync.closing(); }
    private static void message(String text) { var p=Minecraft.getInstance().player; if(p!=null) p.displayClientMessage(Component.literal(text),true); }
    public static boolean quickMove(int source) {
        var mc=Minecraft.getInstance(); var menu=mc.player.containerMenu;
        Slot from=menu.getSlot(source);
        // Crafting/result and specialized menu shift-click semantics are left to the game only when no rules need enforcement.
        boolean playerSource=from.container==mc.player.getInventory();
        List<Slot> targets=new ArrayList<>();
        boolean chest=menu instanceof ChestMenu || menu instanceof HopperMenu || menu instanceof ShulkerBoxMenu || menu instanceof DispenserMenu;
        if ((!chest && menu!=mc.player.inventoryMenu) || (menu==mc.player.inventoryMenu && !playerSource)) {
            if(ClientRules.hasFilters()) { message("MSMR: use manual clicks in this menu."); return true; }
            return false;
        }
        for(Slot slot:menu.slots) {
            if(slot.index==source) continue;
            boolean inv=slot.container==mc.player.getInventory() && slot.getContainerSlot()<36;
            if(chest ? inv!=playerSource : (menu==mc.player.inventoryMenu && inv && ((slot.getContainerSlot()<9)!=(from.getContainerSlot()<9)))) targets.add(slot);
        }
        boolean filtered=targets.stream().anyMatch(s -> !ClientRules.moveRule(s).unrestricted() || !ClientRules.moveInventoryFilter(s).unrestricted());
        if(!filtered) return false;
        if((menu==mc.player.inventoryMenu || menu instanceof ChestMenu) && ClientOperations.start("quick_move",source)) return true;
        if(!chest && menu!=mc.player.inventoryMenu) { message("MSMR: use manual clicks in this menu."); return true; }
        plan(menu,from,targets); return true;
    }
    public static void fillReserved() {
        var mc=Minecraft.getInstance();
        if(mc.player==null || busy() || !mc.player.containerMenu.getCarried().isEmpty()) return;
        if(ClientOperations.start("reserved",-1)) return;
        ClientRules.refresh(); active=mc.player.containerMenu; batch=Batch.RESERVED;
        for(Slot slot:active.slots) if(slot.container==mc.player.getInventory() && slot.getContainerSlot()<36
            && !slot.getItem().isEmpty() && !ClientRules.moveRule(slot).preferred(slot.getItem())) fillSources.add(slot.index);
        message("Filling reserved slots using normal clicks...");
    }
    public static void requestSort() { var p=Minecraft.getInstance().player; if(p!=null) sortRequested=p.containerMenu; }
    public static void cancelRequestedSort() { sortRequested=null; }
    public static void sortInventory() {
        var mc=Minecraft.getInstance();
        if(mc.player==null || busy() || !mc.player.containerMenu.getCarried().isEmpty()) { message("Empty your cursor and finish the current move first."); return; }
        ClientRules.refresh();
        if(ClientOperations.start("sort",-1)) return;
        var menu=mc.player.containerMenu;
        List<Slot> slots=menu.slots.stream().filter(s -> s.container==mc.player.getInventory() && s.getContainerSlot()>=0 && s.getContainerSlot()<36).sorted(Comparator.comparingInt(Slot::getContainerSlot)).toList();
        if(slots.size()!=36) { message("Open your inventory or a supported chest to arrange items."); return; }
        var result=InventorySort.plan(slots.stream().map(s -> s.getItem().copy()).toList(),slots.stream().map(ClientRules::rule).toList(),ClientRules.inventoryFilter());
        if(!result.possible()) { message(result.error()); return; }
        for(int i=0;i<slots.size();i++) {
            Slot slot=slots.get(i); ItemStack goal=result.result().get(i);
            if(!ItemStack.matches(slot.getItem(),goal) && (!slot.mayPickup(mc.player) || !goal.isEmpty() && (!slot.mayPlace(goal) || goal.getCount()>slot.getMaxStackSize(goal)))) { message("This menu restricts inventory arrangement. Open your normal inventory instead."); return; }
        }
        active=menu;
        for(var click:result.clicks()) pending.add(new Action(slots.get(click.slot()).index,click.button(),click.cursor(),click.before()));
        message(pending.isEmpty()?"Inventory is already arranged.":"Arranging inventory for "+ClientRules.activeProfile()+"...");
    }
    public static void transferAll(boolean toChest) {
        var mc=Minecraft.getInstance();
        if(mc.player==null || busy() || !(mc.player.containerMenu instanceof ChestMenu) || !mc.player.containerMenu.getCarried().isEmpty()) return;
        if(toChest?!ClientRules.tools().transferTo():!ClientRules.tools().transferFrom()) return;
        if(ClientOperations.start(toChest?"to_chest":"from_chest",-1)) return;
        ClientRules.refresh(); active=mc.player.containerMenu; batch=toChest?Batch.TO_CHEST:Batch.FROM_CHEST;
        for(Slot slot:active.slots) {
            boolean player=slot.container==mc.player.getInventory() && slot.getContainerSlot()>=0 && slot.getContainerSlot()<36;
            if(player==toChest && !slot.getItem().isEmpty()) fillSources.add(slot.index);
        }
        message(toChest?"Moving allowed items to chest...":"Moving allowed items to inventory...");
    }
    private static void plan(AbstractContainerMenu menu,Slot from,List<Slot> targets) {
        if(busy() && !pending.isEmpty()) return;
        if(!menu.getCarried().isEmpty() || from.getItem().isEmpty() || !from.mayPickup(Minecraft.getInstance().player)) return;
        ItemStack cursor=from.getItem().copy();
        targets.sort(Comparator.comparingInt(s -> ClientRules.moveRule(s).preferred(cursor)?0:1));
        Deque<Action> plan=new ArrayDeque<>();
        plan.add(new Action(from.index,0,ItemStack.EMPTY,cursor.copy()));
        int moved=0;
        for(Slot to:targets) {
            Rule rule=ClientRules.moveRule(to);
            ItemStack existing=to.getItem().copy();
            if(!ClientRules.accepts(to,cursor) || !to.mayPlace(cursor) || (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing,cursor))) continue;
            int available=Math.max(0,Math.min(Math.min(rule.cap(cursor),ClientRules.moveInventoryFilter(to).cap(cursor)),to.getMaxStackSize(cursor))-existing.getCount());
            int amount=Math.min(available,cursor.getCount());
            if(amount==0) continue;
            int vanillaCapacity=to.getMaxStackSize(cursor)-existing.getCount();
            if(amount==Math.min(vanillaCapacity,cursor.getCount())) {
                plan.add(new Action(to.index,0,cursor.copy(),existing.copy())); cursor.shrink(amount);
            } else for(int n=0;n<amount;n++) {
                plan.add(new Action(to.index,1,cursor.copy(),existing.copy()));
                if(existing.isEmpty()) existing=cursor.copyWithCount(1); else existing.grow(1);
                cursor.shrink(1);
            }
            moved+=amount;
            if(cursor.isEmpty()) break;
        }
        if(moved==0) return;
        if(!cursor.isEmpty()) plan.add(new Action(from.index,0,cursor.copy(),ItemStack.EMPTY));
        active=menu; pending.addAll(plan);
    }
    public static void tick() {
        var mc=Minecraft.getInstance();
        if(sortRequested!=null) {
            if(mc.player==null || mc.player.containerMenu!=sortRequested) sortRequested=null;
            else if(mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> && !busy()) { sortRequested=null; sortInventory(); }
        }
        if(mc.player==null || mc.gameMode==null || mc.player.containerMenu!=active || !(mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)) {
            pending.clear(); fillSources.clear(); active=null; return;
        }
        ClientRules.refresh();
        if(pending.isEmpty()) {
            while(!fillSources.isEmpty() && pending.isEmpty()) {
                Slot from=active.getSlot(fillSources.removeFirst());
                List<Slot> targets=new ArrayList<>();
                for(Slot to:active.slots) if(to.index!=from.index) {
                    boolean player=to.container==mc.player.getInventory() && to.getContainerSlot()>=0 && to.getContainerSlot()<36;
                    if(batch==Batch.RESERVED?player && ClientRules.moveRule(to).preferred(from.getItem()):player==(batch==Batch.FROM_CHEST)) targets.add(to);
                }
                plan(active,from,targets);
            }
        }
        if(pending.isEmpty()) return;
        Action action=pending.removeFirst();
        if(!ItemStack.matches(active.getCarried(),action.cursor) || !ItemStack.matches(active.getSlot(action.slot).getItem(),action.before)) {
            pending.clear(); fillSources.clear(); message("MSMR stopped: inventory changed. Check the item on your cursor."); return;
        }
        emitting=true;
        try { mc.gameMode.handleInventoryMouseClick(active.containerId,action.slot,action.button,ClickType.PICKUP,mc.player); }
        finally { emitting=false; }
    }
}
