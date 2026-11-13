package com.tacticizm.adaptivetools;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.Level;

/** One armor piece. Flux and Ender store FE and show an energy bar instead of durability. */
public class AdaptiveArmorItem extends Item implements TieredTool {
    private final ToolTier tier;
    private final ArmorType type;

    public AdaptiveArmorItem(ToolTier tier, ArmorType type, Properties properties) {
        super(properties.stacksTo(1).humanoidArmor(ArmorBalance.material(tier), type));
        this.tier = tier;
        this.type = type;
    }

    @Override
    public ToolTier tier() {
        return tier;
    }

    public ArmorType type() {
        return type;
    }

    public boolean canWaterWalk() {
        return type == ArmorType.BOOTS && ArmorBalance.canWaterWalk(tier);
    }

    public boolean canElytra() {
        return type == ArmorType.CHESTPLATE && tier == ToolTier.FLUX_ALLOY;
    }

    public boolean canFly() {
        return type == ArmorType.CHESTPLATE && tier == ToolTier.ENDER_ALLOY;
    }

    public boolean canShield() {
        return tier.usesEnergy();
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return !tier.usesEnergy() && super.isDamageable(stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        if (!tier.usesEnergy()) {
            return super.isBarVisible(stack);
        }
        return ModEnergy.getStored(stack) < tier.energyCapacity();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        if (!tier.usesEnergy()) {
            return super.getBarWidth(stack);
        }
        int capacity = tier.energyCapacity();
        return capacity <= 0 ? 0 : Math.round(13.0F * ModEnergy.getStored(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (!tier.usesEnergy()) {
            return super.getBarColor(stack);
        }
        return tier == ToolTier.ENDER_ALLOY ? 0xFFB060FF : 0xFF4FD0FF;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            com.tacticizm.adaptivetools.client.ToolSettingsScreen.open(player, hand);
        }
        return InteractionResult.SUCCESS;
    }
}
