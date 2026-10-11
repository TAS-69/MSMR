package dev.msmr;

import dev.msmr.client.LookupCatalog;
import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class LookupTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    @Test void readableNamesNamespacesAndMultipleWords() {
        assertTrue(LookupSearch.score("oak log","Oak Log","minecraft:oak_log","")>=0);
        assertTrue(LookupSearch.score("minecraft:oak","Oak Log","minecraft:oak_log","")>=0);
        assertEquals(-1,LookupSearch.score("birch log","Oak Log","minecraft:oak_log",""));
        assertTrue(LookupSearch.score("OAK_LOG","Oak Log","minecraft:oak_log","")>=0);
        assertTrue(LookupSearch.score("#logs","Logs","#minecraft:logs","")>=0);
    }
    @Test void exactMatchesOutrankMembershipAndTypos() {
        assertTrue(LookupSearch.score("oak log","Oak Log","minecraft:oak_log","")<LookupSearch.score("oak log","Logs","#minecraft:logs","Oak Log Birch Log"));
        assertTrue(LookupSearch.score("diamnd","Diamond Sword","minecraft:diamond_sword","")>=0);
        assertEquals(-1,LookupSearch.score("zzzz","Diamond Sword","minecraft:diamond_sword",""));
    }
    @Test void blockTagsAndItemTagsRemainDistinct() {
        var key=TagKey.create(Registries.BLOCK,ResourceLocation.fromNamespaceAndPath("msmr","test_wood"));
        BuiltInRegistries.BLOCK.bindTags(Map.of(key,List.of(Blocks.OAK_LOG.builtInRegistryHolder(),Blocks.BIRCH_LOG.builtInRegistryHolder())));
        Rule rule=new Rule(Rule.Mode.WHITELIST,List.of("#block:msmr:test_wood"),0);
        assertTrue(rule.accepts(new ItemStack(Items.OAK_LOG))); assertFalse(rule.accepts(new ItemStack(Items.STONE))); assertFalse(rule.accepts(new ItemStack(Items.STICK)));
        assertFalse(new Rule(Rule.Mode.WHITELIST,List.of("#msmr:test_wood"),0).accepts(new ItemStack(Items.OAK_LOG)));
        assertThrows(IllegalArgumentException.class,() -> new Rule(Rule.Mode.WHITELIST,List.of("#block:BAD ID"),0));
        var entry=LookupCatalog.build().stream().filter(e -> e.token().equals("#block:msmr:test_wood")).findFirst().orElseThrow();
        assertEquals(2,entry.size()); assertTrue(entry.memberIds().contains("minecraft:oak_log"));
    }
    @Test void blockTabExcludesNonBlocksAndChosenIncludesGroups() {
        var all=LookupCatalog.build();
        var stone=all.stream().filter(e -> e.token().equals("minecraft:stone")).findFirst().orElseThrow();
        var sword=all.stream().filter(e -> e.token().equals("minecraft:iron_sword")).findFirst().orElseThrow();
        var group=all.stream().filter(e -> e.token().equals("@swords")).findFirst().orElseThrow();
        assertTrue(LookupCatalog.inTab(stone,LookupCatalog.Kind.BLOCKS,Set.of())); assertFalse(LookupCatalog.inTab(sword,LookupCatalog.Kind.BLOCKS,Set.of()));
        assertTrue(LookupCatalog.inTab(group,LookupCatalog.Kind.CHOSEN,Set.of("@swords"))); assertTrue(group.size()>=6);
    }
}
