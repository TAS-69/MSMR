package dev.msmr;

import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class RuleTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    @Test void whitelistBlacklistAndBlock() {
        ItemStack stone=new ItemStack(Items.STONE);
        Rule white=new Rule(Rule.Mode.WHITELIST,List.of("minecraft:stone"),8);
        assertTrue(white.accepts(stone)); assertFalse(white.accepts(new ItemStack(Items.DIRT)));
        assertFalse(new Rule(Rule.Mode.BLACKLIST,white.entries(),0).accepts(stone));
        assertFalse(new Rule(Rule.Mode.BLOCK,List.of(),0).accepts(stone));
        assertEquals(8,white.cap(stone));
        assertEquals(1,new Rule(Rule.Mode.ALL,List.of(),99).cap(new ItemStack(Items.DIAMOND_PICKAXE)));
    }
    @Test void invalidRulesRejected() {
        assertThrows(IllegalArgumentException.class,() -> new Rule(Rule.Mode.ALL,List.of(),-1));
        assertThrows(IllegalArgumentException.class,() -> new Rule(Rule.Mode.WHITELIST,List.of("BAD ID"),0));
        assertThrows(IllegalArgumentException.class,() -> new Rule(Rule.Mode.ALL,List.of(),100));
    }
    @Test void sortPrefersReservedSlotsAndPreservesCounts() {
        ItemStack original=new ItemStack(Items.STONE,40);
        var plan=InventorySort.plan(List.of(original,ItemStack.EMPTY),List.of(Rule.OPEN,new Rule(Rule.Mode.WHITELIST,List.of("minecraft:stone"),8)));
        assertNotNull(plan); assertEquals(8,plan[1].getCount()); assertEquals(32,plan[0].getCount()); assertEquals(40,original.getCount());
    }
    @Test void impossibleSortDoesNotMutateInput() {
        ItemStack original=new ItemStack(Items.STONE,40);
        assertNull(InventorySort.plan(List.of(original),List.of(new Rule(Rule.Mode.BLOCK,List.of(),0))));
        assertEquals(40,original.getCount());
    }
    @Test void componentsAreNeverMergedOrLost() {
        ItemStack named=new ItemStack(Items.STONE,10); named.set(DataComponents.CUSTOM_NAME,Component.literal("Keep me"));
        ItemStack plain=new ItemStack(Items.STONE,12);
        var plan=InventorySort.plan(List.of(named,plain),List.of(Rule.OPEN,Rule.OPEN));
        assertNotNull(plan); assertEquals(22,Arrays.stream(plan).mapToInt(ItemStack::getCount).sum());
        assertTrue(Arrays.stream(plan).anyMatch(s -> Component.literal("Keep me").equals(s.get(DataComponents.CUSTOM_NAME)) && s.getCount()==10));
        assertEquals(10,named.getCount());
    }
    @Test void randomizedSortConservesEveryItem() {
        Random random=new Random(42);
        for (int trial=0;trial<200;trial++) {
            List<ItemStack> input=new ArrayList<>(); List<Rule> rules=new ArrayList<>();
            int stone=0,dirt=0;
            for(int i=0;i<36;i++) {
                boolean isStone=random.nextBoolean(); int count=random.nextInt(65);
                input.add(new ItemStack(isStone?Items.STONE:Items.DIRT,count)); if(isStone) stone+=count; else dirt+=count;
                rules.add(i<6?new Rule(Rule.Mode.WHITELIST,List.of("minecraft:stone"),16):Rule.OPEN);
            }
            var plan=InventorySort.plan(input,rules);
            if (plan==null) continue;
            int actualStone=0,actualDirt=0;
            for(int i=0;i<36;i++) { assertTrue(rules.get(i).accepts(plan[i])); assertTrue(plan[i].getCount()<=rules.get(i).cap(plan[i])); if(plan[i].is(Items.STONE)) actualStone+=plan[i].getCount(); else if(plan[i].is(Items.DIRT)) actualDirt+=plan[i].getCount(); }
            assertEquals(stone,actualStone); assertEquals(dirt,actualDirt);
        }
    }
}
