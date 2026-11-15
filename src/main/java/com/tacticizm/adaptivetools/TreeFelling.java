package com.tacticizm.adaptivetools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Logs joined by leaves are one tree. The log limit still caps the group. Ender axes may also
 * pull in logs a few blocks away that leaves do not touch. There is no wood-type filter yet.
 */
public final class TreeFelling {
    /** How far an Ender axe looks past the leaf-connected group for another tree. */
    public static final int NEARBY_RADIUS = 4;
    private static final int MAX_LEAVES = 256;

    private TreeFelling() {}

    public static List<BlockPos> logs(Level level, BlockPos origin, int limit, boolean nearby) {
        List<BlockPos> found = new ArrayList<>();
        if (limit <= 0 || !isLog(level, origin)) {
            return found;
        }
        Set<BlockPos> seenLogs = new HashSet<>();
        Set<BlockPos> seenLeaves = new HashSet<>();
        List<BlockPos> leafQueue = new ArrayList<>();
        List<BlockPos> logQueue = new ArrayList<>();
        logQueue.add(origin);
        seenLogs.add(origin);
        int leafCursor = 0;
        int logCursor = 0;
        while ((logCursor < logQueue.size() || leafCursor < leafQueue.size()) && found.size() < limit) {
            if (logCursor < logQueue.size()) {
                BlockPos log = logQueue.get(logCursor++);
                found.add(log);
                if (found.size() >= limit) {
                    break;
                }
                for (Direction direction : Direction.values()) {
                    consider(level, log.relative(direction), seenLogs, seenLeaves, logQueue, leafQueue);
                }
            } else {
                BlockPos leaf = leafQueue.get(leafCursor++);
                for (Direction direction : Direction.values()) {
                    consider(level, leaf.relative(direction), seenLogs, seenLeaves, logQueue, leafQueue);
                }
            }
        }
        if (nearby && found.size() < limit) {
            addNearby(level, found, seenLogs, limit);
        }
        return found;
    }

    public static List<BlockPos> leavesTouching(Level level, List<BlockPos> logs) {
        List<BlockPos> leaves = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        List<BlockPos> queue = new ArrayList<>();
        for (BlockPos log : logs) {
            for (Direction direction : Direction.values()) {
                BlockPos next = log.relative(direction);
                if (isLeaf(level, next) && seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        for (int cursor = 0; cursor < queue.size() && leaves.size() < MAX_LEAVES; cursor++) {
            BlockPos leaf = queue.get(cursor);
            leaves.add(leaf);
            for (Direction direction : Direction.values()) {
                BlockPos next = leaf.relative(direction);
                if (isLeaf(level, next) && seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return leaves;
    }

    private static void consider(Level level, BlockPos pos, Set<BlockPos> seenLogs, Set<BlockPos> seenLeaves, List<BlockPos> logQueue, List<BlockPos> leafQueue) {
        if (isLog(level, pos)) {
            if (seenLogs.add(pos)) {
                logQueue.add(pos);
            }
        } else if (isLeaf(level, pos) && seenLeaves.size() < MAX_LEAVES && seenLeaves.add(pos)) {
            leafQueue.add(pos);
        }
    }

    private static void addNearby(Level level, List<BlockPos> found, Set<BlockPos> seenLogs, int limit) {
        List<BlockPos> seeds = new ArrayList<>(found);
        for (BlockPos seed : seeds) {
            if (found.size() >= limit) {
                return;
            }
            for (int dx = -NEARBY_RADIUS; dx <= NEARBY_RADIUS; dx++) {
                for (int dy = -NEARBY_RADIUS; dy <= NEARBY_RADIUS; dy++) {
                    for (int dz = -NEARBY_RADIUS; dz <= NEARBY_RADIUS; dz++) {
                        if (found.size() >= limit) {
                            return;
                        }
                        BlockPos pos = seed.offset(dx, dy, dz);
                        if (isLog(level, pos) && seenLogs.add(pos)) {
                            found.add(pos);
                        }
                    }
                }
            }
        }
    }

    public static boolean isLog(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(BlockTags.LOGS);
    }

    private static boolean isLeaf(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(BlockTags.LEAVES);
    }
}
