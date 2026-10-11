package dev.msmr.mixin;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Transient render visibility only: do not change settings or send recipe-book packets. */
@Mixin(RecipeBookComponent.class)
public interface RecipeBookVisibility {
    @Accessor("visible") void msmr$renderVisible(boolean visible);
}
