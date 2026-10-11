package dev.msmr;

import dev.msmr.client.PanelLayout;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PanelLayoutTest {
    @Test void attachesToTheRightWhenThereIsRoom() {
        var panel=PanelLayout.beside(960,540,392,187,176,316,234);
        assertEquals(574,panel.x()); assertEquals(187,panel.y());
    }
    @Test void usesLeftSideWhenRecipeBookShiftsInventoryRight() {
        var panel=PanelLayout.beside(800,400,490,117,176,316,234);
        assertEquals(138,panel.x());
    }
    @Test void compactFallbackAndAllSupportedGuiSizesStayOnScreen() {
        for(int width=320;width<=1920;width+=17) for(int height=240;height<=1080;height+=13)
            for(int left:new int[]{0,width/2-88,width-176}) {
                var p=PanelLayout.beside(width,height,left,height/2-83,176,316,234);
                assertTrue(p.x()>=4 && p.y()>=4);
                assertTrue(p.x()+p.width()<=width-4);
                assertTrue(p.y()+p.height()<=height-4);
                assertTrue(PanelLayout.tabX(left)>=0);
                assertTrue(PanelLayout.tabX(left)+28<=width);
            }
    }
}
