package dev.msmr;

import java.util.*;

/** Validated player-owned rules; no other player, entity or container can be targeted. */
public record FilterSnapshot(Rule inventory,Map<Integer,Rule> slots) {
    public FilterSnapshot {
        Objects.requireNonNull(inventory); slots=Map.copyOf(slots);
        if(slots.size()>36) throw new IllegalArgumentException("Too many slots");
        for(var entry:slots.entrySet()) if(entry.getKey()<0 || entry.getKey()>=36) throw new IllegalArgumentException("Invalid slot");
    }
}
