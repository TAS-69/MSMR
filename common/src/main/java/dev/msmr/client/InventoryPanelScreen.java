package dev.msmr.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import dev.msmr.mixin.RecipeBookVisibility;

/** Modal drawer: the real inventory remains visible, but cannot receive clicks behind it. */
public abstract class InventoryPanelScreen extends Screen {
    protected final Screen parent;
    protected int panelX,panelY;
    protected int panelHeight;
    protected int panelWidth=316;
    private int backgroundX,backgroundY;
    protected InventoryPanelScreen(Component title,Screen parent,int height) {
        super(title); this.parent=parent; panelHeight=height;
    }
    protected void positionPanel() {
        Screen anchor=parent;
        while(anchor instanceof InventoryPanelScreen panel) anchor=panel.parent;
        int left=width/2-88,top=height/2-83,inventoryWidth=176;
        if(anchor instanceof InventoryAnchor a) { left=a.msmr$left(); top=a.msmr$top(); inventoryWidth=a.msmr$width(); }
        int total=inventoryWidth+panelWidth+42;
        if(width>=total+8) left=(width-total)/2+30;
        backgroundX=left; backgroundY=top;
        PanelLayout layout=PanelLayout.beside(width,height,left,top,inventoryWidth,panelWidth,panelHeight+16);
        panelX=layout.x()+8; panelY=layout.y()+8;
    }
    protected void renderPanel(GuiGraphics g,int mx,int my,float delta) {
        // Render only the inventory ancestor, never another modal drawer.
        Screen background=parent;
        while(background instanceof InventoryPanelScreen panel) background=panel.parent;
        if(background instanceof InventoryAnchor anchor) {
            int oldX=anchor.msmr$left(),oldY=anchor.msmr$top();
            var book=background instanceof RecipeUpdateListener listener?listener.getRecipeBookComponent():null;
            boolean bookVisible=book!=null && book.isVisible();
            if(bookVisible) ((RecipeBookVisibility)book).msmr$renderVisible(false);
            anchor.msmr$move(backgroundX,backgroundY);
            try { background.render(g,-1000,-1000,delta); }
            finally { anchor.msmr$move(oldX,oldY); if(bookVisible) ((RecipeBookVisibility)book).msmr$renderVisible(true); }
        } else background.render(g,-1000,-1000,delta);
        // Inventory items/entities write depth at different levels. Start a fresh depth layer
        // before the modal so they cannot bleed through the drawer at compact GUI scales.
        g.flush(); RenderSystem.clear(256,Minecraft.ON_OSX);
        g.fill(0,0,width,height,0x60000000);
        int x=panelX-8,y=panelY-8,w=panelWidth,h=panelHeight+16;
        g.fill(x,y,x+w,y+h,0xFF373737);
        g.fill(x+1,y+1,x+w-1,y+h-1,0xFFC6C6C6);
        g.fill(x+1,y+1,x+w-1,y+3,0xFFFFFFFF);
        g.fill(x+1,y+1,x+3,y+h-1,0xFFFFFFFF);
        g.fill(x+3,y+h-3,x+w-1,y+h-1,0xFF555555);
        g.fill(x+w-3,y+3,x+w-1,y+h-1,0xFF555555);
    }
    // Screen.render calls this before widgets; the inventory backdrop is already rendered.
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float delta) {}
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
