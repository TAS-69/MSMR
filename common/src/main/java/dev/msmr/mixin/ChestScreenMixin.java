package dev.msmr.mixin;

import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ContainerScreen.class)
public abstract class ChestScreenMixin extends AbstractContainerScreen<ChestMenu> {
    protected ChestScreenMixin(ChestMenu menu,Inventory inventory,Component title) { super(menu,inventory,title); }
    @ModifyArgs(method="renderBg",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"))
    private void msmr$alignStorageBackground(Args args) {
        args.set(1,leftPos);
        args.set(2,topPos+(int)args.get(2)-(height-imageHeight)/2);
    }
}
