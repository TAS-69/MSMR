package dev.msmr.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** Load from the mod JAR directly: Fabric Loader alone does not add mod resource packs. */
public final class ModIcon {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("msmr","supplied_icon");
    private static boolean registered;
    public static ResourceLocation texture() {
        if(!registered) {
            try(var input=ModIcon.class.getResourceAsStream("/assets/msmr/textures/gui/icon.png")) {
                if(input==null) throw new IllegalStateException("MSMR icon missing from mod JAR");
                Minecraft.getInstance().getTextureManager().register(TEXTURE,new DynamicTexture(NativeImage.read(input)));
                registered=true;
            } catch(java.io.IOException ex) { throw new IllegalStateException("Cannot load MSMR icon",ex); }
        }
        return TEXTURE;
    }
}
