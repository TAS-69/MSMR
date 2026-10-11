package dev.msmr;

import com.google.gson.Gson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Optional, versioned protocol. Bounds apply before parsing or allocating rule maps. */
public record FilterPayload(int kind,long revision,byte[] data) implements CustomPacketPayload {
    public static final int PROBE=0,HELLO=1,RULES=2,ACK=3,VERSION=1;
    public static final Type<FilterPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("msmr","filters_v1"));
    private static final Gson JSON=new Gson();
    public static final StreamCodec<RegistryFriendlyByteBuf,FilterPayload> CODEC=new StreamCodec<>() {
        public FilterPayload decode(RegistryFriendlyByteBuf buf) { return new FilterPayload(buf.readVarInt(),buf.readLong(),buf.readByteArray(30000)); }
        public void encode(RegistryFriendlyByteBuf buf,FilterPayload value) { buf.writeVarInt(value.kind); buf.writeLong(value.revision); buf.writeByteArray(value.data); }
    };
    @Override public Type<FilterPayload> type() { return TYPE; }
    public static FilterPayload signal(int kind,long value) { return new FilterPayload(kind,value,new byte[0]); }
    public static FilterPayload rules(long revision,FilterSnapshot rules) {
        try {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(GZIPOutputStream gzip=new GZIPOutputStream(bytes)) { gzip.write(JSON.toJson(rules).getBytes(StandardCharsets.UTF_8)); }
            if(bytes.size()>30000) throw new IllegalArgumentException("Filter too large for server sync.");
            return new FilterPayload(RULES,revision,bytes.toByteArray());
        } catch(IOException ex) { throw new IllegalArgumentException("Cannot encode filters",ex); }
    }
    public FilterSnapshot snapshot() {
        if(kind!=RULES || data.length>30000) throw new IllegalArgumentException("Invalid filters packet");
        try(GZIPInputStream gzip=new GZIPInputStream(new ByteArrayInputStream(data))) {
            byte[] plain=gzip.readNBytes(1_000_001);
            if(plain.length>1_000_000) throw new IllegalArgumentException("Filters packet too large");
            FilterSnapshot result=JSON.fromJson(new String(plain,StandardCharsets.UTF_8),FilterSnapshot.class);
            if(result==null) throw new IllegalArgumentException("Missing filters");
            return result;
        } catch(IOException ex) { throw new IllegalArgumentException("Invalid compressed filters",ex); }
    }
}
