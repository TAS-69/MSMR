package dev.msmr.client;

import dev.msmr.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

/** The everyday pickup controls; other inventory tools live in Advanced. */
public final class PickupScreen extends InventoryPanelScreen {
    private List<LookupCatalog.Entry> catalog=List.of();
    private int page;
    private String notice="";
    private Button enabled;
    private final boolean chest;
    public PickupScreen(Screen parent) { this(parent,false); }
    public PickupScreen(Screen parent,boolean chest) { super(Component.literal(chest?"Chest filters":"Pickup filters"),parent,218); this.chest=chest; }
    private Rule rule() { return chest?ClientRules.chestFilter():ClientRules.inventoryFilter(); }
    private boolean enabled() { return chest?rule().moving():rule().pickups(); }
    // Fresh chest selections allow only chosen items; fresh pickup selections block them.
    private Rule.Mode effectiveMode() { return rule().mode(); }
    private Rule.Mode selectionMode() { return rule().equals(Rule.OPEN)?chest?Rule.Mode.WHITELIST:Rule.Mode.BLACKLIST:rule().mode(); }
    private void save(Rule.Mode mode,List<String> entries,boolean pickups) {
        Rule previous=rule();
        notice=chest?ClientRules.setChestFilter(new Rule(mode,entries,previous.limit(),false,pickups)):ClientRules.setInventoryFilter(new Rule(mode,entries,previous.limit(),pickups,previous.equals(Rule.OPEN)?false:previous.moving()));
        if(notice.equals("Inventory filter applied.")) notice="Saved automatically.";
    }
    @Override protected void init() {
        ClientRules.refresh(); if(catalog.isEmpty()) catalog=LookupCatalog.build(); positionPanel(); int x=panelX,y=panelY;
        enabled=addRenderableWidget(Button.builder(Component.empty(),b -> {
            save(effectiveMode(),rule().entries(),!enabled()); rebuildWidgets();
        }).bounds(x+224,y,76,20).build());
        Rule.Mode[] modes={Rule.Mode.BLACKLIST,Rule.Mode.WHITELIST,Rule.Mode.ALL,Rule.Mode.BLOCK};
        String[] labels={"Block","Only","All","None"};
        String[] tips={"Block selected drops; pick up everything else.","Pick up only selected drops.","Pick up all drops; keep your selection for later.","Leave every drop on the ground."};
        if(chest) tips=new String[]{"Keep selected items out of this chest.","Accept only selected items in this chest.","Accept all items; keep the selection for later.","Reject all insertions into this chest."};
        for(int i=0;i<4;i++) {
            final Rule.Mode mode=modes[i]; final String label=labels[i]; final int color=new int[]{0xFFB46A23,0xFF34763F,0xFF306BA0,0xFFA33737}[i];
            Button button=new Button(x+i*76,y+32,72,30,Component.literal(label),b -> { save(mode,rule().entries(),enabled()); rebuildWidgets(); },supplier -> supplier.get()) {
                @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta) {
                    int bx=getX(),by=getY(); boolean chosen=effectiveMode()==mode;
                    g.fill(bx,by,bx+72,by+30,isHoveredOrFocused()?0xFFFFFFFF:chosen?color:0xFF777777);
                    g.fill(bx+2,by+2,bx+70,by+28,chosen?0xFFF0EEDC:0xFFD8D8D8);
                    // Small symbols drawn from pixels stay readable with every font/resource pack.
                    g.fill(bx+8,by+10,bx+18,by+20,color);
                    if(mode==Rule.Mode.BLACKLIST) g.fill(bx+10,by+14,bx+16,by+16,0xFFFFFFFF);
                    else if(mode==Rule.Mode.BLOCK) for(int d=0;d<6;d++) { g.fill(bx+10+d,by+12+d,bx+11+d,by+13+d,0xFFFFFFFF); g.fill(bx+15-d,by+12+d,bx+16-d,by+13+d,0xFFFFFFFF); }
                    else if(mode==Rule.Mode.WHITELIST) { g.fill(bx+10,by+15,bx+12,by+17,0xFFFFFFFF); g.fill(bx+12,by+16,bx+14,by+18,0xFFFFFFFF); g.fill(bx+14,by+12,bx+16,by+16,0xFFFFFFFF); }
                    else { g.fill(bx+10,by+14,bx+16,by+16,0xFFFFFFFF); g.fill(bx+12,by+12,bx+14,by+18,0xFFFFFFFF); }
                    g.drawString(font,label,bx+24,by+11,0x303030,false);
                    if(chosen) g.fill(bx+3,by+25,bx+69,by+28,color);
                }
            };
            button.setTooltip(Tooltip.create(Component.literal(tips[i]))); addRenderableWidget(button);
        }
        List<String> entries=rule().entries(); page=Math.clamp(page,0,Math.max(0,(entries.size()-1)/24));
        for(int i=0;i<24 && page*24+i<entries.size();i++) {
            String token=entries.get(page*24+i);
            LookupCatalog.Entry entry=catalog.stream().filter(e -> e.token().equals(token)).findFirst().orElseGet(() -> new LookupCatalog.Entry(token,token,LookupCatalog.Kind.CHOSEN,List.of(),Set.of(),""));
            addRenderableWidget(new FilterItemButton(x+(i%12)*25,y+80+(i/12)*25,entry,() -> true,b -> {
                var values=new ArrayList<>(rule().entries()); values.remove(token); save(effectiveMode(),values,enabled()); rebuildWidgets();
            },"Click to remove"));
        }
        if(entries.size()>24) {
            Button prev=addRenderableWidget(Button.builder(Component.literal("<"),b -> { page--; rebuildWidgets(); }).bounds(x,y+132,24,16).build()); prev.active=page>0;
            Button next=addRenderableWidget(Button.builder(Component.literal(">"),b -> { page++; rebuildWidgets(); }).bounds(x+276,y+132,24,16).build()); next.active=(page+1)*24<entries.size();
        }
        addRenderableWidget(Button.builder(Component.literal("+ Add items"),b -> minecraft.setScreen(new ItemPickerScreen(this,rule().entries(),values -> save(selectionMode(),values,enabled())))).bounds(x,y+152,145,20).build());
        addRenderableWidget(Button.builder(Component.literal("+ Held item"),b -> {
            var held=minecraft.player.getMainHandItem();
            if(held.isEmpty()) { notice="Hold an item in your hotbar first."; return; }
            var values=new LinkedHashSet<>(rule().entries()); String token=BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
            if(values.size()>=64 && !values.contains(token)) { notice="64 entries: remove an item first."; return; }
            values.add(token); save(selectionMode(),List.copyOf(values),enabled()); rebuildWidgets();
        }).bounds(x+155,y+152,145,20).tooltip(Tooltip.create(Component.literal("Add the item in your selected hotbar slot"))).build());
        addRenderableWidget(Button.builder(Component.literal("Advanced..."),b -> minecraft.setScreen(new AdvancedScreen(this))).bounds(x,y+198,145,20).build());
        addRenderableWidget(Button.builder(Component.literal("Back to inventory"),b -> onClose()).bounds(x+155,y+198,145,20).build());
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        boolean available=chest?ClientRules.hasChest():ClientCapabilities.pickupsAvailable(); enabled.active=available;
        enabled.setMessage(Component.literal(!available?"Unavailable":enabled()?"ON":"Paused"));
        enabled.setTooltip(Tooltip.create(Component.literal(chest?"Toggle this chest's whole-container filter. Individual slot rules remain separate.":ClientCapabilities.pickupExplanation()+"\nControls this inventory-wide pickup filter. Slot rules have separate switches; movement controls are in Advanced.")));
        renderPanel(g,mx,my,delta); int x=panelX,y=panelY;
        g.blit(ModIcon.texture(),x,y,0,0,14,14,14,14); g.drawString(font,title,x+19,y+2,0x303030,false);
        g.drawString(font,chest?"Choose what goes into this chest.":"Choose what you pick up.",x,y+19,0x404040,false);
        String help=switch(effectiveMode()) { case BLACKLIST -> "Leave selected drops on the ground."; case WHITELIST -> "Pick up only the selected drops."; case ALL -> "Pick up everything. Selection is ignored."; case BLOCK -> "Leave every drop on the ground."; };
        if(chest) help=ClientRules.mixedChestFilters()?"Different half filters; edits apply to both.":switch(effectiveMode()) { case BLACKLIST -> "Keep selected items out of this chest."; case WHITELIST -> "Accept only the selected items."; case ALL -> "Accept every item. Selection is ignored."; case BLOCK -> "Reject every item inserted into this chest."; };
        g.drawString(font,help,x,y+67,0x303030,false);
        if(rule().entries().isEmpty()) {
            g.drawString(font,"No items selected",x+8,y+91,0x404040,false);
            g.drawString(font,chest?"Use + Add items to choose what belongs.":"Use + Add items to choose your drops.",x+8,y+106,0x505050,false);
        }
        if(!rule().entries().isEmpty()) g.drawString(font,rule().entries().size()+" selected | "+(page+1)+"/"+Math.max(1,(rule().entries().size()+23)/24)+" | Click to remove",x+28,y+136,0x404040,false);
        String status=chest?ClientRules.chestDescription():!available?"Server support needed - hover Unavailable.":!rule().pickups()?"This pickup filter is paused.":!ClientCapabilities.error().isEmpty()?ClientCapabilities.error():ClientCapabilities.available() && !ClientCapabilities.synced()?"Syncing pickup filters with the server...":notice.isEmpty()?"Changes save automatically.":notice;
        g.drawString(font,font.plainSubstrByWidth(status,300),x,y+180,available?0x404040:0x803B18,false);
        super.render(g,mx,my,delta);
    }
}
