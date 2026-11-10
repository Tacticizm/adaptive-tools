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
}
