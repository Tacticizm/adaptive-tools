package com.tacticizm.adaptivetools.wireless;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import com.tacticizm.adaptivetools.AdaptiveTools;

/** Channel screens talk to the server with a few small packets. The handler stays off the client classes on a dedicated server. */
public final class WirelessMessages {
    private WirelessMessages() {}

    public record CreateChannelPayload(String name, int kind, BlockPos pos, boolean carried) implements CustomPacketPayload {
        public static final Type<CreateChannelPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "create_channel"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CreateChannelPayload> STREAM = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, CreateChannelPayload::name,
                ByteBufCodecs.VAR_INT, CreateChannelPayload::kind,
                BlockPos.STREAM_CODEC, CreateChannelPayload::pos,
                ByteBufCodecs.BOOL, CreateChannelPayload::carried,
                CreateChannelPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AssignChannelPayload(String channel, BlockPos pos, boolean carried, InteractionHand hand) implements CustomPacketPayload {
        public static final Type<AssignChannelPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "assign_channel"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AssignChannelPayload> STREAM = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, AssignChannelPayload::channel,
                BlockPos.STREAM_CODEC, AssignChannelPayload::pos,
                ByteBufCodecs.BOOL, AssignChannelPayload::carried,
                InteractionHand.STREAM_CODEC, AssignChannelPayload::hand,
                AssignChannelPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DeleteChannelPayload(String channel, boolean confirm) implements CustomPacketPayload {
        public static final Type<DeleteChannelPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "delete_channel"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DeleteChannelPayload> STREAM = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, DeleteChannelPayload::channel,
                ByteBufCodecs.BOOL, DeleteChannelPayload::confirm,
                DeleteChannelPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PermitPayload(String channel, String playerName, boolean revoke) implements CustomPacketPayload {
        public static final Type<PermitPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "permit_channel"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PermitPayload> STREAM = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, PermitPayload::channel,
                ByteBufCodecs.STRING_UTF8, PermitPayload::playerName,
                ByteBufCodecs.BOOL, PermitPayload::revoke,
                PermitPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ChannelDumpPayload(String body) implements CustomPacketPayload {
        public static final Type<ChannelDumpPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "channel_dump"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ChannelDumpPayload> STREAM = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ChannelDumpPayload::body,
                ChannelDumpPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("4");
        registrar.playToServer(CreateChannelPayload.TYPE, CreateChannelPayload.STREAM, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                create(player, payload);
            }
        });
        registrar.playToServer(AssignChannelPayload.TYPE, AssignChannelPayload.STREAM, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                assign(player, payload);
            }
        });
        registrar.playToServer(DeleteChannelPayload.TYPE, DeleteChannelPayload.STREAM, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                delete(player, payload);
            }
        });
        registrar.playToServer(PermitPayload.TYPE, PermitPayload.STREAM, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                permit(player, payload);
            }
        });
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            registerClient(registrar);
        } else {
            registrar.playToClient(ChannelDumpPayload.TYPE, ChannelDumpPayload.STREAM, (payload, context) -> {});
        }
    }

    private static void registerClient(PayloadRegistrar registrar) {
        registrar.playToClient(ChannelDumpPayload.TYPE, ChannelDumpPayload.STREAM, (payload, context) -> context.enqueueWork(() -> com.tacticizm.adaptivetools.client.ChannelScreens.accept(payload.body())));
    }

    public static void openChannels(ServerPlayer player, InteractionHand hand) {
        sendList(player, hand == InteractionHand.MAIN_HAND ? BlockPos.ZERO : BlockPos.ZERO, true);
    }

    public static void sendChannels(ServerPlayer player, TransmitterBlockEntity transmitter) {
        sendList(player, transmitter.getBlockPos(), false);
    }

    public static void sendChannels(ServerPlayer player, ReceiverBlockEntity receiver) {
        sendList(player, receiver.getBlockPos(), false);
    }

    private static void sendList(ServerPlayer player, BlockPos pos, boolean carried) {
        StringBuilder body = new StringBuilder();
        body.append(carried ? "carried" : "block").append('\n');
        body.append(pos.getX()).append(',').append(pos.getY()).append(',').append(pos.getZ()).append('\n');
        ChannelDirectory directory = ChannelDirectory.get(player.level());
        for (WirelessChannel channel : directory.all()) {
            if (!channel.allows(player.getUUID())) {
                continue;
            }
            body.append(channel.id).append('|').append(channel.name.replace('|', ' ')).append('|').append(channel.ownerName.replace('|', ' ')).append('|').append(channel.kind.ordinal()).append('\n');
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new ChannelDumpPayload(body.toString()));
    }

    private static void create(ServerPlayer player, CreateChannelPayload payload) {
        ChannelDirectory directory = ChannelDirectory.get(player.level());
        WirelessChannel channel = directory.create(player, payload.name(), ChannelKind.byOrdinal(payload.kind()));
        if (payload.carried()) {
            ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!(stack.getItem() instanceof CarriedReceiverItem)) {
                stack = player.getItemInHand(InteractionHand.OFF_HAND);
            }
            if (stack.getItem() instanceof CarriedReceiverItem) {
                CarriedReceiverItem.assign(stack, channel.id);
            }
        } else if (player.level().getBlockEntity(payload.pos()) instanceof TransmitterBlockEntity transmitter && channel.allows(player.getUUID())) {
            transmitter.assign(channel.id);
            transmitter.setOwner(player.getUUID());
        } else if (player.level().getBlockEntity(payload.pos()) instanceof ReceiverBlockEntity receiver && channel.allows(player.getUUID())) {
            receiver.assign(channel.id);
        }
        sendList(player, payload.pos(), payload.carried());
    }

    private static void assign(ServerPlayer player, AssignChannelPayload payload) {
        UUID id = payload.channel().isEmpty() ? null : UUID.fromString(payload.channel());
        WirelessChannel channel = id == null ? null : ChannelDirectory.get(player.level()).get(id);
        if (channel != null && !channel.allows(player.getUUID())) {
            return;
        }
        if (payload.carried()) {
            ItemStack stack = player.getItemInHand(payload.hand());
            if (stack.getItem() instanceof CarriedReceiverItem && (channel == null || channel.kind == ChannelKind.POWER)) {
                CarriedReceiverItem.assign(stack, id);
            }
            return;
        }
        if (player.level().getBlockEntity(payload.pos()) instanceof TransmitterBlockEntity transmitter) {
            transmitter.assign(id);
        } else if (player.level().getBlockEntity(payload.pos()) instanceof ReceiverBlockEntity receiver) {
            receiver.assign(id);
        }
    }

    private static void delete(ServerPlayer player, DeleteChannelPayload payload) {
        ChannelDirectory directory = ChannelDirectory.get((ServerLevel) player.level());
        WirelessChannel channel = directory.get(UUID.fromString(payload.channel()));
        if (channel == null || !channel.owner.equals(player.getUUID())) {
            return;
        }
        int count = WirelessDevices.count(player.level().getServer(), channel.id);
        if (!payload.confirm()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Delete " + channel.name + "? " + count + " devices return to Unassigned."));
            return;
        }
        directory.delete(player.level(), channel);
    }

    private static void permit(ServerPlayer player, PermitPayload payload) {
        ChannelDirectory directory = ChannelDirectory.get(player.level());
        WirelessChannel channel = directory.get(UUID.fromString(payload.channel()));
        if (channel == null || !channel.owner.equals(player.getUUID())) {
            return;
        }
        ServerPlayer other = player.level().getServer().getPlayerList().getPlayerByName(payload.playerName());
        if (other == null) {
            return;
        }
        if (payload.revoke()) {
            directory.revoke(channel, other.getUUID());
        } else {
            directory.permit(channel, other.getUUID());
        }
    }
}
