package com.tacticizm.adaptivetools.wireless;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.access.ItemAccess;

final class EnergyMove {
    private EnergyMove() {}

    static int move(EnergyHandler from, EnergyHandler to, int amount) {
        if (from == null || to == null || amount <= 0) {
            return 0;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = from.extract(amount, transaction);
            int inserted = extracted <= 0 ? 0 : to.insert(extracted, transaction);
            if (inserted > 0 && inserted == extracted) {
                transaction.commit();
                return inserted;
            }
            return 0;
        }
    }

    static EnergyHandler item(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return ItemAccess.forStack(stack).getCapability(Capabilities.Energy.ITEM);
    }
}
