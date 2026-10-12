package dev.msmr;

import dev.msmr.ServerProtection.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerProtectionTest {
    @Test void anyDenialWinsRegardlessOfProviderOrder() {
        assertFalse(ServerProtection.combine(List.of(Decision.ALLOW,Decision.DENY),"").allowed());
        assertFalse(ServerProtection.combine(List.of(Decision.DENY,Decision.ALLOW),"").allowed());
    }
    @Test void unclaimedLandRetainsOwnerFallback() {
        var access=ServerProtection.combine(List.of(Decision.PASS,Decision.PASS),"");
        UUID owner=UUID.randomUUID(),other=UUID.randomUUID();
        assertTrue(ServerProtection.mayEdit(access,null,other,false));
        assertTrue(ServerProtection.mayEdit(access,owner,owner,false));
        assertFalse(ServerProtection.mayEdit(access,owner,other,false));
        assertTrue(ServerProtection.mayEdit(access,owner,other,true));
    }
    @Test void authorizedClaimBuilderCanEditOldOwnersFilters() {
        var access=ServerProtection.combine(List.of(Decision.PASS,Decision.ALLOW),"");
        assertTrue(ServerProtection.mayEdit(access,UUID.randomUUID(),UUID.randomUUID(),false));
    }
    @Test void operatorAndFilterOwnerCannotOverrideClaimDenial() {
        UUID owner=UUID.randomUUID(); var denied=ServerProtection.combine(List.of(Decision.DENY),"");
        assertFalse(ServerProtection.mayEdit(denied,owner,owner,true));
    }
    @Test void providerExceptionsDenyAndUnregisterCleanly() throws Exception {
        try(var registration=ServerProtection.register("Test failure",(p,c,op) -> { throw new IllegalStateException("Unavailable"); })) {
            assertFalse(ServerProtection.check(null,null,Operation.ACCESS).allowed());
        }
        assertTrue(ServerProtection.check(null,null,Operation.ACCESS).allowed());
    }
}
