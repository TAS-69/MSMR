package dev.msmr;

import java.util.*;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;

public record Rule(Mode mode, List<String> entries, int limit, Boolean pickups, Boolean moving) {
    private static final Map<Rule,List<Predicate<ItemStack>>> compiled=Collections.synchronizedMap(new WeakHashMap<>());
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
        boolean match=false;
        for(var predicate:compiled.computeIfAbsent(this,Rule::compile)) if(predicate.test(stack)) { match=true; break; }
        return mode == Mode.WHITELIST ? match : !match;
    }
    private static List<Predicate<ItemStack>> compile(Rule rule) {
        List<Predicate<ItemStack>> result=new ArrayList<>();
        for(String entry:rule.entries()) {
            if(entry.startsWith("@")) { String name=entry.substring(1); result.add(stack -> ItemCategories.matches(name,stack)); }
            else if(entry.startsWith("#block:")) { var tag=TagKey.create(Registries.BLOCK,ResourceLocation.parse(entry.substring(7))); result.add(stack -> stack.getItem() instanceof net.minecraft.world.item.BlockItem block && block.getBlock().builtInRegistryHolder().is(tag)); }
            else if(entry.startsWith("#")) { var tag=TagKey.create(Registries.ITEM,ResourceLocation.parse(entry.substring(1))); result.add(stack -> stack.is(tag)); }
            else { var id=ResourceLocation.parse(entry); result.add(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id)); }
        }
        return List.copyOf(result);
    }
    public int cap(ItemStack stack) { return limit == 0 ? stack.getMaxStackSize() : Math.min(limit, stack.getMaxStackSize()); }
    public boolean unrestricted() { return mode==Mode.ALL && limit==0; }
    public boolean preferred(ItemStack stack) { return mode == Mode.WHITELIST && accepts(stack); }
}
