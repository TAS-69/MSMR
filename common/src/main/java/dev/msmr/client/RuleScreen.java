package dev.msmr.client;

import dev.msmr.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.core.registries.BuiltInRegistries;

public final class RuleScreen extends InventoryPanelScreen {
    private final Slot slot;
    private Rule.Mode mode;
    private Button pickupButton;
    private EditBox entries, limit;
    private String error = "";
    private boolean advanced, pickups=true, moving=true, updating;
    private List<LookupCatalog.Entry> catalog=List.of();
    public RuleScreen(Screen parent, Slot slot) {
        super(Component.literal("Slot rules"),parent,224); this.slot = slot;
        Rule rule=ClientRules.rules.getOrDefault(slot.index, Rule.OPEN); mode=rule.mode(); pickups=rule.pickups(); moving=rule.moving();
    }
    @Override protected void init() {
        if(catalog.isEmpty()) catalog=LookupCatalog.build();
        positionPanel(); int x = panelX, y = panelY;
        Rule rule = ClientRules.rules.getOrDefault(slot.index, Rule.OPEN);
        String previousEntries=entries==null?String.join(",",rule.entries()):entries.getValue();
        String previousLimit=limit==null?Integer.toString(rule.limit()):limit.getValue();
        Rule.Mode[] choices={Rule.Mode.BLACKLIST,Rule.Mode.WHITELIST,Rule.Mode.ALL,Rule.Mode.BLOCK};
        for(int i=0;i<choices.length;i++) {
            Rule.Mode choice=choices[i];
            addRenderableWidget(RuleVisuals.choice(x+(i%2)*155,y+22+(i/2)*22,145,choice,mode,b -> { mode=choice; apply(); rebuildWidgets(); }));
        }
        entries = new EditBox(font,x,y+70,300,20,Component.literal("Items or tags"));
        entries.setMaxLength(32767); entries.setValue(previousEntries); addRenderableWidget(entries); entries.visible=advanced; entries.active=advanced;
        addRenderableWidget(Button.builder(Component.literal("Use slot item"),b -> {
            if(!slot.getItem().isEmpty()) { updating=true; entries.setValue(BuiltInRegistries.ITEM.getKey(slot.getItem().getItem()).toString()); mode=Rule.Mode.WHITELIST; updating=false; apply(); rebuildWidgets(); }
        }).bounds(x,y+134,155,20).build());
        addRenderableWidget(Button.builder(Component.literal("Find items / blocks"),b -> browse(LookupCatalog.Kind.ITEMS)).bounds(x,y+110,145,20).build());
        addRenderableWidget(Button.builder(Component.literal("Find tags / groups"),b -> browse(LookupCatalog.Kind.TAGS)).bounds(x+155,y+110,145,20).build());
        limit = new EditBox(font,x+200,y+134,100,20,Component.literal("Stack limit")); limit.setValue(previousLimit); limit.setTooltip(Tooltip.create(Component.literal("Maximum items in this slot. 0 uses the normal stack size."))); addRenderableWidget(limit);
        pickupButton=addRenderableWidget(Button.builder(Component.literal((pickups?"[x] ":"[ ] ")+"Pickups"),b -> { pickups=!pickups; apply(); rebuildWidgets(); }).bounds(x,y+158,145,20).tooltip(Tooltip.create(Component.literal(ClientCapabilities.pickupExplanation()))).build());
        pickupButton.active=ClientCapabilities.pickupsAvailable();
        addRenderableWidget(Button.builder(Component.literal((moving?"[x] ":"[ ] ")+"Moves"),b -> { moving=!moving; apply(); rebuildWidgets(); }).bounds(x+155,y+158,145,20).build());
        addRenderableWidget(Button.builder(Component.literal("Clear"), b -> { ClientRules.edit(slot.index,Rule.OPEN); onClose(); }).bounds(x,y+206,145,18).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(x+155,y+206,145,18).build());
        addRenderableWidget(Button.builder(Component.literal("Copy rule"),b -> {
            try { ClientRules.clipboard=draft(); error="Rule copied."; }
            catch(IllegalArgumentException ex) { error="Check entries and stack limit first."; }
        }).bounds(x,y+182,145,20).build());
        addRenderableWidget(Button.builder(Component.literal("Paste rule"),b -> {
            Rule copied=ClientRules.clipboard; updating=true; mode=copied.mode(); pickups=copied.pickups(); moving=copied.moving(); entries.setValue(String.join(",",copied.entries())); limit.setValue(Integer.toString(copied.limit())); updating=false; apply(); rebuildWidgets();
        }).bounds(x+155,y+182,145,20).build());
        entries.setResponder(value -> { if(!updating) apply(); });
        limit.setResponder(value -> { if(!updating) apply(); });
        addRenderableWidget(Button.builder(Component.literal(advanced?"Hide IDs":"Edit IDs"),b -> { advanced=!advanced; rebuildWidgets(); }).bounds(x+220,y+88,80,20).build());
    }
    private List<String> tokens() { return Arrays.stream(entries.getValue().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList(); }
    @Override public boolean mouseClicked(double mx,double my,int button) {
        if(!advanced && button==0 && my>=panelY+72 && my<panelY+93 && mx>=panelX+4 && mx<panelX+294) {
            int index=((int)mx-panelX-4)/29; var values=new ArrayList<>(tokens());
            if(index<Math.min(10,values.size())) { values.remove(index); entries.setValue(String.join(",",values)); return true; }
        }
        return super.mouseClicked(mx,my,button);
    }
    private void browse(LookupCatalog.Kind kind) {
        List<String> current=Arrays.stream(entries.getValue().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        minecraft.setScreen(new LookupScreen(this,current,kind,values -> { entries.setValue(String.join(",",values)); error="Saved automatically."; }));
    }
    private void apply() {
        if(entries==null || limit==null) return;
        try {
            Rule rule = draft();
            if(rule.equals(ClientRules.rules.getOrDefault(slot.index,Rule.OPEN))) { error="Saved automatically."; return; }
            ClientRules.clipboard = rule; ClientRules.edit(slot.index, rule); error="Saved automatically.";
        } catch (IllegalArgumentException ex) { error = "Check IDs / #tags and limit (0-99)."; }
    }
    private Rule draft() {
        return new Rule(mode,Arrays.stream(entries.getValue().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList(),Integer.parseInt(limit.getValue()),pickups,moving);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        if(pickupButton!=null) {
            boolean available=ClientCapabilities.pickupsAvailable();
            if(pickupButton.active!=available) pickupButton.setTooltip(Tooltip.create(Component.literal(ClientCapabilities.pickupExplanation())));
            pickupButton.active=available;
            pickupButton.setMessage(Component.literal(available?(pickups?"[x] ":"[ ] ")+"Pickups":"Pickups unavailable"));
        }
        renderPanel(g,mx,my,delta);
        int x=panelX,y=panelY;
        g.drawString(font,"Slot rules - slot "+(slot.getContainerSlot()+1),x,y,0x404040,false);

        g.drawString(font,"Cap:",x+164,y+140,0x404040,false);
        g.drawString(font,font.plainSubstrByWidth(ClientCapabilities.error().isEmpty()?error:ClientCapabilities.error(),300),x,y+12,0x883300,false);
        if(!advanced) RuleVisuals.entries(g,font,x,y+68,tokens(),catalog);
        super.render(g,mx,my,delta);
        if(!advanced) RuleVisuals.tooltip(g,font,x,y+68,mx,my,tokens(),catalog);
    }
}
