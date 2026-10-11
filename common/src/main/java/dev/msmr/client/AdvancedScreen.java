package dev.msmr.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AdvancedScreen extends InventoryPanelScreen {
    public AdvancedScreen(Screen parent) { super(Component.literal("Advanced tools"),parent,218); }
    @Override protected void init() {
        positionPanel(); int x=panelX,y=panelY;
        addRenderableWidget(Button.builder(Component.literal("Inventory rules, tags & groups"),b -> minecraft.setScreen(new InventoryFilterScreen(this))).bounds(x,y+38,300,20).build());
        addRenderableWidget(Button.builder(Component.literal("Profiles"),b -> minecraft.setScreen(new InventoryFilterScreen(this,true))).bounds(x,y+76,300,20).build());
        addRenderableWidget(Button.builder(Component.literal("Edit individual slots"),b -> {
            Screen inventory=parent; while(inventory instanceof InventoryPanelScreen p) inventory=p.parent;
            if(inventory instanceof InventoryAnchor anchor) { anchor.msmr$editSlots(true); minecraft.setScreen(inventory); }
        }).bounds(x,y+114,300,20).build());
        addRenderableWidget(Button.builder(Component.literal("Fill reserved slots"),b -> { Screen inventory=parent; while(inventory instanceof InventoryPanelScreen p) inventory=p.parent; minecraft.setScreen(inventory); ClientTransfers.fillReserved(); }).bounds(x,y+152,300,20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"),b -> onClose()).bounds(x,y+198,300,20).build());
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        renderPanel(g,mx,my,delta); int x=panelX,y=panelY;
        g.drawString(font,title,x,y+2,0x303030,false);
        g.drawString(font,"More control when you need it.",x,y+19,0x505050,false);
        String[] hints={"Movement filters, categories and exact IDs","Save, switch, copy or delete setups","Slot filters, stack limits and painting","Move matching items into reserved slots"};
        for(int i=0;i<hints.length;i++) g.drawString(font,hints[i],x,y+61+i*38,0x505050,false);
        super.render(g,mx,my,delta);
    }
}
