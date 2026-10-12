package dev.msmr;
import java.util.*;

/** Local named snapshots. Copies never share a mutable slot map. */
public final class ProfileBook {
    public record Profile(Rule inventory,Map<Integer,Rule> slots) {
        public Profile { inventory=Objects.requireNonNull(inventory); slots=Map.copyOf(slots); }
    }
    private final Map<String,Profile> profiles=new TreeMap<>();
    private transient long revision;
    public long revision() { return revision; }
    public List<String> names() { return List.copyOf(profiles.keySet()); }
    public Profile get(String name) { return profiles.get(name); }
    public void save(String name,Profile profile) {
        validate(name);
        if(profiles.size()>=64 && !profiles.containsKey(name)) throw new IllegalArgumentException("Maximum 64 profiles.");
        profiles.put(name,new Profile(profile.inventory(),profile.slots()));
        revision++;
    }
    public void copy(String source,String target) {
        if(source.equals(target)) throw new IllegalArgumentException("Use a different copy name.");
        if(profiles.containsKey(target)) throw new IllegalArgumentException("That name already exists.");
        Profile profile=get(source);
        if(profile==null) throw new IllegalArgumentException("Select a profile first.");
        save(target,profile);
    }
    public boolean delete(String name) { boolean removed=profiles.remove(name)!=null; if(removed) revision++; return removed; }
    public static void validate(String name) {
        if(name==null || !name.matches("[A-Za-z0-9 _-]{1,32}") || name.isBlank()) throw new IllegalArgumentException("Name: 1–32 letters, numbers, spaces, _ or -.");
    }
}
