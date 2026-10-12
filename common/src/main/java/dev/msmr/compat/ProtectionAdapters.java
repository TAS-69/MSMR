package dev.msmr.compat;

import dev.msmr.ServerProtection;
import dev.msmr.ServerProtection.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** API queries do not replay clicks, break blocks, or emit artificial interaction events. */
public final class ProtectionAdapters {
    private ProtectionAdapters() {}
    public static void register() {
        Class<?> ftb = Reflect.optional("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
        if (ftb != null) ServerProtection.register("FTB Chunks", (player, chest, action) -> {
            Object api = Reflect.call(ftb, "api"), manager = Reflect.call(api, "getManager");
            Class<?> positionType = Class.forName("dev.ftb.mods.ftblibrary.math.ChunkDimPos");
            Object position = positionType.getConstructor(Level.class, BlockPos.class).newInstance(player.level(), chest.getBlockPos());
            Object claimed = Reflect.call(manager, "getChunk", position);
            Class<?> expected = Class.forName("dev.ftb.mods.ftbchunks.FTBChunksExpected");
            Object protection = Reflect.call(expected, action == Operation.EDIT_FILTERS ? "getBlockBreakProtection" : "getBlockInteractProtection");
            boolean denied = Reflect.bool(manager, "shouldPreventInteraction", player, InteractionHand.MAIN_HAND, chest.getBlockPos(), protection, null);
            return denied ? Decision.DENY : claimed == null ? Decision.PASS : Decision.ALLOW;
        });
        Class<?> opac = Reflect.optional("xaero.pac.common.server.api.OpenPACServerAPI");
        if (opac != null) ServerProtection.register("Open Parties and Claims", (player, chest, action) -> {
            Object api = Reflect.call(opac, "get", player.getServer());
            Object manager = Reflect.call(api, "getServerClaimsManager");
            Object claim = Reflect.call(manager, "get", player.level().dimension().location(), chest.getBlockPos());
            Object protection = Reflect.call(api, "getChunkProtection");
            boolean denied = Reflect.bool(protection, "onBlockInteraction", player, InteractionHand.MAIN_HAND, ItemStack.EMPTY,
                player.serverLevel(), chest.getBlockPos(), Direction.UP, action == Operation.EDIT_FILTERS, false, false);
            return denied ? Decision.DENY : claim == null ? Decision.PASS : Decision.ALLOW;
        });
        Class<?> bukkit = Reflect.optional("org.bukkit.Bukkit");
        if (bukkit != null) BukkitProtection.register(bukkit);
    }
}
