package dev.msmr.client;

import dev.msmr.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

public final class InventoryFilterScreen extends InventoryPanelScreen {
    private Rule.Mode mode;
    private Button pickupButton;
    private EditBox entries,name;
    private String selected="",status="";
    private final boolean profiles;
    private final boolean chest;
    private boolean advanced, pickups=true, moving=true, updating;
    private List<LookupCatalog.Entry> catalog=List.of();
    public InventoryFilterScreen(Screen parent) { this(parent,false); }
    public InventoryFilterScreen(Screen parent,boolean profiles) { super(Component.literal(profiles?"Inventory profiles":ClientRules.hasChest()?"Whole chest":"Whole inventory"),parent,218); this.profiles=profiles; this.chest=!profiles && ClientRules.hasChest(); }
    private Rule currentRule() { return chest?ClientRules.chestFilter():ClientRules.inventoryFilter(); }
    private String setRule(Rule rule) { return chest?ClientRules.setChestFilter(rule):ClientRules.setInventoryFilter(rule); }
    @Override protected void init() {
        ClientRules.refresh(); Rule rule=currentRule(); if(entries==null) { mode=rule.mode(); pickups=rule.pickups(); moving=rule.moving(); }
        String previousEntries=entries==null?String.join(",",rule.entries()):entries.getValue();
        String previousName=name==null?null:name.getValue();
        List<String> names=ClientRules.profileNames(); if(!names.contains(selected)) selected=names.isEmpty()?"":names.getFirst();
        positionPanel(); int x=panelX,y=panelY;
        if(!profiles) {
        if(catalog.isEmpty()) catalog=LookupCatalog.build();
        Rule.Mode[] choices={Rule.Mode.BLACKLIST,Rule.Mode.WHITELIST,Rule.Mode.ALL,Rule.Mode.BLOCK};
        for(int i=0;i<choices.length;i++) {
            Rule.Mode choice=choices[i];
            addRenderableWidget(RuleVisuals.choice(x+(i%2)*155,y+24+(i/2)*22,145,choice,mode,b -> {
                mode=choice; apply(); rebuildWidgets();
            }));
        }
        entries=new EditBox(font,x,y+100,300,20,Component.literal("Advanced filter entries")); entries.setMaxLength(32767); entries.setValue(previousEntries); addRenderableWidget(entries); entries.visible=advanced; entries.active=advanced; entries.setResponder(value -> { if(!updating) apply(); });
        addRenderableWidget(Button.builder(Component.literal("Find items / blocks"),b -> browse(LookupCatalog.Kind.ITEMS)).bounds(x,y+140,145,20).build());
        addRenderableWidget(Button.builder(Component.literal("Find tags / groups"),b -> browse(LookupCatalog.Kind.TAGS)).bounds(x+155,y+140,145,20).build());
        addRenderableWidget(Button.builder(Component.literal("Add held item"),b -> {
            var held=minecraft.player.getMainHandItem();
            if(!held.isEmpty()) append(BuiltInRegistries.ITEM.getKey(held.getItem()).toString()); else status="Hold an item in your selected hotbar slot.";
        }).bounds(x,y+164,145,20).build());

        addRenderableWidget(Button.builder(Component.literal("Reset"),b -> {
            mode=Rule.Mode.ALL; pickups=true; moving=true; entries.setValue(""); status=setRule(Rule.OPEN); rebuildWidgets();
        }).bounds(x+155,y+164,145,20).tooltip(Tooltip.create(Component.literal(chest?"Reset the whole-chest rule; individual slot rules remain.":"Reset the whole-inventory rule; individual slot rules remain."))).build());
        pickupButton=addRenderableWidget(Button.builder(Component.literal((pickups?"[x] ":"[ ] ")+"Pickups"),b -> { pickups=!pickups; apply(); rebuildWidgets(); }).bounds(x,y+74,145,18).tooltip(Tooltip.create(Component.literal(ClientCapabilities.pickupExplanation()))).build());
        pickupButton.active=ClientCapabilities.pickupsAvailable();
        addRenderableWidget(Button.builder(Component.literal((moving?"[x] ":"[ ] ")+"Inventory moves"),b -> { moving=!moving; apply(); rebuildWidgets(); }).bounds(x+155,y+74,145,18).build());
        addRenderableWidget(Button.builder(Component.literal(advanced?"Hide IDs":"Edit IDs"),b -> { advanced=!advanced; rebuildWidgets(); }).bounds(x+220,y+120,80,18).build());
        }
        if(profiles) {
        entries=new EditBox(font,0,0,1,1,Component.literal("Applied entries")); entries.setMaxLength(32767); entries.setValue(previousEntries);
        name=new EditBox(font,x,y+80,300,20,Component.literal("Profile name / copy name")); name.setMaxLength(32); name.setValue(previousName!=null?previousName:selected.isEmpty()?"Mining":selected); addRenderableWidget(name);
        addRenderableWidget(Button.builder(Component.literal(selected.isEmpty()?"No saved profiles":"Selected: "+selected),b -> {
            var list=ClientRules.profileNames(); if(list.isEmpty()) return;
            selected=list.get((list.indexOf(selected)+1)%list.size()); name.setValue(selected); b.setMessage(Component.literal("Selected: "+selected));
        }).bounds(x,y+36,300,20).tooltip(Tooltip.create(Component.literal("Click to cycle saved profiles; Switch activates the selection"))).build());
        String[] actions={"save","switch","copy","delete"}; String[] labels={"Save","Switch","Copy","Delete"};
        for(int i=0;i<actions.length;i++) {
            String action=actions[i];
            Button button=addRenderableWidget(Button.builder(Component.literal(labels[i]),b -> profile(action)).bounds(x+i*77,y+110,69,20).build());
            button.active=action.equals("save") || !selected.isEmpty();
        }
        }
        addRenderableWidget(Button.builder(Component.literal(parent instanceof AdvancedScreen?"Back":"Back to inventory"),b -> onClose()).bounds(x,y+198,300,20).build());
    }
    private List<String> tokens() { return Arrays.stream(entries.getValue().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList(); }
    @Override public boolean mouseClicked(double mx,double my,int button) {
        if(!profiles && !advanced && button==0 && my>=panelY+100 && my<panelY+121 && mx>=panelX+4 && mx<panelX+294) {
            int index=((int)mx-panelX-4)/29; var values=new ArrayList<>(tokens());
            if(index<Math.min(10,values.size())) { values.remove(index); entries.setValue(String.join(",",values)); status="Saved automatically."; return true; }
        }
        return super.mouseClicked(mx,my,button);
    }
    private void append(String value) {
        var values=new LinkedHashSet<>(Arrays.stream(entries.getValue().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList()); values.add(value); entries.setValue(String.join(",",values));
    }
    private void browse(LookupCatalog.Kind kind) {
        List<String> current=Arrays.stream(entries.getValue().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        minecraft.setScreen(new LookupScreen(this,current,kind,values -> { entries.setValue(String.join(",",values)); status="Saved automatically."; }));
    }
    private Rule draft() { return new Rule(mode,Arrays.stream(entries.getValue().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList(),0,pickups,moving); }
    private void apply() {
        if(profiles || entries==null) return;
        try { Rule value=draft(); status=value.equals(currentRule())?"Saved automatically.":setRule(value); }
        catch(IllegalArgumentException ex) { status="Invalid ID: last valid filter stays active."; }
    }
    private void profile(String action) {
        String target=name.getValue().trim();
        try {
            if(action.equals("save")) { ProfileBook.validate(target); }
            status=ClientRules.manageProfile(action,selected,target);
            if(ClientRules.profileNames().contains(target) && (status.equals("Profile saved.") || status.equals("Profile copied."))) selected=target;
            if(action.equals("switch") && status.equals("Profile switched.")) {
                Rule loaded=ClientRules.inventoryFilter(); updating=true; mode=loaded.mode(); pickups=loaded.pickups(); moving=loaded.moving(); entries.setValue(String.join(",",loaded.entries())); updating=false; name.setValue(selected);
            }
            rebuildWidgets();
        } catch(IllegalArgumentException ex) { status=ex.getMessage(); }
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        if(pickupButton!=null) {
            boolean available=!chest && ClientCapabilities.pickupsAvailable();
            if(pickupButton.active!=available) pickupButton.setTooltip(Tooltip.create(Component.literal(ClientCapabilities.pickupExplanation())));
            pickupButton.active=available;
            pickupButton.setMessage(Component.literal(chest?"Chest storage":available?(pickups?"[x] ":"[ ] ")+"Pickups":"Pickups unavailable"));
            if(chest) pickupButton.setTooltip(Tooltip.create(Component.literal(ClientRules.chestDescription()+" Ground pickups use your player rules.")));
        }
        renderPanel(g,mx,my,delta); int x=panelX,y=panelY;
        g.blit(ModIcon.texture(),x,y,0,0,12,12,12,12); g.drawString(font,title,x+17,y+2,0x404040,false);
        if(profiles) {
            g.drawString(font,"Active: "+ClientRules.activeProfile(),x,y+16,0x505050,false);
            g.drawString(font,"Profile name / new name for Copy:",x,y+66,0x404040,false);
            g.drawString(font,"Save includes applied inventory + slot rules.",x,y+142,0x505050,false);
        } else {
            Rule applied=currentRule();
            g.drawString(font,"Active: "+RuleVisuals.label(applied.mode()),x+17,y+14,0x303030,false);

            if(!advanced) RuleVisuals.entries(g,font,x,y+96,tokens(),catalog);
            if(mode==Rule.Mode.ALL && !tokens().isEmpty()) status="Allow everything ignores selected items.";
        }
        g.drawString(font,font.plainSubstrByWidth(chest || ClientCapabilities.error().isEmpty()?status:ClientCapabilities.error(),300),x,y+187,0x883300,false);
        super.render(g,mx,my,delta);
        if(!profiles && !advanced) RuleVisuals.tooltip(g,font,x,y+96,mx,my,tokens(),catalog);
    }
}
