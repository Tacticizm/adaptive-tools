package com.tacticizm.adaptivetools.wireless;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import com.tacticizm.adaptivetools.AdaptiveTools;

/** Channels live on the overworld save so every dimension shares them. */
public final class ChannelDirectory extends SavedData {
    private static final Codec<Saved> SAVED_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(Saved::id),
            Codec.STRING.fieldOf("name").forGetter(Saved::name),
            Codec.INT.fieldOf("kind").forGetter(Saved::kind),
            Codec.STRING.fieldOf("owner").forGetter(Saved::owner),
            Codec.STRING.fieldOf("owner_name").forGetter(Saved::ownerName),
            Codec.STRING.listOf().fieldOf("permitted").forGetter(Saved::permitted)
    ).apply(instance, Saved::new));

    public static final Codec<ChannelDirectory> CODEC = SAVED_CODEC.listOf().xmap(ChannelDirectory::from, ChannelDirectory::toSaved);
    public static final SavedDataType<ChannelDirectory> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "channels"),
            ChannelDirectory::new,
            CODEC);

    private final Map<UUID, WirelessChannel> channels = new LinkedHashMap<>();

    public ChannelDirectory() {}

    public static ChannelDirectory get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public WirelessChannel create(ServerPlayer owner, String name, ChannelKind kind) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            trimmed = "Channel";
        }
        if (trimmed.length() > 32) {
            trimmed = trimmed.substring(0, 32);
        }
        WirelessChannel channel = new WirelessChannel(UUID.randomUUID(), trimmed, kind, owner.getUUID(), owner.getName().getString());
        channels.put(channel.id, channel);
        setDirty();
        return channel;
    }

    public WirelessChannel get(UUID id) {
        return id == null ? null : channels.get(id);
    }

    public Collection<WirelessChannel> all() {
        return channels.values();
    }

    public boolean permit(WirelessChannel channel, UUID player) {
        if (channel == null || player == null || channel.owner.equals(player)) {
            return false;
        }
        boolean added = channel.permitted.add(player);
        if (added) {
            setDirty();
        }
        return added;
    }

    public boolean revoke(WirelessChannel channel, UUID player) {
        if (channel == null || player == null) {
            return false;
        }
        boolean removed = channel.permitted.remove(player);
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public int delete(ServerLevel level, WirelessChannel channel) {
        if (channel == null || channels.remove(channel.id) == null) {
            return 0;
        }
        setDirty();
        return WirelessDevices.unassign(level.getServer(), channel.id);
    }

    private static ChannelDirectory from(List<Saved> saved) {
        ChannelDirectory directory = new ChannelDirectory();
        for (Saved row : saved) {
            WirelessChannel channel = new WirelessChannel(UUID.fromString(row.id), row.name, ChannelKind.byOrdinal(row.kind), UUID.fromString(row.owner), row.ownerName);
            for (String permitted : row.permitted) {
                channel.permitted.add(UUID.fromString(permitted));
            }
            directory.channels.put(channel.id, channel);
        }
        return directory;
    }

    private List<Saved> toSaved() {
        List<Saved> saved = new ArrayList<>();
        for (WirelessChannel channel : channels.values()) {
            List<String> permitted = new ArrayList<>();
            for (UUID id : channel.permitted) {
                permitted.add(id.toString());
            }
            saved.add(new Saved(channel.id.toString(), channel.name, channel.kind.ordinal(), channel.owner.toString(), channel.ownerName, permitted));
        }
        return saved;
    }

    private record Saved(String id, String name, int kind, String owner, String ownerName, List<String> permitted) {}
}
