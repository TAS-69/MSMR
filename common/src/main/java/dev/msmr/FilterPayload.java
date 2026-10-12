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
    public static final int PROBE=0,HELLO=1,RULES=2,ACK=3,CHEST_GET=4,CHEST_STATE=5,CHEST_SET=6,ACTION=7,RESULT=8,VERSION=2;
    public static final Type<FilterPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("msmr","filters_v2"));
    private static final Gson JSON=new Gson();
    public static final StreamCodec<RegistryFriendlyByteBuf,FilterPayload> CODEC=new StreamCodec<>() {
        public FilterPayload decode(RegistryFriendlyByteBuf buf) { return new FilterPayload(buf.readVarInt(),buf.readLong(),buf.readByteArray(30000)); }
        public void encode(RegistryFriendlyByteBuf buf,FilterPayload value) { buf.writeVarInt(value.kind); buf.writeLong(value.revision); buf.writeByteArray(value.data); }
    };
    @Override public Type<FilterPayload> type() { return TYPE; }
    public static FilterPayload signal(int kind,long value) { return new FilterPayload(kind,value,new byte[0]); }
    public static FilterPayload rules(long revision,FilterSnapshot rules) {
        return encode(RULES,revision,rules);
    }
    public static FilterPayload encode(int kind,long revision,Object value) {
        try {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(GZIPOutputStream gzip=new GZIPOutputStream(bytes)) { gzip.write(JSON.toJson(value).getBytes(StandardCharsets.UTF_8)); }
            if(bytes.size()>30000) throw new IllegalArgumentException("Filter too large for server sync.");
            return new FilterPayload(kind,revision,bytes.toByteArray());
        } catch(IOException ex) { throw new IllegalArgumentException("Cannot encode filters",ex); }
    }
    public FilterSnapshot snapshot() {
        if(kind!=RULES) throw new IllegalArgumentException("Invalid filters packet");
        return read(FilterSnapshot.class);
    }
    public <T> T read(Class<T> type) {
        if(data.length>30000) throw new IllegalArgumentException("Invalid packet");
        try(GZIPInputStream gzip=new GZIPInputStream(new ByteArrayInputStream(data))) {
            byte[] plain=gzip.readNBytes(1_000_001);
            if(plain.length>1_000_000) throw new IllegalArgumentException("Filters packet too large");
            String json=new String(plain,StandardCharsets.UTF_8);
            validateDepth(json);
            T result=JSON.fromJson(json,type);
            if(result==null) throw new IllegalArgumentException("Missing filters");
            return result;
        } catch(IOException ex) { throw new IllegalArgumentException("Invalid compressed filters",ex); }
    }
    /** Bound parser recursion even when unknown JSON fields are supplied. */
    static void validateDepth(String json) {
        int depth=0;
        try(var reader=new com.google.gson.stream.JsonReader(new StringReader(json))) {
            reader.setLenient(false);
            while(true) switch(reader.peek()) {
                case BEGIN_OBJECT -> { if(++depth>32) throw new IllegalArgumentException("Filters are nested too deeply"); reader.beginObject(); }
                case BEGIN_ARRAY -> { if(++depth>32) throw new IllegalArgumentException("Filters are nested too deeply"); reader.beginArray(); }
                case END_OBJECT -> { reader.endObject(); depth--; }
                case END_ARRAY -> { reader.endArray(); depth--; }
                case NAME -> reader.nextName();
                case STRING,NUMBER -> reader.nextString();
                case BOOLEAN -> reader.nextBoolean();
                case NULL -> reader.nextNull();
                case END_DOCUMENT -> { return; }
            }
        } catch(IOException ex) { throw new IllegalArgumentException("Invalid filters JSON",ex); }
    }
}
