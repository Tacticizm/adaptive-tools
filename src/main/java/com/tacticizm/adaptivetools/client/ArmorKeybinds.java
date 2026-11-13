package com.tacticizm.adaptivetools.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import com.tacticizm.adaptivetools.AdaptiveTools;
import com.tacticizm.adaptivetools.ModNetworking;
import com.tacticizm.adaptivetools.ToolSettings;

/** Optional, unbound keybinds. Each one toggles the matching worn ability. */
public final class ArmorKeybinds {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "armor"));
    private static final KeyMapping WATER = key("key.adaptivetools.water_walking");
    private static final KeyMapping ELYTRA = key("key.adaptivetools.elytra");
    private static final KeyMapping FLIGHT = key("key.adaptivetools.flight");
    private static final KeyMapping SHIELD = key("key.adaptivetools.shield");

    private ArmorKeybinds() {}

    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(WATER);
        event.register(ELYTRA);
        event.register(FLIGHT);
        event.register(SHIELD);
    }

    public static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        click(WATER, ToolSettings.WATER_WALKING);
        click(ELYTRA, ToolSettings.ELYTRA_GLIDE);
        click(FLIGHT, ToolSettings.CREATIVE_FLIGHT);
        click(SHIELD, ToolSettings.ENERGY_SHIELD);
    }

    public static String label(int bit) {
        KeyMapping mapping = mapping(bit);
        if (mapping == null || mapping.getKey().equals(InputConstants.UNKNOWN)) {
            return "Key: -";
        }
        return "Key: " + mapping.getTranslatedKeyMessage().getString();
    }

    public static void bind(int bit, InputConstants.Key key) {
        KeyMapping mapping = mapping(bit);
        if (mapping == null) {
            return;
        }
        mapping.setKey(key);
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
    }

    private static void click(KeyMapping mapping, int bit) {
        if (mapping.consumeClick()) {
            ClientPacketDistributor.sendToServer(new ModNetworking.ToggleWornAbilityPayload(bit));
        }
    }

    private static KeyMapping mapping(int bit) {
        if (bit == ToolSettings.WATER_WALKING) {
            return WATER;
        }
        if (bit == ToolSettings.ELYTRA_GLIDE) {
            return ELYTRA;
        }
        if (bit == ToolSettings.CREATIVE_FLIGHT) {
            return FLIGHT;
        }
        if (bit == ToolSettings.ENERGY_SHIELD) {
            return SHIELD;
        }
        return null;
    }

    private static KeyMapping key(String name) {
        return new KeyMapping(name, InputConstants.UNKNOWN.getValue(), CATEGORY);
    }
}
