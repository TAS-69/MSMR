package dev.msmr.client;

import dev.msmr.Rule;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Shared explicit mode choices and a compact, removable item preview. */
public final class RuleVisuals {
    private RuleVisuals() {}
    public static String label(Rule.Mode mode) {
        return switch(mode) {
            case ALL -> "Allow everything";
            case BLACKLIST -> "Block selected";
            case WHITELIST -> "Allow selected";
            case BLOCK -> "Block everything";
        };
    }
    public static Button choice(int x,int y,int width,Rule.Mode value,Rule.Mode current,Button.OnPress action) {
        return Button.builder(Component.literal((value==current?"[x] ":"[ ] ")+label(value)),action).bounds(x,y,width,20).build();
    }
    public static void tooltip(GuiGraphics g,Font font,int x,int y,int mx,int my,List<String> tokens,List<LookupCatalog.Entry> catalog) {
        int index=(mx-x-4)/29;
        if(mx<x+4 || mx>=x+294 || my<y+4 || my>=y+25 || index>=Math.min(10,tokens.size())) return;
        String token=tokens.get(index);
        var entry=catalog.stream().filter(e -> e.token().equals(token)).findFirst().orElse(null);
        g.renderTooltip(font,Component.literal((entry==null?token:entry.name())+" - click to remove"),mx,my);
    }
    public static void entries(GuiGraphics g,Font font,int x,int y,List<String> tokens,List<LookupCatalog.Entry> catalog) {
        g.fill(x,y,x+300,y+38,0xFFEEEEEE);
        if(tokens.isEmpty()) { g.drawString(font,"No items selected. Choose items below.",x+6,y+14,0x303030,false); return; }
        int count=Math.min(10,tokens.size());
        for(int i=0;i<count;i++) {
            int left=x+4+i*29;
            g.fill(left,y+4,left+25,y+25,0xFFD0D0D0);
            String token=tokens.get(i);
            var entry=catalog.stream().filter(e -> e.token().equals(token)).findFirst().orElse(null);
            if(entry!=null && !entry.icon().isEmpty()) g.renderItem(entry.icon(),left+4,y+5);
            else g.drawString(font,"#",left+9,y+10,0x303030,false);
        }
        g.drawString(font,tokens.size()+" selected - click an icon to remove",x+5,y+29,0x303030,false);
    }
}
