package com.tacticizm.adaptivetools.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import com.tacticizm.adaptivetools.wireless.ChannelKind;
import com.tacticizm.adaptivetools.wireless.WirelessMessages;

/** Channel list and the create-channel screen. Both use the text-free sheets. */
public final class ChannelScreens {
    public static List<Line> lines = List.of();
    public static BlockPos pos = BlockPos.ZERO;
    public static boolean carried;

    private ChannelScreens() {}

    public static void accept(String body) {
        String[] rows = body.split("\n", -1);
        carried = rows.length > 0 && "carried".equals(rows[0]);
        if (rows.length > 1) {
            String[] xyz = rows[1].split(",");
            if (xyz.length == 3) {
                pos = new BlockPos(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
            }
        }
        List<Line> parsed = new ArrayList<>();
        for (int i = 2; i < rows.length; i++) {
            if (rows[i].isEmpty()) {
                continue;
            }
            String[] parts = rows[i].split("\\|", 4);
            if (parts.length == 4) {
                parsed.add(new Line(parts[0], parts[1], parts[2], ChannelKind.byOrdinal(Integer.parseInt(parts[3]))));
            }
        }
        lines = parsed;
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof ChannelListScreen) && !(minecraft.screen instanceof CreateChannelScreen)) {
            minecraft.setScreen(new ChannelListScreen());
        }
    }

    public record Line(String id, String name, String owner, ChannelKind kind) {}

    public static final class ChannelListScreen extends Screen {
        private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("adaptivetools", "textures/gui/container/wireless_energy_transmitter_channels.png");
        private int selected;
        private EditBox name;
        private boolean confirmDelete;

        public ChannelListScreen() {
            super(Component.literal("Channels"));
        }

        @Override
        protected void init() {
            int left = (this.width - 256) / 2;
            int top = (this.height - 176) / 2;
            addRenderableWidget(Button.builder(Component.literal("Select"), button -> select()).bounds(left + 8, top + 134, 70, 16).build());
            addRenderableWidget(Button.builder(Component.literal(confirmDelete ? "Confirm" : "Delete"), button -> delete()).bounds(left + 82, top + 134, 70, 16).build());
            addRenderableWidget(Button.builder(Component.literal("Create"), button -> this.minecraft.setScreen(new CreateChannelScreen())).bounds(left + 156, top + 134, 92, 16).build());
            name = new EditBox(this.font, left + 8, top + 154, 120, 14, Component.literal("Player"));
            addRenderableWidget(name);
            addRenderableWidget(Button.builder(Component.literal("Permit"), button -> permit(false)).bounds(left + 132, top + 152, 56, 16).build());
            addRenderableWidget(Button.builder(Component.literal("Revoke"), button -> permit(true)).bounds(left + 192, top + 152, 56, 16).build());
        }

        private void select() {
            if (lines.isEmpty()) {
                return;
            }
            Line line = lines.get(Math.min(selected, lines.size() - 1));
            ClientPacketDistributor.sendToServer(new WirelessMessages.AssignChannelPayload(line.id, pos, carried, InteractionHand.MAIN_HAND));
            onClose();
        }

        private void delete() {
            if (lines.isEmpty()) {
                return;
            }
            if (!confirmDelete) {
                confirmDelete = true;
                rebuildWidgets();
                return;
            }
            Line line = lines.get(Math.min(selected, lines.size() - 1));
            ClientPacketDistributor.sendToServer(new WirelessMessages.DeleteChannelPayload(line.id, true));
            onClose();
        }

        private void permit(boolean revoke) {
            if (lines.isEmpty() || name.getValue().isBlank()) {
                return;
            }
            Line line = lines.get(Math.min(selected, lines.size() - 1));
            ClientPacketDistributor.sendToServer(new WirelessMessages.PermitPayload(line.id, name.getValue().trim(), revoke));
        }

        @Override
        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
            int left = (this.width - 256) / 2;
            int top = (this.height - 176) / 2;
            double mouseX = event.x();
            double mouseY = event.y();
            if (mouseX >= left + 9 && mouseX <= left + 235 && mouseY >= top + 60 && mouseY <= top + 120) {
                selected = (int) ((mouseY - (top + 60)) / 12);
                return true;
            }
            return super.mouseClicked(event, doubled);
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            int left = (this.width - 256) / 2;
            int top = (this.height - 176) / 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left, top, 0.0F, 0.0F, 256, 176, 256, 256);
            graphics.text(this.font, Component.literal("Channels"), left + 8, top + 6, 0xFFE8E8E8, false);
            graphics.text(this.font, Component.literal("Name"), left + 11, top + 48, 0xFFD0C8A8, false);
            graphics.text(this.font, Component.literal("Owner"), left + 150, top + 48, 0xFFD0C8A8, false);
            for (int i = 0; i < Math.min(5, lines.size()); i++) {
                Line line = lines.get(i);
                int y = top + 62 + i * 12;
                int color = i == selected ? 0xFFFFFFA0 : 0xFFE8E8E8;
                graphics.text(this.font, Component.literal(line.kind.name().charAt(0) + " " + line.name), left + 12, y, color, false);
                graphics.text(this.font, Component.literal(line.owner), left + 150, y, color, false);
            }
            if (confirmDelete && !lines.isEmpty()) {
                graphics.text(this.font, Component.literal("Confirm deletes the channel"), left + 8, top + 36, 0xFFFFC0A0, false);
            }
            for (var renderable : this.renderables) {
                renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    public static final class CreateChannelScreen extends Screen {
        private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("adaptivetools", "textures/gui/container/wireless_energy_transmitter_create_channel.png");
        private EditBox name;
        private ChannelKind kind = ChannelKind.POWER;

        public CreateChannelScreen() {
            super(Component.literal("Create Channel"));
        }

        @Override
        protected void init() {
            int left = (this.width - 256) / 2;
            int top = (this.height - 120) / 2;
            name = new EditBox(this.font, left + 12, top + 38, 220, 12, Component.literal("Name"));
            name.setMaxLength(32);
            addRenderableWidget(name);
            addRenderableWidget(Button.builder(Component.literal(kind.name()), button -> {
                kind = ChannelKind.byOrdinal(kind.ordinal() + 1);
                button.setMessage(Component.literal(kind.name()));
            }).bounds(left + 8, top + 70, 80, 16).build());
            addRenderableWidget(Button.builder(Component.literal("Create"), button -> {
                ClientPacketDistributor.sendToServer(new WirelessMessages.CreateChannelPayload(name.getValue(), kind.ordinal(), pos, carried));
                onClose();
            }).bounds(left + 8, top + 96, 116, 16).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose()).bounds(left + 132, top + 96, 116, 16).build());
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            int left = (this.width - 256) / 2;
            int top = (this.height - 120) / 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left, top, 0.0F, 0.0F, 256, 120, 256, 256);
            graphics.text(this.font, Component.literal("Create Channel"), left + 8, top + 6, 0xFFE8E8E8, false);
            graphics.text(this.font, Component.literal("Name"), left + 8, top + 22, 0xFFD0C8A8, false);
            graphics.text(this.font, Component.literal("You own this channel"), left + 8, top + 56, 0xFFD0C8A8, false);
            for (var renderable : this.renderables) {
                renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
