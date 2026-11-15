package com.tacticizm.adaptivetools;

import net.minecraft.world.item.ItemStack;

/** Sword. Cleave, Chain Strike, and the other attacks are applied by {@link SwordCombat}. */
public class AdaptiveSwordItem extends AdaptiveToolItem {
    public AdaptiveSwordItem(ToolTier tier, Properties properties) {
        super(tier, properties.sword(tier.toolMaterial(), tier.swordDamageBaseline(), tier.swordAttackSpeed()));
    }

    public static boolean isOurSword(ItemStack stack) {
        return stack.getItem() instanceof AdaptiveSwordItem;
    }
}
