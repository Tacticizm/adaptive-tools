package com.tacticizm.adaptivetools;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;

/**
 * Axe. Resonant and above can fell a leaf-connected tree up to the tier's log limit. Ender may
 * also take logs a few blocks away. Embersteel and above can smelt a log into charcoal when a
 * smelting recipe says that log becomes charcoal. Tempered Copper only chops the log it hits.
 */
public class AdaptiveAxeItem extends AdaptiveToolItem {
    private static final ThreadLocal<Boolean> FELLING = ThreadLocal.withInitial(() -> false);

    public AdaptiveAxeItem(ToolTier tier, Properties properties) {
        super(tier, properties.axe(tier.toolMaterial(), tier.axeDamageBaseline(), tier.axeAttackSpeed()));
    }

    public static boolean isFelling() {
        return FELLING.get();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        BlockState stripped = state.getToolModifiedState(context, ItemAbilities.AXE_STRIP, false);
        if (stripped == null) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player != null && !ToolBilling.canAfford(stack, player, tier, false)) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, stripped, 11);
            if (player != null) {
                ToolBilling.pay(stack, player, tier, false);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (level.isClientSide() || !(miner instanceof Player player)) {
            return true;
        }
        ToolSettings settings = ToolSettings.of(stack, tier);
        boolean felling = FELLING.get() || (settings.fell() && tier.maxFellLogs() > 0 && state.is(net.minecraft.tags.BlockTags.LOGS));
        ToolBilling.pay(stack, player, tier, felling);
        if (felling && !FELLING.get() && player instanceof ServerPlayer serverPlayer && !player.getAbilities().instabuild) {
            fell(serverPlayer, pos, settings);
        }
        return true;
    }

    private void fell(ServerPlayer player, BlockPos origin, ToolSettings settings) {
        Level level = player.level();
        List<BlockPos> logs = TreeFelling.logs(level, origin, settings.effectiveFell(tier), tier.includesNearbyTrees());
        List<BlockPos> leaves = settings.leafClear() ? TreeFelling.leavesTouching(level, logs) : List.of();
        FELLING.set(true);
        try {
            for (BlockPos pos : logs) {
                if (pos.equals(origin)) {
                    continue;
                }
                ItemStack current = player.getMainHandItem();
                if (!ToolBilling.canAfford(current, player, tier, true)) {
                    break;
                }
                player.gameMode.destroyBlock(pos);
            }
            for (BlockPos pos : leaves) {
                ItemStack current = player.getMainHandItem();
                if (!ToolBilling.canAfford(current, player, tier, true)) {
                    break;
                }
                if (TreeFelling.isLog(level, pos) || !level.getBlockState(pos).is(net.minecraft.tags.BlockTags.LEAVES)) {
                    continue;
                }
                player.gameMode.destroyBlock(pos);
            }
        } finally {
            FELLING.set(false);
        }
    }
}
