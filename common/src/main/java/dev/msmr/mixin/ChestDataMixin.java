package dev.msmr.mixin;

import com.google.gson.Gson;
import dev.msmr.*;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BaseContainerBlockEntity.class)
public abstract class ChestDataMixin implements ChestData {
    @Unique private ChestProfile msmr$profile=ChestProfile.EMPTY;
    @Unique private UUID msmr$owner;
    @Unique private long msmr$version;
    @Unique private static final Gson msmr$json=new Gson();
    public ChestProfile msmr$profile() { return msmr$profile; }
    public UUID msmr$owner() { return msmr$owner; }
    public long msmr$version() { return msmr$version; }
    public void msmr$update(ChestProfile profile,UUID owner) { msmr$profile=profile; msmr$owner=owner; msmr$version++; ((BaseContainerBlockEntity)(Object)this).setChanged(); }
    @Inject(method="saveAdditional",at=@At("TAIL"))
    private void msmr$save(CompoundTag tag,HolderLookup.Provider registries,CallbackInfo ci) {
        if(msmr$owner!=null) { tag.putUUID("MSMROwner",msmr$owner); tag.putLong("MSMRVersion",msmr$version); tag.putString("MSMRRules",msmr$json.toJson(msmr$profile)); }
    }
    @Inject(method="loadAdditional",at=@At("TAIL"))
    private void msmr$load(CompoundTag tag,HolderLookup.Provider registries,CallbackInfo ci) {
        msmr$profile=ChestProfile.EMPTY; msmr$owner=null; msmr$version=0;
        if(tag.hasUUID("MSMROwner")) try {
            String json=tag.getString("MSMRRules"); if(json.length()>1000000) throw new IllegalArgumentException("Oversized chest rules");
            ChestProfile profile=msmr$json.fromJson(json,ChestProfile.class); if(profile==null) throw new IllegalArgumentException("Missing chest rules");
            msmr$profile=profile; msmr$owner=tag.getUUID("MSMROwner"); msmr$version=Math.max(1,tag.getLong("MSMRVersion"));
        } catch(RuntimeException ex) { org.slf4j.LoggerFactory.getLogger("MSMR").warn("Could not load chest rules at {}",((BaseContainerBlockEntity)(Object)this).getBlockPos()); }
    }
}
