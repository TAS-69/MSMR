package dev.msmr.compat;

import dev.msmr.ServerProtection;
import dev.msmr.ServerProtection.*;
import java.lang.reflect.Array;
import java.util.*;

/** Bukkit APIs are available only on compatible hybrid servers; plain Paper cannot load this mod. */
final class BukkitProtection {
    private record Context(Object plugin, Object player, Object block) {}
    @FunctionalInterface private interface Query { Decision check(Context context, Operation operation) throws Exception; }
    private BukkitProtection() {}
    static void register(Class<?> bukkit) {
        provider(bukkit, "WorldGuard", BukkitProtection::worldGuard);
        provider(bukkit, "GriefPrevention", BukkitProtection::griefPrevention);
        provider(bukkit, "LWC", BukkitProtection::lwc);
        provider(bukkit, "Bolt", BukkitProtection::bolt);
        provider(bukkit, "LockettePro", BukkitProtection::lockette);
    }
    private static void provider(Class<?> bukkit, String name, Query query) {
        ServerProtection.register(name, (player, chest, operation) -> {
            if (Reflect.call(bukkit, "getServer") == null) return Decision.PASS;
            Object manager = Reflect.call(bukkit, "getPluginManager"), plugin = Reflect.call(manager, "getPlugin", name);
            if (plugin == null) return Decision.PASS;
            if (!Reflect.bool(plugin, "isEnabled")) return Decision.DENY;
            Object bukkitPlayer = Reflect.call(bukkit, "getPlayer", player.getUUID());
            if (bukkitPlayer == null) return Decision.DENY;
            Object world = Reflect.call(bukkitPlayer, "getWorld");
            var pos = chest.getBlockPos();
            Object block = Reflect.call(world, "getBlockAt", pos.getX(), pos.getY(), pos.getZ());
            return query.check(new Context(plugin, bukkitPlayer, block), operation);
        });
    }
    private static Decision worldGuard(Context c, Operation operation) throws Exception {
        Class<?> adapter = Reflect.type(c.plugin(), "com.sk89q.worldedit.bukkit.BukkitAdapter");
        Object location = Reflect.call(adapter, "adapt", Reflect.call(c.block(), "getLocation"));
        Object world = Reflect.call(adapter, "adapt", Reflect.call(c.block(), "getWorld"));
        Object localPlayer = Reflect.call(c.plugin(), "wrapPlayer", c.player());
        Object wg = Reflect.call(Reflect.type(c.plugin(), "com.sk89q.worldguard.WorldGuard"), "getInstance");
        Object platform = Reflect.call(wg, "getPlatform"), container = Reflect.call(platform, "getRegionContainer");
        Object query = Reflect.call(container, "createQuery"), regions = Reflect.call(query, "getApplicableRegions", location);
        Object manager = Reflect.call(container, "get", world);
        boolean managed = (Integer) Reflect.call(regions, "size") > 0 || manager != null && Reflect.call(manager, "getRegion", "__global__") != null;
        if (Reflect.bool(Reflect.call(platform, "getSessionManager"), "hasBypass", localPlayer, world)) return managed ? Decision.ALLOW : Decision.PASS;
        Class<?> flags = Reflect.type(c.plugin(), "com.sk89q.worldguard.protection.flags.Flags");
        Object flag = flags.getField(operation == Operation.EDIT_FILTERS ? "BUILD" : "CHEST_ACCESS").get(null);
        Object array = Array.newInstance(Reflect.type(c.plugin(), "com.sk89q.worldguard.protection.flags.StateFlag"), 1);
        Array.set(array, 0, flag);
        boolean allowed = Reflect.bool(query, "testBuild", location, localPlayer, array);
        return !allowed ? Decision.DENY : managed ? Decision.ALLOW : Decision.PASS;
    }
    private static Decision griefPrevention(Context c, Operation operation) throws Exception {
        Object store = c.plugin().getClass().getField("dataStore").get(c.plugin());
        Object claim = Reflect.call(store, "getClaimAt", Reflect.call(c.block(), "getLocation"), false, null);
        if (claim == null) return Decision.PASS;
        Object denial = operation == Operation.EDIT_FILTERS ? Reflect.call(claim, "allowBuild", c.player(), Reflect.call(c.block(), "getType")) : Reflect.call(claim, "allowContainers", c.player());
        return denial == null ? Decision.ALLOW : Decision.DENY;
    }
    private static Decision lwc(Context c, Operation operation) throws Exception {
        Object api = Reflect.call(Reflect.type(c.plugin(), "com.griefcraft.lwc.LWC"), "getInstance");
        if (Reflect.call(api, "findProtection", c.block()) == null) return Decision.PASS;
        return Reflect.bool(api, operation == Operation.EDIT_FILTERS ? "canAdminProtection" : "canAccessProtection", c.player(), c.block()) ? Decision.ALLOW : Decision.DENY;
    }
    private static Decision bolt(Context c, Operation operation) throws Exception {
        if (!Reflect.bool(c.plugin(), "isProtected", c.block())) return Decision.PASS;
        String permission = switch (operation) { case EDIT_FILTERS -> "edit"; case INSERT -> "deposit"; case EXTRACT -> "withdraw"; default -> "open"; };
        return Reflect.bool(c.plugin(), "canAccess", c.block(), c.player(), (Object) new String[]{permission}) ? Decision.ALLOW : Decision.DENY;
    }
    private static Decision lockette(Context c, Operation operation) throws Exception {
        Class<?> api = Reflect.type(c.plugin(), "me.crafter.mc.lockettepro.LocketteProAPI");
        if (!Reflect.bool(api, "isProtected", c.block())) return Decision.PASS;
        boolean owner = Reflect.bool(api, "isOwner", c.block(), c.player());
        boolean allowed = owner || operation != Operation.EDIT_FILTERS && Reflect.bool(api, "isUser", c.block(), c.player());
        return allowed ? Decision.ALLOW : Decision.DENY;
    }
}
