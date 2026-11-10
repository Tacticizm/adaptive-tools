package com.tacticizm.adaptivetools;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;

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
