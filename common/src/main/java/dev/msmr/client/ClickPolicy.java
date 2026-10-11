package dev.msmr.client;

import dev.msmr.Rule;
import net.minecraft.world.item.ItemStack;

/** Conservative click checks: reject a placement that would exceed a local rule. */
public final class ClickPolicy {
    private ClickPolicy() {}
    public static boolean permits(Rule rule,ItemStack existing,ItemStack incoming,int amount) {
        if (incoming.isEmpty() || amount<=0) return true;
        if (!rule.accepts(incoming)) return false;
        int retained=!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing,incoming) ? existing.getCount() : 0;
        return retained+amount<=rule.cap(incoming);
    }
}
