package com.tacticizm.adaptivetools;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shovel. Area digging uses the pickaxe caps, including 1×1 for Tempered Copper and Embersteel.
 * Trench mode, on the tiers that have an area, digs a one-block path in the facing direction and
 * wins over the square when both are on. Grading, from Embersteel up, only removes blocks above
 * the chosen height and never places a block.
 */
public class AdaptiveShovelItem extends AdaptiveToolItem {
    private static final ThreadLocal<Boolean> DIGGING = ThreadLocal.withInitial(() -> false);
    static final ThreadLocal<Direction> FORCED_FACE = new ThreadLocal<>();

    public AdaptiveShovelItem(ToolTier tier, Properties properties) {
        super(tier, properties.shovel(tier.toolMaterial(), tier.shovelDamageBaseline(), tier.shovelAttackSpeed()));
    }

    public static boolean isDigging() {
        return DIGGING.get();
    }

    public boolean nextIsAbility(ItemStack stack) {
        ToolSettings settings = ToolSettings.of(stack, tier);
        return settings.effectiveSide(tier) > 1 || settings.effectiveTrench(tier) > 1;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (level.isClientSide() || !(miner instanceof Player player)) {
            return true;
        }
        boolean ability = nextIsAbility(stack);
        ToolBilling.pay(stack, player, tier, ability);
        if (!DIGGING.get() && player instanceof ServerPlayer serverPlayer && !player.getAbilities().instabuild) {
            dig(serverPlayer, pos);
        }
        return true;
    }

    public void digIfCreative(ServerPlayer player, BlockPos origin) {
        if (player.getAbilities().instabuild) {
            dig(player, origin);
        }
    }

    private void dig(ServerPlayer player, BlockPos origin) {
        ItemStack stack = player.getMainHandItem();
        ToolSettings settings = ToolSettings.of(stack, tier);
        List<BlockPos> positions = targets(player, origin, settings);
        if (positions.size() <= 1) {
            return;
        }
        Level level = player.level();
        DIGGING.set(true);
        try {
            for (BlockPos pos : positions) {
                if (pos.equals(origin)) {
                    continue;
                }
                ItemStack current = player.getMainHandItem();
                if (!ToolBilling.canAfford(current, player, tier, true)) {
                    break;
                }
                BlockState state = level.getBlockState(pos);
                if (!canDig(level, pos, state, settings, player)) {
                    continue;
                }
                player.gameMode.destroyBlock(pos);
            }
        } finally {
            DIGGING.set(false);
        }
    }

    public List<BlockPos> targets(Player player, BlockPos origin, ToolSettings settings) {
        if (settings.effectiveTrench(tier) > 1) {
            Direction facing = player.getDirection();
            List<BlockPos> trench = new ArrayList<>();
            for (int step = 0; step < settings.effectiveTrench(tier); step++) {
                trench.add(origin.relative(facing, step));
            }
            return trench;
        }
        Direction face = FORCED_FACE.get() == null ? Direction.UP : FORCED_FACE.get();
        return AreaMining.positions(origin, face, settings.effectiveSide(tier));
    }

    public boolean canDig(Level level, BlockPos pos, BlockState state, ToolSettings settings, Player player) {
        if (!state.is(BlockTags.MINEABLE_WITH_SHOVEL) || !AreaMining.canMine(level, pos, state)) {
            return false;
        }
        if (!settings.grading() || !tier.canGrade()) {
            return true;
        }
        int height = settings.gradeHeight() == Integer.MIN_VALUE ? player.getBlockY() : settings.gradeHeight();
        return pos.getY() > height;
    }
}
