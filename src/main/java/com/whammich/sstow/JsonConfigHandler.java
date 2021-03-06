package com.whammich.sstow;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.whammich.repack.tehnut.lib.util.BlockStack;
import com.whammich.sstow.item.ItemSoulShard;
import com.whammich.sstow.util.PosWithStack;
import com.whammich.sstow.util.TierHandler;
import com.whammich.sstow.util.serialization.SerializerBlockPos;
import com.whammich.sstow.util.serialization.SerializerBlockStack;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonConfigHandler {

    public static Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .disableHtmlEscaping()
            .registerTypeAdapter(BlockStack.class, new SerializerBlockStack())
            .registerTypeAdapter(BlockPos.class, new SerializerBlockPos())
            .create();

    public static void initShard(File jsonConfig) {
        try {
            if (!jsonConfig.exists() && jsonConfig.createNewFile()) {
                Map<Integer, TierHandler.Tier> defaultMap = handleShardDefaults();
                String json = gson.toJson(defaultMap, new TypeToken<Map<Integer, TierHandler.Tier>>() {
                }.getType());
                FileWriter writer = new FileWriter(jsonConfig);
                writer.write(json);
                writer.close();
            }

            TierHandler.tiers = gson.fromJson(new FileReader(jsonConfig), new TypeToken<Map<Integer, TierHandler.Tier>>() {
            }.getType());
        } catch (IOException e) {
            SoulShardsTOW.instance.getLogHelper().severe("Failed to create a default Tier configuration file.");
        }
    }

    public static void initMultiblock(File jsonConfig) {
        try {
            if (!jsonConfig.exists() && jsonConfig.createNewFile()) {
                List<PosWithStack> defaultList = handleMultiblockDefaults();
                String json = gson.toJson(defaultList, new TypeToken<ArrayList<PosWithStack>>() {
                }.getType());
                FileWriter writer = new FileWriter(jsonConfig);
                writer.write(json);
                writer.close();
            }

            ItemSoulShard.multiblock = gson.fromJson(new FileReader(jsonConfig), new TypeToken<ArrayList<PosWithStack>>() {
            }.getType());
            for (PosWithStack posWithStack : ItemSoulShard.multiblock)
                if (posWithStack.getPos().equals(new BlockPos(0, 0, 0)))
                    ItemSoulShard.originBlock = posWithStack.getBlock();
        } catch (IOException e) {
            SoulShardsTOW.instance.getLogHelper().severe("Failed to create a default Multiblock configuration file.");
        }
    }

    private static Map<Integer, TierHandler.Tier> handleShardDefaults() {
        Map<Integer, TierHandler.Tier> ret = new HashMap<Integer, TierHandler.Tier>();

        ret.put(0, new TierHandler.Tier(0, 63, true, false, false, 0, 0));
        ret.put(1, new TierHandler.Tier(64, 127, true, true, false, 2, 20));
        ret.put(2, new TierHandler.Tier(128, 255, true, true, false, 4, 10));
        ret.put(3, new TierHandler.Tier(256, 511, false, true, false, 4, 5));
        ret.put(4, new TierHandler.Tier(512, 1023, false, true, false, 4, 5));
        ret.put(5, new TierHandler.Tier(1024, 1024, false, false, true, 6, 2));

        return ret;
    }

    private static ArrayList<PosWithStack> handleMultiblockDefaults() {
        ArrayList<PosWithStack> ret = new ArrayList<PosWithStack>();

        ret.add(new PosWithStack(new BlockPos(0, 0, 0), new BlockStack(Blocks.glowstone)));
        ret.add(new PosWithStack(new BlockPos(1, 0, 0), new BlockStack(Blocks.end_stone)));
        ret.add(new PosWithStack(new BlockPos(-1, 0, 0), new BlockStack(Blocks.end_stone)));
        ret.add(new PosWithStack(new BlockPos(0, 0, 1), new BlockStack(Blocks.end_stone)));
        ret.add(new PosWithStack(new BlockPos(0, 0, -1), new BlockStack(Blocks.end_stone)));
        ret.add(new PosWithStack(new BlockPos(1, 0, 1), new BlockStack(Blocks.obsidian)));
        ret.add(new PosWithStack(new BlockPos(1, 0, -1), new BlockStack(Blocks.obsidian)));
        ret.add(new PosWithStack(new BlockPos(-1, 0, 1), new BlockStack(Blocks.obsidian)));
        ret.add(new PosWithStack(new BlockPos(-1, 0, -1), new BlockStack(Blocks.obsidian)));

        return ret;
    }
}
