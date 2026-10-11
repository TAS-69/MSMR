package dev.msmr.client;

/** Inventory coordinates, including recipe-book repositioning. */
public interface InventoryAnchor {
    int msmr$left();
    int msmr$top();
    int msmr$width();
    void msmr$move(int left,int top);
    void msmr$editSlots(boolean editing);
    void msmr$renderTabs(net.minecraft.client.gui.GuiGraphics graphics,int mx,int my,float delta);
    boolean msmr$clickTabs(double x,double y,int button);
}
