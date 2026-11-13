package com.tacticizm.adaptivetools.wireless;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Moves power, items, and fluids between loaded devices on the same channel.
 * Same-dimension transfers need both ends loaded. Cross-dimension transfers also need a loaded
 * transmitter on that channel with a Cross-dimension module. Item and fluid moves do not spend FE.
 */
public final class WirelessNetwork {
    public static final int FE_PER_TICK = 1_000;
    public static final int ITEMS_PER_TICK = 1;
    public static final int MB_PER_TICK = 1_000;

    private WirelessNetwork() {}

    public static void tick(MinecraftServer server) {
        for (TransmitterBlockEntity transmitter : WirelessDevices.TRANSMITTERS) {
            transmitter.forgetMissingChannel();
            transmitter.lastSent = transmitter.sentThisTick;
            transmitter.sentThisTick = 0;
        }
        for (ReceiverBlockEntity receiver : WirelessDevices.RECEIVERS) {
            receiver.beginTick();
        }
        movePower(server);
        moveItems();
        moveFluids();
        chargeCarried(server);
        for (ReceiverBlockEntity receiver : WirelessDevices.RECEIVERS) {
            receiver.endTick();
        }
    }

    private static void movePower(MinecraftServer server) {
        for (ReceiverBlockEntity receiver : List.copyOf(WirelessDevices.RECEIVERS)) {
            if (!(receiver.getLevel() instanceof ServerLevel level)) {
                continue;
            }
            WirelessChannel channel = channel(level, receiver.channelId(), ChannelKind.POWER);
            if (channel == null || !channel.allows(receiver.owner())) {
                continue;
            }
            EnergyHandler machine = level.getCapability(Capabilities.Energy.BLOCK, receiver.getBlockPos().relative(receiver.facing()), receiver.facing().getOpposite());
            if (machine == null) {
                continue;
            }
            int want = FE_PER_TICK;
            for (TransmitterBlockEntity transmitter : WirelessDevices.TRANSMITTERS) {
                if (want <= 0) {
                    break;
                }
                if (!receiver.channelId().equals(transmitter.channelId()) || !(transmitter.getLevel() instanceof ServerLevel from)) {
                    continue;
                }
                if (!channel.allows(transmitter.owner()) || !crossOk(from, level, channel)) {
                    continue;
                }
                int room = FE_PER_TICK - transmitter.sentThisTick;
                int moved = EnergyMove.move(transmitter.energyHandler, machine, Math.min(want, room));
                transmitter.sentThisTick += moved;
                receiver.markMoved(moved);
                want -= moved;
            }
        }
    }

    private static void moveItems() {
        List<ReceiverBlockEntity> senders = mode(ChannelKind.ITEM, ReceiverBlockEntity.LinkMode.SEND);
        List<ReceiverBlockEntity> receivers = mode(ChannelKind.ITEM, ReceiverBlockEntity.LinkMode.RECEIVE);
        for (ReceiverBlockEntity sender : senders) {
            if (!(sender.getLevel() instanceof ServerLevel from)) {
                continue;
            }
            WirelessChannel channel = channel(from, sender.channelId(), ChannelKind.ITEM);
            if (channel == null || !channel.allows(sender.owner())) {
                continue;
            }
            ResourceHandler<ItemResource> source = from.getCapability(Capabilities.Item.BLOCK, sender.getBlockPos().relative(sender.facing()), sender.facing().getOpposite());
            if (source == null) {
                continue;
            }
            for (ReceiverBlockEntity sinkDevice : receivers) {
                if (!sender.channelId().equals(sinkDevice.channelId()) || !(sinkDevice.getLevel() instanceof ServerLevel to)) {
                    continue;
                }
                if (!channel.allows(sinkDevice.owner()) || !crossOk(from, to, channel)) {
                    continue;
                }
                ResourceHandler<ItemResource> sink = to.getCapability(Capabilities.Item.BLOCK, sinkDevice.getBlockPos().relative(sinkDevice.facing()), sinkDevice.facing().getOpposite());
                if (moveItem(source, sink)) {
                    sender.markMoved(1);
                    sinkDevice.markMoved(1);
                    break;
                }
            }
        }
    }

    private static void moveFluids() {
        List<ReceiverBlockEntity> senders = mode(ChannelKind.FLUID, ReceiverBlockEntity.LinkMode.SEND);
        List<ReceiverBlockEntity> receivers = mode(ChannelKind.FLUID, ReceiverBlockEntity.LinkMode.RECEIVE);
        for (ReceiverBlockEntity sender : senders) {
            if (!(sender.getLevel() instanceof ServerLevel from)) {
                continue;
            }
            WirelessChannel channel = channel(from, sender.channelId(), ChannelKind.FLUID);
            if (channel == null || !channel.allows(sender.owner())) {
                continue;
            }
            ResourceHandler<FluidResource> source = from.getCapability(Capabilities.Fluid.BLOCK, sender.getBlockPos().relative(sender.facing()), sender.facing().getOpposite());
            if (source == null) {
                continue;
            }
            for (ReceiverBlockEntity sinkDevice : receivers) {
                if (!sender.channelId().equals(sinkDevice.channelId()) || !(sinkDevice.getLevel() instanceof ServerLevel to)) {
                    continue;
                }
                if (!channel.allows(sinkDevice.owner()) || !crossOk(from, to, channel)) {
                    continue;
                }
                ResourceHandler<FluidResource> sink = to.getCapability(Capabilities.Fluid.BLOCK, sinkDevice.getBlockPos().relative(sinkDevice.facing()), sinkDevice.facing().getOpposite());
                int moved = moveFluid(source, sink, MB_PER_TICK);
                if (moved > 0) {
                    sender.markMoved(moved);
                    sinkDevice.markMoved(moved);
                    break;
                }
            }
        }
    }

    private static void chargeCarried(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel level)) {
                continue;
            }
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack carried = player.getInventory().getItem(slot);
                if (!(carried.getItem() instanceof CarriedReceiverItem)) {
                    continue;
                }
                UUID id = CarriedReceiverItem.channel(carried);
                WirelessChannel channel = ChannelDirectory.get(level).get(id);
                if (channel == null || channel.kind != ChannelKind.POWER) {
                    if (id != null && channel == null) {
                        CarriedReceiverItem.assign(carried, null);
                    }
                    continue;
                }
                if (!channel.allows(player.getUUID())) {
                    continue;
                }
                int target = lowestCharge(player);
                if (target < 0) {
                    continue;
                }
                ItemStack charging = player.getInventory().getItem(target);
                EnergyHandler item = EnergyMove.item(charging);
                int want = FE_PER_TICK;
                for (TransmitterBlockEntity transmitter : WirelessDevices.TRANSMITTERS) {
                    if (want <= 0 || !id.equals(transmitter.channelId()) || !(transmitter.getLevel() instanceof ServerLevel from)) {
                        continue;
                    }
                    if (!channel.allows(transmitter.owner()) || !crossOk(from, level, channel)) {
                        continue;
                    }
                    int room = FE_PER_TICK - transmitter.sentThisTick;
                    int moved = EnergyMove.move(transmitter.energyHandler, item, Math.min(want, room));
                    transmitter.sentThisTick += moved;
                    want -= moved;
                }
            }
        }
    }

    /** Lowest charge percent that is not already full. -1 when nothing can charge. */
    static int lowestCharge(ServerPlayer player) {
        int best = -1;
        double bestRatio = Double.MAX_VALUE;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof CarriedReceiverItem) {
                continue;
            }
            EnergyHandler handler = EnergyMove.item(stack);
            if (handler == null || handler.getCapacityAsLong() <= 0 || handler.getAmountAsLong() >= handler.getCapacityAsLong()) {
                continue;
            }
            double ratio = handler.getAmountAsLong() / (double) handler.getCapacityAsLong();
            if (ratio < bestRatio) {
                bestRatio = ratio;
                best = slot;
            }
        }
        return best;
    }

    private static boolean crossOk(ServerLevel from, ServerLevel to, WirelessChannel channel) {
        if (from.dimension().equals(to.dimension())) {
            return true;
        }
        for (TransmitterBlockEntity transmitter : WirelessDevices.TRANSMITTERS) {
            if (channel.id.equals(transmitter.channelId()) && transmitter.hasCrossDimension() && !transmitter.isRemoved()) {
                return true;
            }
        }
        return false;
    }

    private static WirelessChannel channel(ServerLevel level, UUID id, ChannelKind kind) {
        WirelessChannel channel = ChannelDirectory.get(level).get(id);
        return channel != null && channel.kind == kind ? channel : null;
    }

    private static List<ReceiverBlockEntity> mode(ChannelKind kind, ReceiverBlockEntity.LinkMode mode) {
        List<ReceiverBlockEntity> found = new ArrayList<>();
        for (ReceiverBlockEntity receiver : WirelessDevices.RECEIVERS) {
            if (!(receiver.getLevel() instanceof ServerLevel level)) {
                continue;
            }
            WirelessChannel channel = ChannelDirectory.get(level).get(receiver.channelId());
            if (channel == null || channel.kind != kind) {
                continue;
            }
            ReceiverBlockEntity.LinkMode actual = kind == ChannelKind.ITEM ? receiver.itemMode() : receiver.fluidMode();
            if (actual == mode) {
                found.add(receiver);
            }
        }
        return found;
    }

    private static boolean moveItem(ResourceHandler<ItemResource> from, ResourceHandler<ItemResource> to) {
        if (from == null || to == null) {
            return false;
        }
        for (int slot = 0; slot < from.size(); slot++) {
            ItemResource resource = from.getResource(slot);
            if (resource.isEmpty() || from.getAmountAsLong(slot) <= 0) {
                continue;
            }
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = from.extract(slot, resource, ITEMS_PER_TICK, transaction);
                int inserted = extracted <= 0 ? 0 : to.insert(resource, extracted, transaction);
                if (inserted == extracted && inserted > 0) {
                    transaction.commit();
                    return true;
                }
            }
        }
        return false;
    }

    private static int moveFluid(ResourceHandler<FluidResource> from, ResourceHandler<FluidResource> to, int amount) {
        if (from == null || to == null) {
            return 0;
        }
        for (int slot = 0; slot < from.size(); slot++) {
            FluidResource resource = from.getResource(slot);
            if (resource.isEmpty() || from.getAmountAsLong(slot) <= 0) {
                continue;
            }
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = from.extract(slot, resource, amount, transaction);
                int inserted = extracted <= 0 ? 0 : to.insert(resource, extracted, transaction);
                if (inserted == extracted && inserted > 0) {
                    transaction.commit();
                    return inserted;
                }
            }
        }
        return 0;
    }

}
