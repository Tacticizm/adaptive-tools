package com.tacticizm.adaptivetools;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Writes item models and the item tags this slice needs. Run {@code ./gradlew runData} to refresh
 * {@code src/generated/resources}.
 */
public final class AdaptiveToolsData {
    private AdaptiveToolsData() {}

    public static void gatherClient(GatherDataEvent.Client event) {
        addProvider(event);
    }

    public static void gatherServer(GatherDataEvent.Server event) {
        addProvider(event);
    }

    private static void addProvider(GatherDataEvent event) {
        event.getGenerator().addProvider(true, new Provider(event.getGenerator().getPackOutput()));
    }

    private static final class Provider implements DataProvider {
        private final PackOutput output;

        private Provider(PackOutput output) {
            this.output = output;
        }

        @Override
        public CompletableFuture<?> run(CachedOutput cache) {
            List<CompletableFuture<?>> writes = new ArrayList<>();
            for (ToolTier tier : ToolTier.values()) {
                writes.add(itemModel(cache, tier.ingotId(), "minecraft:item/generated"));
                writes.add(itemModel(cache, tier.pickaxeId(), "minecraft:item/handheld"));
            }
            for (String id : List.of(
                    "power_module",
                    "item_module",
                    "fluid_module",
                    "cross_dimension_module",
                    "capacity_module",
                    "rate_module")) {
                writes.add(itemModel(cache, id, "minecraft:item/generated"));
            }
            writes.add(itemModel(cache, "wrench", "minecraft:item/handheld"));

            writes.add(tag(cache, "minecraft", "pickaxes", List.of(
                    ModItems.TEMPERED_COPPER_PICKAXE,
                    ModItems.RESONANT_PICKAXE,
                    ModItems.EMBERSTEEL_PICKAXE,
                    ModItems.FLUX_PICKAXE,
                    ModItems.ENDER_PICKAXE)));
            for (ToolTier tier : ToolTier.values()) {
                writes.add(tag(cache, AdaptiveTools.MOD_ID, tier.id() + "_ingots", List.of(ModItems.ingotFor(tier))));
            }
            return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
        }

        private CompletableFuture<?> itemModel(CachedOutput cache, String id, String parent) {
            JsonObject model = new JsonObject();
            model.addProperty("parent", parent);
            JsonObject textures = new JsonObject();
            textures.addProperty("layer0", AdaptiveTools.MOD_ID + ":item/" + id);
            model.add("textures", textures);

            JsonObject definition = new JsonObject();
            JsonObject modelRef = new JsonObject();
            modelRef.addProperty("type", "minecraft:model");
            modelRef.addProperty("model", AdaptiveTools.MOD_ID + ":item/" + id);
            definition.add("model", modelRef);

            var root = output.getOutputFolder();
            return CompletableFuture.allOf(
                    DataProvider.saveStable(cache, model, root.resolve("assets/" + AdaptiveTools.MOD_ID + "/models/item/" + id + ".json")),
                    DataProvider.saveStable(cache, definition, root.resolve("assets/" + AdaptiveTools.MOD_ID + "/items/" + id + ".json")));
        }

        private CompletableFuture<?> tag(CachedOutput cache, String namespace, String path, List<? extends net.neoforged.neoforge.registries.DeferredHolder<Item, ? extends Item>> items) {
            JsonObject tag = new JsonObject();
            JsonArray values = new JsonArray();
            for (var item : items) {
                values.add(item.getId().toString());
            }
            tag.add("values", values);
            return DataProvider.saveStable(cache, tag, output.getOutputFolder().resolve("data/" + namespace + "/tags/item/" + path + ".json"));
        }

        @Override
        public String getName() {
            return "Adaptive Tools data";
        }
    }
}
