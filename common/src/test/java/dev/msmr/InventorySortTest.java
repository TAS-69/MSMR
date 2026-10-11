package dev.msmr;

import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class InventorySortTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    private List<ItemStack> inventory() { return new ArrayList<>(Collections.nCopies(36,ItemStack.EMPTY)); }
    private List<Rule> rules() { return new ArrayList<>(Collections.nCopies(36,Rule.OPEN)); }
    private Rule only(String entry,int cap) { return new Rule(Rule.Mode.WHITELIST,List.of(entry),cap); }
    @Test void miningAndCombatUseTheSameSavedSlotWithoutChangingItems() {
        var items=inventory(); items.set(0,new ItemStack(Items.IRON_SWORD)); items.set(12,new ItemStack(Items.DIAMOND_PICKAXE));
        var mining=rules(); mining.set(0,only("@pickaxes",0)); var first=InventorySort.plan(items,mining,Rule.OPEN);
        assertTrue(first.possible(),first.error()); assertTrue(first.result().getFirst().is(Items.DIAMOND_PICKAXE)); conserved(items,first);
        var combat=rules(); combat.set(0,only("@swords",0)); var second=InventorySort.plan(first.result(),combat,Rule.OPEN);
        assertTrue(second.possible(),second.error()); assertTrue(second.result().getFirst().is(Items.IRON_SWORD)); conserved(items,second);
    }
    @Test void missingReservedItemLeavesItsSlotEmpty() {
        var items=inventory(); items.set(0,new ItemStack(Items.STONE,64)); var rules=rules(); rules.set(0,only("@pickaxes",0));
        var plan=InventorySort.plan(items,rules,Rule.OPEN); assertTrue(plan.possible(),plan.error()); assertTrue(plan.result().getFirst().isEmpty()); conserved(items,plan);
    }
    @Test void fullInventorySwapsToolsWithoutAnEmptyBufferSlot() {
        var items=inventory(); for(int i=0;i<36;i++) items.set(i,new ItemStack(Items.IRON_SWORD)); items.set(35,new ItemStack(Items.IRON_PICKAXE));
        var rules=rules(); rules.set(0,only("@pickaxes",0)); var plan=InventorySort.plan(items,rules,Rule.OPEN);
        assertTrue(plan.possible(),plan.error()); assertTrue(plan.result().getFirst().is(Items.IRON_PICKAXE)); conserved(items,plan);
    }
    @Test void impossibleFullInventoryDoesNotPlanPartialChangesOrDiscardItems() {
        var items=inventory(); for(int i=0;i<36;i++) items.set(i,new ItemStack(Items.IRON_SWORD));
        var rules=rules(); rules.set(0,only("@pickaxes",0)); var plan=InventorySort.plan(items,rules,Rule.OPEN);
        assertFalse(plan.possible()); assertTrue(plan.clicks().isEmpty()); assertTrue(plan.result().isEmpty());
    }
    @Test void cappedSlotsSplitStacksAndEmptyBlockedSlots() {
        var items=inventory(); items.set(0,new ItemStack(Items.STONE,64)); items.set(1,new ItemStack(Items.DIRT,16));
        var rules=rules(); rules.set(0,only("minecraft:stone",8)); rules.set(1,new Rule(Rule.Mode.BLOCK,List.of(),0));
        var plan=InventorySort.plan(items,rules,Rule.OPEN); assertTrue(plan.possible(),plan.error()); assertEquals(8,plan.result().getFirst().getCount()); assertTrue(plan.result().get(1).isEmpty()); conserved(items,plan);
    }
    @Test void specificSlotsAreFilledBeforeOverlappingCategories() {
        var items=inventory(); items.set(20,new ItemStack(Items.DIAMOND_PICKAXE)); items.set(21,new ItemStack(Items.IRON_PICKAXE));
        var rules=rules(); rules.set(0,only("@pickaxes",0)); rules.set(1,only("minecraft:diamond_pickaxe",0));
        var plan=InventorySort.plan(items,rules,Rule.OPEN); assertTrue(plan.possible(),plan.error()); assertTrue(plan.result().get(0).is(Items.IRON_PICKAXE)); assertTrue(plan.result().get(1).is(Items.DIAMOND_PICKAXE));
    }
    @Test void differentComponentsNeverMerge() {
        var items=inventory(); var named=new ItemStack(Items.STONE,24); named.set(DataComponents.CUSTOM_NAME,Component.literal("Keep this name"));
        items.set(0,named); items.set(1,new ItemStack(Items.STONE,40)); var rules=rules(); rules.set(0,only("minecraft:stone",8));
        var plan=InventorySort.plan(items,rules,Rule.OPEN); assertTrue(plan.possible(),plan.error()); conserved(items,plan);
        assertEquals(24,plan.result().stream().filter(s -> !s.isEmpty() && s.has(DataComponents.CUSTOM_NAME)).mapToInt(ItemStack::getCount).sum());
    }
    @Test void wholeInventoryMovementFilterAndCapsApply() {
        var items=inventory(); items.set(0,new ItemStack(Items.STONE,32));
        var plan=InventorySort.plan(items,rules(),only("minecraft:stone",8)); assertTrue(plan.possible(),plan.error()); conserved(items,plan);
        assertTrue(plan.result().stream().allMatch(s -> s.isEmpty() || s.getCount()<=8));
        assertFalse(InventorySort.plan(items,rules(),only("minecraft:dirt",0)).possible());
        assertTrue(InventorySort.plan(items,rules(),new Rule(Rule.Mode.BLOCK,List.of(),0,true,false)).possible());
    }
    @Test void randomStackLayoutsConserveItemsAndProduceValidDestinations() {
        var random=new Random(901); Item[] types={Items.STONE,Items.DIRT,Items.OAK_LOG,Items.IRON_PICKAXE,Items.IRON_SWORD};
        for(int trial=0;trial<400;trial++) {
            var items=inventory(); for(int i=0;i<36;i++) if(random.nextInt(3)!=0) { var stack=new ItemStack(types[random.nextInt(types.length)]); stack.setCount(1+random.nextInt(stack.getMaxStackSize())); items.set(i,stack); }
            var rules=rules(); rules.set(0,only("@pickaxes",0)); rules.set(1,only("@swords",0)); rules.set(2,only("minecraft:stone",8));
            var plan=InventorySort.plan(items,rules,Rule.OPEN); assertTrue(plan.possible(),"Trial "+trial+": "+plan.error()); conserved(items,plan);
            for(int i=0;i<36;i++) { assertTrue(rules.get(i).accepts(plan.result().get(i))); if(!plan.result().get(i).isEmpty()) assertTrue(plan.result().get(i).getCount()<=rules.get(i).cap(plan.result().get(i))); }
        }
    }
    private void conserved(List<ItemStack> original,InventorySort.Plan plan) {
        for(var stack:original) if(!stack.isEmpty()) assertEquals(count(original,stack),count(plan.result(),stack));
        assertEquals(original.stream().mapToInt(ItemStack::getCount).sum(),plan.result().stream().mapToInt(ItemStack::getCount).sum());
    }
    private int count(List<ItemStack> items,ItemStack sample) { return items.stream().filter(s -> !s.isEmpty() && ItemStack.isSameItemSameComponents(s,sample)).mapToInt(ItemStack::getCount).sum(); }
}
