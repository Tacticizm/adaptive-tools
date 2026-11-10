package com.tacticizm.adaptivetools;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Pays for one block or one target. An ability-enhanced action costs 125 FE, or 2 durability,
 * instead of the base 100 FE or 1 durability. Creative mode is not billed.
 */
public final class ToolBilling {
    private ToolBilling() {}

    public static boolean canAfford(ItemStack stack, Player player, ToolTier tier, boolean ability) {
        if (player != null && player.getAbilities().instabuild) {
            return true;
        }
        if (tier.usesEnergy()) {
            return ModEnergy.getStored(stack) >= (ability ? ActionCosts.ABILITY_FE : ActionCosts.BASE_FE);
        }
        int maxDamage = stack.getMaxDamage();
        int cost = ability ? 2 : ActionCosts.BASE_DURABILITY;
        return maxDamage <= 0 || stack.getDamageValue() + cost <= maxDamage;
    }

    public static void pay(ItemStack stack, Player player, ToolTier tier, boolean ability) {
        if (player != null && player.getAbilities().instabuild) {
            return;
        }
        if (tier.usesEnergy()) {
            if (player != null && ModEnergy.shouldSkipCost(stack, player)) {
                return;
            }
            ModEnergy.extract(stack, ability ? ActionCosts.ABILITY_FE : ActionCosts.BASE_FE);
            return;
        }
        if (player != null) {
            stack.hurtAndBreak(ability ? 2 : ActionCosts.BASE_DURABILITY, player, EquipmentSlot.MAINHAND);
        }
    }
}
