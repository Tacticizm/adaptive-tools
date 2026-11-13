package com.tacticizm.adaptivetools.wireless;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Carried generator. Plain use opens the fuel screen. Sneak-use opens Item Settings. */
public class FieldGeneratorItem extends Item {
    private final boolean advanced;

    public FieldGeneratorItem(boolean advanced, Properties properties) {
        super(properties.stacksTo(1));
        this.advanced = advanced;
    }

    public boolean advanced() {
        return advanced;
    }

    public int capacity() {
        return advanced ? 5_000_000 : 1_000_000;
    }

    /** FE added to the buffer each burn tick. */
    public int generationPerTick() {
        return advanced ? 1_000 : 200;
    }

    /** FE moved into items each tick. */
    public int chargePerTick() {
        return advanced ? 4_000 : 1_000;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (level.isClientSide()) {
                com.tacticizm.adaptivetools.client.ToolSettingsScreen.open(player, hand);
            }
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer server) {
            int slot = hand == InteractionHand.MAIN_HAND ? server.getInventory().getSelectedSlot() : net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND;
            server.openMenu(new net.minecraft.world.MenuProvider() {
                @Override
                public net.minecraft.network.chat.Component getDisplayName() {
                    return net.minecraft.network.chat.Component.translatable(advanced
                            ? "item.adaptivetools.advanced_field_generator"
                            : "item.adaptivetools.basic_field_generator");
                }

                @Override
                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player opener) {
                    return new FieldGeneratorMenu(id, inventory, slot);
                }
            }, buf -> buf.writeVarInt(slot));
        }
        return InteractionResult.SUCCESS;
    }
}
