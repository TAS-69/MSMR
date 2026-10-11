package dev.msmr.client;

import dev.msmr.ChestAddress;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.inventory.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** Binds a confirmed vanilla storage menu to the block interaction that opened it. */
public final class ChestTracker {
    public record Target(ClientLevel level,List<ChestAddress> blocks,int menuId) {
        public Target { blocks=List.copyOf(blocks); }
        public ChestAddress address(int slot) { return blocks.get(slot/27); }
    }
    private static Target pending,active;
    private static long requestedAt;
    private ChestTracker() {}
    public static void clear() { pending=null; active=null; }
    public static void interacted(BlockPos position,boolean consumed) {
        pending=null;
        var level=Minecraft.getInstance().level;
        if(!consumed || level==null) return;
        var state=level.getBlockState(position);
        List<BlockPos> positions;
        if(state.getBlock() instanceof ChestBlock chest) {
            positions=chest.combine(state,level,position,false).apply(new DoubleBlockCombiner.Combiner<ChestBlockEntity,List<BlockPos>>() {
                public List<BlockPos> acceptDouble(ChestBlockEntity first,ChestBlockEntity second) { return List.of(first.getBlockPos(),second.getBlockPos()); }
                public List<BlockPos> acceptSingle(ChestBlockEntity single) { return List.of(single.getBlockPos()); }
                public List<BlockPos> acceptNone() { return List.of(); }
            });
        } else if(state.getBlock() instanceof BarrelBlock) positions=List.of(position);
        else return;
        if(positions.isEmpty()) return;
        String dimension=level.dimension().location().toString();
        var addresses=positions.stream().map(pos -> new ChestAddress(dimension,BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString(),pos.getX(),pos.getY(),pos.getZ())).toList();
        pending=new Target(level,addresses,-1); requestedAt=System.nanoTime();
    }
    public static void opening(ClientboundOpenScreenPacket packet) {
        active=null;
        Target candidate=pending; pending=null;
        if(candidate==null || candidate.level()!=Minecraft.getInstance().level || System.nanoTime()-requestedAt>3_000_000_000L) return;
        var expected=candidate.blocks().size()==2?MenuType.GENERIC_9x6:MenuType.GENERIC_9x3;
        if(packet.getType()!=expected) return;
        for(var address:candidate.blocks()) {
            var state=candidate.level().getBlockState(new BlockPos(address.x(),address.y(),address.z()));
            if(!BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(address.block())) return;
        }
        active=new Target(candidate.level(),candidate.blocks(),packet.getContainerId());
    }
    public static Target current(AbstractContainerMenu menu) {
        var mc=Minecraft.getInstance();
        if(active==null || active.level()!=mc.level || !(menu instanceof ChestMenu chest) || menu.containerId!=active.menuId()
            || chest.getRowCount()*9!=active.blocks().size()*27) return null;
        return active;
    }
}
