package com.tacticizm.adaptivetools;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;

/**
 * Starting stats for the five tool tiers. Every number lives here so it can be tuned in one place.
 *
 * <p>Attack damage shown in the tooltip is {@code attackDamageBonus + pickaxe baseline (1) + the player's 1}.
 * Speeds and durabilities sit just under the vanilla tool each tier is meant to leave useful:
 * Tempered Copper under iron, Resonant Alloy under diamond. Embersteel sits under netherite.
 * Flux matches netherite's speed and damage; Ender is a step past that. Harvest tags follow the
 * same ladder (iron, diamond, then netherite).
 *
 * <p>Area mining is a square, one block deep, on the face that was hit. The spec names caps for
 * Resonant (3×3), Flux (5×5) and Ender (7×7). Tempered Copper and Embersteel are not given an area
 * cap, so they stay 1×1. Smaller odd sizes are selectable up to the cap.
 *
 * <p>TODO(embersteel): lava repair is a material property, not a learned ability. Agreed outline is
 * throw the tool into a lava source, it floats while repairing, and on finish the source becomes
 * obsidian and the tool returns to its owner. Timing, rate, and multi-tool behavior are still open,
 * so nothing is implemented yet.
 *
 * <p>TODO(smithing): tier upgrades should be smithing transforms that keep enchantments, energy, and
 * tool settings. 26.1 smithing still requires a template item, and the template plus the ingot
 * recipes are unconfirmed, so no upgrade recipe is registered yet.
 */
public enum ToolTier {
    TEMPERED_COPPER("tempered_copper", "Tempered Copper", 200, 5.0F, 1.0F, 12, BlockTags.INCORRECT_FOR_IRON_TOOL, 1, 0),
    RESONANT_ALLOY("resonant_alloy", "Resonant Alloy", 1000, 7.0F, 2.0F, 10, BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 3, 0),
    EMBERSTEEL("embersteel", "Embersteel", 1800, 8.5F, 3.0F, 15, BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 1, 0),
    FLUX_ALLOY("flux_alloy", "Flux Alloy", 0, 9.0F, 4.0F, 18, BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 5, 100_000),
    ENDER_ALLOY("ender_alloy", "Ender Alloy", 0, 10.0F, 5.0F, 22, BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 7, 1_000_000);

    /** Added on top of {@link #attackDamageBonus} by every pickaxe, matching vanilla pickaxes. */
    public static final float PICKAXE_ATTACK_DAMAGE = 1.0F;
    /** Vanilla pickaxe attack speed. */
    public static final float PICKAXE_ATTACK_SPEED = -2.8F;

    private final String id;
    private final String displayName;
    private final int durability;
    private final float speed;
    private final float attackDamageBonus;
    private final int enchantmentValue;
    private final TagKey<Block> incorrectBlocksForDrops;
    private final int maxAreaSide;
    private final int energyCapacity;

    ToolTier(
            String id,
            String displayName,
            int durability,
            float speed,
            float attackDamageBonus,
            int enchantmentValue,
            TagKey<Block> incorrectBlocksForDrops,
            int maxAreaSide,
            int energyCapacity) {
        this.id = id;
        this.displayName = displayName;
        this.durability = durability;
        this.speed = speed;
        this.attackDamageBonus = attackDamageBonus;
        this.enchantmentValue = enchantmentValue;
        this.incorrectBlocksForDrops = incorrectBlocksForDrops;
        this.maxAreaSide = maxAreaSide;
        this.energyCapacity = energyCapacity;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String ingotId() {
        return id + "_ingot";
    }

    public String pickaxeId() {
        return id + "_pickaxe";
    }

    public int durability() {
        return durability;
    }

    public float speed() {
        return speed;
    }

    public float attackDamageBonus() {
        return attackDamageBonus;
    }

    public int enchantmentValue() {
        return enchantmentValue;
    }

    /** Odd side length of the largest area square. 1 means area mining is not available. */
    public int maxAreaSide() {
        return maxAreaSide;
    }

    public boolean usesEnergy() {
        return energyCapacity > 0;
    }

    public int energyCapacity() {
        return energyCapacity;
    }

    public TagKey<Block> incorrectBlocksForDrops() {
        return incorrectBlocksForDrops;
    }

    /**
     * Durability tiers use their real durability. Energy tiers still need a {@link ToolMaterial} for
     * speed and harvest, but they are not damaged; the large durability is only a safety cap.
     */
    public ToolMaterial toolMaterial() {
        int materialDurability = usesEnergy() ? 100_000 : durability;
        return new ToolMaterial(incorrectBlocksForDrops, materialDurability, speed, attackDamageBonus, enchantmentValue, ModItems.repairTag(this));
    }
}
