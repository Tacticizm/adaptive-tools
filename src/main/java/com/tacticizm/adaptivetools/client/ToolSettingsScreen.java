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

import com.tacticizm.adaptivetools.AdaptiveAxeItem;
import com.tacticizm.adaptivetools.AdaptiveHoeItem;
import com.tacticizm.adaptivetools.AdaptiveShovelItem;
import com.tacticizm.adaptivetools.AdaptiveSwordItem;
import com.tacticizm.adaptivetools.ModEnergy;
import com.tacticizm.adaptivetools.ModNetworking;
import com.tacticizm.adaptivetools.TieredTool;
import com.tacticizm.adaptivetools.ToolSettings;
import com.tacticizm.adaptivetools.ToolTier;

/**
 * Tool Settings. Each ability the held tool's tier can learn has an on/off toggle, and a size or
 * limit where the spec has one. Abilities a tier cannot learn are not shown. Tool XP is not built yet.
 */
public class ToolSettingsScreen extends Screen {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("adaptivetools", "textures/gui/tool_settings.png");
    private static final int IMAGE_WIDTH = 256;
    private static final int IMAGE_HEIGHT = 256;
    private static final int[] SIZES = {1, 3, 5, 7};

    private final int slot;
    private ToolSettings settings;

    public ToolSettingsScreen(Player player, InteractionHand hand) {
        super(Component.translatable("adaptivetools.screen.tool_settings"));
        this.slot = ModNetworking.slotFor(player, hand);
        ItemStack stack = player.getItemInHand(hand);
        ToolTier tier = ((TieredTool) stack.getItem()).tier();
        this.settings = ToolSettings.of(stack, tier);
    }

    public static void open(Player player, InteractionHand hand) {
        Minecraft.getInstance().setScreen(new ToolSettingsScreen(player, hand));
    }

    @Override
    public void init() {
        clearWidgets();
        int left = (this.width - IMAGE_WIDTH) / 2;
        int top = (this.height - IMAGE_HEIGHT) / 2;
        ItemStack stack = currentStack();
        if (!(stack.getItem() instanceof TieredTool tool)) {
            return;
        }
        ToolTier tier = tool.tier();
        if (tool instanceof AdaptiveAxeItem) {
            axe(left, top, tier);
        } else if (tool instanceof AdaptiveShovelItem) {
            digger(left, top, tier, true);
        } else if (tool instanceof AdaptiveHoeItem) {
            hoe(left, top, tier);
        } else if (tool instanceof AdaptiveSwordItem) {
            sword(left, top, tier);
        } else {
            digger(left, top, tier, false);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + 176, top + 228, 68, 18)
                .build());
    }

    private void digger(int left, int top, ToolTier tier, boolean shovel) {
        toggle(left + 176, top + 34, settings.areaMining() ? "Area: On" : "Area: Off", () -> {
            settings = settings.withArea(!settings.areaMining(), settings.areaSize());
            send();
        });
        for (int i = 0; i < SIZES.length; i++) {
            int size = SIZES[i];
            Button button = Button.builder(Component.literal(size + "x" + size), ignored -> {
                settings = settings.withArea(true, size);
                send();
            }).bounds(left + 12 + i * 38, top + 78, 36, 18).build();
            button.active = size <= tier.maxAreaSide();
            addRenderableWidget(button);
        }
        if (shovel && tier.canTrench()) {
            toggle(left + 12, top + 130, settings.trench() ? "Trench: On" : "Trench: Off", () -> {
                settings = settings.withTrench(!settings.trench(), settings.trenchLength());
                send();
            });
            toggle(left + 100, top + 130, "Len " + settings.effectiveTrench(tier), () -> {
                int next = settings.effectiveTrench(tier) >= tier.maxTrench() ? 1 : settings.effectiveTrench(tier) + 2;
                settings = settings.withTrench(true, next);
                send();
            });
        }
        if (shovel && tier.canGrade()) {
            toggle(left + 12, top + 152, settings.grading() ? "Grade: On" : "Grade: Off", () -> {
                int height = settings.gradeHeight() == Integer.MIN_VALUE && Minecraft.getInstance().player != null
                        ? Minecraft.getInstance().player.getBlockY()
                        : settings.gradeHeight();
                settings = settings.withGrading(!settings.grading(), height);
                send();
            });
            toggle(left + 100, top + 152, "Y-", () -> {
                settings = settings.withGrading(true, height() - 1);
                send();
            });
            toggle(left + 150, top + 152, "Y+", () -> {
                settings = settings.withGrading(true, height() + 1);
                send();
            });
        }
    }

    private void axe(int left, int top, ToolTier tier) {
        if (tier.maxFellLogs() <= 0) {
            return;
        }
        toggle(left + 12, top + 78, settings.fell() ? "Fell: On" : "Fell: Off", () -> {
            settings = settings.withFell(!settings.fell(), settings.fellLimit(), settings.leafClear(), settings.charcoal());
            send();
        });
        toggle(left + 100, top + 78, "Logs " + settings.effectiveFell(tier), () -> {
            int next = settings.effectiveFell(tier) >= tier.maxFellLogs() ? Math.min(16, tier.maxFellLogs()) : Math.min(tier.maxFellLogs(), settings.effectiveFell(tier) * 2);
            settings = settings.withFell(true, next, settings.leafClear(), settings.charcoal());
            send();
        });
        toggle(left + 12, top + 102, settings.leafClear() ? "Leaves: On" : "Leaves: Off", () -> {
            settings = settings.withFell(settings.fell(), settings.fellLimit(), !settings.leafClear(), settings.charcoal());
            send();
        });
        if (tier.canCharcoal()) {
            toggle(left + 12, top + 126, settings.charcoal() ? "Charcoal: On" : "Charcoal: Off", () -> {
                settings = settings.withFell(settings.fell(), settings.fellLimit(), settings.leafClear(), !settings.charcoal());
                send();
            });
        }
    }

    private void hoe(int left, int top, ToolTier tier) {
        digger(left, top, tier, false);
        toggle(left + 12, top + 160, settings.till() ? "Till: On" : "Till: Off", () -> {
            settings = settings.withHoe(!settings.till(), settings.harvest(), settings.replant());
            send();
        });
        toggle(left + 90, top + 160, settings.harvest() ? "Harvest: On" : "Harvest: Off", () -> {
            settings = settings.withHoe(settings.till(), !settings.harvest(), settings.replant());
            send();
        });
        toggle(left + 176, top + 160, settings.replant() ? "Seed: On" : "Seed: Off", () -> {
            settings = settings.withHoe(settings.till(), settings.harvest(), !settings.replant());
            send();
        });
    }

    private void sword(int left, int top, ToolTier tier) {
        int y = top + 70;
        if (tier.canCleave()) {
            toggle(left + 12, y, settings.cleave() ? "Cleave: On" : "Cleave: Off", () -> {
                settings = sword(tier, !settings.cleave(), settings.chain());
                send();
            });
            toggle(left + 110, y, "N " + settings.cleaveTargets(), () -> {
                int next = settings.cleaveTargets() >= tier.maxCleaveTargets() ? 1 : settings.cleaveTargets() + 1;
                settings = settings.withSword(true, next, settings.cleaveRange(), settings.chain(), settings.chainTargets(), settings.chainRange(),
                        settings.momentum(), settings.guardBreak(), settings.siphon(), settings.targetPassives(), settings.targetPlayers());
                send();
            });
            y += 22;
        }
        if (tier.canChain()) {
            toggle(left + 12, y, settings.chain() ? "Chain: On" : "Chain: Off", () -> {
                settings = sword(tier, settings.cleave(), !settings.chain());
                send();
            });
            y += 22;
        }
        if (tier.canMomentum()) {
            toggle(left + 12, y, settings.momentum() ? "Momentum: On" : "Momentum: Off", () -> {
                settings = settings.withSword(settings.cleave(), settings.cleaveTargets(), settings.cleaveRange(), settings.chain(), settings.chainTargets(), settings.chainRange(),
                        !settings.momentum(), settings.guardBreak(), settings.siphon(), settings.targetPassives(), settings.targetPlayers());
                send();
            });
            y += 22;
        }
        if (tier.canGuardBreak()) {
            toggle(left + 12, y, settings.guardBreak() ? "Guard: On" : "Guard: Off", () -> {
                settings = settings.withSword(settings.cleave(), settings.cleaveTargets(), settings.cleaveRange(), settings.chain(), settings.chainTargets(), settings.chainRange(),
                        settings.momentum(), !settings.guardBreak(), settings.siphon(), settings.targetPassives(), settings.targetPlayers());
                send();
            });
            y += 22;
        }
        if (tier.canSiphon()) {
            toggle(left + 12, y, settings.siphon() ? "Siphon: On" : "Siphon: Off", () -> {
                settings = settings.withSword(settings.cleave(), settings.cleaveTargets(), settings.cleaveRange(), settings.chain(), settings.chainTargets(), settings.chainRange(),
                        settings.momentum(), settings.guardBreak(), !settings.siphon(), settings.targetPassives(), settings.targetPlayers());
                send();
            });
            y += 22;
        }
        toggle(left + 12, y, settings.targetPassives() ? "Passives: On" : "Passives: Off", () -> {
            settings = settings.withSword(settings.cleave(), settings.cleaveTargets(), settings.cleaveRange(), settings.chain(), settings.chainTargets(), settings.chainRange(),
                    settings.momentum(), settings.guardBreak(), settings.siphon(), !settings.targetPassives(), settings.targetPlayers());
            send();
        });
        toggle(left + 120, y, settings.targetPlayers() ? "Players: On" : "Players: Off", () -> {
            settings = settings.withSword(settings.cleave(), settings.cleaveTargets(), settings.cleaveRange(), settings.chain(), settings.chainTargets(), settings.chainRange(),
                    settings.momentum(), settings.guardBreak(), settings.siphon(), settings.targetPassives(), !settings.targetPlayers());
            send();
        });
    }

    private ToolSettings sword(ToolTier tier, boolean cleave, boolean chain) {
        return settings.withSword(cleave, settings.cleaveTargets(), settings.cleaveRange(), chain, settings.chainTargets(), settings.chainRange(),
                settings.momentum(), settings.guardBreak(), settings.siphon(), settings.targetPassives(), settings.targetPlayers()).clamped(tier);
    }

    private int height() {
        if (settings.gradeHeight() != Integer.MIN_VALUE) {
            return settings.gradeHeight();
        }
        Player player = Minecraft.getInstance().player;
        return player == null ? 64 : player.getBlockY();
    }

    private void toggle(int x, int y, String label, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), button -> action.run()).bounds(x, y, 84, 18).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = (this.width - IMAGE_WIDTH) / 2;
        int top = (this.height - IMAGE_HEIGHT) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left, top, 0.0F, 0.0F, IMAGE_WIDTH, IMAGE_HEIGHT, 256, 256);
        for (var renderable : this.renderables) {
            renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        ItemStack stack = currentStack();
        if (!(stack.getItem() instanceof TieredTool tool)) {
            return;
        }
        graphics.text(this.font, stack.getHoverName(), left + 12, top + 6, 0xFF2A2A32, false);
        graphics.text(this.font, Component.literal("Tier cap " + tool.tier().maxAreaSide() + "x" + tool.tier().maxAreaSide()), left + 12, top + 44, 0xFFE8E8E8, false);
        if (tool.tier().usesEnergy()) {
            graphics.text(this.font, Component.translatable("adaptivetools.tooltip.energy", ModEnergy.getStored(stack), tool.tier().energyCapacity()), left + 12, top + 56, 0xFFB7E7FF, false);
        }
        if (settings.grading()) {
            graphics.text(this.font, Component.literal("Grade above Y " + height()), left + 12, top + 176, 0xFFE8E8E8, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void send() {
        ItemStack stack = currentStack();
        if (stack.getItem() instanceof TieredTool tool) {
            settings = settings.clamped(tool.tier());
        }
        ClientPacketDistributor.sendToServer(new ModNetworking.UpdateFullToolSettingsPayload(slot, settings));
        init();
    }

    private ItemStack currentStack() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return ItemStack.EMPTY;
        }
        return player.getInventory().getItem(slot);
    }
}
