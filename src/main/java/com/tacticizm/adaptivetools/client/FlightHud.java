package com.tacticizm.adaptivetools.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import com.tacticizm.adaptivetools.AdaptiveArmorItem;
import com.tacticizm.adaptivetools.ModEnergy;
import com.tacticizm.adaptivetools.ToolSettings;

/** Small energy readout, drawn only while the Flux chest is gliding or the Ender chest is flying. */
public final class FlightHud {
    private static final Identifier BAR = Identifier.fromNamespaceAndPath("adaptivetools", "textures/gui/hud_energy_bar.png");

    private FlightHud() {}

    public static void onRender(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof AdaptiveArmorItem armor)) {
            return;
        }
        ToolSettings settings = ToolSettings.of(chest, armor.tier());
        boolean gliding = player.isFallFlying() && armor.canElytra() && settings.elytra();
        boolean flying = player.getAbilities().flying && armor.canFly() && settings.flight();
        if (!gliding && !flying) {
            return;
        }
        int capacity = armor.tier().energyCapacity();
        int stored = ModEnergy.getStored(chest);
        int percent = capacity <= 0 ? 0 : stored * 100 / capacity;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int x = width / 2 - 32;
        int y = height - 54;
        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        graphics.blit(RenderPipelines.GUI_TEXTURED, BAR, x, y, 0.0F, 0.0F, 64, 10, 256, 256);
        int fill = capacity <= 0 ? 0 : stored * 64 / capacity;
        if (fill > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, BAR, x, y, 0.0F, 16.0F, fill, 10, 256, 256);
        }
        graphics.text(minecraft.font, Component.literal(percent + "%"), x + 66, y + 1, 0xFFE8E8E8, true);
    }
}
