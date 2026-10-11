package dev.msmr.client;

import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** A real focusable button: item pictures also have spoken names and hover help. */
final class FilterItemButton extends Button {
    private final LookupCatalog.Entry entry;
    private final BooleanSupplier selected;
    FilterItemButton(int x,int y,LookupCatalog.Entry entry,BooleanSupplier selected,OnPress press,String help) {
        super(x,y,23,23,Component.literal(entry.name()),press,DEFAULT_NARRATION);
        this.entry=entry; this.selected=selected;
        setTooltip(Tooltip.create(Component.literal(entry.name()+"\n"+help)));
    }
    @Override public void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE,Component.literal(entry.name()+", "+(selected.getAsBoolean()?"selected":"not selected")));
        output.add(net.minecraft.client.gui.narration.NarratedElementType.USAGE,Component.literal("Press Enter to add or remove this entry."));
    }
    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta) {
        int x=getX(),y=getY(); boolean chosen=selected.getAsBoolean();
        g.fill(x,y,x+23,y+23,isHoveredOrFocused()?0xFFFFFFFF:chosen?0xFF38834B:0xFF555555);
        g.fill(x+1,y+1,x+22,y+22,chosen?0xFFC2E6C6:0xFF999999);
        if(!entry.icon().isEmpty()) g.renderItem(entry.icon(),x+3,y+3);
        else g.drawString(Minecraft.getInstance().font,"#",x+8,y+7,0x303030,false);
        if(chosen) {
            g.fill(x+16,y+16,x+22,y+22,0xFF245C32);
            g.fill(x+17,y+18,x+19,y+20,0xFFFFFFFF);
            g.fill(x+19,y+17,x+21,y+19,0xFFFFFFFF);
        }
    }
}
