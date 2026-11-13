package com.tacticizm.adaptivetools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.ItemLike;

/**
 * Every obtainable item has a recipe. Numbers and ingredients that were not specified are provisional.
 * A shaped craft makes a fresh item. Smithing is what carries settings, enchantments, and other components.
 */
public final class ModRecipes extends RecipeProvider.Runner {
    public ModRecipes(PackOutput output, java.util.concurrent.CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        return new Builder(registries, output);
    }

    @Override
    public String getName() {
        return "Adaptive Tools recipes";
    }

    private static final class Builder extends RecipeProvider {
        private final Set<Item> crafted = new HashSet<>();

        private Builder(HolderLookup.Provider registries, RecipeOutput output) {
            super(registries, output);
        }

        @Override
        protected void buildRecipes() {
            ingots();
            storage();
            templates();
            toolsAndArmor();
            modules();
            cables();
            shaped(RecipeCategory.TOOLS, ModItems.WRENCH.get())
                    .pattern("T T")
                    .pattern(" T ")
                    .pattern(" I ")
                    .define('T', ModItems.TEMPERED_COPPER_INGOT.get())
                    .define('I', Items.IRON_INGOT)
                    .unlockedBy("has_tempered_copper", has(ModItems.TEMPERED_COPPER_INGOT.get()))
                    .save(output);
            mark(ModItems.WRENCH.get());
            shaped(RecipeCategory.MISC, ModBlocks.GENERATOR_ITEM.get())
                    .pattern("TTT")
                    .pattern("TFT")
                    .pattern("RRR")
                    .define('T', ModItems.TEMPERED_COPPER_INGOT.get())
                    .define('F', Items.FURNACE)
                    .define('R', Items.REDSTONE)
                    .unlockedBy("has_tempered_copper", has(ModItems.TEMPERED_COPPER_INGOT.get()))
                    .save(output);
            mark(ModBlocks.GENERATOR_ITEM.get());
            shapeless(RecipeCategory.MISC, ModBlocks.TRANSMITTER_ITEM.get())
                    .requires(ModItems.EMBERSTEEL_INGOT.get(), 2)
                    .requires(Items.REDSTONE_BLOCK)
                    .requires(Items.ENDER_PEARL)
                    .unlockedBy("has_embersteel", has(ModItems.EMBERSTEEL_INGOT.get()))
                    .save(output);
            mark(ModBlocks.TRANSMITTER_ITEM.get());
            shapeless(RecipeCategory.MISC, ModBlocks.RECEIVER_ITEM.get())
                    .requires(ModItems.EMBERSTEEL_INGOT.get())
                    .requires(Items.REDSTONE)
                    .requires(Items.COPPER_INGOT)
                    .unlockedBy("has_embersteel", has(ModItems.EMBERSTEEL_INGOT.get()))
                    .save(output);
            mark(ModBlocks.RECEIVER_ITEM.get());
            shapeless(RecipeCategory.MISC, ModItems.CARRIED_RECEIVER.get())
                    .requires(ModItems.EMBERSTEEL_INGOT.get())
                    .requires(Items.ENDER_PEARL)
                    .requires(Items.DIAMOND)
                    .unlockedBy("has_embersteel", has(ModItems.EMBERSTEEL_INGOT.get()))
                    .save(output);
            mark(ModItems.CARRIED_RECEIVER.get());
            requireEveryItem();
        }

        private void ingots() {
            // 3 copper + 1 redstone. Cheap first alloy, vanilla only.
            shapeless(RecipeCategory.MISC, ModItems.TEMPERED_COPPER_INGOT.get())
                    .requires(Items.COPPER_INGOT, 3)
                    .requires(Items.REDSTONE)
                    .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                    .save(output);
            mark(ModItems.TEMPERED_COPPER_INGOT.get());
            // 2 tempered copper + 1 iron + 1 amethyst.
            shapeless(RecipeCategory.MISC, ModItems.RESONANT_ALLOY_INGOT.get())
                    .requires(ModItems.TEMPERED_COPPER_INGOT.get(), 2)
                    .requires(Items.IRON_INGOT)
                    .requires(Items.AMETHYST_SHARD)
                    .unlockedBy("has_tempered_copper", has(ModItems.TEMPERED_COPPER_INGOT.get()))
                    .save(output);
            mark(ModItems.RESONANT_ALLOY_INGOT.get());
            // Nether materials: blaze powder and nether brick, plus the previous alloy.
            shapeless(RecipeCategory.MISC, ModItems.EMBERSTEEL_INGOT.get())
                    .requires(ModItems.RESONANT_ALLOY_INGOT.get(), 2)
                    .requires(Items.BLAZE_POWDER)
                    .requires(Items.NETHER_BRICK)
                    .unlockedBy("has_resonant_alloy", has(ModItems.RESONANT_ALLOY_INGOT.get()))
                    .save(output);
            mark(ModItems.EMBERSTEEL_INGOT.get());
            // 2 embersteel + 1 redstone block + 1 diamond.
            shapeless(RecipeCategory.MISC, ModItems.FLUX_ALLOY_INGOT.get())
                    .requires(ModItems.EMBERSTEEL_INGOT.get(), 2)
                    .requires(Items.REDSTONE_BLOCK)
                    .requires(Items.DIAMOND)
                    .unlockedBy("has_embersteel", has(ModItems.EMBERSTEEL_INGOT.get()))
                    .save(output);
            mark(ModItems.FLUX_ALLOY_INGOT.get());
            // Netherite ingot, not scrap.
            shapeless(RecipeCategory.MISC, ModItems.ENDER_ALLOY_INGOT.get())
                    .requires(ModItems.FLUX_ALLOY_INGOT.get(), 2)
                    .requires(Items.NETHERITE_INGOT)
                    .requires(Items.ENDER_PEARL)
                    .unlockedBy("has_flux_alloy", has(ModItems.FLUX_ALLOY_INGOT.get()))
                    .save(output);
            mark(ModItems.ENDER_ALLOY_INGOT.get());
        }

        private void storage() {
            for (ToolTier tier : ToolTier.values()) {
                ItemLike ingot = ModItems.ingotFor(tier).get();
                ItemLike block = ModBlocks.storage(tier).get();
                nineBlockStorageRecipes(
                        RecipeCategory.MISC,
                        ingot,
                        RecipeCategory.BUILDING_BLOCKS,
                        block,
                        AdaptiveTools.MOD_ID + ":" + tier.id() + "_block",
                        null,
                        AdaptiveTools.MOD_ID + ":" + tier.id() + "_ingot_from_block",
                        null);
                mark(ModBlocks.storageItem(tier).get());
            }
        }

        private void templates() {
            firstTemplate(ToolTier.RESONANT_ALLOY, ModItems.TEMPERED_COPPER_INGOT.get(), Items.COPPER_BLOCK);
            firstTemplate(ToolTier.EMBERSTEEL, ModItems.RESONANT_ALLOY_INGOT.get(), Items.NETHERRACK);
            firstTemplate(ToolTier.FLUX_ALLOY, ModItems.EMBERSTEEL_INGOT.get(), Items.REDSTONE_BLOCK);
            firstTemplate(ToolTier.ENDER_ALLOY, ModItems.FLUX_ALLOY_INGOT.get(), Items.END_STONE);
            // Same shape as the netherite template: 7 diamonds, the template, and one material, for 2.
            copySmithingTemplate(ModItems.upgradeTemplate(ToolTier.RESONANT_ALLOY).get(), Items.COPPER_BLOCK);
            copySmithingTemplate(ModItems.upgradeTemplate(ToolTier.EMBERSTEEL).get(), Items.NETHERRACK);
            copySmithingTemplate(ModItems.upgradeTemplate(ToolTier.FLUX_ALLOY).get(), Items.REDSTONE_BLOCK);
            copySmithingTemplate(ModItems.upgradeTemplate(ToolTier.ENDER_ALLOY).get(), Items.END_STONE);
        }

        private void firstTemplate(ToolTier to, ItemLike previous, ItemLike material) {
            Item template = ModItems.upgradeTemplate(to).get();
            shapeless(RecipeCategory.MISC, template)
                    .requires(Items.DIAMOND)
                    .requires(previous)
                    .requires(material)
                    .unlockedBy("has_previous", has(previous))
                    .save(output, AdaptiveTools.MOD_ID + ":" + BuiltInRegistries.ITEM.getKey(template).getPath() + "_craft");
            mark(template);
        }

        private void toolsAndArmor() {
            for (ToolTier tier : ToolTier.values()) {
                ItemLike ingot = ModItems.ingotFor(tier).get();
                gear(RecipeCategory.TOOLS, ModItems.pickaxeFor(tier).get(), ingot, "III", " S ", " S ");
                gear(RecipeCategory.TOOLS, ModItems.axeFor(tier).get(), ingot, "II", "IS", " S");
                gear(RecipeCategory.TOOLS, ModItems.shovelFor(tier).get(), ingot, "I", "S", "S");
                gear(RecipeCategory.TOOLS, ModItems.hoeFor(tier).get(), ingot, "II", " S", " S");
                gear(RecipeCategory.TOOLS, ModItems.swordFor(tier).get(), ingot, "I", "I", "S");
                gear(RecipeCategory.COMBAT, ModItems.armor(tier, ArmorType.HELMET).get(), ingot, "III", "I I");
                gear(RecipeCategory.COMBAT, ModItems.armor(tier, ArmorType.CHESTPLATE).get(), ingot, "I I", "III", "III");
                gear(RecipeCategory.COMBAT, ModItems.armor(tier, ArmorType.LEGGINGS).get(), ingot, "III", "I I", "I I");
                gear(RecipeCategory.COMBAT, ModItems.armor(tier, ArmorType.BOOTS).get(), ingot, "I I", "I I");
            }
            ToolTier[] tiers = ToolTier.values();
            for (int index = 0; index < tiers.length - 1; index++) {
                upgrade(tiers[index], tiers[index + 1]);
            }
        }

        private void gear(RecipeCategory category, ItemLike result, ItemLike ingot, String... rows) {
            var recipe = shaped(category, result).define('I', ingot).unlockedBy("has_ingot", has(ingot));
            boolean stick = false;
            for (String row : rows) {
                stick |= row.indexOf('S') >= 0;
                recipe.pattern(row);
            }
            if (stick) {
                recipe.define('S', Items.STICK);
            }
            recipe.save(output);
            mark(result);
        }

        private void upgrade(ToolTier from, ToolTier to) {
            Item template = ModItems.upgradeTemplate(to).get();
            Ingredient pattern = Ingredient.of(template);
            Ingredient ingot = Ingredient.of(ModItems.ingotFor(to).get());
            for (ItemLike piece : pieces(from)) {
                ItemLike result = matching(to, piece);
                RecipeCategory category = result.asItem() instanceof AdaptiveArmorItem ? RecipeCategory.COMBAT : RecipeCategory.TOOLS;
                Identifier id = BuiltInRegistries.ITEM.getKey(result.asItem());
                SmithingTransformRecipeBuilder.smithing(pattern, Ingredient.of(piece), ingot, category, result.asItem())
                        .unlocks("has_template", has(template))
                        .save(output, AdaptiveTools.MOD_ID + ":smithing/" + id.getPath());
                mark(result);
            }
        }

        private void modules() {
            shapeless(RecipeCategory.MISC, ModItems.POWER_MODULE.get())
                    .requires(Items.REDSTONE)
                    .requires(ModItems.TEMPERED_COPPER_INGOT.get())
                    .unlockedBy("has_tempered_copper", has(ModItems.TEMPERED_COPPER_INGOT.get()))
                    .save(output);
            mark(ModItems.POWER_MODULE.get());
            shapeless(RecipeCategory.MISC, ModItems.ITEM_MODULE.get())
                    .requires(Items.CHEST)
                    .requires(ModItems.TEMPERED_COPPER_INGOT.get())
                    .unlockedBy("has_tempered_copper", has(ModItems.TEMPERED_COPPER_INGOT.get()))
                    .save(output);
            mark(ModItems.ITEM_MODULE.get());
            shapeless(RecipeCategory.MISC, ModItems.FLUID_MODULE.get())
                    .requires(Items.BUCKET)
                    .requires(ModItems.TEMPERED_COPPER_INGOT.get())
                    .unlockedBy("has_tempered_copper", has(ModItems.TEMPERED_COPPER_INGOT.get()))
                    .save(output);
            mark(ModItems.FLUID_MODULE.get());
            shapeless(RecipeCategory.MISC, ModItems.RATE_MODULE.get())
                    .requires(Items.REDSTONE, 2)
                    .requires(ModItems.RESONANT_ALLOY_INGOT.get())
                    .unlockedBy("has_resonant_alloy", has(ModItems.RESONANT_ALLOY_INGOT.get()))
                    .save(output);
            mark(ModItems.RATE_MODULE.get());
            shapeless(RecipeCategory.MISC, ModItems.CAPACITY_MODULE.get())
                    .requires(Items.REDSTONE_BLOCK)
                    .requires(ModItems.EMBERSTEEL_INGOT.get())
                    .unlockedBy("has_embersteel", has(ModItems.EMBERSTEEL_INGOT.get()))
                    .save(output);
            mark(ModItems.CAPACITY_MODULE.get());
            shapeless(RecipeCategory.MISC, ModItems.CROSS_DIMENSION_MODULE.get())
                    .requires(Items.ENDER_PEARL)
                    .requires(ModItems.FLUX_ALLOY_INGOT.get())
                    .unlockedBy("has_flux_alloy", has(ModItems.FLUX_ALLOY_INGOT.get()))
                    .save(output);
            mark(ModItems.CROSS_DIMENSION_MODULE.get());
            shapeless(RecipeCategory.MISC, ModItems.WATER_WALKING_MODULE.get())
                    .requires(Items.WATER_BUCKET)
                    .requires(ModItems.RESONANT_ALLOY_INGOT.get(), 2)
                    .requires(Items.PRISMARINE_CRYSTALS, 2)
                    .unlockedBy("has_resonant_alloy", has(ModItems.RESONANT_ALLOY_INGOT.get()))
                    .save(output);
            mark(ModItems.WATER_WALKING_MODULE.get());
        }

        private void cables() {
            // 1 tempered copper + 1 redstone -> 16. Cheap, and it uses the lowest tier.
            shapeless(RecipeCategory.MISC, ModBlocks.LIGHT_CABLE_ITEM.get(), 16)
                    .requires(ModItems.TEMPERED_COPPER_INGOT.get())
                    .requires(Items.REDSTONE)
                    .unlockedBy("has_tempered_copper", has(ModItems.TEMPERED_COPPER_INGOT.get()))
                    .save(output);
            mark(ModBlocks.LIGHT_CABLE_ITEM.get());
            shapeless(RecipeCategory.MISC, ModBlocks.MEDIUM_CABLE_ITEM.get(), 8)
                    .requires(ModBlocks.LIGHT_CABLE_ITEM.get(), 2)
                    .requires(ModItems.EMBERSTEEL_INGOT.get(), 2)
                    .requires(Items.REDSTONE)
                    .unlockedBy("has_light_cable", has(ModBlocks.LIGHT_CABLE_ITEM.get()))
                    .save(output);
            mark(ModBlocks.MEDIUM_CABLE_ITEM.get());
            shapeless(RecipeCategory.MISC, ModBlocks.HEAVY_CABLE_ITEM.get(), 8)
                    .requires(ModBlocks.MEDIUM_CABLE_ITEM.get(), 2)
                    .requires(ModItems.ENDER_ALLOY_INGOT.get(), 2)
                    .requires(Items.REDSTONE)
                    .unlockedBy("has_medium_cable", has(ModBlocks.MEDIUM_CABLE_ITEM.get()))
                    .save(output);
            mark(ModBlocks.HEAVY_CABLE_ITEM.get());
        }

        private static List<ItemLike> pieces(ToolTier tier) {
            List<ItemLike> pieces = new ArrayList<>();
            pieces.add(ModItems.pickaxeFor(tier).get());
            pieces.add(ModItems.axeFor(tier).get());
            pieces.add(ModItems.shovelFor(tier).get());
            pieces.add(ModItems.hoeFor(tier).get());
            pieces.add(ModItems.swordFor(tier).get());
            for (ArmorType type : List.of(ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS)) {
                pieces.add(ModItems.armor(tier, type).get());
            }
            return pieces;
        }

        private static ItemLike matching(ToolTier tier, ItemLike from) {
            Item item = from.asItem();
            if (item instanceof AdaptivePickaxeItem) {
                return ModItems.pickaxeFor(tier).get();
            }
            if (item instanceof AdaptiveAxeItem) {
                return ModItems.axeFor(tier).get();
            }
            if (item instanceof AdaptiveShovelItem) {
                return ModItems.shovelFor(tier).get();
            }
            if (item instanceof AdaptiveHoeItem) {
                return ModItems.hoeFor(tier).get();
            }
            if (item instanceof AdaptiveSwordItem) {
                return ModItems.swordFor(tier).get();
            }
            if (item instanceof AdaptiveArmorItem armor) {
                return ModItems.armor(tier, armor.type()).get();
            }
            throw new IllegalArgumentException(String.valueOf(item));
        }

        private void mark(ItemLike item) {
            crafted.add(item.asItem());
        }

        private void requireEveryItem() {
            List<String> missing = new ArrayList<>();
            int ours = 0;
            for (Item item : BuiltInRegistries.ITEM) {
                Identifier id = BuiltInRegistries.ITEM.getKey(item);
                if (id == null || !AdaptiveTools.MOD_ID.equals(id.getNamespace()) || "test_buffer".equals(id.getPath())) {
                    continue;
                }
                ours++;
                if (!crafted.contains(item)) {
                    missing.add(id.toString());
                }
            }
            if (ours > 0 && !missing.isEmpty()) {
                throw new IllegalStateException("Items without a recipe: " + missing);
            }
        }
    }
}
