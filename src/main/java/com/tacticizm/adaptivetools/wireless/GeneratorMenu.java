package com.tacticizm.adaptivetools.wireless;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.tacticizm.adaptivetools.ModBlocks;

public class GeneratorMenu extends AbstractContainerMenu {
    private final GeneratorBlockEntity generator;
    private final SimpleContainerData data = new SimpleContainerData(2);

    public GeneratorMenu(int id, Inventory inventory, GeneratorBlockEntity generator) {
        super(ModBlocks.GENERATOR_MENU.get(), id);
        this.generator = generator;
        addSlot(new Slot(generator.fuel, 0, 37, 23));
        addPlayerSlots(inventory);
        addDataSlots(data);
    }

    private void addPlayerSlots(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    public int energy() {
        return data.get(0);
    }

    public int capacity() {
        return data.get(1);
    }

    @Override
    public void broadcastChanges() {
        data.set(0, generator == null ? 0 : generator.energy());
        data.set(1, GeneratorBlockEntity.CAPACITY);
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return generator != null && !generator.isRemoved()
                && player.distanceToSqr(generator.getBlockPos().getX() + 0.5, generator.getBlockPos().getY() + 0.5, generator.getBlockPos().getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
