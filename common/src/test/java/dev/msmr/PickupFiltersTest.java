package dev.msmr;

import java.util.*;
import com.google.gson.Gson;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class PickupFiltersTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    private Map<Integer,Rule> blocked() {
        Map<Integer,Rule> rules=new HashMap<>();
        for(int i=0;i<36;i++) rules.put(i,new Rule(Rule.Mode.BLOCK,List.of(),0));
        return rules;
    }
    @Test void blacklistLeavesGroundStackUnchanged() {
        var inv=new Inventory(null); var drop=new ItemStack(Items.STICK,8);
        assertFalse(PickupFilters.insert(inv,drop,new Rule(Rule.Mode.BLACKLIST,List.of("minecraft:stick"),0),Map.of()));
        assertEquals(8,drop.getCount()); assertTrue(inv.isEmpty());
    }
    @Test void capLeavesExactRemainderAndPrefersMatchingSlot() {
        var inv=new Inventory(null); var drop=new ItemStack(Items.STICK,8); var rules=blocked();
        rules.put(12,new Rule(Rule.Mode.WHITELIST,List.of("minecraft:stick"),3));
        assertTrue(PickupFilters.insert(inv,drop,Rule.OPEN,rules));
        assertEquals(3,inv.getItem(12).getCount()); assertEquals(5,drop.getCount());
        assertFalse(PickupFilters.insert(inv,drop,Rule.OPEN,rules)); assertEquals(5,drop.getCount());
    }
    @Test void componentsAndIncompatibleStacksArePreserved() {
        var inv=new Inventory(null); inv.setItem(12,new ItemStack(Items.STICK,2));
        var drop=new ItemStack(Items.STICK,8); drop.set(DataComponents.CUSTOM_NAME,Component.literal("Keep me"));
        var rules=blocked(); rules.put(12,new Rule(Rule.Mode.WHITELIST,List.of("minecraft:stick"),3));
        assertFalse(PickupFilters.insert(inv,drop,Rule.OPEN,rules));
        rules.put(13,new Rule(Rule.Mode.WHITELIST,List.of("minecraft:stick"),3));
        assertTrue(PickupFilters.insert(inv,drop,Rule.OPEN,rules));
        assertEquals(5,drop.getCount()); assertEquals(Component.literal("Keep me"),inv.getItem(13).get(DataComponents.CUSTOM_NAME));
        assertEquals(2,inv.getItem(12).getCount());
    }
    @Test void actionFlagsAreIndependentAndOldSettingsDefaultToEnabled() {
        Rule pickupOnly=new Rule(Rule.Mode.BLOCK,List.of(),0,true,false);
        assertTrue(pickupOnly.forMoving().accepts(new ItemStack(Items.STICK)));
        assertFalse(pickupOnly.forPickups().accepts(new ItemStack(Items.STICK)));
        Rule old=new Gson().fromJson("{mode:'BLACKLIST',entries:['minecraft:stick'],limit:0}",Rule.class);
        assertTrue(old.pickups()); assertTrue(old.moving());
        var inv=new Inventory(null); var rules=blocked(); rules.put(5,new Rule(Rule.Mode.BLOCK,List.of(),0,false,true));
        var drop=new ItemStack(Items.STICK,4);
        assertTrue(PickupFilters.insert(inv,drop,Rule.OPEN,rules)); assertEquals(4,inv.getItem(5).getCount());
    }
    @Test void compressedProtocolRoundTripsFlagsAndRejectsMalformedRules() {
        var snapshot=new FilterSnapshot(new Rule(Rule.Mode.BLACKLIST,List.of("minecraft:stick"),0,true,false),Map.of(3,Rule.OPEN));
        assertEquals(snapshot,FilterPayload.rules(1,snapshot).snapshot());
        assertThrows(RuntimeException.class,() -> new FilterPayload(FilterPayload.RULES,2,new byte[]{1,2,3}).snapshot());
        assertThrows(RuntimeException.class,() -> new FilterSnapshot(Rule.OPEN,Map.of(36,Rule.OPEN)));
    }
}
