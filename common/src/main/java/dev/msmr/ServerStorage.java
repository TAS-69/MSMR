package dev.msmr;

import dev.msmr.mixin.CompoundContainerAccess;
import java.util.*;
import java.util.function.Consumer;
import java.lang.ref.WeakReference;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;

/** All shared edits and item mutations run on the server thread against live containers. */
public final class ServerStorage {
    public record Part(BaseContainerBlockEntity block,int slot) {}
    private record Session(WeakReference<AbstractContainerMenu> menu,long token) {}
    private record Completed(long id,FilterPayload result) {}
    private static final Map<ServerPlayer,Session> sessions=new WeakHashMap<>();
    private static final Map<ServerPlayer,Completed> completed=new WeakHashMap<>();
    private static final Map<ServerPlayer,Completed> edited=new WeakHashMap<>();
    private ServerStorage() {}
    public static Part resolve(Container container,int slot) {
        if(slot<0 || slot>=container.getContainerSize()) return null;
        if(container instanceof CompoundContainerAccess doubleChest) {
            var first=doubleChest.msmr$first(); return slot<first.getContainerSize()?resolve(first,slot):resolve(doubleChest.msmr$second(),slot-first.getContainerSize());
        }
        if(container instanceof ChestBlockEntity || container instanceof BarrelBlockEntity) return new Part((BaseContainerBlockEntity)container,slot);
        return null;
    }
    public static ChestProfile profile(Container container,int slot) { var part=resolve(container,slot); return part==null?ChestProfile.EMPTY:((ChestData)part.block()).msmr$profile(); }
    public static boolean guarded(Container container,int slot) { var p=resolve(container,slot); return p!=null && p.block().getLevel()!=null && !p.block().getLevel().isClientSide && !((ChestData)p.block()).msmr$profile().empty(); }
    public static int cap(Container container,int slot,ItemStack stack) {
        var part=resolve(container,slot);
        if(part==null) return stack.getMaxStackSize();
        var profile=((ChestData)part.block()).msmr$profile(); Rule wide=profile.filter().forMoving(),rule=profile.slot(part.slot()).forMoving();
        return wide.accepts(stack) && rule.accepts(stack)?Math.min(wide.cap(stack),rule.cap(stack)):0;
    }
    public static int slotCap(Slot slot,ItemStack stack) {
        if(slot.container instanceof Inventory inv && !inv.player.level().isClientSide && slot.getContainerSlot()<36) {
            var snapshot=ServerPickupRules.forPlayer(inv.player);
            if(snapshot!=null) { var wide=snapshot.inventory().forMoving(); var rule=snapshot.slots().getOrDefault(slot.getContainerSlot(),Rule.OPEN).forMoving(); return wide.accepts(stack) && rule.accepts(stack)?Math.min(wide.cap(stack),rule.cap(stack)):0; }
        }
        return guarded(slot.container,slot.getContainerSlot())?cap(slot.container,slot.getContainerSlot(),stack):stack.getMaxStackSize();
    }
    /** Returns a remainder; never discards a rejected item or mutates a simulated insertion. */
    public static ItemStack insert(Container destination,int slot,ItemStack stack,boolean simulate) {
        ItemStack remainder=stack.copy(),existing=destination.getItem(slot);
        if(!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing,stack)) return remainder;
        int amount=Math.min(stack.getCount(),Math.max(0,Math.min(destination.getMaxStackSize(stack),cap(destination,slot,stack))-existing.getCount()));
        if(amount>0) { remainder.shrink(amount); if(!simulate) { destination.setItem(slot,stack.copyWithCount(existing.getCount()+amount)); destination.setChanged(); } }
        return remainder;
    }
    private static List<BaseContainerBlockEntity> blocks(ServerPlayer player) {
        var menu=player.containerMenu;
        if(!(menu instanceof ChestMenu chest) || !menu.stillValid(player) || player.isSpectator()) return List.of();
        var result=new ArrayList<BaseContainerBlockEntity>();
        for(int i=0;i<chest.getRowCount()*9;i+=27) {
            Slot slot=menu.getSlot(i); var part=resolve(slot.container,slot.getContainerSlot());
            if(part==null || part.slot()!=0 || part.block().getLevel()!=player.level() || part.block().isRemoved() || !part.block().canOpen(player)) return List.of();
            result.add(part.block());
        }
        return result.size()>2?List.of():result;
    }
    private static Session session(ServerPlayer player) {
        return sessions.compute(player,(p,old) -> old!=null && old.menu().get()==p.containerMenu?old:new Session(new WeakReference<>(p.containerMenu),UUID.randomUUID().getMostSignificantBits()));
    }
    private static boolean editable(ServerPlayer player,List<BaseContainerBlockEntity> blocks) { return blocks.stream().allMatch(b -> { UUID owner=((ChestData)b).msmr$owner(); return owner==null || owner.equals(player.getUUID()) || player.hasPermissions(2); }); }
    private static StorageProtocol.State state(ServerPlayer player,String message) {
        var blocks=blocks(player); var session=session(player);
        return new StorageProtocol.State(player.containerMenu.containerId,session.token(),blocks.stream().map(b -> ((ChestData)b).msmr$version()).toList(),blocks.stream().map(b -> ((ChestData)b).msmr$profile()).toList(),!blocks.isEmpty() && editable(player,blocks),message);
    }
    public static void receive(ServerPlayer player,FilterPayload packet,Consumer<FilterPayload> reply) {
        try {
            if(packet.kind()==FilterPayload.CHEST_GET) {
                if(packet.read(StorageProtocol.Get.class).menu()==player.containerMenu.containerId) reply.accept(FilterPayload.encode(FilterPayload.CHEST_STATE,packet.revision(),state(player,"")));
            } else if(packet.kind()==FilterPayload.CHEST_SET) {
                var previous=edited.get(player);
                if(previous!=null && packet.revision()<=previous.id()) { if(packet.revision()==previous.id()) reply.accept(previous.result()); return; }
                if(packet.revision()<1) return;
                var edit=packet.read(StorageProtocol.Edit.class); var current=state(player,""); var targets=blocks(player);
                String message="";
                if(edit.menu()!=current.menu() || edit.token()!=current.token() || targets.size()!=edit.profiles().size()) message="Chest closed or changed. Reopen it before editing.";
                else if(!current.editable()) message="Only the filter owner or a server operator can edit these shared rules.";
                else if(!edit.versions().equals(current.versions())) message="Chest rules changed. The server's current rules were restored.";
                else for(int i=0;i<targets.size();i++) { var data=(ChestData)targets.get(i); if(!data.msmr$profile().equals(edit.profiles().get(i))) data.msmr$update(edit.profiles().get(i),data.msmr$owner()==null?player.getUUID():data.msmr$owner()); }
                var response=FilterPayload.encode(FilterPayload.CHEST_STATE,packet.revision(),state(player,message)); edited.put(player,new Completed(packet.revision(),response)); reply.accept(response);
            } else if(packet.kind()==FilterPayload.ACTION) {
                var old=completed.get(player);
                if(old!=null && packet.revision()<=old.id()) { if(packet.revision()==old.id()) reply.accept(old.result()); return; }
                if(packet.revision()<1) return;
                var result=operate(player,packet.read(StorageProtocol.Action.class));
                var response=FilterPayload.encode(FilterPayload.RESULT,packet.revision(),result); completed.put(player,new Completed(packet.revision(),response));
                player.containerMenu.sendAllDataToRemote(); reply.accept(response);
            }
        } catch(RuntimeException ex) { org.slf4j.LoggerFactory.getLogger("MSMR").warn("Ignored invalid storage request from {}",player.getUUID()); }
    }
    private static StorageProtocol.Result failure(String message) { return new StorageProtocol.Result(false,message); }
    private static StorageProtocol.Result operate(ServerPlayer player,StorageProtocol.Action request) {
        var menu=player.containerMenu;
        if(request.menu()!=menu.containerId || request.state()!=menu.getStateId() || !menu.stillValid(player) || player.isSpectator() || !menu.getCarried().isEmpty()) return failure("Inventory changed. Nothing moved; try again.");
        if(request.rules()!=ServerPickupRules.revision(player)) return failure("Wait for inventory filters to finish syncing.");
        if(menu!=player.inventoryMenu && !(menu instanceof ChestMenu)) return failure("Open your inventory or a supported chest.");
        if(menu instanceof ChestMenu && request.chestToken()!=session(player).token()) return failure("Chest session changed. Reopen it before transferring.");
        var snapshot=ServerPickupRules.forPlayer(player); if(snapshot==null) return failure("Inventory filters have not synced.");
        if(request.operation().equals("sort")) {
            List<Slot> slots=menu.slots.stream().filter(s -> s.container==player.getInventory() && s.getContainerSlot()<36).sorted(Comparator.comparingInt(Slot::getContainerSlot)).toList();
            if(slots.size()!=36) return failure("This menu does not support arrangement.");
            var plan=InventorySort.plan(slots.stream().map(s -> s.getItem().copy()).toList(),slots.stream().map(s -> snapshot.slots().getOrDefault(s.getContainerSlot(),Rule.OPEN)).toList(),snapshot.inventory());
            if(!plan.possible()) return failure(plan.error());
            for(int i=0;i<slots.size();i++) { var s=slots.get(i); var goal=plan.result().get(i); if(!ItemStack.matches(goal,s.getItem()) && (!s.mayPickup(player) || !goal.isEmpty() && (!s.mayPlace(goal) || goal.getCount()>s.getMaxStackSize(goal)))) return failure("This menu restricts arrangement."); }
            for(int i=0;i<slots.size();i++) slots.get(i).set(plan.result().get(i).copy());
            return new StorageProtocol.Result(true,"Inventory arranged.");
        }
        boolean to=request.operation().equals("to_chest"),from=request.operation().equals("from_chest"),reserved=request.operation().equals("reserved"),quick=request.operation().equals("quick_move");
        if((to || from) && !(menu instanceof ChestMenu)) return failure("A chest must be open.");
        if(quick && (request.source()<0 || request.source()>=menu.slots.size())) return failure("Invalid source slot.");
        List<ItemStack> stacks=new ArrayList<>(menu.slots.stream().map(s -> s.getItem().copy()).toList());
        int moved=0;
        for(Slot source:menu.slots) {
            boolean playerSource=source.container==player.getInventory() && source.getContainerSlot()<36;
            if(quick?source.index!=request.source():reserved?!playerSource:playerSource!=to) continue;
            if(!source.mayPickup(player)) continue;
            ItemStack remaining=stacks.get(source.index); if(remaining.isEmpty()) continue;
            if(reserved && snapshot.slots().getOrDefault(source.getContainerSlot(),Rule.OPEN).forMoving().preferred(remaining)) continue;
            List<Slot> targets=new ArrayList<>();
            for(Slot target:menu.slots) if(target!=source) {
                boolean playerTarget=target.container==player.getInventory() && target.getContainerSlot()<36;
                boolean selected=reserved?playerTarget && snapshot.slots().getOrDefault(target.getContainerSlot(),Rule.OPEN).forMoving().preferred(remaining)
                    :quick?(menu instanceof ChestMenu?playerSource!=playerTarget:playerSource && playerTarget && ((target.getContainerSlot()<9)!=(source.getContainerSlot()<9)))
                    :playerTarget==from;
                if(selected) targets.add(target);
            }
            targets.sort(Comparator.comparingInt(s -> preferred(s,snapshot,remaining)?0:1));
            for(Slot target:targets) {
                var existing=stacks.get(target.index);
                if(!target.mayPlace(remaining) || !existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing,remaining)) continue;
                int amount=Math.min(remaining.getCount(),Math.max(0,target.getMaxStackSize(remaining)-existing.getCount()));
                if(amount>0) { stacks.set(target.index,remaining.copyWithCount(existing.getCount()+amount)); remaining.shrink(amount); moved+=amount; }
                if(remaining.isEmpty()) break;
            }
        }
        for(Slot slot:menu.slots) if(!ItemStack.matches(slot.getItem(),stacks.get(slot.index))) slot.set(stacks.get(slot.index));
        return new StorageProtocol.Result(true,moved==0?"No allowed items could move.":"Moved "+moved+" items.");
    }
    private static boolean preferred(Slot slot,FilterSnapshot snapshot,ItemStack item) {
        if(slot.container instanceof Inventory) return snapshot.slots().getOrDefault(slot.getContainerSlot(),Rule.OPEN).forMoving().preferred(item);
        var part=resolve(slot.container,slot.getContainerSlot()); return part!=null && ((ChestData)part.block()).msmr$profile().slot(part.slot()).forMoving().preferred(item);
    }
}
