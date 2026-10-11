package dev.msmr.client;

/** Pure layout calculation shared by editors and layout regression tests. */
public record PanelLayout(int x, int y, int width, int height) {
    public static PanelLayout beside(int screenWidth, int screenHeight, int inventoryX, int inventoryY,
                                     int inventoryWidth, int panelWidth, int panelHeight) {
        int w=Math.min(panelWidth,screenWidth-8), h=Math.min(panelHeight,screenHeight-8);
        int right=inventoryX+inventoryWidth+6;
        int x=right+w<=screenWidth-4?right:inventoryX-36-w;
        if(x<4) x=(screenWidth-w)/2;
        return new PanelLayout(Math.max(4,x),Math.max(4,Math.min(inventoryY,screenHeight-h-4)),w,h);
    }
    public static int tabX(int inventoryX) { return Math.max(0,inventoryX-30); }
    public static int tabX(int inventoryX,boolean recipeBook) { return tabX(inventoryX-(recipeBook?180:0)); }
}
