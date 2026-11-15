package com.tacticizm.adaptivetools.cable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Cached cable networks. Topology and the widest path between two cables are rebuilt only when a
 * cable is placed, removed, wrenched, or a neighbor changes. Each tick spends a shared budget on
 * every cable, so the slowest cable on a route is the limit and several routes have to share it.
 * Rate modules multiply that cable's item and fluid budget only.
 */
public final class CableNetworks {
    private static final Map<ServerLevel, CableNetworks> LEVELS = new HashMap<>();

    private final Set<BlockPos> cables = new HashSet<>();
    private final Map<BlockPos, Network> index = new HashMap<>();
    private final Map<BlockPos, Integer> itemRemainder = new HashMap<>();
    private boolean dirty = true;

    public static CableNetworks get(ServerLevel level) {
        return LEVELS.computeIfAbsent(level, ignored -> new CableNetworks());
    }

    public static void tick(ServerLevel level) {
        get(level).tickAll(level);
    }

    public void addCable(BlockPos pos) {
        if (cables.add(pos.immutable())) {
            dirty = true;
        }
    }

    public void removeCable(BlockPos pos) {
        cables.remove(pos);
        itemRemainder.remove(pos);
        dirty = true;
    }

    public void invalidate(BlockPos pos) {
        dirty = true;
    }

    /** Ticks only the network containing {@code pos}. Game tests use this so they do not move other tests. */
    public void tickAt(ServerLevel level, BlockPos pos) {
        if (dirty) {
            rebuild(level);
        }
        Network network = index.get(pos);
        if (network != null) {
            transfer(level, network);
        }
    }

    private void tickAll(ServerLevel level) {
        if (dirty) {
            rebuild(level);
        }
        for (Network network : new HashSet<>(index.values())) {
            transfer(level, network);
        }
    }

    private void rebuild(ServerLevel level) {
        index.clear();
        cables.removeIf(pos -> !(level.getBlockState(pos).getBlock() instanceof CableBlock));
        Set<BlockPos> pending = new HashSet<>(cables);
        while (!pending.isEmpty()) {
            BlockPos start = pending.iterator().next();
            Network network = build(level, start);
            for (BlockPos cable : network.cables) {
                pending.remove(cable);
                index.put(cable, network);
            }
        }
        itemRemainder.keySet().retainAll(cables);
        dirty = false;
    }

    private Network build(ServerLevel level, BlockPos start) {
        Set<BlockPos> members = new HashSet<>();
        Map<BlockPos, Set<BlockPos>> neighbors = new HashMap<>();
        List<BlockPos> queue = new ArrayList<>();
        queue.add(start.immutable());
        members.add(start.immutable());
        for (int cursor = 0; cursor < queue.size(); cursor++) {
            BlockPos pos = queue.get(cursor);
            Set<BlockPos> linked = new HashSet<>();
            for (Direction direction : Direction.values()) {
                if (!linkedBothWays(level, pos, direction)) {
                    continue;
                }
                BlockPos next = pos.relative(direction).immutable();
                linked.add(next);
                if (members.add(next)) {
                    queue.add(next);
                }
            }
            neighbors.put(pos, linked);
        }
        List<Endpoint> endpoints = new ArrayList<>();
        for (BlockPos pos : members) {
            CableBlockEntity cable = entity(level, pos);
            if (cable == null) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                if (cable.face(direction).disconnected) {
                    continue;
                }
                BlockPos machine = pos.relative(direction);
                if (members.contains(machine) || level.getBlockState(machine).isAir()) {
                    continue;
                }
                endpoints.add(new Endpoint(pos, direction, machine.immutable()));
            }
        }
        Network network = new Network(members, neighbors, endpoints);
        network.cachePaths(level);
        return network;
    }

    private static boolean linkedBothWays(ServerLevel level, BlockPos pos, Direction direction) {
        CableBlockEntity cable = entity(level, pos);
        if (cable == null || cable.face(direction).disconnected) {
            return false;
        }
        BlockPos next = pos.relative(direction);
        if (!(level.getBlockState(next).getBlock() instanceof CableBlock)) {
            return false;
        }
        CableBlockEntity other = entity(level, next);
        return other != null && !other.face(direction.getOpposite()).disconnected;
    }

    private void transfer(ServerLevel level, Network network) {
        Map<BlockPos, Integer> items = new HashMap<>();
        Map<BlockPos, Integer> fe = new HashMap<>();
        Map<BlockPos, Integer> fluid = new HashMap<>();
        for (BlockPos pos : network.cables) {
            CableBlockEntity cable = entity(level, pos);
            if (cable == null) {
                continue;
            }
            int factor = 1;
            for (Direction direction : Direction.values()) {
                factor = Math.max(factor, cable.face(direction).rateMultiplier());
            }
            int remainder = itemRemainder.getOrDefault(pos, 0) + cable.tier().itemsPerSecond() * factor;
            items.put(pos, remainder / 20);
            itemRemainder.put(pos, remainder % 20);
            fe.put(pos, cable.tier().fePerTick());
            fluid.put(pos, cable.tier().milliBucketsPerSecond() * factor / 20);
        }
        moveItems(level, network, items);
        moveEnergy(level, network, fe);
        moveFluid(level, network, fluid);
    }

    private void moveItems(ServerLevel level, Network network, Map<BlockPos, Integer> budget) {
        List<Endpoint> sinks = live(level, network, true, TransferMode.RECEIVE);
        Set<Endpoint> blocked = new HashSet<>();
        int guard = 0;
        while (guard++ < 10_000) {
            List<Endpoint> group = priorityGroup(level, sinks, blocked, true);
            if (group.isEmpty()) {
                return;
            }
            boolean moved = false;
            for (int attempt = 0; attempt < group.size(); attempt++) {
                Endpoint sink = group.get(Math.floorMod(network.itemCursor + attempt, group.size()));
                if (moveOneItem(level, network, sink, budget)) {
                    network.itemCursor = Math.floorMod(network.itemCursor + attempt + 1, group.size());
                    moved = true;
                    break;
                }
                blocked.add(sink);
            }
            if (!moved) {
                return;
            }
        }
    }

    private boolean moveOneItem(ServerLevel level, Network network, Endpoint sink, Map<BlockPos, Integer> budget) {
        List<Endpoint> sources = live(level, network, true, TransferMode.SEND);
        if (sources.isEmpty()) {
            return false;
        }
        for (int i = 0; i < sources.size(); i++) {
            Endpoint source = sources.get(Math.floorMod(network.sourceCursor + i, sources.size()));
            if (source.machine.equals(sink.machine)) {
                continue;
            }
            List<BlockPos> path = network.path(source.cable, sink.cable);
            if (path == null || min(budget, path) < 1) {
                continue;
            }
            ResourceHandler<ItemResource> from = items(level, source);
            ResourceHandler<ItemResource> to = items(level, sink);
            FaceConfig sourceFace = face(level, source);
            FaceConfig sinkFace = face(level, sink);
            if (from == null || to == null || sourceFace == null || sinkFace == null) {
                continue;
            }
            for (int slot = 0; slot < from.size(); slot++) {
                ItemResource resource = from.getResource(slot);
                if (resource.isEmpty() || from.getAmountAsLong(slot) <= 0) {
                    continue;
                }
                ItemStack probe = new ItemStack(resource.getItem());
                if (!sourceFace.allowsItem(probe) || !sinkFace.allowsItem(probe)) {
                    continue;
                }
                int moved = transferItem(from, slot, resource, to);
                if (moved > 0) {
                    consume(budget, path, moved);
                    network.sourceCursor = Math.floorMod(network.sourceCursor + i + 1, sources.size());
                    return true;
                }
            }
        }
        return false;
    }

    private void moveEnergy(ServerLevel level, Network network, Map<BlockPos, Integer> budget) {
        List<Endpoint> faces = powerFaces(level, network);
        // A machine that already sent or received this tick cannot reverse that move. Otherwise a
        // heavy cable spends the rest of its budget pushing the same FE back and forth.
        Set<BlockPos> drained = new HashSet<>();
        Set<BlockPos> filled = new HashSet<>();
        int guard = 0;
        while (guard++ < Math.max(4, faces.size() * 4)) {
            boolean moved = false;
            for (int attempt = 0; attempt < faces.size(); attempt++) {
                Endpoint source = faces.get(Math.floorMod(network.energyCursor + attempt, faces.size()));
                if (filled.contains(source.machine)) {
                    continue;
                }
                EnergyHandler from = energy(level, source);
                if (from == null || from.getAmountAsLong() <= 0) {
                    continue;
                }
                Endpoint sink = lowestSink(level, network, faces, source, from.getAmountAsLong(), budget, drained);
                if (sink == null) {
                    continue;
                }
                List<BlockPos> path = network.path(source.cable, sink.cable);
                EnergyHandler to = energy(level, sink);
                int room = path == null ? 0 : min(budget, path);
                if (to == null || room <= 0) {
                    continue;
                }
                long gap = from.getAmountAsLong() - to.getAmountAsLong();
                int amount = (int) Math.min(room, gap / 2);
                int movedAmount = transferEnergy(from, to, amount);
                if (movedAmount > 0) {
                    consume(budget, path, movedAmount);
                    drained.add(source.machine);
                    filled.add(sink.machine);
                    network.energyCursor = Math.floorMod(network.energyCursor + attempt + 1, faces.size());
                    moved = true;
                    break;
                }
            }
            if (!moved) {
                return;
            }
        }
    }

    private Endpoint lowestSink(ServerLevel level, Network network, List<Endpoint> faces, Endpoint source, long sourceEnergy, Map<BlockPos, Integer> budget, Set<BlockPos> drained) {
        Endpoint best = null;
        long lowest = Long.MAX_VALUE;
        for (Endpoint sink : faces) {
            if (sink.machine.equals(source.machine) || drained.contains(sink.machine)) {
                continue;
            }
            EnergyHandler handler = energy(level, sink);
            List<BlockPos> path = network.path(source.cable, sink.cable);
            if (handler == null || path == null || handler.getAmountAsLong() >= sourceEnergy || min(budget, path) <= 0) {
                continue;
            }
            if (handler.getAmountAsLong() < lowest) {
                best = sink;
                lowest = handler.getAmountAsLong();
            }
        }
        return best;
    }

    private void moveFluid(ServerLevel level, Network network, Map<BlockPos, Integer> budget) {
        List<Endpoint> sinks = live(level, network, false, TransferMode.RECEIVE);
        List<Endpoint> sources = live(level, network, false, TransferMode.SEND);
        if (sinks.isEmpty() || sources.isEmpty()) {
            return;
        }
        List<Integer> priorities = new ArrayList<>();
        for (Endpoint sink : sinks) {
            FaceConfig config = face(level, sink);
            if (config != null && !priorities.contains(config.fluidPriority)) {
                priorities.add(config.fluidPriority);
            }
        }
        priorities.sort(Comparator.reverseOrder());
        for (int priority : priorities) {
            List<Endpoint> group = new ArrayList<>();
            for (Endpoint sink : sinks) {
                FaceConfig config = face(level, sink);
                if (config != null && config.fluidPriority == priority) {
                    group.add(sink);
                }
            }
            for (int attempt = 0; attempt < group.size(); attempt++) {
                Endpoint sink = group.get(Math.floorMod(network.fluidCursor + attempt, group.size()));
                if (pushFluid(level, network, sources, sink, budget)) {
                    network.fluidCursor = Math.floorMod(network.fluidCursor + attempt + 1, group.size());
                    return;
                }
            }
        }
    }

    private boolean pushFluid(ServerLevel level, Network network, List<Endpoint> sources, Endpoint sink, Map<BlockPos, Integer> budget) {
        FaceConfig sinkFace = face(level, sink);
        ResourceHandler<FluidResource> to = fluids(level, sink);
        if (sinkFace == null || to == null) {
            return false;
        }
        for (Endpoint source : sources) {
            if (source.machine.equals(sink.machine)) {
                continue;
            }
            List<BlockPos> path = network.path(source.cable, sink.cable);
            int room = path == null ? 0 : min(budget, path);
            FaceConfig sourceFace = face(level, source);
            ResourceHandler<FluidResource> from = fluids(level, source);
            if (sourceFace == null || from == null || room <= 0) {
                continue;
            }
            for (int slot = 0; slot < from.size(); slot++) {
                FluidResource resource = from.getResource(slot);
                if (resource.isEmpty() || from.getAmountAsLong(slot) <= 0) {
                    continue;
                }
                if (!sourceFace.allowsFluid(resource.getFluid()) || !sinkFace.allowsFluid(resource.getFluid())) {
                    continue;
                }
                int moved = transferFluid(from, slot, resource, to, room);
                if (moved > 0) {
                    consume(budget, path, moved);
                    return true;
                }
            }
        }
        return false;
    }

    private static int transferItem(ResourceHandler<ItemResource> from, int slot, ItemResource resource, ResourceHandler<ItemResource> to) {
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = from.extract(slot, resource, 1, transaction);
            if (extracted <= 0) {
                return 0;
            }
            int inserted = to.insert(resource, extracted, transaction);
            if (inserted == extracted) {
                transaction.commit();
                return inserted;
            }
            return 0;
        }
    }

    private static int transferEnergy(EnergyHandler from, EnergyHandler to, int amount) {
        if (amount <= 0) {
            return 0;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = from.extract(amount, transaction);
            int inserted = extracted <= 0 ? 0 : to.insert(extracted, transaction);
            if (inserted == extracted && inserted > 0) {
                transaction.commit();
                return inserted;
            }
            return 0;
        }
    }

    private static int transferFluid(ResourceHandler<FluidResource> from, int slot, FluidResource resource, ResourceHandler<FluidResource> to, int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = from.extract(slot, resource, amount, transaction);
            int inserted = extracted <= 0 ? 0 : to.insert(resource, extracted, transaction);
            if (inserted == extracted && inserted > 0) {
                transaction.commit();
                return inserted;
            }
            return 0;
        }
    }

    private static List<Endpoint> live(ServerLevel level, Network network, boolean items, TransferMode mode) {
        List<Endpoint> result = new ArrayList<>();
        for (Endpoint endpoint : network.endpoints) {
            FaceConfig config = face(level, endpoint);
            if (config == null) {
                continue;
            }
            boolean match = items ? config.itemModule && config.itemMode == mode : config.fluidModule && config.fluidMode == mode;
            if (match) {
                result.add(endpoint);
            }
        }
        return result;
    }

    private static List<Endpoint> powerFaces(ServerLevel level, Network network) {
        List<Endpoint> result = new ArrayList<>();
        for (Endpoint endpoint : network.endpoints) {
            FaceConfig config = face(level, endpoint);
            if (config != null && config.powerModule) {
                result.add(endpoint);
            }
        }
        return result;
    }

    private static List<Endpoint> priorityGroup(ServerLevel level, List<Endpoint> sinks, Set<Endpoint> blocked, boolean items) {
        int best = Integer.MIN_VALUE;
        List<Endpoint> group = new ArrayList<>();
        for (Endpoint sink : sinks) {
            if (blocked.contains(sink)) {
                continue;
            }
            FaceConfig config = face(level, sink);
            if (config == null) {
                continue;
            }
            int priority = items ? config.itemPriority : config.fluidPriority;
            if (priority > best) {
                best = priority;
                group.clear();
            }
            if (priority == best) {
                group.add(sink);
            }
        }
        return group;
    }

    private static int min(Map<BlockPos, Integer> budget, List<BlockPos> path) {
        int value = Integer.MAX_VALUE;
        for (BlockPos pos : path) {
            value = Math.min(value, budget.getOrDefault(pos, 0));
        }
        return value == Integer.MAX_VALUE ? 0 : value;
    }

    private static void consume(Map<BlockPos, Integer> budget, List<BlockPos> path, int amount) {
        for (BlockPos pos : path) {
            budget.merge(pos, -amount, Integer::sum);
        }
    }

    private static CableBlockEntity entity(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof CableBlockEntity cable ? cable : null;
    }

    private static FaceConfig face(ServerLevel level, Endpoint endpoint) {
        CableBlockEntity cable = entity(level, endpoint.cable);
        return cable == null ? null : cable.face(endpoint.face);
    }

    private static ResourceHandler<ItemResource> items(ServerLevel level, Endpoint endpoint) {
        return level.getCapability(Capabilities.Item.BLOCK, endpoint.machine, endpoint.face.getOpposite());
    }

    private static ResourceHandler<FluidResource> fluids(ServerLevel level, Endpoint endpoint) {
        return level.getCapability(Capabilities.Fluid.BLOCK, endpoint.machine, endpoint.face.getOpposite());
    }

    private static EnergyHandler energy(ServerLevel level, Endpoint endpoint) {
        return level.getCapability(Capabilities.Energy.BLOCK, endpoint.machine, endpoint.face.getOpposite());
    }

    private record Endpoint(BlockPos cable, Direction face, BlockPos machine) {
    }

    private static final class Network {
        private final Set<BlockPos> cables;
        private final Map<BlockPos, Set<BlockPos>> neighbors;
        private final List<Endpoint> endpoints;
        private final Map<BlockPos, Map<BlockPos, List<BlockPos>>> paths = new HashMap<>();
        private int itemCursor;
        private int sourceCursor;
        private int energyCursor;
        private int fluidCursor;

        private Network(Set<BlockPos> cables, Map<BlockPos, Set<BlockPos>> neighbors, List<Endpoint> endpoints) {
            this.cables = cables;
            this.neighbors = neighbors;
            this.endpoints = endpoints;
        }

        private void cachePaths(ServerLevel level) {
            Set<BlockPos> nodes = new HashSet<>();
            for (Endpoint endpoint : endpoints) {
                nodes.add(endpoint.cable);
            }
            for (BlockPos from : nodes) {
                for (BlockPos to : nodes) {
                    paths.computeIfAbsent(from, ignored -> new HashMap<>()).put(to, widest(level, from, to));
                }
            }
        }

        private List<BlockPos> path(BlockPos from, BlockPos to) {
            if (from.equals(to)) {
                return List.of(from);
            }
            Map<BlockPos, List<BlockPos>> fromPaths = paths.get(from);
            return fromPaths == null ? null : fromPaths.get(to);
        }

        private List<BlockPos> widest(ServerLevel level, BlockPos start, BlockPos goal) {
            if (start.equals(goal)) {
                return List.of(start);
            }
            record State(BlockPos pos, int bottleneck, int distance) {
            }
            Map<BlockPos, Integer> best = new HashMap<>();
            Map<BlockPos, BlockPos> parent = new HashMap<>();
            PriorityQueue<State> queue = new PriorityQueue<>(Comparator.<State>comparingInt(State::bottleneck).reversed().thenComparingInt(State::distance));
            best.put(start, rank(level, start));
            queue.add(new State(start, best.get(start), 0));
            while (!queue.isEmpty()) {
                State current = queue.poll();
                if (current.bottleneck < best.getOrDefault(current.pos, -1)) {
                    continue;
                }
                if (current.pos.equals(goal)) {
                    return reconstruct(parent, start, goal);
                }
                for (BlockPos next : neighbors.getOrDefault(current.pos, Set.of())) {
                    int bottleneck = Math.min(current.bottleneck, rank(level, next));
                    if (bottleneck > best.getOrDefault(next, -1)) {
                        best.put(next, bottleneck);
                        parent.put(next, current.pos);
                        queue.add(new State(next, bottleneck, current.distance + 1));
                    }
                }
            }
            return null;
        }

        private static List<BlockPos> reconstruct(Map<BlockPos, BlockPos> parent, BlockPos start, BlockPos goal) {
            List<BlockPos> path = new ArrayList<>();
            BlockPos cursor = goal;
            while (cursor != null && path.size() <= parent.size() + 1) {
                path.add(cursor);
                if (cursor.equals(start)) {
                    java.util.Collections.reverse(path);
                    return path;
                }
                cursor = parent.get(cursor);
            }
            return null;
        }

        private static int rank(ServerLevel level, BlockPos pos) {
            return entity(level, pos) instanceof CableBlockEntity cable ? cable.tier().rank() : 0;
        }
    }
}
