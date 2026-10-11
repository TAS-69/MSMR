package dev.msmr.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AdvancedScreen extends InventoryPanelScreen {
    public enum Page { HOME, FILTERS, PROFILES, TRANSFERS }
    private final Page page;
    private String status="";
    public AdvancedScreen(Screen parent) { this(parent,Page.HOME); }
    public AdvancedScreen(Screen parent,Page page) { super(Component.literal(switch(page) { case HOME -> "Advanced tools"; case FILTERS -> "Filters & slots"; case PROFILES -> "Profiles"; case TRANSFERS -> "Transfers & sorting"; }),parent,218); this.page=page; }
    private void row(int index,String label,Button.OnPress press) { addRenderableWidget(Button.builder(Component.literal(label),press).bounds(panelX,panelY+34+index*36,300,20).build()); }
    private Screen inventory() { Screen screen=parent; while(screen instanceof InventoryPanelScreen p) screen=p.parent; return screen; }
    @Override protected void init() {
        ClientRules.refresh(); positionPanel(); boolean chest=ClientRules.hasChest(); var t=ClientRules.tools();
        switch(page) {
            case HOME -> {
                row(0,"Filters & slots",b -> minecraft.setScreen(new AdvancedScreen(this,Page.FILTERS)));
                row(1,"Profiles",b -> minecraft.setScreen(new AdvancedScreen(this,Page.PROFILES)));
                row(2,"Transfers & sorting",b -> minecraft.setScreen(new AdvancedScreen(this,Page.TRANSFERS)));
            }
            case FILTERS -> {
                row(0,chest?"Chest rules, tags & groups":"Inventory rules, tags & groups",b -> minecraft.setScreen(new InventoryFilterScreen(this)));
                row(1,"Edit individual slots",b -> { Screen screen=inventory(); if(screen instanceof InventoryAnchor anchor) { anchor.msmr$editSlots(true); minecraft.setScreen(screen); } });
                row(2,chest?"Reset chest filters":"Pickup filters",b -> { if(chest) ClientRules.resetChest(); minecraft.setScreen(new PickupScreen(inventory(),chest)); });
            }
            case PROFILES -> {
                row(0,"Saved player profiles",b -> minecraft.setScreen(new InventoryFilterScreen(this,true)));
                row(1,"Shortcut letters, icons & colours",b -> minecraft.setScreen(new ShortcutScreen(this)));
                row(2,"Quick profile panel: "+(t.shortcuts()?"ON":"OFF"),b -> { status=ClientRules.tools(new ClientRules.Tools(t.transferTo(),t.transferFrom(),!t.shortcuts(),t.sortOnSwitch())); rebuildWidgets(); });
                row(3,"Arrange on profile switch: "+(t.sortOnSwitch()?"ON":"OFF"),b -> { status=ClientRules.tools(new ClientRules.Tools(t.transferTo(),t.transferFrom(),t.shortcuts(),!t.sortOnSwitch())); rebuildWidgets(); });
            }
            case TRANSFERS -> {
                row(0,"Transfer to chest: "+(t.transferTo()?"ON":"OFF"),b -> { status=ClientRules.tools(new ClientRules.Tools(!t.transferTo(),t.transferFrom(),t.shortcuts(),t.sortOnSwitch())); rebuildWidgets(); });
                row(1,"Transfer to inventory: "+(t.transferFrom()?"ON":"OFF"),b -> { status=ClientRules.tools(new ClientRules.Tools(t.transferTo(),!t.transferFrom(),t.shortcuts(),t.sortOnSwitch())); rebuildWidgets(); });
                row(2,"Arrange inventory now",b -> { minecraft.setScreen(inventory()); ClientTransfers.sortInventory(); });
                row(3,"Fill reserved slots",b -> { minecraft.setScreen(inventory()); ClientTransfers.fillReserved(); });
            }
        }
        addRenderableWidget(Button.builder(Component.literal("Back"),b -> onClose()).bounds(panelX,panelY+198,300,20).build());
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        renderPanel(g,mx,my,delta); int x=panelX,y=panelY;
        g.drawString(font,title,x,y+2,0x303030,false);
        String description=switch(page) { case HOME -> "Choose a category."; case FILTERS -> "Rules, smart lookup and individual slots."; case PROFILES -> "Saved slots and up to five quick shortcuts."; case TRANSFERS -> "Move only what destination filters allow."; };
        g.drawString(font,description,x,y+17,0x505050,false);
        if(page==Page.HOME) {
            String[] hints={"Items, tags, categories and stack limits","Switch setups and customize the quick panel","Chest transfers and inventory arrangement"};
            for(int i=0;i<hints.length;i++) g.drawString(font,hints[i],x,y+57+i*36,0x505050,false);
        }
        g.drawString(font,font.plainSubstrByWidth(status,300),x,y+184,0x803B18,false); super.render(g,mx,my,delta);
    }
}
