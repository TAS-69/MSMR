package dev.msmr.mixin;

import dev.msmr.client.InventoryAnchor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Vanilla skips the entire container renderer/input path when the narrow recipe book is open. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Shadow private boolean widthTooNarrow;
    @Inject(method="render",at=@At("TAIL"))
    private void msmr$narrowTabs(GuiGraphics g,int mx,int my,float delta,CallbackInfo ci) {
        if(widthTooNarrow && ((RecipeUpdateListener)this).getRecipeBookComponent().isVisible())
            ((InventoryAnchor)this).msmr$renderTabs(g,mx,my,delta);
    }
    @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
    private void msmr$narrowClick(double x,double y,int button,CallbackInfoReturnable<Boolean> cir) {
        if(widthTooNarrow && ((RecipeUpdateListener)this).getRecipeBookComponent().isVisible()
            && ((InventoryAnchor)this).msmr$clickTabs(x,y,button)) cir.setReturnValue(true);
    }
}
