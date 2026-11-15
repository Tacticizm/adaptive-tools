package com.tacticizm.adaptivetools.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.tacticizm.adaptivetools.ModBlocks;
import com.tacticizm.adaptivetools.ModItems;

/** Server-side cable config. Buttons and ghost filters cannot skip the install limits. */
public class CableMenu extends AbstractContainerMenu {
    public static final int POWER_INSTALL = 10;
    public static final int ITEM_INSTALL = 11;
    public static final int FLUID_INSTALL = 12;
    public static final int ITEM_MODE_BASE = 20;
    public static final int FLUID_MODE_BASE = 30;
    public static final int ITEM_PRIORITY_DOWN = 40;
    public static final int ITEM_PRIORITY_UP = 41;
    public static final int FLUID_PRIORITY_DOWN = 42;
    public static final int FLUID_PRIORITY_UP = 43;
    public static final int RATE_ADD = 50;
    public static final int RATE_REMOVE = 51;
    public static final int RESET = 60;

    private final Player player;
    private final CableBlockEntity cable;
    private final BlockPos pos;
    private final SimpleContainer filters = new SimpleContainer(18);
    private final SimpleContainerData data = new SimpleContainerData(9);
    private Direction selected;
    private boolean loading;
    private boolean resetArmed;

    public CableMenu(int id, Inventory inventory, CableBlockEntity cable, Direction face) {
        super(ModBlocks.CABLE_MENU.get(), id);
        this.player = inventory.player;
        this.cable = cable;
        this.pos = cable == null ? BlockPos.ZERO : cable.getBlockPos();
        this.selected = face == null ? Direction.NORTH : face;
        for (int i = 0; i < 9; i++) {
            addSlot(new GhostSlot(i, 47 + i * 18, 161));
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new GhostSlot(9 + i, 47 + i * 18, 197));
        }
        addDataSlots(data);
        loadFilters();
        pullData();
    }

    public Direction selected() {
        return Direction.from3DDataValue(data.get(0));
    }

    public boolean power() {
        return data.get(1) != 0;
    }

    public TransferMode itemMode() {
        return TransferMode.byOrdinal(data.get(2));
    }

    public TransferMode fluidMode() {
        return TransferMode.byOrdinal(data.get(3));
    }

    public int itemPriority() {
        return data.get(4);
    }

    public int fluidPriority() {
        return data.get(5);
    }

    public int rateModules() {
        return data.get(6);
    }

    public boolean disconnected() {
        return data.get(7) != 0;
    }

    public boolean resetArmed() {
        return data.get(8) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (cable == null || player.level().isClientSide()) {
            return id >= 0 && id < 6 || id == POWER_INSTALL || id == RESET;
        }
        if (id >= 0 && id < 6) {
            selected = Direction.from3DDataValue(id);
            resetArmed = false;
            loadFilters();
            pullData();
            return true;
        }
        FaceConfig face = cable.face(selected);
        boolean handled = switch (id) {
            case POWER_INSTALL -> toggle(player, face.powerModule, ModItems.POWER_MODULE.get(), () -> cable.installPower(selected), () -> cable.removePower(selected));
            case ITEM_INSTALL -> toggle(player, face.itemModule, ModItems.ITEM_MODULE.get(), () -> cable.installItem(selected), () -> cable.removeItem(selected));
            case FLUID_INSTALL -> toggle(player, face.fluidModule, ModItems.FLUID_MODULE.get(), () -> cable.installFluid(selected), () -> cable.removeFluid(selected));
            case ITEM_MODE_BASE, ITEM_MODE_BASE + 1, ITEM_MODE_BASE + 2 -> {
                cable.setItemMode(selected, TransferMode.byOrdinal(id - ITEM_MODE_BASE));
                yield true;
            }
            case FLUID_MODE_BASE, FLUID_MODE_BASE + 1, FLUID_MODE_BASE + 2 -> {
                cable.setFluidMode(selected, TransferMode.byOrdinal(id - FLUID_MODE_BASE));
                yield true;
            }
            case ITEM_PRIORITY_DOWN -> {
                cable.setItemPriority(selected, clamp(face.itemPriority - 1));
                yield true;
            }
            case ITEM_PRIORITY_UP -> {
                cable.setItemPriority(selected, clamp(face.itemPriority + 1));
                yield true;
            }
            case FLUID_PRIORITY_DOWN -> {
                cable.setFluidPriority(selected, clamp(face.fluidPriority - 1));
                yield true;
            }
            case FLUID_PRIORITY_UP -> {
                cable.setFluidPriority(selected, clamp(face.fluidPriority + 1));
                yield true;
            }
            case RATE_ADD -> addRate();
            case RATE_REMOVE -> removeRate(player);
            case RESET -> reset();
            default -> false;
        };
        if (handled && id != RESET) {
            resetArmed = false;
        }
        pullData();
        return handled;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput type, Player player) {
        if (slotId >= 0 && slotId < 18) {
            ItemStack carried = getCarried();
            getSlot(slotId).set(carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
            return;
        }
        super.clicked(slotId, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return cable != null && !cable.isRemoved()
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void broadcastChanges() {
        pullData();
        super.broadcastChanges();
    }

    private boolean toggle(Player player, boolean installed, Item module, Runnable install, Runnable remove) {
        if (installed) {
            remove.run();
            player.getInventory().placeItemBackInInventory(new ItemStack(module));
            return true;
        }
        if (!getCarried().is(module)) {
            return false;
        }
        install.run();
        getCarried().shrink(1);
        return true;
    }

    private boolean addRate() {
        if (!getCarried().is(ModItems.RATE_MODULE.get()) || !cable.addRate(selected)) {
            return false;
        }
        getCarried().shrink(1);
        return true;
    }

    private boolean removeRate(Player player) {
        if (!cable.removeRate(selected)) {
            return false;
        }
        player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.RATE_MODULE.get()));
        return true;
    }

    private boolean reset() {
        if (!resetArmed) {
            resetArmed = true;
            return true;
        }
        cable.resetFace(selected);
        resetArmed = false;
        loadFilters();
        return true;
    }

    private void loadFilters() {
        if (cable == null) {
            return;
        }
        loading = true;
        FaceConfig face = cable.face(selected);
        for (int i = 0; i < 9; i++) {
            filters.setItem(i, copy(face.itemFilters[i]));
            filters.setItem(9 + i, copy(face.fluidFilters[i]));
        }
        loading = false;
    }

    private void onFilterSet(int index, ItemStack stack) {
        if (loading || cable == null || player.level().isClientSide()) {
            return;
        }
        if (index < 9) {
            cable.setItemFilter(selected, index, stack);
        } else {
            cable.setFluidFilter(selected, index - 9, stack);
        }
    }

    private void pullData() {
        FaceConfig face = cable == null ? new FaceConfig() : cable.face(selected);
        data.set(0, selected.get3DDataValue());
        data.set(1, face.powerModule ? 1 : 0);
        data.set(2, face.itemMode.ordinal());
        data.set(3, face.fluidMode.ordinal());
        data.set(4, face.itemPriority);
        data.set(5, face.fluidPriority);
        data.set(6, face.rateModules);
        data.set(7, face.disconnected ? 1 : 0);
        data.set(8, resetArmed ? 1 : 0);
    }

    private static int clamp(int priority) {
        return Math.max(-100, Math.min(100, priority));
    }

    private static ItemStack copy(ItemStack stack) {
        return stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    private final class GhostSlot extends Slot {
        private GhostSlot(int index, int x, int y) {
            super(filters, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public void set(ItemStack stack) {
            super.set(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
            onFilterSet(getSlotIndex(), getItem());
        }
    }
}
