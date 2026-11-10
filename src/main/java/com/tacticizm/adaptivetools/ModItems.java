package com.tacticizm.adaptivetools;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Items in this slice.
 *
 * <p>TODO(textures): the ingot and pickaxe PNGs under {@code assets/adaptivetools/textures/item/}
 * are flat placeholders. Replace these files (do not rename them):
 * <ul>
 * <li>{@code textures/item/tempered_copper_ingot.png}</li>
 * <li>{@code textures/item/resonant_alloy_ingot.png}</li>
 * <li>{@code textures/item/embersteel_ingot.png}</li>
 * <li>{@code textures/item/flux_alloy_ingot.png}</li>
 * <li>{@code textures/item/ender_alloy_ingot.png}</li>
 * <li>{@code textures/item/tempered_copper_pickaxe.png}</li>
 * <li>{@code textures/item/resonant_alloy_pickaxe.png}</li>
 * <li>{@code textures/item/embersteel_pickaxe.png}</li>
 * <li>{@code textures/item/flux_alloy_pickaxe.png}</li>
 * <li>{@code textures/item/ender_alloy_pickaxe.png}</li>
 * </ul>
 * Module and wrench textures are the approved finals and must not be edited.
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AdaptiveTools.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AdaptiveTools.MOD_ID);

    public static final DeferredItem<Item> TEMPERED_COPPER_INGOT = ingot(ToolTier.TEMPERED_COPPER);
    public static final DeferredItem<Item> RESONANT_ALLOY_INGOT = ingot(ToolTier.RESONANT_ALLOY);
    public static final DeferredItem<Item> EMBERSTEEL_INGOT = ingot(ToolTier.EMBERSTEEL);
    public static final DeferredItem<Item> FLUX_ALLOY_INGOT = ingot(ToolTier.FLUX_ALLOY);
    public static final DeferredItem<Item> ENDER_ALLOY_INGOT = ingot(ToolTier.ENDER_ALLOY);

    public static final DeferredItem<AdaptivePickaxeItem> TEMPERED_COPPER_PICKAXE = pickaxe(ToolTier.TEMPERED_COPPER);
    public static final DeferredItem<AdaptivePickaxeItem> RESONANT_PICKAXE = pickaxe(ToolTier.RESONANT_ALLOY);
    public static final DeferredItem<AdaptivePickaxeItem> EMBERSTEEL_PICKAXE = pickaxe(ToolTier.EMBERSTEEL);
    public static final DeferredItem<AdaptivePickaxeItem> FLUX_PICKAXE = pickaxe(ToolTier.FLUX_ALLOY);
    public static final DeferredItem<AdaptivePickaxeItem> ENDER_PICKAXE = pickaxe(ToolTier.ENDER_ALLOY);

    public static final DeferredItem<Item> POWER_MODULE = plain("power_module");
    public static final DeferredItem<Item> ITEM_MODULE = plain("item_module");
    public static final DeferredItem<Item> FLUID_MODULE = plain("fluid_module");
    public static final DeferredItem<Item> CROSS_DIMENSION_MODULE = plain("cross_dimension_module");
    public static final DeferredItem<Item> CAPACITY_MODULE = plain("capacity_module");
    public static final DeferredItem<Item> RATE_MODULE = plain("rate_module");
    public static final DeferredItem<Item> WRENCH = ITEMS.registerSimpleItem("wrench", props -> props.stacksTo(1));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.adaptivetools"))
            .withTabsBefore(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .icon(() -> TEMPERED_COPPER_PICKAXE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(TEMPERED_COPPER_INGOT.get());
                output.accept(RESONANT_ALLOY_INGOT.get());
                output.accept(EMBERSTEEL_INGOT.get());
                output.accept(FLUX_ALLOY_INGOT.get());
                output.accept(ENDER_ALLOY_INGOT.get());
                output.accept(TEMPERED_COPPER_PICKAXE.get());
                output.accept(RESONANT_PICKAXE.get());
                output.accept(EMBERSTEEL_PICKAXE.get());
                output.accept(FLUX_PICKAXE.get());
                output.accept(ENDER_PICKAXE.get());
                output.accept(POWER_MODULE.get());
                output.accept(ITEM_MODULE.get());
                output.accept(FLUID_MODULE.get());
                output.accept(CROSS_DIMENSION_MODULE.get());
                output.accept(CAPACITY_MODULE.get());
                output.accept(RATE_MODULE.get());
                output.accept(WRENCH.get());
            })
            .build());

    private ModItems() {}

    public static TagKey<Item> repairTag(ToolTier tier) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, tier.id() + "_ingots"));
    }

    public static DeferredItem<Item> ingotFor(ToolTier tier) {
        return switch (tier) {
            case TEMPERED_COPPER -> TEMPERED_COPPER_INGOT;
            case RESONANT_ALLOY -> RESONANT_ALLOY_INGOT;
            case EMBERSTEEL -> EMBERSTEEL_INGOT;
            case FLUX_ALLOY -> FLUX_ALLOY_INGOT;
            case ENDER_ALLOY -> ENDER_ALLOY_INGOT;
        };
    }

    public static DeferredItem<AdaptivePickaxeItem> pickaxeFor(ToolTier tier) {
        return switch (tier) {
            case TEMPERED_COPPER -> TEMPERED_COPPER_PICKAXE;
            case RESONANT_ALLOY -> RESONANT_PICKAXE;
            case EMBERSTEEL -> EMBERSTEEL_PICKAXE;
            case FLUX_ALLOY -> FLUX_PICKAXE;
            case ENDER_ALLOY -> ENDER_PICKAXE;
        };
    }

    private static DeferredItem<Item> ingot(ToolTier tier) {
        return ITEMS.registerSimpleItem(tier.ingotId(), props -> props);
    }

    private static DeferredItem<Item> plain(String id) {
        return ITEMS.registerSimpleItem(id, props -> props);
    }

    private static DeferredItem<AdaptivePickaxeItem> pickaxe(ToolTier tier) {
        return ITEMS.registerItem(tier.pickaxeId(), props -> new AdaptivePickaxeItem(tier, tier == ToolTier.ENDER_ALLOY ? props.rarity(Rarity.EPIC) : props));
    }
}
