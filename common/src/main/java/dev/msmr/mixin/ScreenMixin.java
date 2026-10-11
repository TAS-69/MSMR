package dev.msmr.mixin;

import dev.msmr.*;
import dev.msmr.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(AbstractContainerScreen.class)
public abstract class ScreenMixin extends Screen implements InventoryAnchor {
    protected ScreenMixin(Component title) { super(title); }
    @Shadow protected Slot hoveredSlot;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Override public int msmr$left() { return leftPos; }
    @Override public int msmr$top() { return topPos; }
    @Override public int msmr$width() { return imageWidth; }
    @Override public void msmr$move(int left,int top) { leftPos=left; topPos=top; }
    @Unique private int msmr$painted = -1;
    @Unique private boolean msmr$consumed;
    @Unique private boolean msmr$editing;
    @Unique private Button msmr$button;
    @Unique private final java.util.List<Button> msmr$tabs=new java.util.ArrayList<>();
    @Unique private final java.util.List<Button> msmr$shortcuts=new java.util.ArrayList<>();
    @Unique private final java.util.List<Button> msmr$transfers=new java.util.ArrayList<>();
    @Unique private String msmr$controls="";
    @Inject(method="init", at=@At("TAIL"))
    private void msmr$request(CallbackInfo ci) {
        ClientRules.refresh();
        msmr$consumed=false;
        msmr$tabs.clear();
        msmr$shortcuts.clear(); msmr$transfers.clear(); msmr$controls="";
        boolean chest=ClientRules.hasChest();
        msmr$button=msmr$tab(msmr$editing?"Done":chest?"Chest":"Pickups",chest?"Chest filters: saved automatically to this chest":"Pickup filters: choose which drops to collect",b -> {
            if(msmr$editing) msmr$editSlots(false);
            else if(!ClientTransfers.busy()) minecraft.setScreen(new PickupScreen(this,ClientRules.hasChest()));
        });
        msmr$tab("Advanced","Advanced tools: profiles, slot rules and inventory moves",b -> {
            if(!ClientTransfers.busy()) minecraft.setScreen(new AdvancedScreen(this));
        });
    }
    @Override public void msmr$editSlots(boolean editing) {
        msmr$editing=editing;
        if(msmr$button!=null) msmr$button.setMessage(Component.literal(editing?"Done":ClientRules.hasChest()?"Chest":"Pickups"));
    }
    @Unique private Button msmr$tab(String text,String tip,Button.OnPress press) {
        Button button=new Button(0,0,28,22,Component.literal(text),press,supplier -> supplier.get()) {
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta) {
                int x=getX(),y=getY();
                g.fill(x,y,x+28,y+22,isHoveredOrFocused()?0xFFFFFFFF:0xFF555555);
                g.fill(x+1,y+1,x+27,y+21,0xFFC6C6C6);
                if(getMessage().getString().equals("Done")) g.drawString(Minecraft.getInstance().font,"Done",x+2,y+7,0x303030,false);
                else if(getMessage().getString().equals("Pickups") || getMessage().getString().equals("Chest")) g.blit(ModIcon.texture(),x+6,y+3,0,0,16,16,16,16);
                else {
                    // Settings sliders, with no font-dependent symbol glyphs.
                    for(int i=0;i<3;i++) { g.fill(x+5,y+5+i*5,x+23,y+7+i*5,0xFF404040); int knob=x+8+(i%2)*9; g.fill(knob,y+3+i*5,knob+3,y+9+i*5,0xFF404040); }
                }
            }
        };
        button.setTooltip(Tooltip.create(Component.literal("MSMR - "+tip))); addRenderableWidget(button);
        msmr$tabs.add(button); return button;
    }
    @Unique private int msmr$tabLeft() {
        boolean book=(Object)this instanceof net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener listener && listener.getRecipeBookComponent().isVisible();
        return PanelLayout.tabX(leftPos,book);
    }
    @Inject(method="render",at=@At("HEAD"))
    private void msmr$position(GuiGraphics g,int mx,int my,float delta,CallbackInfo ci) {
        msmr$positionTabs();
    }
    @Inject(method="onClose",at=@At("HEAD"),cancellable=true)
    private void msmr$saveBeforeClose(CallbackInfo ci) { if(ClientChestSync.deferClose()) ci.cancel(); }
    @Unique private void msmr$positionTabs() {
        msmr$updateControls();
        int y=Math.max(0,Math.min(topPos+6,height-52-msmr$shortcuts.size()*20));
        for(int i=0;i<msmr$tabs.size();i++) {
            Button tab=msmr$tabs.get(i); tab.setX(msmr$tabLeft()); tab.setY(y+i*24);
            tab.active=!ClientTransfers.busy();
        }
        for(int i=0;i<msmr$shortcuts.size();i++) { Button b=msmr$shortcuts.get(i); b.setX(msmr$tabLeft()+5); b.setY(y+52+i*20); b.active=!ClientTransfers.busy() && Minecraft.getInstance().player.containerMenu.getCarried().isEmpty(); }
        for(int i=0;i<msmr$transfers.size();i++) { Button b=msmr$transfers.get(i); b.setX(leftPos+imageWidth-44+i*22); b.setY(Math.max(0,topPos-20)); b.active=!ClientTransfers.busy() && Minecraft.getInstance().player.containerMenu.getCarried().isEmpty(); }
    }
    @Unique private void msmr$updateControls() {
        var mc=Minecraft.getInstance(); if(mc.player==null) return;
        var options=ClientRules.tools(); var profiles=options.shortcuts()?ClientRules.quickProfiles():java.util.List.<String>of();
        boolean chest=((AbstractContainerScreen<?>)(Object)this).getMenu() instanceof net.minecraft.world.inventory.ChestMenu;
        String signature=profiles.toString()+options.toString()+chest;
        if(signature.equals(msmr$controls)) return;
        msmr$shortcuts.forEach(this::removeWidget); msmr$transfers.forEach(this::removeWidget); msmr$shortcuts.clear(); msmr$transfers.clear(); msmr$controls=signature;
        for(String name:profiles) {
            Button b=new ProfileButton(0,0,name,pressed -> { String result=ClientRules.manageProfile("switch",name,""); mc.player.displayClientMessage(Component.literal(result),true); });
            addRenderableWidget(b); msmr$shortcuts.add(b);
        }
        if(chest) { if(options.transferTo()) msmr$transfer(true); if(options.transferFrom()) msmr$transfer(false); }
    }
    @Unique private void msmr$transfer(boolean toChest) {
        Button b=new Button(0,0,20,18,Component.literal(toChest?"Transfer to chest":"Transfer to inventory"),pressed -> ClientTransfers.transferAll(toChest),supplier -> supplier.get()) {
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta) {
                int x=getX(),y=getY(); g.fill(x,y,x+20,y+18,isHoveredOrFocused()?0xFFFFFFFF:0xFF555555); g.fill(x+1,y+1,x+19,y+17,0xFFC6C6C6);
                int color=active?0xFF303030:0xFF888888; g.fill(x+9,y+4,x+11,y+14,color);
                for(int n=0;n<4;n++) { int ay=toChest?y+3+n:y+14-n; g.fill(x+9-n,ay,x+11+n,ay+1,color); }
            }
        };
        b.setTooltip(Tooltip.create(Component.literal(toChest?"Move all allowed inventory items to this chest, including your hotbar. Blocked items stay put.":"Move all allowed chest items to your inventory. Player and slot filters still apply.")));
        addRenderableWidget(b); msmr$transfers.add(b);
    }
    @Override public void msmr$renderTabs(GuiGraphics g,int mx,int my,float delta) {
        msmr$positionTabs();
        for(Button button:msmr$tabs) button.render(g,mx,my,delta);
        for(Button button:msmr$shortcuts) button.render(g,mx,my,delta);
        for(Button button:msmr$transfers) button.render(g,mx,my,delta);
    }
    @Override public boolean msmr$clickTabs(double x,double y,int button) {
        for(Button tab:msmr$tabs) if(tab.mouseClicked(x,y,button)) { setFocused(tab); return true; }
        for(Button tab:msmr$shortcuts) if(tab.mouseClicked(x,y,button)) { setFocused(tab); return true; }
        for(Button tab:msmr$transfers) if(tab.mouseClicked(x,y,button)) { setFocused(tab); return true; }
        return false;
    }

    @Inject(method="mouseClicked", at=@At("HEAD"), cancellable=true)
    private void msmr$click(double x, double y, int button, CallbackInfoReturnable<Boolean> cir) {
        if ((!Screen.hasAltDown() && !msmr$editing) || hoveredSlot == null || !ClientRules.rules.containsKey(hoveredSlot.index)) return;
        if (ClientTransfers.busy()) { cir.setReturnValue(true); return; }
        msmr$painted = hoveredSlot.index;
        msmr$consumed=true;
        if (Screen.hasControlDown()) ClientRules.edit(hoveredSlot.index, Rule.OPEN);
        else if (button == 1) ClientRules.edit(hoveredSlot.index, ClientRules.clipboard);
        else if (Screen.hasShiftDown()) ClientRules.clipboard = ClientRules.rules.get(hoveredSlot.index);
        else Minecraft.getInstance().setScreen(new RuleScreen((Screen)(Object)this,hoveredSlot));
        cir.setReturnValue(true);
    }
    @Inject(method="mouseReleased", at=@At("HEAD"), cancellable=true)
    private void msmr$release(double x,double y,int button,CallbackInfoReturnable<Boolean> cir) {
        if (msmr$consumed) { msmr$consumed=false; cir.setReturnValue(true); }
    }
    @Inject(method="mouseDragged", at=@At("HEAD"), cancellable=true)
    private void msmr$paint(double x,double y,int button,double dx,double dy,CallbackInfoReturnable<Boolean> cir) {
        if ((!Screen.hasAltDown() && !msmr$editing) || button != 1 || !msmr$consumed || ClientTransfers.busy()) return;
        if (hoveredSlot != null && hoveredSlot.index != msmr$painted && ClientRules.rules.containsKey(hoveredSlot.index)) {
            msmr$painted=hoveredSlot.index; ClientRules.edit(hoveredSlot.index,ClientRules.clipboard);
        }
        cir.setReturnValue(true);
    }
    @Inject(method="render", at=@At("TAIL"))
    private void msmr$overlay(GuiGraphics g,int mx,int my,float delta,CallbackInfo ci) {
        ClientRules.refresh();
        var screen = (AbstractContainerScreen<?>)(Object)this;
        Rule wide=ClientRules.hasChest()?ClientRules.chestFilter():ClientRules.inventoryFilter();
        if(!wide.unrestricted()) g.fill(msmr$tabLeft()+2,Math.max(0,Math.min(topPos+6,height-48))+2,msmr$tabLeft()+4,Math.max(0,Math.min(topPos+6,height-48))+20,0xFF55DD77);
        if (msmr$editing) {
            g.drawCenteredString(Minecraft.getInstance().font,"MSMR: click a slot to edit; Done to move items",width/2,height-12,0xFFDD77);
        }
        for (Slot slot : screen.getMenu().slots) {
            Rule rule = ClientRules.rules.get(slot.index);
            if (rule == null || rule.unrestricted()) continue;
            int color = switch(rule.mode()) { case ALL -> 0xFF66BBFF; case WHITELIST -> 0xFF55DD77; case BLACKLIST -> 0xFFFFBB44; case BLOCK -> 0xFFFF5555; };
            int x=leftPos+slot.x,y=topPos+slot.y;
            g.fill(x,y,x+16,y+1,color); g.fill(x,y+15,x+16,y+16,color);
            g.fill(x,y,x+1,y+16,color); g.fill(x+15,y,x+16,y+16,color);
        }
        if ((Screen.hasAltDown() || msmr$editing) && hoveredSlot != null && ClientRules.rules.containsKey(hoveredSlot.index))
            g.renderTooltip(Minecraft.getInstance().font, net.minecraft.network.chat.Component.literal("Alt-click: edit | Alt-right: paste/paint | Alt-Shift: copy | Alt-Ctrl: clear"),mx,my);
    }
}
