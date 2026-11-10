package com.tacticizm.adaptivetools.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import com.tacticizm.adaptivetools.AdaptivePickaxeItem;
import com.tacticizm.adaptivetools.ModEnergy;
import com.tacticizm.adaptivetools.ModNetworking;
import com.tacticizm.adaptivetools.ToolSettings;
import com.tacticizm.adaptivetools.ToolTier;

/**
 * First Tool Settings screen. The background is the approved {@code tool_settings.png}.
 * This slice only edits area mining and the area size; the texture's other wells are left for
 * filters and abilities.
 */
public class ToolSettingsScreen extends Screen {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("adaptivetools", "textures/gui/tool_settings.png");
    private static final int IMAGE_WIDTH = 256;
    private static final int IMAGE_HEIGHT = 256;
    private static final int[] SIZES = {1, 3, 5, 7};

    private final int slot;
    private boolean areaMining;
    private int areaSize;
    private Button areaButton;
    private final Button[] sizeButtons = new Button[SIZES.length];

    public ToolSettingsScreen(Player player, InteractionHand hand) {
        super(Component.translatable("adaptivetools.screen.tool_settings"));
        this.slot = ModNetworking.slotFor(player, hand);
        ItemStack stack = player.getItemInHand(hand);
        ToolTier tier = ((AdaptivePickaxeItem) stack.getItem()).tier();
        ToolSettings settings = ToolSettings.of(stack, tier);
        this.areaMining = settings.areaMining();
        this.areaSize = settings.areaSize();
    }

    public static void open(Player player, InteractionHand hand) {
        Minecraft.getInstance().setScreen(new ToolSettingsScreen(player, hand));
    }

    @Override
    public void init() {
        int left = (this.width - IMAGE_WIDTH) / 2;
        int top = (this.height - IMAGE_HEIGHT) / 2;
        ToolTier tier = tier();

        areaButton = Button.builder(areaLabel(), button -> {
            areaMining = !areaMining;
            button.setMessage(areaLabel());
            send();
        }).bounds(left + 176, top + 34, 72, 18).build();
        addRenderableWidget(areaButton);

        for (int i = 0; i < SIZES.length; i++) {
            int size = SIZES[i];
            int x = left + 12 + i * 38;
            Button button = Button.builder(Component.literal(size + "x" + size), ignored -> {
                areaSize = size;
                refreshSizeButtons();
                send();
            }).bounds(x, top + 78, 36, 20).build();
            button.active = size <= tier.maxAreaSide() && size != areaSize;
            sizeButtons[i] = button;
            addRenderableWidget(button);
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + 12, top + 104, 72, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = (this.width - IMAGE_WIDTH) / 2;
        int top = (this.height - IMAGE_HEIGHT) / 2;
        this.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left, top, 0.0F, 0.0F, IMAGE_WIDTH, IMAGE_HEIGHT, 256, 256);
        for (var renderable : this.renderables) {
            renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }

        ItemStack stack = currentStack();
        ToolTier tier = tier();
        graphics.text(this.font, stack.getHoverName(), left + 12, top + 6, 0xFF2A2A32, false);
        graphics.text(this.font, Component.translatable("adaptivetools.screen.cap", tier.maxAreaSide(), tier.maxAreaSide()), left + 12, top + 44, 0xFFE8E8E8, false);
        if (tier.usesEnergy()) {
            graphics.text(this.font, Component.translatable("adaptivetools.tooltip.energy", ModEnergy.getStored(stack), tier.energyCapacity()), left + 12, top + 56, 0xFFB7E7FF, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void refreshSizeButtons() {
        ToolTier tier = tier();
        for (int i = 0; i < SIZES.length; i++) {
            int size = SIZES[i];
            sizeButtons[i].active = size <= tier.maxAreaSide() && size != areaSize;
        }
    }

    private void send() {
        ClientPacketDistributor.sendToServer(new ModNetworking.UpdateToolSettingsPayload(slot, areaMining, areaSize));
    }

    private Component areaLabel() {
        return Component.translatable(areaMining ? "adaptivetools.screen.area_on" : "adaptivetools.screen.area_off");
    }

    private ItemStack currentStack() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return ItemStack.EMPTY;
        }
        return player.getInventory().getItem(slot);
    }

    private ToolTier tier() {
        ItemStack stack = currentStack();
        if (stack.getItem() instanceof AdaptivePickaxeItem pick) {
            return pick.tier();
        }
        return ToolTier.TEMPERED_COPPER;
    }
}
