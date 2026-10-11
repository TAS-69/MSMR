package dev.msmr;

import net.minecraft.resources.ResourceLocation;

/** A physical storage block within a world-scoped settings entry. */
public record ChestAddress(String dimension,String block,int x,int y,int z) {
    public ChestAddress {
        if(ResourceLocation.tryParse(dimension)==null || ResourceLocation.tryParse(block)==null)
            throw new IllegalArgumentException("Invalid chest address");
    }
    public String key() { return dimension+"|"+x+","+y+","+z+"|"+block; }
    public static ChestAddress parse(String key) {
        String[] parts=key.split("\\|",-1);
        if(parts.length!=3) throw new IllegalArgumentException("Invalid chest address");
        String[] position=parts[1].split(",",-1);
        if(position.length!=3) throw new IllegalArgumentException("Invalid chest position");
        return new ChestAddress(parts[0],parts[2],Integer.parseInt(position[0]),Integer.parseInt(position[1]),Integer.parseInt(position[2]));
    }
    public boolean at(String dimension,int x,int y,int z) {
        return this.dimension.equals(dimension) && this.x==x && this.y==y && this.z==z;
    }
}
