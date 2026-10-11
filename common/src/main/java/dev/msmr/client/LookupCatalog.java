package dev.msmr.client;

import dev.msmr.*;
import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;

public final class LookupCatalog {
    public enum Kind { ITEMS, BLOCKS, TAGS, BLOCK_TAGS, GROUPS, CHOSEN }
    public record Entry(String token,String name,Kind kind,List<ItemStack> members,Set<String> memberIds,String related) {
        public int size() { return members.size(); }
        public ItemStack icon() { return members.isEmpty()?ItemStack.EMPTY:members.getFirst(); }
    }
    private LookupCatalog() {}
    public static String readable(String id) {
        String path=id.contains(":")?id.substring(id.indexOf(':')+1):id;
        return Arrays.stream(path.replace('/',' ').replace('_',' ').split(" ")).filter(s -> !s.isEmpty()).map(s -> Character.toUpperCase(s.charAt(0))+s.substring(1)).collect(Collectors.joining(" "));
    }
    private static Entry entry(String token,String name,Kind kind,List<ItemStack> members) {
        Set<String> ids=members.stream().map(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString()).collect(Collectors.toUnmodifiableSet());
        String related=members.stream().map(s -> s.getHoverName().getString()+" "+BuiltInRegistries.ITEM.getKey(s.getItem())).collect(Collectors.joining(" "));
        return new Entry(token,name,kind,List.copyOf(members),ids,related);
    }
    public static List<Entry> build() {
        List<Entry> result=new ArrayList<>(); List<ItemStack> items=new ArrayList<>();
        for(Item item:BuiltInRegistries.ITEM) {
            if(item==Items.AIR) continue;
            ItemStack stack=item.getDefaultInstance(); items.add(stack); String id=BuiltInRegistries.ITEM.getKey(item).toString();
            Entry plain=entry(id,stack.getHoverName().getString(),Kind.ITEMS,List.of(stack));
            String aliases=stack.getTags().map(t -> t.location().toString()).collect(Collectors.joining(" "))+" "+ItemCategories.NAMES.stream().filter(n -> ItemCategories.matches(n,stack)).collect(Collectors.joining(" "));
            if(item instanceof BlockItem block) aliases+=" "+block.getBlock().builtInRegistryHolder().tags().map(t -> t.location().toString()).collect(Collectors.joining(" "));
            result.add(new Entry(plain.token(),plain.name(),plain.kind(),plain.members(),plain.memberIds(),plain.related()+" "+aliases));
        }
        BuiltInRegistries.ITEM.getTags().forEach(pair -> {
            String id=pair.getFirst().location().toString();
            List<ItemStack> members=pair.getSecond().stream().map(h -> h.value().getDefaultInstance()).filter(s -> !s.isEmpty()).toList();
            result.add(entry("#"+id,readable(id),Kind.TAGS,members));
        });
        BuiltInRegistries.BLOCK.getTags().forEach(pair -> {
            String id=pair.getFirst().location().toString();
            List<ItemStack> members=pair.getSecond().stream().map(h -> h.value().asItem()).filter(i -> i!=Items.AIR).distinct().map(Item::getDefaultInstance).toList();
            result.add(entry("#block:"+id,readable(id),Kind.BLOCK_TAGS,members));
        });
        for(String name:ItemCategories.NAMES) result.add(entry("@"+name,readable(name),Kind.GROUPS,items.stream().filter(s -> ItemCategories.matches(name,s)).toList()));
        result.sort(Comparator.comparing(Entry::name,String.CASE_INSENSITIVE_ORDER).thenComparing(Entry::token));
        return List.copyOf(result);
    }
    public static boolean inTab(Entry e,Kind tab,Set<String> chosen) {
        return tab==Kind.CHOSEN ? chosen.contains(e.token()) : tab==Kind.BLOCKS ? e.kind()==Kind.ITEMS && e.icon().getItem() instanceof BlockItem : e.kind()==tab;
    }
}
