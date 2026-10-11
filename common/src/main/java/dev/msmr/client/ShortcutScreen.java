package dev.msmr.client;

import dev.msmr.QuickProfiles;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ShortcutScreen extends InventoryPanelScreen {
    private String selected="",status="";
    private EditBox letter,color;
    public ShortcutScreen(Screen parent) { super(Component.literal("Profile shortcuts"),parent,218); }
    @Override protected void init() {
        ClientRules.refresh(); var names=ClientRules.profileNames(); if(!names.contains(selected)) selected=names.isEmpty()?"":names.getFirst();
        positionPanel(); int x=panelX,y=panelY;
        Button cycle=addRenderableWidget(Button.builder(Component.literal(selected.isEmpty()?"No saved profiles":"Profile: "+selected),b -> { selected=names.get((names.indexOf(selected)+1)%names.size()); rebuildWidgets(); }).bounds(x,y+30,300,20).build()); cycle.active=!names.isEmpty();
        if(!selected.isEmpty()) {
            var badge=ClientRules.badge(selected);
            addRenderableWidget(new ProfileButton(x+138,y+57,selected,b -> {}));
            letter=new EditBox(font,x,y+94,40,20,Component.literal("Shortcut letter")); letter.setMaxLength(1); letter.setValue(badge.letter()); addRenderableWidget(letter);
            letter.setResponder(value -> { if(value.length()==1 && Character.isLetterOrDigit(value.charAt(0))) { var old=ClientRules.badge(selected); status=ClientRules.badge(selected,new QuickProfiles.Badge(String.valueOf(Character.toUpperCase(value.charAt(0))),"",old.color())); } });
            addRenderableWidget(Button.builder(Component.literal("Use letter"),b -> { var old=ClientRules.badge(selected); status=ClientRules.badge(selected,new QuickProfiles.Badge(old.letter(),"",old.color())); }).bounds(x+46,y+94,100,20).build());
            addRenderableWidget(Button.builder(Component.literal("Choose icon"),b -> minecraft.setScreen(new ItemPickerScreen(this,List.of(),values -> { var old=ClientRules.badge(selected); status=ClientRules.badge(selected,new QuickProfiles.Badge(old.letter(),values.isEmpty()?"":values.getFirst(),old.color())); },true))).bounds(x+155,y+94,145,20).build());
            for(int i=0;i<QuickProfiles.COLORS.length;i++) {
                int value=QuickProfiles.COLORS[i];
                Button swatch=new Button(x+i*23,y+135,20,20,Component.literal(String.format("Colour #%06X",value)),b -> { var old=ClientRules.badge(selected); status=ClientRules.badge(selected,new QuickProfiles.Badge(old.letter(),old.item(),value)); rebuildWidgets(); },supplier -> supplier.get()) {
                    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta) { g.fill(getX(),getY(),getX()+20,getY()+20,isHoveredOrFocused()?0xFFFFFFFF:0xFF444444); g.fill(getX()+2,getY()+2,getX()+18,getY()+18,0xFF000000|value); }
                }; addRenderableWidget(swatch);
            }
            color=new EditBox(font,x+193,y+135,107,20,Component.literal("Background colour hex")); color.setMaxLength(7); color.setValue(String.format("#%06X",badge.color())); addRenderableWidget(color);
            color.setResponder(value -> { if(value.matches("#?[0-9a-fA-F]{6}")) { var old=ClientRules.badge(selected); status=ClientRules.badge(selected,new QuickProfiles.Badge(old.letter(),old.item(),Integer.parseInt(value.replace("#",""),16))); } });
            boolean pinned=ClientRules.quickProfiles().contains(selected);
            addRenderableWidget(Button.builder(Component.literal(pinned?"On quick panel: ON":"On quick panel: OFF"),b -> { status=ClientRules.pin(selected,!pinned); rebuildWidgets(); }).bounds(x,y+163,300,20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Back"),b -> onClose()).bounds(x,y+198,300,20).build());
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        renderPanel(g,mx,my,delta); int x=panelX,y=panelY;
        g.drawString(font,title,x,y+2,0x303030,false);
        g.drawString(font,"Five shortcuts. Your letters, icons and colours.",x,y+16,0x505050,false);
        if(selected.isEmpty()) g.drawString(font,"Save a profile first in Advanced > Profiles.",x,y+85,0x404040,false);
        else { g.drawString(font,"Letter",x,y+82,0x404040,false); g.drawString(font,"Background colour / hex",x,y+122,0x404040,false); }
        g.drawString(font,font.plainSubstrByWidth(status,300),x,y+186,0x803B18,false); super.render(g,mx,my,delta);
    }
}
