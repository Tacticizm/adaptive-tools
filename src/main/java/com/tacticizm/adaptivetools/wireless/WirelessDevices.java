package com.tacticizm.adaptivetools.wireless;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Loaded devices. Unloaded ones clear a missing channel the next time they load. */
public final class WirelessDevices {
    public static final List<TransmitterBlockEntity> TRANSMITTERS = new ArrayList<>();
    public static final List<ReceiverBlockEntity> RECEIVERS = new ArrayList<>();

    private WirelessDevices() {}

    public static void add(TransmitterBlockEntity transmitter) {
        if (!TRANSMITTERS.contains(transmitter)) {
            TRANSMITTERS.add(transmitter);
        }
    }

    public static void remove(TransmitterBlockEntity transmitter) {
        TRANSMITTERS.remove(transmitter);
    }

    public static void add(ReceiverBlockEntity receiver) {
        if (!RECEIVERS.contains(receiver)) {
            RECEIVERS.add(receiver);
        }
    }

    public static void remove(ReceiverBlockEntity receiver) {
        RECEIVERS.remove(receiver);
    }

    public static int count(net.minecraft.server.MinecraftServer server, UUID channel) {
        int count = 0;
        for (TransmitterBlockEntity transmitter : TRANSMITTERS) {
            if (channel.equals(transmitter.channelId())) {
                count++;
            }
        }
        for (ReceiverBlockEntity receiver : RECEIVERS) {
            if (channel.equals(receiver.channelId())) {
                count++;
            }
        }
        count += CarriedReceiverItem.assignedCount(server, channel);
        return count;
    }

    public static int unassign(net.minecraft.server.MinecraftServer server, UUID channel) {
        int count = 0;
        for (TransmitterBlockEntity transmitter : TRANSMITTERS) {
            if (channel.equals(transmitter.channelId())) {
                transmitter.assign(null);
                count++;
            }
        }
        for (ReceiverBlockEntity receiver : RECEIVERS) {
            if (channel.equals(receiver.channelId())) {
                receiver.assign(null);
                count++;
            }
        }
        count += CarriedReceiverItem.unassign(server, channel);
        return count;
    }
}
