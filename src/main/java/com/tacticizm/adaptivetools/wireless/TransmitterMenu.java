package com.tacticizm.adaptivetools.wireless;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.tacticizm.adaptivetools.ModBlocks;
import com.tacticizm.adaptivetools.ModItems;

public class TransmitterMenu extends AbstractContainerMenu {
    public static final int ARM_REMOVAL = 1;
    public static final int OPEN_CHANNELS = 2;

    private final TransmitterBlockEntity transmitter;
    private final SimpleContainerData data = new SimpleContainerData(4);

    public TransmitterMenu(int id, Inventory inventory, TransmitterBlockEntity transmitter) {
        super(ModBlocks.TRANSMITTER_MENU.get(), id);
        this.transmitter = transmitter;
        addSlot(new Slot(transmitter.modules, 0, 83, 39) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.CAPACITY_MODULE.get());
            }

            @Override
            public boolean mayPickup(Player player) {
                return transmitter.canTakeCapacity();
            }

            @Override
            public int getMaxStackSize() {
                return TransmitterBlockEntity.MAX_MODULES;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                transmitter.onCapacityTaken();
            }
        });
        addSlot(new Slot(transmitter.modules, 1, 131, 39) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.CROSS_DIMENSION_MODULE.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
        addDataSlots(data);
    }

    public int energy() {
        return data.get(0);
    }

    public int capacity() {
        return data.get(1);
    }

    public int lostIfRemoved() {
        return data.get(2);
    }

    public TransmitterBlockEntity transmitter() {
        return transmitter;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (transmitter == null) {
            return false;
        }
        if (id == ARM_REMOVAL) {
            transmitter.armRemoval();
            return true;
        }
        if (id == OPEN_CHANNELS && player instanceof net.minecraft.server.level.ServerPlayer server) {
            WirelessMessages.sendChannels(server, transmitter);
            return true;
        }
        return false;
    }

    @Override
    public void broadcastChanges() {
        data.set(0, transmitter == null ? 0 : transmitter.energy());
        data.set(1, transmitter == null ? 0 : transmitter.capacity());
        data.set(2, transmitter == null ? 0 : transmitter.energyLostIfRemoved());
        data.set(3, transmitter == null ? 0 : transmitter.lastSent);
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return transmitter != null && !transmitter.isRemoved()
                && player.distanceToSqr(transmitter.getBlockPos().getX() + 0.5, transmitter.getBlockPos().getY() + 0.5, transmitter.getBlockPos().getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
