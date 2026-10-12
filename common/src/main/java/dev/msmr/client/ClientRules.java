package dev.msmr.client;

import dev.msmr.Rule;
import dev.msmr.ProfileBook;
import dev.msmr.ChestAddress;
import dev.msmr.ChestProfile;
import dev.msmr.QuickProfiles;
import com.google.gson.Gson;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;

public final class ClientRules {
    private static final Gson JSON = new Gson();
    public static final Map<Integer, Rule> rules = new HashMap<>();
    public static Rule clipboard = Rule.OPEN;
    private static final Map<Integer,Rule> containerRules = new HashMap<>();
    private static AbstractContainerMenu attached;
    private static ChestTracker.Target chest;
    private static String scope = "";
    private static Settings settings = new Settings();
    private static boolean loaded;
    private static Path file;
    private static boolean savePending;
    private static long saveRetryAt;
    private static ProfileBook shortcutBook;
    private static QuickProfiles shortcutState;
    private static long shortcutRevision=-1;
    public record Tools(Boolean transferTo,Boolean transferFrom,Boolean shortcuts,Boolean sortOnSwitch) {
        public Tools { transferTo=transferTo==null?true:transferTo; transferFrom=transferFrom==null?true:transferFrom; shortcuts=shortcuts==null?true:shortcuts; sortOnSwitch=sortOnSwitch==null?true:sortOnSwitch; }
        public static final Tools DEFAULT=new Tools(true,true,true,true);
    }
    public static final class Settings {
        public Map<String, Map<Integer, Rule>> inventories = new HashMap<>();
        public Map<String, Map<Integer, Rule>> profiles = new HashMap<>();
        public Map<String,Rule> inventoryFilters = new HashMap<>();
        public Map<String,ProfileBook> books = new HashMap<>();
        public Map<String,String> activeProfiles = new HashMap<>();
        public Map<String,Map<String,ChestProfile>> chests = new HashMap<>();
        public Map<String,QuickProfiles> shortcuts = new HashMap<>();
        public Map<String,Set<String>> sharedChests = new HashMap<>();
        public Tools tools=Tools.DEFAULT;
    }
    public static void init() {
        if (loaded) return;
        file = Minecraft.getInstance().gameDirectory.toPath().resolve("config/msmr-client.json");
        try {
            if (Files.exists(file)) settings=JSON.fromJson(Files.readString(file),Settings.class);
            if (settings==null || settings.inventories==null || settings.profiles==null) throw new IllegalArgumentException("Invalid settings");
            if(settings.inventoryFilters==null) settings.inventoryFilters=new HashMap<>();
            if(settings.books==null) settings.books=new HashMap<>();
            if(settings.activeProfiles==null) settings.activeProfiles=new HashMap<>();
            if(settings.chests==null) settings.chests=new HashMap<>();
            if(settings.shortcuts==null) settings.shortcuts=new HashMap<>();
            if(settings.sharedChests==null) settings.sharedChests=new HashMap<>();
            if(settings.tools==null) settings.tools=Tools.DEFAULT;
            validate(settings);
            for(var old:settings.profiles.entrySet()) {
                int divider=old.getKey().lastIndexOf('/');
                if(divider<0) continue;
                String oldScope=old.getKey().substring(0,divider), name=old.getKey().substring(divider+1);
                ProfileBook book=settings.books.computeIfAbsent(oldScope,k -> new ProfileBook());
                if(book.get(name)==null) book.save(name,new ProfileBook.Profile(Rule.OPEN,old.getValue()));
            }
            settings.profiles.clear();
            loaded=true;
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger("MSMR").error("Cannot read MSMR client settings; preserving the file",ex);
            settings=new Settings(); // Do not overwrite a damaged file.
        }
    }
    /** Reject malformed nested data before it can crash an inventory screen or overwrite the file. */
    public static void validate(Settings data) {
        Objects.requireNonNull(data.inventories); Objects.requireNonNull(data.profiles);
        for(var slots:data.inventories.values()) validateSlots(slots);
        for(var slots:data.profiles.values()) validateSlots(slots);
        for(Rule rule:data.inventoryFilters.values()) Objects.requireNonNull(rule);
        for(ProfileBook book:data.books.values()) {
            Objects.requireNonNull(book);
            if(book.names().size()>64) throw new IllegalArgumentException("Too many profiles");
            for(String name:book.names()) {
                ProfileBook.validate(name); var profile=Objects.requireNonNull(book.get(name));
                Objects.requireNonNull(profile.inventory()); validateSlots(profile.slots());
            }
        }
        for(String name:data.activeProfiles.values()) ProfileBook.validate(name);
        Objects.requireNonNull(data.chests);
        Objects.requireNonNull(data.tools); Objects.requireNonNull(data.shortcuts);
        for(var shortcuts:data.shortcuts.values()) Objects.requireNonNull(shortcuts).validate();
        Objects.requireNonNull(data.sharedChests); for(var known:data.sharedChests.values()) { Objects.requireNonNull(known); known.forEach(ChestAddress::parse); }
        for(var storage:data.chests.values()) {
            Objects.requireNonNull(storage);
            for(var entry:storage.entrySet()) {
                ChestAddress.parse(entry.getKey());
                var profile=Objects.requireNonNull(entry.getValue());
                new ChestProfile(profile.filter(),profile.slots());
            }
        }
    }
    private static void validateSlots(Map<Integer,Rule> slots) {
        Objects.requireNonNull(slots);
        for(var entry:slots.entrySet()) {
            if(entry.getKey()==null || entry.getKey()<0 || entry.getKey()>=36) throw new IllegalArgumentException("Invalid inventory slot");
            Objects.requireNonNull(entry.getValue());
        }
    }
    private static Map<Integer,Rule> inventory() { return settings.inventories.computeIfAbsent(scope,k -> new HashMap<>()); }
    private static String currentScope(Minecraft mc) {
        String world=mc.getCurrentServer()!=null ? "server/"+mc.getCurrentServer().ip
            : mc.getSingleplayerServer()!=null ? "world/"+mc.getSingleplayerServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize()
            : "local";
        return world+"/"+mc.player.getUUID();
    }
    public static void refresh() {
        Minecraft mc=Minecraft.getInstance();
        if(loaded && savePending && System.nanoTime()>saveRetryAt) save();
        if (mc.player==null) { attached=null; chest=null; ChestTracker.clear(); rules.clear(); dev.msmr.ServerPickupRules.clearLocal(); ClientCapabilities.update(Rule.OPEN,Map.of()); return; }
        if (file==null) init();
        String next=currentScope(mc);
        if (!next.equals(scope) || attached!=mc.player.containerMenu) { containerRules.clear(); attached=mc.player.containerMenu; scope=next; }
        chest=ChestTracker.current(attached);
        rules.clear();
        for (Slot slot:attached.slots) {
            if (slot.container==mc.player.getInventory()) {
                int i=slot.getContainerSlot();
                if(i>=0 && i<36) rules.put(slot.index,inventory().getOrDefault(i,Rule.OPEN));
            } else if (supported(attached)) rules.put(slot.index,chest!=null?chestProfile(chest.address(slot.getContainerSlot())).slot(slot.getContainerSlot()%27):containerRules.getOrDefault(slot.index,Rule.OPEN));
        }
        dev.msmr.ServerPickupRules.publishLocal(mc.getSingleplayerServer(),mc.player.getUUID(),inventoryFilter(),inventory());
        ClientCapabilities.update(inventoryFilter(),inventory());
    }
    private static boolean supported(AbstractContainerMenu menu) {
        return menu instanceof ChestMenu || menu instanceof HopperMenu || menu instanceof ShulkerBoxMenu || menu instanceof DispenserMenu;
    }
    public static Rule rule(Slot slot) { return rules.getOrDefault(slot.index,Rule.OPEN); }
    public static Rule inventoryFilter() { return settings.inventoryFilters.getOrDefault(scope,Rule.OPEN); }
    public static Rule inventoryFilter(Slot slot) {
        var player=Minecraft.getInstance().player;
        if(player!=null && slot.container==player.getInventory() && slot.getContainerSlot()>=0 && slot.getContainerSlot()<36) return inventoryFilter();
        return chest!=null && slot.index>=0 && slot.index<chest.blocks().size()*27 ? chestProfile(chest.address(slot.getContainerSlot())).filter() : Rule.OPEN;
    }
    public static Rule moveRule(Slot slot) { return rule(slot).forMoving(); }
    public static Rule moveInventoryFilter(Slot slot) { return inventoryFilter(slot).forMoving(); }
    public static boolean hasFilters() { return !inventoryFilter().forMoving().unrestricted() || rules.values().stream().anyMatch(r -> !r.forMoving().unrestricted()) || chest!=null && chest.blocks().stream().anyMatch(a -> !chestProfile(a).filter().forMoving().unrestricted()); }
    public static boolean accepts(Slot slot,net.minecraft.world.item.ItemStack stack) { return moveRule(slot).accepts(stack) && moveInventoryFilter(slot).accepts(stack); }
    public static String setInventoryFilter(Rule rule) {
        refresh(); settings.inventoryFilters.put(scope,rule); settings.activeProfiles.remove(scope); refresh();
        return save() ? "Inventory filter applied." : "Applied in memory; settings could not be saved.";
    }
    private static ProfileBook book() { return settings.books.computeIfAbsent(scope,k -> new ProfileBook()); }
    public static List<String> profileNames() { refresh(); return book().names(); }
    public static String activeProfile() { return settings.activeProfiles.getOrDefault(scope,"Custom"); }
    public static Tools tools() { return settings.tools; }
    public static String tools(Tools value) { settings.tools=Objects.requireNonNull(value); if(!value.sortOnSwitch()) ClientTransfers.cancelRequestedSort(); return save()?"Saved automatically.":"Settings could not be saved."; }
    private static QuickProfiles shortcuts() {
        var current=book(); var value=settings.shortcuts.computeIfAbsent(scope,k -> new QuickProfiles());
        if(current!=shortcutBook || value!=shortcutState || current.revision()!=shortcutRevision) {
            shortcutBook=current; shortcutState=value; shortcutRevision=current.revision();
            if(value.sync(current.names())) save();
        }
        return value;
    }
    public static List<String> quickProfiles() { return shortcuts().names(); }
    public static QuickProfiles.Badge badge(String name) { return shortcuts().badge(name); }
    public static String badge(String name,QuickProfiles.Badge value) {
        try { shortcuts().appearance(name,value); return save()?"Saved automatically.":"Settings could not be saved."; } catch(IllegalArgumentException ex) { return ex.getMessage(); }
    }
    public static String pin(String name,boolean value) {
        try { shortcuts().pin(name,value); shortcutRevision=-1; return save()?"Saved automatically.":"Settings could not be saved."; } catch(IllegalArgumentException ex) { return ex.getMessage(); }
    }
    public static String manageProfile(String action,String selected,String name) {
        refresh();
        if(ClientTransfers.busy()) return "Finish the current inventory move first.";
        try {
            switch(action) {
                case "save" -> { book().save(name,new ProfileBook.Profile(inventoryFilter(),inventory())); settings.activeProfiles.put(scope,name); }
                case "switch" -> {
                    if(tools().sortOnSwitch() && !Minecraft.getInstance().player.containerMenu.getCarried().isEmpty()) return "Empty your cursor before switching profiles.";
                    var profile=book().get(selected);
                    if(profile==null) return "Select a saved profile.";
                    settings.inventories.put(scope,new HashMap<>(profile.slots())); settings.inventoryFilters.put(scope,profile.inventory()); settings.activeProfiles.put(scope,selected);
                }
                case "copy" -> book().copy(selected,name);
                case "delete" -> { if(!book().delete(selected)) return "Select a saved profile."; if(selected.equals(activeProfile())) settings.activeProfiles.remove(scope); }
                default -> { return "Unknown action."; }
            }
            shortcuts(); boolean saved=save(); refresh();
            if(action.equals("switch") && tools().sortOnSwitch()) ClientTransfers.requestSort();
            return saved ? switch(action) { case "save" -> "Profile saved."; case "switch" -> "Profile switched."; case "copy" -> "Profile copied."; default -> "Profile deleted; current rules kept."; } : "Active in memory; settings could not be saved.";
        } catch(IllegalArgumentException ex) { return ex.getMessage(); }
    }
    public static String edit(int index,Rule rule) {
        refresh(); if (attached==null || !rules.containsKey(index)) return "Slot is no longer available.";
        Slot slot=attached.getSlot(index);
        boolean saved=true;
        if (slot.container instanceof Inventory) { inventory().put(slot.getContainerSlot(),rule); settings.activeProfiles.remove(scope); saved=save(); }
        else if(chest!=null) { if(!ClientChestSync.canEdit()) return "Wait for shared rules, or ask the filter owner to edit them."; putChest(chest.address(slot.getContainerSlot()),chestProfile(chest.address(slot.getContainerSlot())).withSlot(slot.getContainerSlot()%27,rule)); saved=save(); ClientChestSync.edited(); }
        else containerRules.put(index,rule);
        refresh();
        return saved?"Saved automatically.":"Applied in memory; settings could not be saved.";
    }
    public static boolean hasChest() { return chest!=null; }
    public static boolean chestSlot(Slot slot) { return chest!=null && slot.container!=Minecraft.getInstance().player.getInventory(); }
    private static Map<String,ChestProfile> storage() { return settings.chests.computeIfAbsent(scope,k -> new HashMap<>()); }
    private static ChestProfile chestProfile(ChestAddress address) { return storage().getOrDefault(address.key(),ChestProfile.EMPTY); }
    private static void putChest(ChestAddress address,ChestProfile profile) {
        if(profile.empty()) storage().remove(address.key()); else storage().put(address.key(),profile);
    }
    public static Rule chestFilter() { return chest==null?Rule.OPEN:chestProfile(chest.blocks().getFirst()).filter(); }
    public static boolean mixedChestFilters() { return chest!=null && chest.blocks().stream().anyMatch(a -> !chestProfile(a).filter().equals(chestFilter())); }
    public static String chestDescription() { return chest==null?"":ClientChestSync.description(); }
    public static List<ChestProfile> chestProfiles() { return chest==null?List.of():chest.blocks().stream().map(ClientRules::chestProfile).toList(); }
    public static boolean mayImportChestProfiles() { var known=settings.sharedChests.getOrDefault(scope,Set.of()); return chest!=null && chest.blocks().stream().noneMatch(a -> known.contains(a.key())); }
    public static void markSharedChest() { if(chest==null) return; var known=settings.sharedChests.computeIfAbsent(scope,k -> new HashSet<>()); boolean changed=false; for(var a:chest.blocks()) changed|=known.add(a.key()); if(changed) save(); }
    public static void acceptChestProfiles(List<ChestProfile> values) { if(chest==null || values.size()!=chest.blocks().size()) return; boolean changed=false; for(int i=0;i<values.size();i++) { var address=chest.blocks().get(i); if(!chestProfile(address).equals(values.get(i))) { putChest(address,values.get(i)); changed=true; } } if(changed) save(); }
    public static String setChestFilter(Rule rule) {
        refresh(); if(chest==null) return "Chest is no longer open.";
        if(!ClientChestSync.canEdit()) return "Wait for shared rules, or ask the filter owner to edit them.";
        for(var address:chest.blocks()) putChest(address,chestProfile(address).withFilter(rule));
        ClientChestSync.edited(); refresh(); return save()?"Saved automatically.":"Applied in memory; settings could not be saved.";
    }
    public static void resetChest() {
        refresh(); if(chest==null) return;
        if(!ClientChestSync.canEdit()) return;
        chest.blocks().forEach(address -> storage().remove(address.key())); save(); ClientChestSync.edited(); refresh();
    }
    public static void storageChanged(net.minecraft.client.multiplayer.ClientLevel level,net.minecraft.core.BlockPos position,net.minecraft.world.level.block.state.BlockState state) {
        var mc=Minecraft.getInstance();
        if(!loaded || level!=mc.level || mc.player==null) return;
        var profiles=settings.chests.get(currentScope(mc));
        if(profiles==null || profiles.isEmpty()) return;
        String prefix=level.dimension().location()+"|"+position.getX()+","+position.getY()+","+position.getZ()+"|";
        String current=prefix+net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        boolean removed=profiles.keySet().removeIf(key -> key.startsWith(prefix) && !key.equals(current));
        if(removed) save();
    }
    private static boolean save() {
        if (!loaded) return false;
        try {
            Files.createDirectories(file.getParent());
            Path temporary=file.resolveSibling("msmr-client.json.tmp");
            Files.writeString(temporary,JSON.toJson(settings));
            try { Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING); }
            savePending=false; return true;
        } catch (Exception ex) { if(!savePending) org.slf4j.LoggerFactory.getLogger("MSMR").error("Cannot save client rules; will retry",ex); savePending=true; saveRetryAt=System.nanoTime()+2_000_000_000L; return false; }
    }
    public static String preset(String action,String name) {
        return manageProfile(action.equals("save")?"save":"switch",name,name);
    }
}
