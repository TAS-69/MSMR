package dev.msmr;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;

public record Rule(Mode mode, List<String> entries, int limit, Boolean pickups, Boolean moving) {
    public Rule(Mode mode,List<String> entries,int limit) { this(mode,entries,limit,true,true); }
    public Rule forMoving() { return moving ? this : OPEN; }
    public Rule forPickups() { return pickups ? this : OPEN; }

    public enum Mode { ALL, WHITELIST, BLACKLIST, BLOCK }
    public static final Rule OPEN = new Rule(Mode.ALL, List.of(), 0);
    public Rule {
        Objects.requireNonNull(mode);
        pickups=pickups==null?true:pickups; moving=moving==null?true:moving;
        entries = List.copyOf(entries);
        if (entries.size() > 64 || limit < 0 || limit > 99) throw new IllegalArgumentException("Invalid rule size or limit (0–99)");
        for (String entry : entries) {
            if (entry.startsWith("@")) {
                if (!ItemCategories.NAMES.contains(entry.substring(1))) throw new IllegalArgumentException("Unknown category: "+entry);
                continue;
            }
            if (entry.startsWith("#block:")) {
                if(entry.length()>256 || ResourceLocation.tryParse(entry.substring(7))==null) throw new IllegalArgumentException("Invalid block tag: "+entry);
                continue;
            }
            if (entry.length() > 256 || ResourceLocation.tryParse(entry.startsWith("#") ? entry.substring(1) : entry) == null)
                throw new IllegalArgumentException("Invalid item or tag: " + entry);
        }
    }
    public boolean accepts(ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (mode == Mode.ALL) return true;
        if (mode == Mode.BLOCK) return false;
        boolean match = entries.stream().anyMatch(e -> e.startsWith("@") ? ItemCategories.matches(e.substring(1),stack)
            : e.startsWith("#block:") ? stack.getItem() instanceof net.minecraft.world.item.BlockItem block && block.getBlock().builtInRegistryHolder().is(TagKey.create(Registries.BLOCK,ResourceLocation.parse(e.substring(7)))) : e.startsWith("#")
            ? stack.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(e.substring(1))))
            : BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ResourceLocation.parse(e)));
        return mode == Mode.WHITELIST ? match : !match;
    }
    public int cap(ItemStack stack) { return limit == 0 ? stack.getMaxStackSize() : Math.min(limit, stack.getMaxStackSize()); }
    public boolean unrestricted() { return mode==Mode.ALL && limit==0; }
    public boolean preferred(ItemStack stack) { return mode == Mode.WHITELIST && accepts(stack); }
}
