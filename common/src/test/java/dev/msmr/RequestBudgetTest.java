package dev.msmr;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RequestBudgetTest {
    @Test void normalBurstRefillsAtBoundedRate() {
        var budget=new RequestBudget(0);
        for(int i=0;i<64;i++) assertTrue(budget.allow(0,0));
        assertFalse(budget.allow(0,0));
        assertTrue(budget.allow(0,1_000_000_000L));
        for(int i=0;i<15;i++) assertTrue(budget.allow(0,1_000_000_000L));
        assertFalse(budget.allow(0,1_000_000_000L));
    }
    @Test void largePacketsConsumeMoreBudget() {
        var budget=new RequestBudget(0);
        for(int i=0;i<8;i++) assertTrue(budget.allow(30000,0));
        assertFalse(budget.allow(30000,0));
        assertTrue(budget.allow(30000,500_000_000L));
    }
}
