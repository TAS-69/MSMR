package dev.msmr.client;

import dev.msmr.QuickProfiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Compact, focusable shortcut with a readable letter or one item picture. */
public final class ProfileButton extends Button {
    private final String profile;
    public ProfileButton(int x,int y,String profile,OnPress press) {
        super(x,y,24,22,Component.literal("Profile: "+profile),press,DEFAULT_NARRATION); this.profile=profile;
        setTooltip(Tooltip.create(Component.literal(profile+"\nClick to switch. Customize in Advanced > Profiles.")));
    }
    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta) {
        QuickProfiles.Badge badge=ClientRules.badge(profile); if(badge==null) return;
        int x=getX(),y=getY(),color=badge.color();
        boolean selected=profile.equals(ClientRules.activeProfile());
        g.fill(x,y,x+width,y+height,isHoveredOrFocused()?0xFFFFFFFF:selected?0xFFFFD66B:0xFF333333);
        g.fill(x+2,y+2,x+width-2,y+height-2,0xFF000000|color);
        if(!badge.item().isEmpty()) {
            var item=BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(badge.item()));
            if(item.isPresent()) g.renderItem(new ItemStack(item.get()),x+4,y+3);
            else letter(g,badge.letter(),color,x,y);
        } else letter(g,badge.letter(),color,x,y);
        if(!active) g.fill(x+2,y+2,x+width-2,y+height-2,0x60000000);
    }
    private void letter(GuiGraphics g,String letter,int color,int x,int y) {
        int brightness=(((color>>16)&255)*299+((color>>8)&255)*587+(color&255)*114)/1000;
        var font=Minecraft.getInstance().font; g.drawString(font,letter,x+(width-font.width(letter))/2,y+7,brightness>145?0xFF202020:0xFFFFFFFF,false);
    }
}
