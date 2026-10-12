package dev.msmr;

import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class StorageProtocolTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    @Test void sharedChestStateRoundTripsRulesAndOwnershipPermission() {
        var profile=new ChestProfile(new Rule(Rule.Mode.WHITELIST,List.of("@seeds"),8,false,true),Map.of(0,new Rule(Rule.Mode.BLOCK,List.of(),0)));
        var state=new StorageProtocol.State(3,555,List.of(8L),List.of(profile),false,"Owner edits only.");
        assertEquals(state,FilterPayload.encode(FilterPayload.CHEST_STATE,7,state).read(StorageProtocol.State.class));
        var edit=new StorageProtocol.Edit(3,555,List.of(8L),List.of(profile));
        assertEquals(edit,FilterPayload.encode(FilterPayload.CHEST_SET,8,edit).read(StorageProtocol.Edit.class));
    }
    @Test void actionsCarryNoClientSuppliedItemsOrWorldCoordinates() {
        var action=new StorageProtocol.Action(4,12,8,33,"sort",-1);
        assertEquals(action,FilterPayload.encode(FilterPayload.ACTION,99,action).read(StorageProtocol.Action.class));
        assertThrows(IllegalArgumentException.class,() -> new StorageProtocol.Action(0,0,0,0,"delete",0));
    }
    @Test void chestProtocolRejectsMismatchedOrOversizedHalves() {
        assertThrows(IllegalArgumentException.class,() -> new StorageProtocol.Edit(1,2,List.of(),List.of(ChestProfile.EMPTY)));
        assertThrows(IllegalArgumentException.class,() -> new StorageProtocol.State(1,2,List.of(0L,0L,0L),List.of(ChestProfile.EMPTY,ChestProfile.EMPTY,ChestProfile.EMPTY),true,""));
        assertThrows(IllegalArgumentException.class,() -> new StorageProtocol.Edit(1,2,List.of(),List.of()));
    }
    @Test void insertionSimulationNeverMutatesEitherStack() {
        var container=new SimpleContainer(1); container.setItem(0,new ItemStack(Items.STONE,60)); var offered=new ItemStack(Items.STONE,10);
        assertEquals(6,ServerStorage.insert(container,0,offered,true).getCount()); assertEquals(60,container.getItem(0).getCount()); assertEquals(10,offered.getCount());
        assertEquals(6,ServerStorage.insert(container,0,offered,false).getCount()); assertEquals(64,container.getItem(0).getCount()); assertEquals(10,offered.getCount());
    }
    @Test void insertionPreservesComponentsAndRejectsIncompatibleStacks() {
        var container=new SimpleContainer(1); var named=new ItemStack(Items.STONE,12); named.set(DataComponents.CUSTOM_NAME,Component.literal("Named"));
        assertTrue(ServerStorage.insert(container,0,named,false).isEmpty()); assertEquals("Named",container.getItem(0).getHoverName().getString());
        assertEquals(8,ServerStorage.insert(container,0,new ItemStack(Items.STONE,8),false).getCount()); assertEquals(12,container.getItem(0).getCount());
    }
    @Test void invalidCompressedStorageMessagesFailClosed() {
        assertThrows(RuntimeException.class,() -> new FilterPayload(FilterPayload.CHEST_SET,1,new byte[]{1,2,3}).read(StorageProtocol.Edit.class));
        assertThrows(RuntimeException.class,() -> new FilterPayload(FilterPayload.ACTION,1,new byte[30001]).read(StorageProtocol.Action.class));
    }
    @Test void containerInsertionRestrictionsArePreserved() {
        var container=new SimpleContainer(1) { @Override public boolean canPlaceItem(int slot,ItemStack stack) { return false; } };
        var offered=new ItemStack(Items.STONE,10);
        assertEquals(10,ServerStorage.insert(container,0,offered,true).getCount());
        assertEquals(10,ServerStorage.insert(container,0,offered,false).getCount());
        assertTrue(container.isEmpty()); assertEquals(10,offered.getCount());
    }
    @Test void deeplyNestedAndCommentedNetworkJsonIsRejectedBeforeGsonParsing() {
        assertThrows(RuntimeException.class,() -> FilterPayload.encode(FilterPayload.CHEST_SET,1,Map.of("ignored",new Object[0])).read(StorageProtocol.Edit.class));
        assertThrows(IllegalArgumentException.class,() -> FilterPayload.validateDepth("[".repeat(33)+"0"+"]".repeat(33)));
        assertThrows(IllegalArgumentException.class,() -> FilterPayload.validateDepth("/* \" */ [0]"));
        assertDoesNotThrow(() -> FilterPayload.validateDepth("{\"text\":\"[\\\"{\",\"values\":[1,true,null]}"));
    }
}
