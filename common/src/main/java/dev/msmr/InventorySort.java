package dev.msmr;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;

public final class InventorySort {
    private InventorySort() {}
    /** Pure planner retained for testing; never directly mutates a live inventory. */
    public static ItemStack[] plan(List<ItemStack> original,List<Rule> rules) {
        if (original.size()!=rules.size()) throw new IllegalArgumentException("Rule count differs from slot count");
        List<ItemStack> pool=new ArrayList<>();
        for (ItemStack stack:original) if (!stack.isEmpty()) pool.add(stack.copy());
        pool.sort(Comparator.comparing(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString()));
        ItemStack[] result=new ItemStack[original.size()]; Arrays.fill(result,ItemStack.EMPTY);
        for (int pass=0;pass<2;pass++) for (int i=0;i<original.size();i++) {
            Rule rule=rules.get(i);
            for (ItemStack stack:pool) {
                if (stack.isEmpty() || !rule.accepts(stack) || rule.preferred(stack)!=(pass==0)) continue;
                ItemStack existing=result[i];
                if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing,stack)) continue;
                int amount=Math.min(stack.getCount(),rule.cap(stack)-existing.getCount());
                if (amount<=0) continue;
                if (existing.isEmpty()) result[i]=stack.split(amount); else { existing.grow(amount); stack.shrink(amount); }
            }
        }
        if (pool.stream().anyMatch(s -> !s.isEmpty())) return null;
        return result;
    }
}
