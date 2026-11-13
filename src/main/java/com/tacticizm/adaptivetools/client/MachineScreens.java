package com.tacticizm.adaptivetools.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import com.tacticizm.adaptivetools.wireless.GeneratorMenu;
import com.tacticizm.adaptivetools.wireless.ReceiverBlockEntity;
import com.tacticizm.adaptivetools.wireless.ReceiverMenu;
import com.tacticizm.adaptivetools.wireless.TransmitterMenu;

final class MachineScreens {
    private MachineScreens() {}

    static final class GeneratorScreen extends AbstractContainerScreen<GeneratorMenu> {
        private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("adaptivetools", "textures/gui/container/base_generator.png");

        GeneratorScreen(GeneratorMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, 176, 176);
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
            }
        }
    }

    static final class TransmitterScreen extends AbstractContainerScreen<TransmitterMenu> {
        private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("adaptivetools", "textures/gui/container/wireless_energy_transmitter.png");

        TransmitterScreen(TransmitterMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, 176, 176);
        }

        @Override
        protected void init() {
            super.init();
            addRenderableWidget(Button.builder(Component.literal("Channels"), button -> click(TransmitterMenu.OPEN_CHANNELS)).bounds(this.leftPos + 142, this.topPos + 19, 16, 14).build());
            addRenderableWidget(Button.builder(Component.literal("Remove Anyway"), button -> click(TransmitterMenu.ARM_REMOVAL)).bounds(this.leftPos + 8, this.topPos + 60, 90, 16).build());
        }

        private void click(int id) {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
            }
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, 176, 176, 256, 256);
            for (var renderable : this.renderables) {
                renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }
            this.extractContents(graphics, mouseX, mouseY, partialTick);
            graphics.text(this.font, this.title, this.leftPos + 8, this.topPos + 6, 0xFF2A2A32, false);
            if (menu.lostIfRemoved() > 0) {
                graphics.text(this.font, Component.literal("Remove loses " + menu.lostIfRemoved() + " FE"), this.leftPos + 8, this.topPos + 60, 0xFFFFC0A0, true);
            }
            if (mouseX >= this.leftPos + 160 && mouseX <= this.leftPos + 170 && mouseY >= this.topPos + 20 && mouseY <= this.topPos + 70) {
                graphics.text(this.font, Component.literal(menu.energy() + " / " + menu.capacity() + " FE"), this.leftPos + 8, this.topPos + 24, 0xFFE8E8E8, true);
            }
        }
    }

    static final class ReceiverScreen extends AbstractContainerScreen<ReceiverMenu> {
        private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("adaptivetools", "textures/gui/container/machine_receiver.png");

        ReceiverScreen(ReceiverMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, 176, 176);
        }

        @Override
        protected void init() {
            super.init();
            addRenderableWidget(Button.builder(Component.literal("Select"), button -> click(ReceiverMenu.OPEN_CHANNELS)).bounds(this.leftPos + 8, this.topPos + 132, 78, 16).build());
            addRenderableWidget(Button.builder(Component.literal("Disconnect"), button -> click(ReceiverMenu.DISCONNECT)).bounds(this.leftPos + 90, this.topPos + 132, 78, 16).build());
            addRenderableWidget(Button.builder(Component.literal("Item"), button -> click(ReceiverMenu.CYCLE_ITEM)).bounds(this.leftPos + 8, this.topPos + 108, 50, 16).build());
            addRenderableWidget(Button.builder(Component.literal("Fluid"), button -> click(ReceiverMenu.CYCLE_FLUID)).bounds(this.leftPos + 62, this.topPos + 108, 50, 16).build());
        }

        private void click(int id) {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
            }
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, 176, 176, 256, 256);
            for (var renderable : this.renderables) {
                renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }
            graphics.text(this.font, this.title, this.leftPos + 8, this.topPos + 6, 0xFF2A2A32, false);
            graphics.text(this.font, Component.literal("Item " + ReceiverBlockEntity.LinkMode.byOrdinal(menu.itemMode()).name()), this.leftPos + 8, this.topPos + 36, 0xFFE8E8E8, false);
            graphics.text(this.font, Component.literal("Fluid " + ReceiverBlockEntity.LinkMode.byOrdinal(menu.fluidMode()).name()), this.leftPos + 90, this.topPos + 36, 0xFFE8E8E8, false);
        }
    }
}
