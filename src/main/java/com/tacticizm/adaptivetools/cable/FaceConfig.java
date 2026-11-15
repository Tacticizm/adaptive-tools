package com.tacticizm.adaptivetools.cable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * One face of a cable. Disconnected faces stay in the config screen so their modules can be taken
 * back off. An empty filter allows every item or fluid.
 */
public final class FaceConfig {
    public static final int FILTER_SIZE = 9;

    public boolean disconnected;
    public boolean powerModule;
    public boolean itemModule;
    public TransferMode itemMode = TransferMode.DISABLED;
    public int itemPriority;
    public final ItemStack[] itemFilters = emptyFilters();
    public boolean fluidModule;
    public TransferMode fluidMode = TransferMode.DISABLED;
    public int fluidPriority;
    public final ItemStack[] fluidFilters = emptyFilters();
    public int rateModules;

    public int rateMultiplier() {
        return 1 << Math.max(0, Math.min(2, rateModules));
    }

    public boolean allowsItem(ItemStack stack) {
        return allows(itemFilters, stack);
    }

    public boolean allowsFluid(Fluid fluid) {
        return allows(fluidFilters, new ItemStack(fluid.getBucket()));
    }

    public void save(ValueOutput output) {
        output.putBoolean("disconnected", disconnected);
        output.putBoolean("power", powerModule);
        output.putBoolean("item_module", itemModule);
        output.putInt("item_mode", itemMode.ordinal());
        output.putInt("item_priority", itemPriority);
        output.putBoolean("fluid_module", fluidModule);
        output.putInt("fluid_mode", fluidMode.ordinal());
        output.putInt("fluid_priority", fluidPriority);
        output.putInt("rate", rateModules);
        writeFilters(output.list("item_filters", ItemStack.OPTIONAL_CODEC), itemFilters);
        writeFilters(output.list("fluid_filters", ItemStack.OPTIONAL_CODEC), fluidFilters);
    }

    public void load(ValueInput input) {
        disconnected = input.getBooleanOr("disconnected", false);
        powerModule = input.getBooleanOr("power", false);
        itemModule = input.getBooleanOr("item_module", false);
        itemMode = TransferMode.byOrdinal(input.getIntOr("item_mode", 0));
        itemPriority = input.getIntOr("item_priority", 0);
        fluidModule = input.getBooleanOr("fluid_module", false);
        fluidMode = TransferMode.byOrdinal(input.getIntOr("fluid_mode", 0));
        fluidPriority = input.getIntOr("fluid_priority", 0);
        rateModules = Math.max(0, Math.min(2, input.getIntOr("rate", 0)));
        readFilters(input, "item_filters", itemFilters);
        readFilters(input, "fluid_filters", fluidFilters);
    }

    public void resetSettings() {
        itemMode = TransferMode.DISABLED;
        fluidMode = TransferMode.DISABLED;
        itemPriority = 0;
        fluidPriority = 0;
        clear(itemFilters);
        clear(fluidFilters);
    }

    private static boolean allows(ItemStack[] filters, ItemStack stack) {
        boolean any = false;
        for (ItemStack filter : filters) {
            if (filter.isEmpty()) {
                continue;
            }
            any = true;
            if (ItemStack.isSameItem(filter, stack)) {
                return true;
            }
        }
        return !any;
    }

    private static void writeFilters(ValueOutput.TypedOutputList<ItemStack> list, ItemStack[] filters) {
        for (ItemStack filter : filters) {
            list.add(filter.isEmpty() ? ItemStack.EMPTY : filter.copyWithCount(1));
        }
    }

    private static void readFilters(ValueInput input, String name, ItemStack[] filters) {
        clear(filters);
        int index = 0;
        for (ItemStack stack : input.listOrEmpty(name, ItemStack.OPTIONAL_CODEC)) {
            if (index >= filters.length) {
                break;
            }
            filters[index++] = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        }
    }

    private static ItemStack[] emptyFilters() {
        ItemStack[] filters = new ItemStack[FILTER_SIZE];
        clear(filters);
        return filters;
    }

    private static void clear(ItemStack[] filters) {
        for (int i = 0; i < filters.length; i++) {
            filters[i] = ItemStack.EMPTY;
        }
    }
}
