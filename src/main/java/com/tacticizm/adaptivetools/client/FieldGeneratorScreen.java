package com.tacticizm.adaptivetools.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import com.tacticizm.adaptivetools.wireless.FieldGeneratorMenu;

/** Field generator UI drawn on the base generator background. There is no separate sheet. */
public class FieldGeneratorScreen extends AbstractContainerScreen<FieldGeneratorMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("adaptivetools", "textures/gui/container/base_generator.png");

    public FieldGeneratorScreen(FieldGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 176);
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal(menu.on() ? "On" : "Off"), button -> click(FieldGeneratorMenu.TOGGLE))
                .bounds(this.leftPos + 150, this.topPos + 22, 18, 16).build());
        if (menu.advanced()) {
            addRenderableWidget(Button.builder(Component.literal(menu.hotbar() ? "H" : "h"), button -> click(FieldGeneratorMenu.HOTBAR))
                    .bounds(this.leftPos + 80, this.topPos + 40, 18, 16).build());
            addRenderableWidget(Button.builder(Component.literal(menu.main() ? "M" : "m"), button -> click(FieldGeneratorMenu.MAIN))
                    .bounds(this.leftPos + 100, this.topPos + 40, 18, 16).build());
            addRenderableWidget(Button.builder(Component.literal(menu.armorArea() ? "A" : "a"), button -> click(FieldGeneratorMenu.ARMOR))
                    .bounds(this.leftPos + 120, this.topPos + 40, 18, 16).build());
        }
    }

    private void click(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
        rebuildWidgets();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, 176, 176, 256, 256);
        for (var renderable : this.renderables) {
            renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        this.extractContents(graphics, mouseX, mouseY, partialTick);
        graphics.text(this.font, this.title, this.leftPos + 8, this.topPos + 6, 0xFF2A2A32, false);
        if (mouseX >= this.leftPos + 62 && mouseX <= this.leftPos + 74 && mouseY >= this.topPos + 20 && mouseY <= this.topPos + 72) {
            graphics.text(this.font, Component.literal(menu.energy() + " / " + menu.capacity() + " FE"), this.leftPos + 80, this.topPos + 24, 0xFFE8E8E8, true);
            graphics.text(this.font, Component.literal(menu.rate() + " FE/t"), this.leftPos + 80, this.topPos + 36, 0xFFE8E8E8, true);
        }
    }
}
