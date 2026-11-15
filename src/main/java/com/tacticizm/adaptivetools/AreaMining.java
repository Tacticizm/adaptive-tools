package com.tacticizm.adaptivetools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Square area mining, one layer deep, on the plane of the face that was hit.
 * The targeted block is first, then the rest of the square ordered outward.
 */
public final class AreaMining {
    private AreaMining() {}

    public static List<BlockPos> positions(BlockPos origin, Direction face, int sideLength) {
        int radius = Math.max(0, (sideLength - 1) / 2);
        Direction.Axis axis = face.getAxis();
        List<BlockPos> positions = new ArrayList<>((radius * 2 + 1) * (radius * 2 + 1));
        for (int u = -radius; u <= radius; u++) {
            for (int v = -radius; v <= radius; v++) {
                positions.add(offsetOnPlane(origin, axis, u, v));
            }
        }
        positions.sort(Comparator
                .comparingInt((BlockPos pos) -> distanceOnPlane(origin, pos, axis))
                .thenComparingInt(BlockPos::getX)
                .thenComparingInt(BlockPos::getY)
                .thenComparingInt(BlockPos::getZ));
        return positions;
    }

    /**
     * Blocks this slice will try to break. Air, fluids, and unbreakable blocks are skipped.
     * There is no block filter yet.
     *
     * <p>TODO(filters): tools will grow a block filter. Protected containers stay skipped even if
     * they are listed. Claim protection is a separate check: each extra block fires
     * {@code BreakBlockEvent}, and a cancelled event is not charged.
     */
    public static boolean canMine(Level level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        if (state.getBlock() instanceof LiquidBlock) {
            return false;
        }
        return state.getFluidState().isEmpty() || !state.getCollisionShape(level, pos).isEmpty();
    }

    private static BlockPos offsetOnPlane(BlockPos origin, Direction.Axis axis, int u, int v) {
        return switch (axis) {
            case Y -> origin.offset(u, 0, v);
            case X -> origin.offset(0, u, v);
            case Z -> origin.offset(u, v, 0);
        };
    }

    private static int distanceOnPlane(BlockPos origin, BlockPos pos, Direction.Axis axis) {
        int dx = Math.abs(pos.getX() - origin.getX());
        int dy = Math.abs(pos.getY() - origin.getY());
        int dz = Math.abs(pos.getZ() - origin.getZ());
        return switch (axis) {
            case Y -> Math.max(dx, dz);
            case X -> Math.max(dy, dz);
            case Z -> Math.max(dx, dy);
        };
    }
}
