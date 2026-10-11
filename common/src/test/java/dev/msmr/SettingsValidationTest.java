package dev.msmr;

import com.google.gson.Gson;
import dev.msmr.client.ClientRules;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SettingsValidationTest {
    private static final Gson JSON=new Gson();
    @Test void acceptsEmptyAndRoundTrippedSettings() {
        assertDoesNotThrow(() -> ClientRules.validate(new ClientRules.Settings()));
        var data=new ClientRules.Settings();
        var book=new ProfileBook(); book.save("Mining",new ProfileBook.Profile(Rule.OPEN,java.util.Map.of(0,Rule.OPEN)));
        data.books.put("world/test",book);
        assertDoesNotThrow(() -> ClientRules.validate(JSON.fromJson(JSON.toJson(data),ClientRules.Settings.class)));
    }
    @Test void rejectsNullNestedRulesAndInventories() {
        for(String text:new String[]{"{inventories:{test:null}}","{inventories:{test:{'0':null}}}","{books:{test:null}}","{books:{test:{profiles:null}}}","{inventoryFilters:{test:null}}"})
            assertThrows(RuntimeException.class,() -> ClientRules.validate(JSON.fromJson(text,ClientRules.Settings.class)),text);
    }
    @Test void rejectsOutOfRangeSlotsAndInvalidProfileNames() {
        for(String text:new String[]{"{inventories:{test:{'36':{mode:'ALL',entries:[],limit:0}}}}","{activeProfiles:{test:'../bad'}}"})
            assertThrows(RuntimeException.class,() -> ClientRules.validate(JSON.fromJson(text,ClientRules.Settings.class)),text);
    }
}
