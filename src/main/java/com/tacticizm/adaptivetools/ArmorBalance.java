package com.tacticizm.adaptivetools;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * Provisional armor numbers. Protection steps from just under iron to just past netherite.
 * Durability bases follow the same path; Flux and Ender pieces are not damageable, so their
 * bases are unused. Knockback resistance stays at 0.
 *
 * <pre>
 * Tier            Helm Chest Legs Boots  Toughness  Durability base
 * Tempered Copper  2    5    4    2        0            13
 * Resonant Alloy   2    6    5    2        1            24
 * Embersteel       3    7    6    2        2            33
 * Flux Alloy       3    8    6    3        3            37 (no bar)
 * Ender Alloy      3    8    6    3        4            37 (no bar)
 * </pre>
 *
 * Shield cost is {@link ArmorAbilities#FE_PER_DAMAGE} FE per 1 damage point.
 */
public final class ArmorBalance {
    private static final Map<ToolTier, ArmorMaterial> MATERIALS = new EnumMap<>(ToolTier.class);

    private ArmorBalance() {}

    public static ArmorMaterial material(ToolTier tier) {
        return MATERIALS.computeIfAbsent(tier, ArmorBalance::create);
    }

    public static boolean canWaterWalk(ToolTier tier) {
        return tier != ToolTier.TEMPERED_COPPER;
    }

    private static ArmorMaterial create(ToolTier tier) {
        Stats stats = switch (tier) {
            case TEMPERED_COPPER -> new Stats(13, 2, 5, 4, 2, 0.0F, SoundEvents.ARMOR_EQUIP_COPPER);
            case RESONANT_ALLOY -> new Stats(24, 2, 6, 5, 2, 1.0F, SoundEvents.ARMOR_EQUIP_IRON);
            case EMBERSTEEL -> new Stats(33, 3, 7, 6, 2, 2.0F, SoundEvents.ARMOR_EQUIP_DIAMOND);
            case FLUX_ALLOY -> new Stats(37, 3, 8, 6, 3, 3.0F, SoundEvents.ARMOR_EQUIP_NETHERITE);
            case ENDER_ALLOY -> new Stats(37, 3, 8, 6, 3, 4.0F, SoundEvents.ARMOR_EQUIP_NETHERITE);
        };
        Map<ArmorType, Integer> defense = new EnumMap<>(ArmorType.class);
        for (ArmorType type : ArmorType.values()) {
            defense.put(type, 0);
        }
        defense.put(ArmorType.HELMET, stats.helmet);
        defense.put(ArmorType.CHESTPLATE, stats.chest);
        defense.put(ArmorType.LEGGINGS, stats.legs);
        defense.put(ArmorType.BOOTS, stats.boots);
        return new ArmorMaterial(stats.durability, defense, tier.enchantmentValue(), stats.sound, stats.toughness, 0.0F, ModItems.repairTag(tier), asset(tier));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ResourceKey<EquipmentAsset> asset(ToolTier tier) {
        return ResourceKey.create((ResourceKey) EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, tier.id()));
    }

    private record Stats(int durability, int helmet, int chest, int legs, int boots, float toughness, Holder<SoundEvent> sound) {}
}
