package dev.msmr.client;

import dev.msmr.LookupSearch;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Searchable picture grid for everyday use. Advanced lookup retains tags and IDs. */
public final class ItemPickerScreen extends InventoryPanelScreen {
    private final LinkedHashSet<String> chosen;
    private final Consumer<List<String>> accept;
    private final List<LookupCatalog.Entry> catalog;
    private final List<Button> tiles=new ArrayList<>();
    private List<LookupCatalog.Entry> matches=List.of();
    private EditBox search;
    private Button prev,next,selected;
    private int cols,capacity,page;
    private boolean selectedOnly;
    private String query="",notice="";
    private final boolean single;
    public ItemPickerScreen(Screen parent,List<String> values,Consumer<List<String>> accept) {
        this(parent,values,accept,false);
    }
    public ItemPickerScreen(Screen parent,List<String> values,Consumer<List<String>> accept,boolean single) {
        super(Component.literal("Choose items"),parent,314); chosen=new LinkedHashSet<>(values); this.accept=accept;
        this.single=single;
        var all=new ArrayList<>(LookupCatalog.build());
        for(String token:chosen) if(all.stream().noneMatch(e -> e.token().equals(token))) all.add(new LookupCatalog.Entry(token,token,LookupCatalog.Kind.CHOSEN,List.of(),Set.of(),""));
        catalog=List.copyOf(all);
    }
    @Override protected void init() {
        panelWidth=Math.min(416,width-4); panelHeight=Math.min(314,height-24); positionPanel(); int x=panelX,y=panelY,w=panelWidth-16;
        cols=w/25; capacity=cols*Math.max(1,(panelHeight-116)/25);
        search=new EditBox(font,x,y+28,w-86,20,Component.literal("Search items")); search.setMaxLength(128); search.setHint(Component.literal("Search items...")); search.setValue(query); addRenderableWidget(search);
        selected=addRenderableWidget(Button.builder(Component.literal("Selected"),b -> { selectedOnly=!selectedOnly; page=0; refresh(); }).bounds(x+w-82,y+28,82,20).tooltip(Tooltip.create(Component.literal("Show selected entries, including groups added in Advanced"))).build());
        prev=addRenderableWidget(Button.builder(Component.literal("<"),b -> { page--; refresh(); }).bounds(x,y+panelHeight-44,28,18).build());
        next=addRenderableWidget(Button.builder(Component.literal(">"),b -> { page++; refresh(); }).bounds(x+w-28,y+panelHeight-44,28,18).build());
        addRenderableWidget(Button.builder(Component.literal("Clear selection"),b -> { chosen.clear(); accept.accept(List.of()); refresh(); }).bounds(x,y+panelHeight-22,w/2-4,20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"),b -> onClose()).bounds(x+w/2+4,y+panelHeight-22,w/2-4,20).build());
        search.setResponder(value -> { query=value; page=0; notice=""; refresh(); }); refresh(); setInitialFocus(search);
    }
    private void refresh() {
        boolean refocus=tiles.contains(getFocused());
        tiles.forEach(this::removeWidget); tiles.clear();
        record Ranked(LookupCatalog.Entry entry,int score) {}
        matches=catalog.stream().filter(e -> selectedOnly?chosen.contains(e.token()):e.kind()==LookupCatalog.Kind.ITEMS)
            .map(e -> new Ranked(e,LookupSearch.score(query,e.name(),e.token(),e.related()))).filter(r -> r.score()>=0)
            .sorted(Comparator.comparingInt(Ranked::score).thenComparing(r -> r.entry().name(),String.CASE_INSENSITIVE_ORDER).thenComparing(r -> r.entry().token())).map(Ranked::entry).toList();
        int pages=Math.max(1,(matches.size()+capacity-1)/capacity); page=Math.clamp(page,0,pages-1);
        for(int i=0;i<capacity && page*capacity+i<matches.size();i++) {
            var entry=matches.get(page*capacity+i);
            Button tile=new FilterItemButton(panelX+(i%cols)*25,panelY+68+(i/cols)*25,entry,() -> chosen.contains(entry.token()),b -> toggle(entry),"Click to add or remove");
            tiles.add(tile); addRenderableWidget(tile);
        }
        if(refocus) setFocused(tiles.isEmpty()?search:tiles.getFirst());
        prev.active=page>0; next.active=page+1<pages; selected.setMessage(Component.literal(selectedOnly?"All items":"Selected"));
    }
    private void toggle(LookupCatalog.Entry entry) {
        if(single) { chosen.clear(); chosen.add(entry.token()); accept.accept(List.copyOf(chosen)); onClose(); return; }
        if(!chosen.remove(entry.token())) {
            if(chosen.size()>=64) { notice="64 entries: remove an item first."; return; }
            chosen.add(entry.token());
        }
        accept.accept(List.copyOf(chosen)); notice="Saved automatically.";
        if(selectedOnly) refresh();
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if((key==257 || key==335) && search.isFocused() && !matches.isEmpty()) { toggle(matches.get(page*capacity)); return true; }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        if(vertical!=0) { page+=vertical>0?-1:1; refresh(); return true; } return super.mouseScrolled(mx,my,horizontal,vertical);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        renderPanel(g,mx,my,delta); int x=panelX,y=panelY,w=panelWidth-16;
        g.drawString(font,title,x,y+2,0x303030,false); g.drawString(font,"Click items to select. Changes save immediately.",x,y+15,0x505050,false);
        g.drawString(font,chosen.size()+" selected | "+matches.size()+" found",x,y+54,0x404040,false);
        if(matches.isEmpty()) g.drawString(font,"No items found. Try a shorter name.",x,y+79,0x404040,false);
        String footer=(page+1)+" / "+Math.max(1,(matches.size()+capacity-1)/capacity)+(notice.isEmpty()?"":" | "+notice);
        g.drawString(font,font.plainSubstrByWidth(footer,w-70),x+36,y+panelHeight-39,0x404040,false);
        super.render(g,mx,my,delta);
    }
}
