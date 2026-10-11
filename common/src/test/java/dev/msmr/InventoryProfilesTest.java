package dev.msmr;

import com.google.gson.Gson;
import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class InventoryProfilesTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    @Test void allSwordMaterialsMatchCategory() {
        Rule swords=new Rule(Rule.Mode.WHITELIST,List.of("@swords"),0);
        for(Item item:List.of(Items.WOODEN_SWORD,Items.STONE_SWORD,Items.IRON_SWORD,Items.GOLDEN_SWORD,Items.DIAMOND_SWORD,Items.NETHERITE_SWORD)) assertTrue(swords.accepts(new ItemStack(item)));
        assertFalse(swords.accepts(new ItemStack(Items.DIAMOND_AXE)));
    }
    @Test void seedsIncludePodsButExcludePlantableFood() {
        Rule seeds=new Rule(Rule.Mode.WHITELIST,List.of("@seeds"),0);
        for(Item item:List.of(Items.WHEAT_SEEDS,Items.BEETROOT_SEEDS,Items.PUMPKIN_SEEDS,Items.MELON_SEEDS,Items.TORCHFLOWER_SEEDS,Items.PITCHER_POD)) assertTrue(seeds.accepts(new ItemStack(item)),item.toString());
        assertFalse(seeds.accepts(new ItemStack(Items.CARROT))); assertFalse(seeds.accepts(new ItemStack(Items.POTATO)));
    }
    @Test void mixedItemsAndCategoriesAndBlacklist() {
        Rule mixed=new Rule(Rule.Mode.WHITELIST,List.of("@swords","minecraft:torch"),0);
        assertTrue(mixed.accepts(new ItemStack(Items.IRON_SWORD))); assertTrue(mixed.accepts(new ItemStack(Items.TORCH))); assertFalse(mixed.accepts(new ItemStack(Items.DIRT)));
        Rule blacklist=new Rule(Rule.Mode.BLACKLIST,List.of("@swords"),0);
        assertFalse(blacklist.accepts(new ItemStack(Items.IRON_SWORD))); assertTrue(blacklist.accepts(new ItemStack(Items.DIRT)));
    }
    @Test void otherCategoriesAndValidation() {
        assertTrue(new Rule(Rule.Mode.ALL,List.of("@swords"),0).unrestricted());
        assertTrue(new Rule(Rule.Mode.WHITELIST,List.of("@food"),0).accepts(new ItemStack(Items.APPLE)));
        assertTrue(new Rule(Rule.Mode.WHITELIST,List.of("@armor"),0).accepts(new ItemStack(Items.DIAMOND_CHESTPLATE)));
        assertTrue(new Rule(Rule.Mode.WHITELIST,List.of("@blocks"),0).accepts(new ItemStack(Items.STONE)));
        assertThrows(IllegalArgumentException.class,() -> new Rule(Rule.Mode.WHITELIST,List.of("@madeup"),0));
    }
    private ProfileBook.Profile sample() { return new ProfileBook.Profile(new Rule(Rule.Mode.BLACKLIST,List.of("@swords"),0,true,false),Map.of(0,new Rule(Rule.Mode.WHITELIST,List.of("minecraft:torch"),8,false,true))); }
    @Test void saveCopyDeleteAndSwitchSnapshot() {
        ProfileBook book=new ProfileBook(); book.save("Mining",sample()); book.copy("Mining","Mining spare");
        assertEquals(sample(),book.get("Mining spare")); assertTrue(book.delete("Mining")); assertNull(book.get("Mining")); assertEquals(sample(),book.get("Mining spare"));
        assertFalse(book.delete("missing"));
    }
    @Test void copyingCannotOverwriteAnotherProfile() {
        ProfileBook book=new ProfileBook(); book.save("Mining",sample()); book.save("Combat",new ProfileBook.Profile(Rule.OPEN,Map.of()));
        assertThrows(IllegalArgumentException.class,() -> book.copy("Mining","Combat")); assertEquals(Rule.OPEN,book.get("Combat").inventory());
        assertThrows(IllegalArgumentException.class,() -> book.copy("missing","Copy"));
        assertThrows(IllegalArgumentException.class,() -> book.save("../bad",sample()));
    }
    @Test void snapshotsCannotBeMutatedBySourceOrCaller() {
        Map<Integer,Rule> slots=new HashMap<>(); slots.put(1,Rule.OPEN);
        ProfileBook.Profile profile=new ProfileBook.Profile(Rule.OPEN,slots); slots.clear();
        assertEquals(1,profile.slots().size()); assertThrows(UnsupportedOperationException.class,() -> profile.slots().clear());
    }
    @Test void jsonRoundTripPreservesCategoriesAndProfileOperations() {
        Gson gson=new Gson(); ProfileBook book=new ProfileBook(); book.save("Mining",sample());
        ProfileBook restored=gson.fromJson(gson.toJson(book),ProfileBook.class);
        assertEquals(sample(),restored.get("Mining")); restored.copy("Mining","Copy"); restored.delete("Mining"); assertEquals(sample(),restored.get("Copy"));
    }
}
