package dev.msmr;

import java.util.*;

/** One chest half or barrel, independent of player rules and named player profiles. */
public record ChestProfile(Rule filter,Map<Integer,Rule> slots) {
    public static final ChestProfile EMPTY=new ChestProfile(Rule.OPEN,Map.of());
    public ChestProfile {
        Objects.requireNonNull(filter); Objects.requireNonNull(slots);
        slots.forEach((slot,rule) -> {
            if(slot==null || slot<0 || slot>=27 || rule==null) throw new IllegalArgumentException("Invalid chest slot");
        });
        slots=Map.copyOf(slots);
    }
    public ChestProfile withFilter(Rule rule) { return new ChestProfile(rule,slots); }
    public ChestProfile withSlot(int slot,Rule rule) {
        if(slot<0 || slot>=27) throw new IllegalArgumentException("Invalid chest slot");
        Objects.requireNonNull(rule);
        var updated=new HashMap<>(slots);
        if(rule.equals(Rule.OPEN)) updated.remove(slot); else updated.put(slot,rule);
        return new ChestProfile(filter,updated);
    }
    public Rule slot(int slot) { return slots.getOrDefault(slot,Rule.OPEN); }
    public boolean empty() { return filter.equals(Rule.OPEN) && slots.isEmpty(); }
}
