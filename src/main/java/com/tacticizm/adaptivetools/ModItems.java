package com.tacticizm.adaptivetools;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorType;
import com.tacticizm.adaptivetools.cable.WrenchItem;

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

    public static final DeferredItem<AdaptiveAxeItem> TEMPERED_COPPER_AXE = axe(ToolTier.TEMPERED_COPPER);
    public static final DeferredItem<AdaptiveAxeItem> RESONANT_AXE = axe(ToolTier.RESONANT_ALLOY);
    public static final DeferredItem<AdaptiveAxeItem> EMBERSTEEL_AXE = axe(ToolTier.EMBERSTEEL);
    public static final DeferredItem<AdaptiveAxeItem> FLUX_AXE = axe(ToolTier.FLUX_ALLOY);
    public static final DeferredItem<AdaptiveAxeItem> ENDER_AXE = axe(ToolTier.ENDER_ALLOY);

    public static final DeferredItem<AdaptiveShovelItem> TEMPERED_COPPER_SHOVEL = shovel(ToolTier.TEMPERED_COPPER);
    public static final DeferredItem<AdaptiveShovelItem> RESONANT_SHOVEL = shovel(ToolTier.RESONANT_ALLOY);
    public static final DeferredItem<AdaptiveShovelItem> EMBERSTEEL_SHOVEL = shovel(ToolTier.EMBERSTEEL);
    public static final DeferredItem<AdaptiveShovelItem> FLUX_SHOVEL = shovel(ToolTier.FLUX_ALLOY);
    public static final DeferredItem<AdaptiveShovelItem> ENDER_SHOVEL = shovel(ToolTier.ENDER_ALLOY);

    public static final DeferredItem<AdaptiveHoeItem> TEMPERED_COPPER_HOE = hoe(ToolTier.TEMPERED_COPPER);
    public static final DeferredItem<AdaptiveHoeItem> RESONANT_HOE = hoe(ToolTier.RESONANT_ALLOY);
    public static final DeferredItem<AdaptiveHoeItem> EMBERSTEEL_HOE = hoe(ToolTier.EMBERSTEEL);
    public static final DeferredItem<AdaptiveHoeItem> FLUX_HOE = hoe(ToolTier.FLUX_ALLOY);
    public static final DeferredItem<AdaptiveHoeItem> ENDER_HOE = hoe(ToolTier.ENDER_ALLOY);

    public static final DeferredItem<AdaptiveSwordItem> TEMPERED_COPPER_SWORD = sword(ToolTier.TEMPERED_COPPER);
    public static final DeferredItem<AdaptiveSwordItem> RESONANT_SWORD = sword(ToolTier.RESONANT_ALLOY);
    public static final DeferredItem<AdaptiveSwordItem> EMBERSTEEL_SWORD = sword(ToolTier.EMBERSTEEL);
    public static final DeferredItem<AdaptiveSwordItem> FLUX_SWORD = sword(ToolTier.FLUX_ALLOY);
    public static final DeferredItem<AdaptiveSwordItem> ENDER_SWORD = sword(ToolTier.ENDER_ALLOY);

    public static final Map<ToolTier, Map<ArmorType, DeferredItem<AdaptiveArmorItem>>> ARMOR = armorPieces();

    public static final DeferredItem<Item> POWER_MODULE = plain("power_module");
    public static final DeferredItem<Item> ITEM_MODULE = plain("item_module");
    public static final DeferredItem<Item> FLUID_MODULE = plain("fluid_module");
    public static final DeferredItem<Item> CROSS_DIMENSION_MODULE = plain("cross_dimension_module");
    public static final DeferredItem<Item> CAPACITY_MODULE = plain("capacity_module");
    public static final DeferredItem<Item> RATE_MODULE = plain("rate_module");
    public static final DeferredItem<Item> WATER_WALKING_MODULE = plain("water_walking_module");
    public static final DeferredItem<WrenchItem> WRENCH = ITEMS.registerItem("wrench", props -> new WrenchItem(props.stacksTo(1)));

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
                for (ToolTier tier : ToolTier.values()) {
                    output.accept(pickaxeFor(tier).get());
                    output.accept(axeFor(tier).get());
                    output.accept(shovelFor(tier).get());
                    output.accept(hoeFor(tier).get());
                    output.accept(swordFor(tier).get());
                    for (ArmorType type : List.of(ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS)) {
                        output.accept(armor(tier, type).get());
                    }
                }
                output.accept(POWER_MODULE.get());
                output.accept(ITEM_MODULE.get());
                output.accept(FLUID_MODULE.get());
                output.accept(CROSS_DIMENSION_MODULE.get());
                output.accept(CAPACITY_MODULE.get());
                output.accept(RATE_MODULE.get());
                output.accept(WATER_WALKING_MODULE.get());
                output.accept(WRENCH.get());
                output.accept(ModBlocks.LIGHT_CABLE_ITEM.get());
                output.accept(ModBlocks.MEDIUM_CABLE_ITEM.get());
                output.accept(ModBlocks.HEAVY_CABLE_ITEM.get());
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

    public static DeferredItem<AdaptiveAxeItem> axeFor(ToolTier tier) {
        return switch (tier) {
            case TEMPERED_COPPER -> TEMPERED_COPPER_AXE;
            case RESONANT_ALLOY -> RESONANT_AXE;
            case EMBERSTEEL -> EMBERSTEEL_AXE;
            case FLUX_ALLOY -> FLUX_AXE;
            case ENDER_ALLOY -> ENDER_AXE;
        };
    }

    public static DeferredItem<AdaptiveShovelItem> shovelFor(ToolTier tier) {
        return switch (tier) {
            case TEMPERED_COPPER -> TEMPERED_COPPER_SHOVEL;
            case RESONANT_ALLOY -> RESONANT_SHOVEL;
            case EMBERSTEEL -> EMBERSTEEL_SHOVEL;
            case FLUX_ALLOY -> FLUX_SHOVEL;
            case ENDER_ALLOY -> ENDER_SHOVEL;
        };
    }

    public static DeferredItem<AdaptiveHoeItem> hoeFor(ToolTier tier) {
        return switch (tier) {
            case TEMPERED_COPPER -> TEMPERED_COPPER_HOE;
            case RESONANT_ALLOY -> RESONANT_HOE;
            case EMBERSTEEL -> EMBERSTEEL_HOE;
            case FLUX_ALLOY -> FLUX_HOE;
            case ENDER_ALLOY -> ENDER_HOE;
        };
    }

    public static DeferredItem<AdaptiveArmorItem> armor(ToolTier tier, ArmorType type) {
        return ARMOR.get(tier).get(type);
    }

    public static Item[] energyArmor() {
        java.util.ArrayList<Item> items = new java.util.ArrayList<>();
        for (ToolTier tier : List.of(ToolTier.FLUX_ALLOY, ToolTier.ENDER_ALLOY)) {
            for (ArmorType type : List.of(ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS)) {
                items.add(armor(tier, type).get());
            }
        }
        return items.toArray(Item[]::new);
    }

    private static Map<ToolTier, Map<ArmorType, DeferredItem<AdaptiveArmorItem>>> armorPieces() {
        EnumMap<ToolTier, Map<ArmorType, DeferredItem<AdaptiveArmorItem>>> all = new EnumMap<>(ToolTier.class);
        for (ToolTier tier : ToolTier.values()) {
            EnumMap<ArmorType, DeferredItem<AdaptiveArmorItem>> pieces = new EnumMap<>(ArmorType.class);
            for (ArmorType type : List.of(ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS)) {
                String name = tier.id() + "_" + pieceName(type);
                pieces.put(type, ITEMS.registerItem(name, props -> new AdaptiveArmorItem(tier, type, ender(tier, props))));
            }
            all.put(tier, pieces);
        }
        return all;
    }

    private static String pieceName(ArmorType type) {
        return switch (type) {
            case HELMET -> "helmet";
            case CHESTPLATE -> "chestplate";
            case LEGGINGS -> "leggings";
            case BOOTS -> "boots";
            case BODY -> "body";
        };
    }

    public static DeferredItem<AdaptiveSwordItem> swordFor(ToolTier tier) {
        return switch (tier) {
            case TEMPERED_COPPER -> TEMPERED_COPPER_SWORD;
            case RESONANT_ALLOY -> RESONANT_SWORD;
            case EMBERSTEEL -> EMBERSTEEL_SWORD;
            case FLUX_ALLOY -> FLUX_SWORD;
            case ENDER_ALLOY -> ENDER_SWORD;
        };
    }

    private static DeferredItem<AdaptivePickaxeItem> pickaxe(ToolTier tier) {
        return ITEMS.registerItem(tier.pickaxeId(), props -> new AdaptivePickaxeItem(tier, ender(tier, props)));
    }

    private static DeferredItem<AdaptiveAxeItem> axe(ToolTier tier) {
        return ITEMS.registerItem(tier.id() + "_axe", props -> new AdaptiveAxeItem(tier, ender(tier, props)));
    }

    private static DeferredItem<AdaptiveShovelItem> shovel(ToolTier tier) {
        return ITEMS.registerItem(tier.id() + "_shovel", props -> new AdaptiveShovelItem(tier, ender(tier, props)));
    }

    private static DeferredItem<AdaptiveHoeItem> hoe(ToolTier tier) {
        return ITEMS.registerItem(tier.id() + "_hoe", props -> new AdaptiveHoeItem(tier, ender(tier, props)));
    }

    private static DeferredItem<AdaptiveSwordItem> sword(ToolTier tier) {
        return ITEMS.registerItem(tier.id() + "_sword", props -> new AdaptiveSwordItem(tier, ender(tier, props)));
    }

    private static Item.Properties ender(ToolTier tier, Item.Properties properties) {
        return tier == ToolTier.ENDER_ALLOY ? properties.rarity(Rarity.EPIC) : properties;
    }
}
