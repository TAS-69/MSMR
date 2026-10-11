package dev.msmr;

import java.util.*;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuickProfilesTest {
    @Test void automaticallyAddsFiveAndPromotesAfterDeletion() {
        var quick=new QuickProfiles(); quick.sync(List.of("Mining","Combat","Farming","Building","Travel","Spare"));
        assertEquals(List.of("Mining","Combat","Farming","Building","Travel"),quick.names()); assertEquals("M",quick.badge("Mining").letter());
        assertThrows(IllegalArgumentException.class,() -> quick.pin("Spare",true));
        quick.sync(List.of("Mining","Combat","Building","Travel","Spare")); assertTrue(quick.names().contains("Spare")); assertNull(quick.badge("Farming"));
    }
    @Test void appearanceAndHiddenSelectionSurviveSettingsReload() {
        var quick=new QuickProfiles(); quick.sync(List.of("Mining","Combat")); var badge=new QuickProfiles.Badge("Z","minecraft:diamond_pickaxe",0x123456); quick.appearance("Mining",badge); quick.pin("Combat",false);
        var gson=new Gson(); var restored=gson.fromJson(gson.toJson(quick),QuickProfiles.class); restored.validate(); restored.sync(List.of("Mining","Combat","Farming"));
        assertEquals(badge,restored.badge("Mining")); assertEquals(List.of("Mining","Farming"),restored.names());
    }
    @Test void namesAndInvalidAppearanceAreHandledSafely() {
        assertEquals("P",QuickProfiles.Badge.create("___").letter()); assertEquals("M",QuickProfiles.Badge.create(" Mining").letter());
        assertThrows(IllegalArgumentException.class,() -> new QuickProfiles.Badge("AB","",0)); assertThrows(IllegalArgumentException.class,() -> new QuickProfiles.Badge("A","bad id",0)); assertThrows(IllegalArgumentException.class,() -> new QuickProfiles.Badge("A","",-1));
        var quick=new QuickProfiles(); quick.sync(List.of("Mining")); var before=quick.badge("Mining"); assertFalse(quick.sync(List.of("Mining"))); assertEquals(before,quick.badge("Mining"));
    }
}
