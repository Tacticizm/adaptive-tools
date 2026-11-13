package com.tacticizm.adaptivetools;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private ModNetworking() {}

    public record UpdateToolSettingsPayload(int slot, boolean areaMining, int areaSize) implements CustomPacketPayload {
        public static final Type<UpdateToolSettingsPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "update_tool_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UpdateToolSettingsPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, UpdateToolSettingsPayload::slot,
                ByteBufCodecs.BOOL, UpdateToolSettingsPayload::areaMining,
                ByteBufCodecs.VAR_INT, UpdateToolSettingsPayload::areaSize,
                UpdateToolSettingsPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record UpdateFullToolSettingsPayload(int slot, ToolSettings settings) implements CustomPacketPayload {
        public static final Type<UpdateFullToolSettingsPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "update_full_tool_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UpdateFullToolSettingsPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, UpdateFullToolSettingsPayload::slot,
                ToolSettings.STREAM_CODEC, UpdateFullToolSettingsPayload::settings,
                UpdateFullToolSettingsPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ToggleWornAbilityPayload(int bit) implements CustomPacketPayload {
        public static final Type<ToggleWornAbilityPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "toggle_worn_ability"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ToggleWornAbilityPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ToggleWornAbilityPayload::bit,
                ToggleWornAbilityPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("3");
        registrar.playToServer(UpdateToolSettingsPayload.TYPE, UpdateToolSettingsPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                applySettings(player, payload.slot(), payload.areaMining(), payload.areaSize());
            }
        });
        registrar.playToServer(UpdateFullToolSettingsPayload.TYPE, UpdateFullToolSettingsPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                applyFullSettings(player, payload.slot(), payload.settings());
            }
        });
        registrar.playToServer(ToggleWornAbilityPayload.TYPE, ToggleWornAbilityPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                ArmorAbilities.toggleWorn(player, payload.bit());
            }
        });
    }

    public static int slotFor(Player player, net.minecraft.world.InteractionHand hand) {
        if (hand == net.minecraft.world.InteractionHand.MAIN_HAND) {
            return player.getInventory().getSelectedSlot();
        }
        return Inventory.SLOT_OFFHAND;
    }

    /**
     * Writes settings onto the stack in {@code slot}. The size is clamped to that pickaxe's tier cap,
     * so a client cannot enable a larger area than the tool allows.
     */
    public static void applySettings(Player player, int slot, boolean areaMining, int areaSize) {
        Inventory inventory = player.getInventory();
        if (slot < 0 || slot >= inventory.getContainerSize()) {
            return;
        }
        ItemStack stack = inventory.getItem(slot);
        if (!(stack.getItem() instanceof TieredTool pick)) {
            return;
        }
        ToolSettings current = ToolSettings.of(stack, pick.tier());
        stack.set(ModComponents.TOOL_SETTINGS.get(), current.withArea(areaMining, areaSize).clamped(pick.tier()));
    }

    public static void applyFullSettings(Player player, int slot, ToolSettings settings) {
        Inventory inventory = player.getInventory();
        if (slot < 0 || slot >= inventory.getContainerSize()) {
            return;
        }
        ItemStack stack = inventory.getItem(slot);
        if (!(stack.getItem() instanceof TieredTool tool)) {
            return;
        }
        stack.set(ModComponents.TOOL_SETTINGS.get(), settings.clamped(tool.tier()));
    }
}
