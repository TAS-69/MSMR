package dev.msmr.client;

import dev.msmr.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Accessible multi-select lookup shared by whole-inventory and slot editors. */
public final class LookupScreen extends InventoryPanelScreen {
    private final Consumer<List<String>> accept;
    private final LinkedHashSet<String> chosen;
    private final List<LookupCatalog.Entry> catalog;
    private final List<Row> rows=new ArrayList<>();
    private List<LookupCatalog.Entry> matches=List.of();
    private LookupCatalog.Kind tab;
    private EditBox search;
    private Button previous,next,heldButton;
    private int x,y,w,h,page,rowCount;
    private String query="",notice="";
    private boolean heldOnly;
    public LookupScreen(Screen parent,List<String> selected,LookupCatalog.Kind initial,Consumer<List<String>> accept) {
        super(Component.literal("Choose filter entries"),parent,314); this.accept=accept; this.chosen=new LinkedHashSet<>(selected); tab=initial;
        List<LookupCatalog.Entry> all=new ArrayList<>(LookupCatalog.build()); Set<String> known=new HashSet<>(); all.forEach(e -> known.add(e.token()));
        for(String token:chosen) if(!known.contains(token)) all.add(new LookupCatalog.Entry(token,token,LookupCatalog.Kind.CHOSEN,List.of(),Set.of(),"Unavailable in this world"));
        catalog=List.copyOf(all);
    }
    @Override protected void init() {
        w=Math.min(400,width-24); h=Math.min(314,height-24); panelWidth=w+16; panelHeight=h; positionPanel(); x=panelX; y=panelY; rowCount=Math.max(1,(h-140)/28);
        String[] names={"Items","Blocks","Tags","Block tags","Groups","Chosen"};
        String[] tips={"Individual items","Blocks that can be carried","Item tags supplied by this world/server","Block tags, matching their inventory items","Built-in groups, such as all swords or seeds","Review and remove selected entries"};
        for(int i=0;i<names.length;i++) {
            LookupCatalog.Kind kind=LookupCatalog.Kind.values()[i]; int index=i;
            addRenderableWidget(Button.builder(Component.literal(names[i]),b -> { tab=kind; heldOnly=false; notice=""; page=0; refreshRows(); }).bounds(x+i*w/6,y+28,w/6-2,20).tooltip(Tooltip.create(Component.literal(tips[index]))).build());
        }
        search=new EditBox(font,x,y+52,w-83,20,Component.literal("Search names, IDs or namespaces")); search.setMaxLength(128); search.setHint(Component.literal("Search names or IDs...")); search.setValue(query); addRenderableWidget(search);
        heldButton=addRenderableWidget(Button.builder(Component.literal("Held tags"),b -> {
            if(minecraft.player==null || minecraft.player.getMainHandItem().isEmpty()) { notice="Hold an item first."; return; }
            heldOnly=!heldOnly; if(tab!=LookupCatalog.Kind.BLOCK_TAGS) tab=LookupCatalog.Kind.TAGS; query=""; search.setValue(""); page=0; refreshRows();
        }).bounds(x+w-79,y+52,79,20).tooltip(Tooltip.create(Component.literal("Show tags containing the item in your selected hotbar slot; click again to show all"))).build());
        previous=addRenderableWidget(Button.builder(Component.literal("Previous"),b -> { page--; refreshRows(); }).bounds(x,y+h-44,82,18).build());
        next=addRenderableWidget(Button.builder(Component.literal("Next"),b -> { page++; refreshRows(); }).bounds(x+w-82,y+h-44,82,18).build());
        addRenderableWidget(Button.builder(Component.literal("Clear selection"),b -> { chosen.clear(); accept.accept(List.of()); refreshRows(); }).bounds(x,y+h-22,w/2-4,20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"),b -> { accept.accept(List.copyOf(chosen)); onClose(); }).bounds(x+w/2+4,y+h-22,w/2-4,20).build());
        search.setResponder(value -> { query=value; notice=""; page=0; refreshRows(); });
        refreshRows(); setInitialFocus(search);
    }
    private void refreshRows() {
        boolean refocus=rows.contains(getFocused());
        for(Row row:rows) removeWidget(row); rows.clear();
        String held=minecraft.player==null?"":BuiltInRegistries.ITEM.getKey(minecraft.player.getMainHandItem().getItem()).toString();
        record Ranked(LookupCatalog.Entry entry,int score) {}
        matches=catalog.stream().filter(e -> LookupCatalog.inTab(e,tab,chosen)).filter(e -> !heldOnly || e.memberIds().contains(held))
            .map(e -> new Ranked(e,LookupSearch.score(query,e.name(),e.token(),e.related())))
            .filter(r -> r.score()>=0).sorted(Comparator.comparingInt(Ranked::score).thenComparing(r -> r.entry().name(),String.CASE_INSENSITIVE_ORDER).thenComparing(r -> r.entry().token())).map(Ranked::entry).toList();
        int pages=Math.max(1,(matches.size()+rowCount-1)/rowCount); page=Math.max(0,Math.min(page,pages-1));
        for(int i=0;i<rowCount && page*rowCount+i<matches.size();i++) {
            Row row=new Row(x,y+88+i*28,w,matches.get(page*rowCount+i)); rows.add(row); addRenderableWidget(row);
        }
        if(previous!=null) { previous.active=page>0; next.active=page+1<pages; }
        if(heldButton!=null) heldButton.setMessage(Component.literal(heldOnly?"All tags":"Held tags"));
        if(refocus) setFocused(rows.isEmpty()?search:rows.getFirst());
    }
    private void toggle(LookupCatalog.Entry entry) {
        if(chosen.contains(entry.token())) chosen.remove(entry.token());
        else if(chosen.size()>=64) { notice="Maximum 64 entries; remove one in Chosen."; return; }
        else chosen.add(entry.token());
        accept.accept(List.copyOf(chosen));
        notice=chosen.contains(entry.token())?"Added "+entry.name():"Removed "+entry.name();
        for(Row row:rows) row.setMessage(Component.literal((chosen.contains(row.entry.token())?"Selected: ":"Not selected: ")+row.entry.name()+", "+row.entry.token()+", "+row.entry.size()+" matching items. Press to toggle."));
        if(tab==LookupCatalog.Kind.CHOSEN) refreshRows();
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if((key==257 || key==335) && search.isFocused() && !matches.isEmpty()) { toggle(matches.get(page*rowCount)); return true; }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        if(vertical!=0) { page+=vertical>0?-1:1; refreshRows(); return true; } return super.mouseScrolled(mx,my,horizontal,vertical);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        renderPanel(g,mx,my,delta);
        g.blit(ModIcon.texture(),x,y,0,0,24,24,24,24); g.drawString(font,title,x+30,y+3,0x404040,false);
        g.drawString(font,"Click to add/remove. Tab + Enter also works.",x+30,y+15,0x505050,false);
        String info=tab.name().replace('_',' ')+" | "+matches.size()+" matches | "+chosen.size()+" selected"+(heldOnly?" | held item":"");
        if(!notice.isEmpty()) info=notice+" | "+chosen.size()+" selected";
        g.drawString(font,font.plainSubstrByWidth(info,w),x,y+77,0x404040,false);
        if(matches.isEmpty()) {
            center(g,"No matches. Try fewer words or another tab.",x+w/2,y+102,0x303030);
            center(g,"Tags are supplied by the current world/server.",x+w/2,y+115,0x303030);
        }
        center(g,(page+1)+" / "+Math.max(1,(matches.size()+rowCount-1)/rowCount),x+w/2,y+h-40,0x404040);
        super.render(g,mx,my,delta);
    }
    private void center(GuiGraphics g,String text,int x,int y,int color) { g.drawString(font,text,x-font.width(text)/2,y,color,false); }
    private final class Row extends AbstractButton {
        private final LookupCatalog.Entry entry;
        private Row(int x,int y,int width,LookupCatalog.Entry entry) {
            super(x,y,width,26,Component.literal((chosen.contains(entry.token())?"Selected: ":"Not selected: ")+entry.name()+", "+entry.token()+", "+entry.size()+" matching items. Press to toggle selection.")); this.entry=entry;
            String members=entry.members().stream().limit(8).map(s -> s.getHoverName().getString()).collect(java.util.stream.Collectors.joining(", "));
            setTooltip(Tooltip.create(Component.literal(entry.token()+"\n"+entry.size()+" matching inventory items\n"+members+(entry.size()>8?", …":""))));
        }
        @Override public void onPress() { toggle(entry); }
        @Override protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) { defaultButtonNarrationText(output); }
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta) {
            boolean selected=chosen.contains(entry.token()); int background=isHoveredOrFocused()?0xFF36465B:0xFF202A38;
            g.fill(getX(),getY(),getX()+getWidth(),getY()+getHeight(),background);
            g.fill(getX(),getY(),getX()+3,getY()+getHeight(),selected?0xFF55DD77:0xFF667788);
            if(!entry.icon().isEmpty()) g.renderItem(entry.icon(),getX()+7,getY()+5);
            g.drawString(font,font.plainSubstrByWidth(entry.name(),getWidth()-100),getX()+29,getY()+3,0xFFFFFF,false);
            String detail=entry.token()+(entry.kind()==LookupCatalog.Kind.ITEMS?"":" • "+entry.size()+" items");
            g.drawString(font,font.plainSubstrByWidth(detail,getWidth()-46),getX()+29,getY()+15,0xAABBCC,false);
            g.drawString(font,selected?"Added":"+ Add",getX()+getWidth()-40,getY()+3,selected?0x77FF99:0xFFFFFF);
        }
    }
}
