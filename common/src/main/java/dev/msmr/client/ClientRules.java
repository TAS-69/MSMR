package dev.msmr.client;

import dev.msmr.Rule;
import dev.msmr.ProfileBook;
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
    private static String scope = "";
    private static Settings settings = new Settings();
    private static boolean loaded;
    private static Path file;
    public static final class Settings {
        public Map<String, Map<Integer, Rule>> inventories = new HashMap<>();
        public Map<String, Map<Integer, Rule>> profiles = new HashMap<>();
        public Map<String,Rule> inventoryFilters = new HashMap<>();
        public Map<String,ProfileBook> books = new HashMap<>();
        public Map<String,String> activeProfiles = new HashMap<>();
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
    }
    private static void validateSlots(Map<Integer,Rule> slots) {
        Objects.requireNonNull(slots);
        for(var entry:slots.entrySet()) {
            if(entry.getKey()==null || entry.getKey()<0 || entry.getKey()>=36) throw new IllegalArgumentException("Invalid inventory slot");
            Objects.requireNonNull(entry.getValue());
        }
    }
    private static Map<Integer,Rule> inventory() { return settings.inventories.computeIfAbsent(scope,k -> new HashMap<>()); }
    public static void refresh() {
        Minecraft mc=Minecraft.getInstance();
        if (mc.player==null) { attached=null; rules.clear(); dev.msmr.ServerPickupRules.clearLocal(); ClientCapabilities.update(Rule.OPEN,Map.of()); return; }
        if (file==null) init();
        String world=mc.getCurrentServer()!=null ? "server/"+mc.getCurrentServer().ip
            : mc.getSingleplayerServer()!=null ? "world/"+mc.getSingleplayerServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize()
            : "local";
        String next=world+"/"+mc.player.getUUID();
        if (!next.equals(scope) || attached!=mc.player.containerMenu) { containerRules.clear(); attached=mc.player.containerMenu; scope=next; }
        rules.clear();
        for (Slot slot:attached.slots) {
            if (slot.container==mc.player.getInventory()) {
                int i=slot.getContainerSlot();
                if(i>=0 && i<36) rules.put(slot.index,inventory().getOrDefault(i,Rule.OPEN));
            } else if (supported(attached)) rules.put(slot.index,containerRules.getOrDefault(slot.index,Rule.OPEN));
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
        return player!=null && slot.container==player.getInventory() && slot.getContainerSlot()>=0 && slot.getContainerSlot()<36 ? inventoryFilter() : Rule.OPEN;
    }
    public static Rule moveRule(Slot slot) { return rule(slot).forMoving(); }
    public static Rule moveInventoryFilter(Slot slot) { return inventoryFilter(slot).forMoving(); }
    public static boolean hasFilters() { return !inventoryFilter().forMoving().unrestricted() || rules.values().stream().anyMatch(r -> !r.forMoving().unrestricted()); }
    public static boolean accepts(Slot slot,net.minecraft.world.item.ItemStack stack) { return moveRule(slot).accepts(stack) && moveInventoryFilter(slot).accepts(stack); }
    public static String setInventoryFilter(Rule rule) {
        refresh(); settings.inventoryFilters.put(scope,rule); settings.activeProfiles.remove(scope); refresh();
        return save() ? "Inventory filter applied." : "Applied in memory; settings could not be saved.";
    }
    private static ProfileBook book() { return settings.books.computeIfAbsent(scope,k -> new ProfileBook()); }
    public static List<String> profileNames() { refresh(); return book().names(); }
    public static String activeProfile() { return settings.activeProfiles.getOrDefault(scope,"Custom"); }
    public static String manageProfile(String action,String selected,String name) {
        refresh();
        try {
            switch(action) {
                case "save" -> { book().save(name,new ProfileBook.Profile(inventoryFilter(),inventory())); settings.activeProfiles.put(scope,name); }
                case "switch" -> {
                    var profile=book().get(selected);
                    if(profile==null) return "Select a saved profile.";
                    settings.inventories.put(scope,new HashMap<>(profile.slots())); settings.inventoryFilters.put(scope,profile.inventory()); settings.activeProfiles.put(scope,selected);
                }
                case "copy" -> book().copy(selected,name);
                case "delete" -> { if(!book().delete(selected)) return "Select a saved profile."; if(selected.equals(activeProfile())) settings.activeProfiles.remove(scope); }
                default -> { return "Unknown action."; }
            }
            boolean saved=save(); refresh();
            return saved ? switch(action) { case "save" -> "Profile saved."; case "switch" -> "Profile switched."; case "copy" -> "Profile copied."; default -> "Profile deleted; current rules kept."; } : "Active in memory; settings could not be saved.";
        } catch(IllegalArgumentException ex) { return ex.getMessage(); }
    }
    public static void edit(int index,Rule rule) {
        refresh(); if (attached==null || !rules.containsKey(index)) return;
        Slot slot=attached.getSlot(index);
        if (slot.container instanceof Inventory) { inventory().put(slot.getContainerSlot(),rule); settings.activeProfiles.remove(scope); save(); }
        else containerRules.put(index,rule);
        refresh();
    }
    private static boolean save() {
        if (!loaded) return false;
        try {
            Files.createDirectories(file.getParent());
            Path temporary=file.resolveSibling("msmr-client.json.tmp");
            Files.writeString(temporary,JSON.toJson(settings));
            try { Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING); }
            return true;
        } catch (Exception ex) { org.slf4j.LoggerFactory.getLogger("MSMR").error("Cannot save client rules",ex); return false; }
    }
    public static String preset(String action,String name) {
        return manageProfile(action.equals("save")?"save":"switch",name,name);
    }
}
