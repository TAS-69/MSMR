package dev.msmr;

import dev.msmr.compat.ProtectionAdapters;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;

/** Optional protection providers run on the server thread. A denial always wins. */
public final class ServerProtection {
    public enum Operation { ACCESS, INSERT, EXTRACT, EDIT_FILTERS }
    public enum Decision { PASS, ALLOW, DENY }
    @FunctionalInterface public interface Provider {
        Decision check(ServerPlayer player, BaseContainerBlockEntity chest, Operation operation) throws Exception;
    }
    public record Access(boolean allowed, boolean managed, String message) {}
    private record Entry(String name, Provider provider) {}
    private static final List<Entry> providers = new CopyOnWriteArrayList<>();
    private static final Map<String, Long> warnings = new HashMap<>();
    static { ProtectionAdapters.register(); }
    private ServerProtection() {}

    /** Register during server/mod initialization. Close the handle when unloading a dynamic integration. */
    public static AutoCloseable register(String name, Provider provider) {
        Entry entry = new Entry(Objects.requireNonNull(name), Objects.requireNonNull(provider));
        providers.add(entry);
        return () -> providers.remove(entry);
    }

    public static Access check(ServerPlayer player, BaseContainerBlockEntity chest, Operation operation) {
        List<Decision> decisions = new ArrayList<>();
        for (Entry entry : providers) {
            try {
                Decision decision = Objects.requireNonNull(entry.provider().check(player, chest, operation));
                decisions.add(decision);
                if (decision == Decision.DENY) return new Access(false, true, entry.name() + " does not permit this chest action.");
            } catch (Exception | LinkageError ex) {
                long now = System.nanoTime();
                synchronized (warnings) {
                    if (now - warnings.getOrDefault(entry.name(), 0L) > 60_000_000_000L) {
                        warnings.put(entry.name(), now);
                        org.slf4j.LoggerFactory.getLogger("MSMR").warn("Protection check failed for {}; chest action denied", entry.name(), ex);
                    }
                }
                return new Access(false, true, entry.name() + " protection is unavailable. Nothing changed.");
            }
        }
        return combine(decisions, "");
    }

    static Access combine(Collection<Decision> decisions, String message) {
        return new Access(!decisions.contains(Decision.DENY), decisions.stream().anyMatch(d -> d != Decision.PASS), message);
    }

    public static boolean mayEdit(Access access, UUID owner, UUID player, boolean operator) {
        return access.allowed() && (access.managed() || owner == null || owner.equals(player) || operator);
    }
}
