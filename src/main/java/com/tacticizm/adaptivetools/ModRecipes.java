package com.tacticizm.adaptivetools;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.ArmorType;

/**
 * Wrench, medium cable, and heavy cable. Light cables stay uncraftable until their ingredients
 * are settled.
 */
public final class ModRecipes extends RecipeProvider.Runner {
    public ModRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        return new RecipeProvider(registries, output) {
            @Override
            protected void buildRecipes() {
                shaped(RecipeCategory.TOOLS, ModItems.WRENCH.get())
                        .pattern("T T")
                        .pattern(" T ")
                        .pattern(" I ")
                        .define('T', ModItems.TEMPERED_COPPER_INGOT.get())
                        .define('I', Items.IRON_INGOT)
                        .unlockedBy("has_tempered_copper", has(ModItems.TEMPERED_COPPER_INGOT.get()))
                        .save(output);
                shapeless(RecipeCategory.MISC, ModBlocks.MEDIUM_CABLE_ITEM.get(), 8)
                        .requires(ModBlocks.LIGHT_CABLE_ITEM.get(), 2)
                        .requires(ModItems.EMBERSTEEL_INGOT.get(), 2)
                        .requires(Items.REDSTONE)
                        .unlockedBy("has_light_cable", has(ModBlocks.LIGHT_CABLE_ITEM.get()))
                        .save(output);
                for (ToolTier tier : ToolTier.values()) {
                    var ingot = ModItems.ingotFor(tier).get();
                    shaped(RecipeCategory.TOOLS, ModItems.axeFor(tier).get())
                            .pattern("II")
                            .pattern("IS")
                            .pattern(" S")
                            .define('I', ingot)
                            .define('S', Items.STICK)
                            .unlockedBy("has_ingot", has(ingot))
                            .save(output);
                    shaped(RecipeCategory.TOOLS, ModItems.shovelFor(tier).get())
                            .pattern("I")
                            .pattern("S")
                            .pattern("S")
                            .define('I', ingot)
                            .define('S', Items.STICK)
                            .unlockedBy("has_ingot", has(ingot))
                            .save(output);
                    shaped(RecipeCategory.TOOLS, ModItems.hoeFor(tier).get())
                            .pattern("II")
                            .pattern(" S")
                            .pattern(" S")
                            .define('I', ingot)
                            .define('S', Items.STICK)
                            .unlockedBy("has_ingot", has(ingot))
                            .save(output);
                    shaped(RecipeCategory.TOOLS, ModItems.swordFor(tier).get())
                            .pattern("I")
                            .pattern("I")
                            .pattern("S")
                            .define('I', ingot)
                            .define('S', Items.STICK)
                            .unlockedBy("has_ingot", has(ingot))
                            .save(output);
                    shaped(RecipeCategory.COMBAT, ModItems.armor(tier, ArmorType.HELMET).get())
                            .pattern("III")
                            .pattern("I I")
                            .define('I', ingot)
                            .unlockedBy("has_ingot", has(ingot))
                            .save(output);
                    shaped(RecipeCategory.COMBAT, ModItems.armor(tier, ArmorType.CHESTPLATE).get())
                            .pattern("I I")
                            .pattern("III")
                            .pattern("III")
                            .define('I', ingot)
                            .unlockedBy("has_ingot", has(ingot))
                            .save(output);
                    shaped(RecipeCategory.COMBAT, ModItems.armor(tier, ArmorType.LEGGINGS).get())
                            .pattern("III")
                            .pattern("I I")
                            .pattern("I I")
                            .define('I', ingot)
                            .unlockedBy("has_ingot", has(ingot))
                            .save(output);
                    shaped(RecipeCategory.COMBAT, ModItems.armor(tier, ArmorType.BOOTS).get())
                            .pattern("I I")
                            .pattern("I I")
                            .define('I', ingot)
                            .unlockedBy("has_ingot", has(ingot))
                            .save(output);
                }
                shapeless(RecipeCategory.MISC, ModItems.WATER_WALKING_MODULE.get())
                        .requires(Items.WATER_BUCKET)
                        .requires(ModItems.RESONANT_ALLOY_INGOT.get(), 2)
                        .requires(Items.PRISMARINE_CRYSTALS, 2)
                        .unlockedBy("has_resonant_alloy", has(ModItems.RESONANT_ALLOY_INGOT.get()))
                        .save(output);
                shapeless(RecipeCategory.MISC, ModBlocks.HEAVY_CABLE_ITEM.get(), 8)
                        .requires(ModBlocks.MEDIUM_CABLE_ITEM.get(), 2)
                        .requires(ModItems.ENDER_ALLOY_INGOT.get(), 2)
                        .requires(Items.REDSTONE)
                        .unlockedBy("has_medium_cable", has(ModBlocks.MEDIUM_CABLE_ITEM.get()))
                        .save(output);
            }
        };
    }

    @Override
    public String getName() {
        return "Adaptive Tools recipes";
    }
}
