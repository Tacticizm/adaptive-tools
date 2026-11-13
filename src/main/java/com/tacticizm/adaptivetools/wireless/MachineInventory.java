package com.tacticizm.adaptivetools.wireless;

import net.minecraft.world.item.ItemStack;

/** A machine that drops itself with its energy, modules, and settings. */
public interface MachineInventory {
    ItemStack toStack();
}
