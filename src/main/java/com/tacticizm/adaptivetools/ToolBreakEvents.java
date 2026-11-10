package com.tacticizm.adaptivetools;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * Cancels a break this mod cannot pay for. Other mods still see the event, and a cancel from a
 * claim plugin skips that block with no charge. The listener does nothing for items that are not
 * our pickaxes.
 */
public final class ToolBreakEvents {
    /**
     * Positions a game test has marked as protected. Empty during normal play, so this never
     * cancels a real break on its own.
     */
    public static final Set<BlockPos> TEST_DENIED_POSITIONS = ConcurrentHashMap.newKeySet();

    private ToolBreakEvents() {}

    public static void onBreak(BreakBlockEvent event) {
        BlockPos pos = event.getPos();
        if (TEST_DENIED_POSITIONS.contains(pos)) {
            event.setCanceled(true);
            return;
        }
        if (event.isCanceled()) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof AdaptiveShovelItem shovel) {
            guardShovel(event, player, stack, shovel);
            return;
        }
        if (stack.getItem() instanceof AdaptiveAxeItem axe) {
            ToolSettings settings = ToolSettings.of(stack, axe.tier());
            boolean ability = AdaptiveAxeItem.isFelling()
                    || (TreeFelling.isLog(event.getLevel(), pos) && settings.fell() && axe.tier().maxFellLogs() > 0);
            if (!ToolBilling.canAfford(stack, player, axe.tier(), ability)) {
                event.setCanceled(true);
            }
            return;
        }
        if (!(stack.getItem() instanceof AdaptivePickaxeItem pick)) {
            return;
        }
        if (AdaptivePickaxeItem.isBreakingArea()) {
            if (!pick.canAfford(stack, player)) {
                event.setCanceled(true);
            }
            return;
        }
        if (!pick.canAfford(stack, player)) {
            event.setCanceled(true);
            return;
        }
        if (event.getLevel().isClientSide() || !(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        AdaptivePickaxeItem.captureFace(player, pos);
        pick.breakAreaIfCreative(serverPlayer, pos);
    }

    private static void guardShovel(BreakBlockEvent event, Player player, ItemStack stack, AdaptiveShovelItem shovel) {
        ToolSettings settings = ToolSettings.of(stack, shovel.tier());
        if (settings.grading() && shovel.tier().canGrade()) {
            int height = settings.gradeHeight() == Integer.MIN_VALUE ? player.getBlockY() : settings.gradeHeight();
            if (event.getPos().getY() <= height) {
                event.setCanceled(true);
                return;
            }
        }
        boolean ability = shovel.nextIsAbility(stack);
        if (AdaptiveShovelItem.isDigging() || !shovel.nextIsAbility(stack)) {
            if (!ToolBilling.canAfford(stack, player, shovel.tier(), ability && AdaptiveShovelItem.isDigging())) {
                event.setCanceled(true);
            }
        } else if (!ToolBilling.canAfford(stack, player, shovel.tier(), ability)) {
            event.setCanceled(true);
            return;
        }
        if (event.isCanceled() || event.getLevel().isClientSide() || !(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        if (!AdaptiveShovelItem.isDigging()) {
            shovel.digIfCreative(serverPlayer, event.getPos());
        }
    }
}
