package dev.msmr;

import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;

public final class ItemCategories {
    public static final List<String> NAMES=List.of("swords","seeds","pickaxes","axes","shovels","hoes","tools","armor","food","blocks","logs");
    private ItemCategories() {}
    private static boolean tag(ItemStack stack,String id) { return stack.is(TagKey.create(Registries.ITEM,ResourceLocation.parse(id))); }
    public static boolean matches(String name,ItemStack stack) {
        Item item=stack.getItem();
        return switch(name) {
            case "swords" -> item instanceof SwordItem || tag(stack,"minecraft:swords") || tag(stack,"c:tools/sword");
            case "seeds" -> tag(stack,"c:seeds") || tag(stack,"forge:seeds")
                || item instanceof ItemNameBlockItem seed && !stack.has(DataComponents.FOOD) && (seed.getBlock() instanceof CropBlock || seed.getBlock() instanceof StemBlock || seed.getBlock() instanceof PitcherCropBlock)
                || BuiltInRegistries.ITEM.getKey(item).getPath().endsWith("_seeds");
            case "pickaxes" -> item instanceof PickaxeItem || tag(stack,"minecraft:pickaxes");
            case "axes" -> item instanceof AxeItem || tag(stack,"minecraft:axes");
            case "shovels" -> item instanceof ShovelItem || tag(stack,"minecraft:shovels");
            case "hoes" -> item instanceof HoeItem || tag(stack,"minecraft:hoes");
            case "tools" -> item instanceof DiggerItem || item instanceof SwordItem || item instanceof ShearsItem || item instanceof FishingRodItem || item instanceof TridentItem || tag(stack,"c:tools");
            case "armor" -> item instanceof ArmorItem || tag(stack,"c:armors");
            case "food" -> stack.has(DataComponents.FOOD);
            case "blocks" -> item instanceof BlockItem;
            case "logs" -> tag(stack,"minecraft:logs");
            default -> false;
        };
    }
}
