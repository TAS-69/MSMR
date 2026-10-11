package dev.msmr;

import java.util.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Applies server-side pickup rules while retaining rejected items in the ground stack. */
public final class PickupFilters {
    private PickupFilters() {}
    public static boolean insert(Inventory inventory,ItemStack incoming,Rule wide,Map<Integer,Rule> slots) {
        wide=wide.forPickups();
        if(wide.unrestricted() && slots.values().stream().allMatch(r -> r.forPickups().unrestricted())) return inventory.add(incoming);
        if(incoming.isEmpty() || !wide.accepts(incoming)) return false;
        int before=incoming.getCount();
        List<Integer> order=new ArrayList<>();
        for(int i=0;i<36;i++) order.add(i);
        order.sort(Comparator.comparingInt((Integer i) -> slots.getOrDefault(i,Rule.OPEN).forPickups().preferred(incoming)?0:1)
            .thenComparingInt(i -> inventory.getItem(i).isEmpty()?1:0)
            .thenComparingInt(i -> i==inventory.selected?-1:i));
        for(int i:order) {
            Rule rule=slots.getOrDefault(i,Rule.OPEN).forPickups();
            ItemStack existing=inventory.getItem(i);
            if(!rule.accepts(incoming) || (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing,incoming))) continue;
            int capacity=Math.min(Math.min(rule.cap(incoming),wide.cap(incoming)),inventory.getMaxStackSize(incoming));
            int count=Math.min(incoming.getCount(),Math.max(0,capacity-existing.getCount()));
            if(count==0) continue;
            if(existing.isEmpty()) { existing=incoming.copyWithCount(count); inventory.setItem(i,existing); }
            else existing.grow(count);
            existing.setPopTime(5); incoming.shrink(count);
            if(incoming.isEmpty()) break;
        }
        if(incoming.getCount()<before) { inventory.setChanged(); return true; }
        return false;
    }
}
