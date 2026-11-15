package com.tacticizm.adaptivetools.wireless;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.tacticizm.adaptivetools.ModBlocks;
import com.tacticizm.adaptivetools.ModComponents;
import com.tacticizm.adaptivetools.ToolSettings;

/** Fuel and charge slots stored on the field generator item. */
public class FieldGeneratorMenu extends AbstractContainerMenu {
    public static final int TOGGLE = 1;
    public static final int HOTBAR = 2;
    public static final int MAIN = 3;
    public static final int ARMOR = 4;

    public final SimpleContainer fuel = new SimpleContainer(1);
    public final SimpleContainer charge = new SimpleContainer(1);
    private final int slot;
    private final Player player;
    private final SimpleContainerData data = new SimpleContainerData(8);

    public FieldGeneratorMenu(int id, Inventory inventory, int slot) {
        super(ModBlocks.FIELD_MENU.get(), id);
        this.slot = slot;
        this.player = inventory.player;
        ItemStack stack = stack();
        fuel.setItem(0, copy(saved(stack, ModComponents.FIELD_FUEL.get())));
        charge.setItem(0, copy(saved(stack, ModComponents.FIELD_CHARGE.get())));
        addSlot(new Slot(fuel, 0, 37, 23) {
            @Override
            public boolean mayPlace(ItemStack incoming) {
                return !(incoming.getItem() instanceof FieldGeneratorItem);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                write();
            }
        });
        addSlot(new Slot(charge, 0, 37, 53) {
            @Override
            public boolean mayPlace(ItemStack incoming) {
                return !(incoming.getItem() instanceof FieldGeneratorItem);
            }

            @Override
            public void setChanged() {
                super.setChanged();
                write();
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
        write();
    }

    public int slot() {
        return slot;
    }

    public int energy() {
        return data.get(0);
    }

    public int capacity() {
        return data.get(1);
    }

    public int rate() {
        return data.get(2);
    }

    public boolean on() {
        return data.get(3) != 0;
    }

    public boolean advanced() {
        return data.get(4) != 0;
    }

    private ItemStack stack() {
        if (player == null || slot < 0 || slot >= player.getInventory().getContainerSize()) {
            return ItemStack.EMPTY;
        }
        return player.getInventory().getItem(slot);
    }

    private void write() {
        ItemStack stack = stack();
        if (!(stack.getItem() instanceof FieldGeneratorItem)) {
            return;
        }
        store(stack, ModComponents.FIELD_FUEL.get(), fuel.getItem(0));
        store(stack, ModComponents.FIELD_CHARGE.get(), charge.getItem(0));
    }

    private static ItemStack saved(ItemStack stack, net.minecraft.core.component.DataComponentType<FrozenStack> type) {
        FrozenStack saved = stack.get(type);
        return saved == null ? ItemStack.EMPTY : saved.copy();
    }

    private static void store(ItemStack stack, net.minecraft.core.component.DataComponentType<FrozenStack> type, ItemStack value) {
        if (value.isEmpty()) {
            stack.remove(type);
        } else {
            stack.set(type, new FrozenStack(value));
        }
    }

    private static ItemStack copy(ItemStack stack) {
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    @Override
    public boolean clickMenuButton(Player who, int id) {
        ItemStack stack = stack();
        if (!(stack.getItem() instanceof FieldGeneratorItem)) {
            return false;
        }
        int bit = switch (id) {
            case TOGGLE -> ToolSettings.FIELD_ON;
            case HOTBAR -> ToolSettings.FIELD_HOTBAR;
            case MAIN -> ToolSettings.FIELD_MAIN;
            case ARMOR -> ToolSettings.FIELD_ARMOR;
            default -> 0;
        };
        if (bit == 0) {
            return false;
        }
        stack.set(ModComponents.TOOL_SETTINGS.get(), FieldGenerators.settings(stack).toggle(bit));
        return true;
    }

    @Override
    public void broadcastChanges() {
        ItemStack stack = stack();
        FieldGeneratorItem generator = stack.getItem() instanceof FieldGeneratorItem found ? found : null;
        data.set(0, stack.getOrDefault(ModComponents.ENERGY.get(), 0));
        data.set(1, generator == null ? 0 : generator.capacity());
        data.set(2, generator == null ? 0 : generator.generationPerTick());
        ToolSettings settings = FieldGenerators.settings(stack);
        data.set(3, settings.fieldOn() ? 1 : 0);
        data.set(4, generator != null && generator.advanced() ? 1 : 0);
        data.set(5, settings.fieldHotbar() ? 1 : 0);
        data.set(6, settings.fieldMain() ? 1 : 0);
        data.set(7, settings.fieldArmor() ? 1 : 0);
        super.broadcastChanges();
    }

    public boolean armorArea() {
        return data.get(7) != 0;
    }

    public boolean hotbar() {
        return data.get(5) != 0;
    }

    public boolean main() {
        return data.get(6) != 0;
    }

    @Override
    public boolean stillValid(Player who) {
        return stack().getItem() instanceof FieldGeneratorItem;
    }

    @Override
    public ItemStack quickMoveStack(Player who, int index) {
        return ItemStack.EMPTY;
    }
}
