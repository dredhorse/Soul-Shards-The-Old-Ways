package com.whammich.sstow.registry;

import com.whammich.repack.tehnut.lib.annot.ModItem;
import com.whammich.sstow.SoulShardsTOW;
import com.whammich.sstow.item.ItemMaterials;
import com.whammich.sstow.item.ItemSoulSword;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;

import java.util.HashMap;
import java.util.Map;

public class ModItems {

    private static Map<Class<? extends Item>, String> classToName = new HashMap<Class<? extends Item>, String>();

    public static void init() {
        for (ASMDataTable.ASMData data : SoulShardsTOW.instance.getModItems()) {
            try {
                Class<?> asmClass = Class.forName(data.getClassName());
                Class<? extends Item> modItemClass = asmClass.asSubclass(Item.class);
                String name = modItemClass.getAnnotation(ModItem.class).name();

                Item modItem = modItemClass.newInstance();

                GameRegistry.registerItem(modItem, name);
                classToName.put(modItemClass, name);
            } catch (Exception e) {
                SoulShardsTOW.instance.getLogHelper().error(String.format("Unable to register item for class %s", data.getClassName()));
            }
        }

        OreDictionary.registerOre(ItemMaterials.INGOT_CORRUPTED, ItemMaterials.getStack(ItemMaterials.INGOT_CORRUPTED));
        OreDictionary.registerOre(ItemMaterials.CORRUPTED_ESSENCE, ItemMaterials.getStack(ItemMaterials.CORRUPTED_ESSENCE));
        OreDictionary.registerOre(ItemMaterials.DUST_VILE, ItemMaterials.getStack(ItemMaterials.DUST_VILE));

        ItemSoulSword.MATERIAL_SOUL.setRepairItem(ItemMaterials.getStack(ItemMaterials.INGOT_CORRUPTED));
    }

    public static Item getItem(String name) {
        return GameRegistry.findItem(SoulShardsTOW.MODID, name);
    }

    public static Item getItem(Class<? extends Item> itemClass) {
        return getItem(classToName.get(itemClass));
    }

    public static String getName(Class<? extends Item> itemClass) {
        return classToName.get(itemClass);
    }
}
