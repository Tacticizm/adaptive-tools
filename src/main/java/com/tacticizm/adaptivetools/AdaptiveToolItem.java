package com.tacticizm.adaptivetools;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Shared bar, settings key, and empty-tool behavior for the axe, shovel, hoe, and sword. */
public abstract class AdaptiveToolItem extends Item implements TieredTool {
    protected final ToolTier tier;

    protected AdaptiveToolItem(ToolTier tier, Properties properties) {
        super(properties.stacksTo(1));
        this.tier = tier;
    }

    @Override
    public ToolTier tier() {
        return tier;
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
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);
        if (!ToolBilling.canAfford(stack, null, tier, false)) {
            return 0.0F;
        }
        return speed;
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

    protected boolean abilityBreak(ItemStack stack) {
        return false;
    }
}
