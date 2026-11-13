package com.tacticizm.adaptivetools.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
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

import com.mojang.blaze3d.platform.InputConstants;

import com.tacticizm.adaptivetools.AdaptiveArmorItem;
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
    /** Dark content panel on tool_settings.png, in texture pixels. */
    private static final int PANEL_LEFT = 12;
    private static final int PANEL_RIGHT = 160;
    private static final int BUTTON_TOP = 42;
    private static final int BUTTON_HEIGHT = 16;
    private static final int ROW = 18;
    private static final int COLUMN = 74;

    private final int slot;
    private ToolSettings settings;
    private int listening;

    public ToolSettingsScreen(Player player, InteractionHand hand) {
        super(Component.translatable(player.getItemInHand(hand).getItem() instanceof AdaptiveArmorItem
                ? "adaptivetools.screen.item_settings"
                : "adaptivetools.screen.tool_settings"));
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
        if (tool instanceof AdaptiveArmorItem armor) {
            armor(left, top, armor);
        } else if (tool instanceof AdaptiveAxeItem) {
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
        // The narrow slot on the right of the texture, not the main panel.
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + 176, top + 112, 64, 16)
                .build());
    }

    private void armor(int left, int top, AdaptiveArmorItem armor) {
        int row = 0;
        if (armor.canWaterWalk()) {
            ability(left, top, row++, ToolSettings.WATER_WALKING, settings.waterWalking(), "Water");
        }
        if (armor.canElytra()) {
            ability(left, top, row++, ToolSettings.ELYTRA_GLIDE, settings.elytra(), "Elytra");
        }
        if (armor.canFly()) {
            ability(left, top, row++, ToolSettings.CREATIVE_FLIGHT, settings.flight(), "Flight");
        }
        if (armor.canShield()) {
            ability(left, top, row, ToolSettings.ENERGY_SHIELD, settings.energyShield(), "Shield");
        }
    }

    private void ability(int left, int top, int row, int bit, boolean on, String name) {
        toggle(left, top, row, 0, COLUMN, name + (on ? ": On" : ": Off"), () -> {
            settings = settings.toggle(bit);
            send();
        });
        toggle(left, top, row, 1, COLUMN, listening == bit ? "Press key" : ArmorKeybinds.label(bit), () -> {
            listening = bit;
            init();
        });
    }

    private void digger(int left, int top, ToolTier tier, boolean shovel) {
        for (int i = 0; i < SIZES.length; i++) {
            int size = SIZES[i];
            Button button = Button.builder(Component.literal(size + "x" + size), ignored -> {
                settings = settings.withArea(true, size);
                send();
            }).bounds(left + PANEL_LEFT + i * 37, top + BUTTON_TOP, 34, BUTTON_HEIGHT).build();
            button.active = size <= tier.maxAreaSide();
            addRenderableWidget(button);
        }
        toggle(left, top, 1, 0, COLUMN * 2 - 4, settings.areaMining() ? "Area: On" : "Area: Off", () -> {
            settings = settings.withArea(!settings.areaMining(), settings.areaSize());
            send();
        });
        int row = 2;
        if (shovel && tier.canTrench()) {
            int length = settings.configuredTrench(tier);
            toggle(left, top, row, 0, COLUMN, settings.trench() ? "Trench: On" : "Trench: Off", () -> {
                settings = settings.withTrench(!settings.trench(), settings.configuredTrench(tier));
                send();
            });
            toggle(left, top, row, 1, COLUMN, "Len " + length, () -> {
                int next = length >= tier.maxTrench() ? 1 : Math.min(tier.maxTrench(), length + 2);
                settings = settings.withTrench(true, next);
                send();
            });
            row++;
        }
        if (shovel && tier.canGrade()) {
            toggle(left, top, row, 0, COLUMN, settings.grading() ? "Grade: On" : "Grade: Off", () -> {
                settings = settings.withGrading(!settings.grading(), height());
                send();
            });
            toggle(left, top, row, 1, 34, "Y-", () -> {
                settings = settings.withGrading(true, height() - 1);
                send();
            });
            place(left + PANEL_LEFT + COLUMN + 38, top + BUTTON_TOP + row * ROW, 34, "Y+", () -> {
                settings = settings.withGrading(true, height() + 1);
                send();
            });
        }
    }

    private void axe(int left, int top, ToolTier tier) {
        if (tier.maxFellLogs() <= 0) {
            return;
        }
        int limit = settings.configuredFell(tier);
        toggle(left, top, 0, 0, COLUMN, settings.fell() ? "Fell: On" : "Fell: Off", () -> {
            settings = settings.withFell(!settings.fell(), settings.configuredFell(tier), settings.leafClear(), settings.charcoal());
            send();
        });
        toggle(left, top, 0, 1, COLUMN, "Logs " + limit, () -> {
            int next = limit >= tier.maxFellLogs() ? Math.min(16, tier.maxFellLogs()) : Math.min(tier.maxFellLogs(), Math.max(16, limit * 2));
            settings = settings.withFell(true, next, settings.leafClear(), settings.charcoal());
            send();
        });
        toggle(left, top, 1, 0, COLUMN, settings.leafClear() ? "Leaf: On" : "Leaf: Off", () -> {
            settings = settings.withFell(settings.fell(), settings.configuredFell(tier), !settings.leafClear(), settings.charcoal());
            send();
        });
        if (tier.canCharcoal()) {
            toggle(left, top, 2, 0, COLUMN * 2 - 4, settings.charcoal() ? "Charcoal: On" : "Charcoal: Off", () -> {
                settings = settings.withFell(settings.fell(), settings.configuredFell(tier), settings.leafClear(), !settings.charcoal());
                send();
            });
        }
    }

    private void hoe(int left, int top, ToolTier tier) {
        digger(left, top, tier, false);
        int row = 2;
        toggle(left, top, row, 0, COLUMN, settings.till() ? "Till: On" : "Till: Off", () -> {
            settings = settings.withHoe(!settings.till(), settings.harvest(), settings.replant());
            send();
        });
        toggle(left, top, row, 1, COLUMN, settings.harvest() ? "Reap: On" : "Reap: Off", () -> {
            settings = settings.withHoe(settings.till(), !settings.harvest(), settings.replant());
            send();
        });
        toggle(left, top, row + 1, 0, COLUMN, settings.replant() ? "Seed: On" : "Seed: Off", () -> {
            settings = settings.withHoe(settings.till(), settings.harvest(), !settings.replant());
            send();
        });
    }

    private void sword(int left, int top, ToolTier tier) {
        int row = 0;
        if (tier.canCleave()) {
            toggle(left, top, row, 0, COLUMN, settings.cleave() ? "Cleave: On" : "Cleave: Off", () -> {
                settings = sword(tier, !settings.cleave(), settings.chain());
                send();
            });
            toggle(left, top, row, 1, COLUMN, "N " + settings.cleaveTargets(), () -> {
                int next = settings.cleaveTargets() >= tier.maxCleaveTargets() ? 1 : settings.cleaveTargets() + 1;
                settings = settings.withSword(true, next, settings.cleaveRange(), settings.chain(), settings.chainTargets(), settings.chainRange(),
                        settings.momentum(), settings.guardBreak(), settings.siphon(), settings.targetPassives(), settings.targetPlayers());
                send();
            });
            row++;
        }
        if (tier.canChain()) {
            toggle(left, top, row, 0, COLUMN, settings.chain() ? "Chain: On" : "Chain: Off", () -> {
                settings = sword(tier, settings.cleave(), !settings.chain());
                send();
            });
            toggle(left, top, row, 1, COLUMN, "N " + settings.chainTargets(), () -> {
                int next = settings.chainTargets() >= tier.maxChainTargets() ? 1 : settings.chainTargets() + 1;
                settings = settings.withSword(settings.cleave(), settings.cleaveTargets(), settings.cleaveRange(), true, next, settings.chainRange(),
                        settings.momentum(), settings.guardBreak(), settings.siphon(), settings.targetPassives(), settings.targetPlayers());
                send();
            });
            row++;
        }
        if (tier.canMomentum()) {
            toggle(left, top, row, 0, COLUMN, settings.momentum() ? "Mom.: On" : "Mom.: Off", () -> {
                settings = settings.withSword(settings.cleave(), settings.cleaveTargets(), settings.cleaveRange(), settings.chain(), settings.chainTargets(), settings.chainRange(),
                        !settings.momentum(), settings.guardBreak(), settings.siphon(), settings.targetPassives(), settings.targetPlayers());
                send();
            });
        }
        if (tier.canGuardBreak()) {
            toggle(left, top, row, tier.canMomentum() ? 1 : 0, COLUMN, settings.guardBreak() ? "Guard: On" : "Guard: Off", () -> {
                settings = settings.withSword(settings.cleave(), settings.cleaveTargets(), settings.cleaveRange(), settings.chain(), settings.chainTargets(), settings.chainRange(),
                        settings.momentum(), !settings.guardBreak(), settings.siphon(), settings.targetPassives(), settings.targetPlayers());
                send();
            });
            row++;
        } else if (tier.canMomentum()) {
            row++;
        }
        if (tier.canSiphon()) {
            toggle(left, top, row, 0, COLUMN, settings.siphon() ? "Siphon: On" : "Siphon: Off", () -> {
                settings = settings.withSword(settings.cleave(), settings.cleaveTargets(), settings.cleaveRange(), settings.chain(), settings.chainTargets(), settings.chainRange(),
                        settings.momentum(), settings.guardBreak(), !settings.siphon(), settings.targetPassives(), settings.targetPlayers());
                send();
            });
            row++;
        }
        toggle(left, top, row, 0, COLUMN, settings.targetPassives() ? "Mobs: On" : "Mobs: Off", () -> {
            settings = settings.withSword(settings.cleave(), settings.cleaveTargets(), settings.cleaveRange(), settings.chain(), settings.chainTargets(), settings.chainRange(),
                    settings.momentum(), settings.guardBreak(), settings.siphon(), !settings.targetPassives(), settings.targetPlayers());
            send();
        });
        toggle(left, top, row, 1, COLUMN, settings.targetPlayers() ? "PvP: On" : "PvP: Off", () -> {
            settings = settings.withSword(settings.cleave(), settings.cleaveTargets(), settings.cleaveRange(), settings.chain(), settings.chainTargets(), settings.chainRange(),
                    settings.momentum(), settings.guardBreak(), settings.siphon(), settings.targetPassives(), !settings.targetPlayers());
            send();
        });
    }

    private ToolSettings sword(ToolTier tier, boolean cleave, boolean chain) {
        return settings.withSword(cleave, settings.cleaveTargets(), settings.cleaveRange(), chain, settings.chainTargets(), settings.chainRange(),
                settings.momentum(), settings.guardBreak(), settings.siphon(), settings.targetPassives(), settings.targetPlayers()).clamped(tier);
    }

    private static String capLabel(TieredTool tool) {
        ToolTier tier = tool.tier();
        if (tool instanceof AdaptiveArmorItem) {
            return null;
        }
        if (tool instanceof AdaptiveAxeItem) {
            return tier.maxFellLogs() > 0 ? "Log cap " + tier.maxFellLogs() : "No tree felling";
        }
        if (tool instanceof AdaptiveSwordItem) {
            if (tier.canChain()) {
                return "Cleave " + tier.maxCleaveTargets() + ", chain " + tier.maxChainTargets();
            }
            if (tier.canCleave()) {
                return "Cleave cap " + tier.maxCleaveTargets();
            }
            return null;
        }
        return "Area cap " + tier.maxAreaSide() + "x" + tier.maxAreaSide();
    }

    private int height() {
        if (settings.gradeHeight() != Integer.MIN_VALUE) {
            return settings.gradeHeight();
        }
        Player player = Minecraft.getInstance().player;
        return player == null ? 64 : player.getBlockY();
    }

    private void toggle(int left, int top, int row, int column, int width, String label, Runnable action) {
        place(left + PANEL_LEFT + column * COLUMN, top + BUTTON_TOP + row * ROW, width, label, action);
    }

    private void place(int x, int y, int width, String label, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), button -> action.run()).bounds(x, y, width, BUTTON_HEIGHT).build());
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
        graphics.text(this.font, stack.getHoverName(), left + 12, top + 8, 0xFF2A2A32, false);
        String cap = capLabel(tool);
        if (cap != null) {
            graphics.text(this.font, Component.literal(cap), left + 12, top + 24, 0xFFE8E8E8, false);
        }
        if (tool.tier().usesEnergy()) {
            graphics.text(this.font, Component.translatable("adaptivetools.tooltip.energy", ModEnergy.getStored(stack), tool.tier().energyCapacity()), left + 120, top + 24, 0xFFB7E7FF, false);
        }
        if (settings.grading() && tool instanceof AdaptiveShovelItem) {
            graphics.text(this.font, Component.literal("Above Y " + height()), left + 120, top + 8, 0xFFE8E8E8, false);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listening != 0) {
            int key = event.key();
            ArmorKeybinds.bind(listening, key == 256 ? InputConstants.UNKNOWN : InputConstants.Type.KEYSYM.getOrCreate(key));
            listening = 0;
            init();
            return true;
        }
        return super.keyPressed(event);
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
