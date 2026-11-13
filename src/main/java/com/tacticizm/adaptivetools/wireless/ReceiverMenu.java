package com.tacticizm.adaptivetools.wireless;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import com.tacticizm.adaptivetools.ModBlocks;

public class ReceiverMenu extends AbstractContainerMenu {
    public static final int CYCLE_ITEM = 1;
    public static final int CYCLE_FLUID = 2;
    public static final int OPEN_CHANNELS = 3;
    public static final int DISCONNECT = 4;

    private final ReceiverBlockEntity receiver;
    private final SimpleContainerData data = new SimpleContainerData(2);

    public ReceiverMenu(int id, Inventory inventory, ReceiverBlockEntity receiver) {
        super(ModBlocks.RECEIVER_MENU.get(), id);
        this.receiver = receiver;
        addDataSlots(data);
    }

    public ReceiverBlockEntity receiver() {
        return receiver;
    }

    public int itemMode() {
        return data.get(0);
    }

    public int fluidMode() {
        return data.get(1);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (receiver == null) {
            return false;
        }
        if (id == CYCLE_ITEM) {
            receiver.cycleItem();
            return true;
        }
        if (id == CYCLE_FLUID) {
            receiver.cycleFluid();
            return true;
        }
        if (id == DISCONNECT) {
            receiver.assign(null);
            return true;
        }
        if (id == OPEN_CHANNELS && player instanceof net.minecraft.server.level.ServerPlayer server) {
            WirelessMessages.sendChannels(server, receiver);
            return true;
        }
        return false;
    }

    @Override
    public void broadcastChanges() {
        data.set(0, receiver == null ? 0 : receiver.itemMode().ordinal());
        data.set(1, receiver == null ? 0 : receiver.fluidMode().ordinal());
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return receiver != null && !receiver.isRemoved()
                && player.distanceToSqr(receiver.getBlockPos().getX() + 0.5, receiver.getBlockPos().getY() + 0.5, receiver.getBlockPos().getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
