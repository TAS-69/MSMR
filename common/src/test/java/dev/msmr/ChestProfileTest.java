package dev.msmr;

import com.google.gson.Gson;
import dev.msmr.client.ClientRules;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChestProfileTest {
    private static final Gson JSON=new Gson();
    private static final Rule STONE=new Rule(Rule.Mode.WHITELIST,List.of("minecraft:stone"),8,false,true);
    @Test void addressesDistinguishDimensionsPositionsAndBlockTypes() {
        var address=new ChestAddress("minecraft:overworld","minecraft:chest",-12,65,90);
        assertEquals(address,ChestAddress.parse(address.key()));
        assertNotEquals(address.key(),new ChestAddress("minecraft:the_nether","minecraft:chest",-12,65,90).key());
        assertNotEquals(address.key(),new ChestAddress("minecraft:overworld","minecraft:chest",-11,65,90).key());
        assertNotEquals(address.key(),new ChestAddress("minecraft:overworld","minecraft:trapped_chest",-12,65,90).key());
    }
    @Test void slotProfilesAreImmutableAndBoundToOnePhysicalHalf() {
        var input=new HashMap<Integer,Rule>(); input.put(26,STONE);
        var profile=new ChestProfile(STONE,input); input.clear();
        assertEquals(STONE,profile.slot(26));
        assertEquals(Rule.OPEN,profile.slot(0));
        assertTrue(profile.withSlot(26,Rule.OPEN).slots().isEmpty());
        assertEquals(STONE,profile.slot(26));
        assertThrows(IllegalArgumentException.class,() -> profile.withSlot(27,STONE));
        assertThrows(IllegalArgumentException.class,() -> profile.withSlot(-1,Rule.OPEN));
    }
    @Test void settingsRoundTripKeepsChestsAndPlayerProfilesIndependent() {
        var data=new ClientRules.Settings();
        String first=new ChestAddress("minecraft:overworld","minecraft:chest",0,64,0).key();
        String second=new ChestAddress("minecraft:overworld","minecraft:chest",2,64,0).key();
        data.chests.put("world/one/player",new HashMap<>(Map.of(first,new ChestProfile(STONE,Map.of(0,STONE)),second,ChestProfile.EMPTY)));
        data.chests.put("server/two/player",Map.of(first,ChestProfile.EMPTY));
        var book=new ProfileBook(); book.save("Mining",new ProfileBook.Profile(Rule.OPEN,Map.of())); data.books.put("world/one/player",book);
        var restored=JSON.fromJson(JSON.toJson(data),ClientRules.Settings.class);
        assertDoesNotThrow(() -> ClientRules.validate(restored));
        assertEquals(STONE,restored.chests.get("world/one/player").get(first).slot(0));
        assertEquals(Rule.OPEN,restored.chests.get("world/one/player").get(second).filter());
        assertEquals(Rule.OPEN,restored.chests.get("server/two/player").get(first).filter());
        assertEquals(Rule.OPEN,restored.books.get("world/one/player").get("Mining").inventory());
    }
    @Test void damagedChestSettingsAreRejected() {
        for(String text:List.of("{chests:{world:null}}","{chests:{world:{bad:{filter:{mode:'ALL',entries:[],limit:0},slots:{}}}}}",
            "{chests:{world:{'minecraft:overworld|0,64,0|minecraft:chest':null}}}",
            "{chests:{world:{'minecraft:overworld|0,64,0|minecraft:chest':{filter:{mode:'ALL',entries:[],limit:0},slots:{'27':{mode:'ALL',entries:[],limit:0}}}}}}"))
            assertThrows(RuntimeException.class,() -> ClientRules.validate(JSON.fromJson(text,ClientRules.Settings.class)),text);
    }
}
