package com.tacticizm.adaptivetools.wireless;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** One named channel. The owner can permit other players; everyone else is refused. */
public final class WirelessChannel {
    public final UUID id;
    public String name;
    public final ChannelKind kind;
    public final UUID owner;
    public final String ownerName;
    public final Set<UUID> permitted = new HashSet<>();

    public WirelessChannel(UUID id, String name, ChannelKind kind, UUID owner, String ownerName) {
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.owner = owner;
        this.ownerName = ownerName;
    }

    public boolean allows(UUID player) {
        return player != null && (owner.equals(player) || permitted.contains(player));
    }
}
