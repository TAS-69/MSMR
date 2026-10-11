package dev.msmr;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Plans a complete, count-conserving arrangement before sending any inventory clicks. */
public final class InventorySort {
    public record Click(int slot,int button,ItemStack before,ItemStack cursor) {}
    public record Plan(List<ItemStack> result,List<Click> clicks,String error) {
        public boolean possible() { return error.isEmpty(); }
    }
    private static final class Stock {
        final ItemStack item; int count;
        Stock(ItemStack stack) { item=stack.copyWithCount(1); count=stack.getCount(); }
    }
    private InventorySort() {}
    public static ItemStack[] plan(List<ItemStack> inventory,List<Rule> rules) {
        Plan plan=plan(inventory,rules,Rule.OPEN); return plan.possible()?plan.result().toArray(ItemStack[]::new):null;
    }
    public static Plan plan(List<ItemStack> inventory,List<Rule> rules,Rule inventoryRule) {
        if(inventory.size()!=rules.size()) throw new IllegalArgumentException("Slot rules do not match inventory");
        Rule wide=inventoryRule.forMoving();
        List<Rule> movement=rules.stream().map(Rule::forMoving).toList();
        List<Stock> stock=new ArrayList<>();
        for(var stack:inventory) if(!stack.isEmpty()) {
            Stock found=stock.stream().filter(s -> ItemStack.isSameItemSameComponents(s.item,stack)).findFirst().orElse(null);
            if(found==null) stock.add(new Stock(stack)); else found.count+=stack.getCount();
        }
        stock.sort(Comparator.comparing(s -> BuiltInRegistries.ITEM.getKey(s.item.getItem()).toString()));
        List<ItemStack> result=new ArrayList<>(Collections.nCopies(inventory.size(),ItemStack.EMPTY));
        List<Integer> order=new ArrayList<>(); for(int i=0;i<inventory.size();i++) order.add(i);
        order.sort(Comparator.<Integer>comparingInt(i -> movement.get(i).mode()==Rule.Mode.WHITELIST?0:1)
            .thenComparingLong(i -> stock.stream().filter(s -> movement.get(i).accepts(s.item) && wide.accepts(s.item)).count()).thenComparingInt(i -> i));
        for(int i:order) {
            Rule rule=movement.get(i);
            for(Stock s:stock) if(s.count>0 && rule.accepts(s.item) && wide.accepts(s.item)) {
                int amount=Math.min(s.count,Math.min(rule.cap(s.item),wide.cap(s.item)));
                result.set(i,s.item.copyWithCount(amount)); s.count-=amount; break;
            }
        }
        if(stock.stream().anyMatch(s -> s.count>0)) return failure("Not enough allowed space. Items were left in place.");
        return clicks(inventory,result);
    }
    private static Plan failure(String message) { return new Plan(List.of(),List.of(),message); }
    private static boolean same(ItemStack a,ItemStack b) { return !a.isEmpty() && !b.isEmpty() && ItemStack.isSameItemSameComponents(a,b); }
    private static int excess(ItemStack present,ItemStack wanted) { return present.isEmpty()?0:present.getCount()-(same(present,wanted)?wanted.getCount():0); }
    private static Plan clicks(List<ItemStack> original,List<ItemStack> wanted) {
        var state=new ArrayList<>(original.stream().map(ItemStack::copy).toList());
        var actions=new ArrayList<Click>();
        for(int pass=0;pass<4096;pass++) {
            boolean complete=true;
            for(int i=0;i<state.size();i++) if(!ItemStack.matches(state.get(i),wanted.get(i))) { complete=false; break; }
            if(complete) return new Plan(List.copyOf(wanted),List.copyOf(actions),"");
            boolean moved=false;
            // Fill empty or matching destinations from surplus stacks, preserving the donor's own allocation.
            for(int to=0;to<state.size() && !moved;to++) {
                var goal=wanted.get(to); var existing=state.get(to);
                if(goal.isEmpty() || !existing.isEmpty() && !same(existing,goal)) continue;
                int needed=goal.getCount()-existing.getCount(); if(needed<=0) continue;
                for(int from=0;from<state.size();from++) if(from!=to && same(state.get(from),goal)) {
                    int amount=Math.min(needed,excess(state.get(from),wanted.get(from))); if(amount<=0) continue;
                    ItemStack cursor=click(state,actions,from,0,ItemStack.EMPTY);
                    int capacity=cursor.getMaxStackSize()-state.get(to).getCount();
                    if(amount==Math.min(capacity,cursor.getCount())) cursor=click(state,actions,to,0,cursor);
                    else for(int n=0;n<amount;n++) cursor=click(state,actions,to,1,cursor);
                    if(!cursor.isEmpty()) cursor=click(state,actions,from,0,cursor);
                    if(!cursor.isEmpty()) return failure("Could not safely arrange these stacks. Items were left in place.");
                    moved=true; break;
                }
            }
            if(moved) continue;
            // Resolve a cycle of misplaced stacks; the empty donor slot holds the displaced stack.
            for(int to=0;to<state.size() && !moved;to++) if(!wanted.get(to).isEmpty() && !same(state.get(to),wanted.get(to))) {
                for(int from=0;from<state.size();from++) if(from!=to && same(state.get(from),wanted.get(to)) && !same(state.get(from),wanted.get(from))) {
                    ItemStack cursor=click(state,actions,from,0,ItemStack.EMPTY);
                    cursor=click(state,actions,to,0,cursor);
                    if(!cursor.isEmpty()) cursor=click(state,actions,from,0,cursor);
                    if(!cursor.isEmpty()) return failure("Could not safely arrange these stacks. Items were left in place.");
                    moved=true; break;
                }
            }
            if(!moved) break;
        }
        return failure("Could not safely arrange these stacks. Items were left in place.");
    }
    private static ItemStack click(List<ItemStack> state,List<Click> actions,int slot,int button,ItemStack held) {
        ItemStack before=state.get(slot),cursor=held.copy();
        actions.add(new Click(slot,button,before.copy(),cursor.copy()));
        if(cursor.isEmpty()) { cursor=before.copy(); state.set(slot,ItemStack.EMPTY); }
        else if(before.isEmpty() || same(before,cursor)) {
            int amount=Math.min(button==1?1:cursor.getCount(),cursor.getMaxStackSize()-before.getCount());
            state.set(slot,cursor.copyWithCount(before.getCount()+amount)); cursor.shrink(amount);
        } else { state.set(slot,cursor.copy()); cursor=before.copy(); }
        return cursor;
    }
}
