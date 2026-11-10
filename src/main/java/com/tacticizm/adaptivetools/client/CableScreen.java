package com.tacticizm.adaptivetools.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import com.tacticizm.adaptivetools.cable.CableMenu;
import com.tacticizm.adaptivetools.cable.TransferMode;

/** Config screen drawn on the approved cable_config.png. */
public class CableScreen extends AbstractContainerScreen<CableMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("adaptivetools", "textures/gui/cable_config.png");

    public CableScreen(CableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 256, 256);
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < 6; i++) {
            addRenderableWidget(button(12 + i * 28, 24, 24, 16, Component.literal(faceLabel(Direction.from3DDataValue(i))), i));
        }
        addRenderableWidget(button(12, 57, 18, 16, Component.literal("P"), CableMenu.POWER_INSTALL));
        addRenderableWidget(button(12, 87, 18, 16, Component.literal("I"), CableMenu.ITEM_INSTALL));
        addRenderableWidget(button(40, 87, 32, 16, Component.literal("Off"), CableMenu.ITEM_MODE_BASE));
        addRenderableWidget(button(76, 87, 32, 16, Component.literal("Out"), CableMenu.ITEM_MODE_BASE + 1));
        addRenderableWidget(button(112, 87, 32, 16, Component.literal("In"), CableMenu.ITEM_MODE_BASE + 2));
        addRenderableWidget(button(12, 117, 18, 16, Component.literal("F"), CableMenu.FLUID_INSTALL));
        addRenderableWidget(button(40, 117, 32, 16, Component.literal("Off"), CableMenu.FLUID_MODE_BASE));
        addRenderableWidget(button(76, 117, 32, 16, Component.literal("Out"), CableMenu.FLUID_MODE_BASE + 1));
        addRenderableWidget(button(112, 117, 32, 16, Component.literal("In"), CableMenu.FLUID_MODE_BASE + 2));
        addRenderableWidget(button(160, 87, 42, 16, Component.literal("Rate+"), CableMenu.RATE_ADD));
        addRenderableWidget(button(204, 87, 42, 16, Component.literal("Rate-"), CableMenu.RATE_REMOVE));
        addRenderableWidget(button(160, 117, 22, 16, Component.literal("I-"), CableMenu.ITEM_PRIORITY_DOWN));
        addRenderableWidget(button(184, 117, 22, 16, Component.literal("I+"), CableMenu.ITEM_PRIORITY_UP));
        addRenderableWidget(button(208, 117, 22, 16, Component.literal("F-"), CableMenu.FLUID_PRIORITY_DOWN));
        addRenderableWidget(button(232, 117, 22, 16, Component.literal("F+"), CableMenu.FLUID_PRIORITY_UP));
        addRenderableWidget(button(160, 57, 86, 16, Component.literal("Reset"), CableMenu.RESET));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, 256, 256, 256, 256);
        for (var renderable : this.renderables) {
            renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        this.extractContents(graphics, mouseX, mouseY, partialTick);
        CableMenu menu = this.menu;
        String face = faceLabel(menu.selected()) + (menu.disconnected() ? "  disconnected" : "");
        graphics.text(this.font, Component.literal(face), this.leftPos + 12, this.topPos + 6, 0xFF2A2A32, false);
        graphics.text(this.font, Component.literal(menu.power() ? "Power on" : "Power off"), this.leftPos + 78, this.topPos + 61, 0xFF3A2A10, false);
        graphics.text(this.font, Component.literal("Items " + label(menu.itemMode()) + "  p" + menu.itemPriority()), this.leftPos + 12, this.topPos + 148, 0xFF204020, false);
        graphics.text(this.font, Component.literal("Fluids " + label(menu.fluidMode()) + "  p" + menu.fluidPriority() + "  rate " + menu.rateModules() + "/2"), this.leftPos + 12, this.topPos + 240, 0xFF203050, false);
        if (menu.resetArmed()) {
            graphics.text(this.font, Component.literal("Click Reset again"), this.leftPos + 160, this.topPos + 46, 0xFF8A2010, false);
        }
    }

    private Button button(int x, int y, int width, int height, Component label, int id) {
        return Button.builder(label, ignored -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
            }
        }).bounds(this.leftPos + x, this.topPos + y, width, height).build();
    }

    private static String faceLabel(Direction direction) {
        return switch (direction) {
            case DOWN -> "Down";
            case UP -> "Up";
            case NORTH -> "North";
            case SOUTH -> "South";
            case WEST -> "West";
            case EAST -> "East";
        };
    }

    private static String label(TransferMode mode) {
        return switch (mode) {
            case DISABLED -> "off";
            case SEND -> "send";
            case RECEIVE -> "receive";
        };
    }
}
