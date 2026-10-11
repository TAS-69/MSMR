package dev.msmr;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.resources.ResourceLocation;

/** Up to five shortcuts, with appearances independent of the saved inventory rules. */
public final class QuickProfiles {
    public static final int[] COLORS={0x426A9B,0x397B53,0x95513F,0x80599C,0x96732F,0x397F83,0x995B7C,0x626873};
    public record Badge(String letter,String item,int color) {
        public Badge {
            if(letter==null || letter.length()!=1 || !Character.isLetterOrDigit(letter.charAt(0))) throw new IllegalArgumentException("Choose one letter or number.");
            if(item==null || !item.isEmpty() && ResourceLocation.tryParse(item)==null) throw new IllegalArgumentException("Choose a valid item icon.");
            if(color<0 || color>0xFFFFFF) throw new IllegalArgumentException("Choose a valid background colour.");
        }
        public static Badge create(String name) { char first=(char)name.chars().filter(Character::isLetterOrDigit).findFirst().orElse('P'); return new Badge(String.valueOf(Character.toUpperCase(first)),"",COLORS[ThreadLocalRandom.current().nextInt(COLORS.length)]); }
    }
    private Map<String,Badge> badges=new LinkedHashMap<>();
    private List<String> pinned=new ArrayList<>();
    private Set<String> hidden=new HashSet<>();
    public boolean sync(List<String> names) {
        var oldBadges=new LinkedHashMap<>(badges); var oldPinned=new ArrayList<>(pinned); var oldHidden=new HashSet<>(hidden);
        badges.keySet().retainAll(names); pinned.removeIf(n -> !names.contains(n)); hidden.retainAll(names);
        for(String name:names) {
            badges.computeIfAbsent(name,Badge::create);
            if(pinned.size()<5 && !pinned.contains(name) && !hidden.contains(name)) pinned.add(name);
        }
        return !oldBadges.equals(badges) || !oldPinned.equals(pinned) || !oldHidden.equals(hidden);
    }
    public List<String> names() { return List.copyOf(pinned); }
    public Badge badge(String name) { return badges.get(name); }
    public void appearance(String name,Badge badge) { if(!badges.containsKey(name)) throw new IllegalArgumentException("Select a saved profile."); badges.put(name,Objects.requireNonNull(badge)); }
    public void pin(String name,boolean value) {
        if(!badges.containsKey(name)) throw new IllegalArgumentException("Select a saved profile.");
        if(value) { if(!pinned.contains(name) && pinned.size()>=5) throw new IllegalArgumentException("Five shortcuts maximum. Hide another first."); hidden.remove(name); if(!pinned.contains(name)) pinned.add(name); }
        else { pinned.remove(name); hidden.add(name); }
    }
    public void validate() {
        Objects.requireNonNull(badges); Objects.requireNonNull(pinned); Objects.requireNonNull(hidden);
        if(pinned.size()>5 || new HashSet<>(pinned).size()!=pinned.size()) throw new IllegalArgumentException("Invalid profile shortcuts");
        for(var e:badges.entrySet()) { ProfileBook.validate(e.getKey()); var b=Objects.requireNonNull(e.getValue()); new Badge(b.letter(),b.item(),b.color()); }
        for(String name:pinned) if(!badges.containsKey(name)) throw new IllegalArgumentException("Missing shortcut appearance");
        for(String name:hidden) ProfileBook.validate(name);
    }
}
