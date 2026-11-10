package com.tacticizm.adaptivetools;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Flux and Ender tools expose NeoForge's item energy capability ({@code neoforge:energy_handler})
 * so other mods' chargers can fill them. Stored FE lives in {@link ModComponents#ENERGY}.
 */
public final class ModEnergy {
    private ModEnergy() {}

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.Energy.ITEM, (stack, access) -> {
            if (!(stack.getItem() instanceof AdaptivePickaxeItem pick) || !pick.tier().usesEnergy()) {
                return null;
            }
            return new ItemAccessEnergyHandler(access, ModComponents.ENERGY.get(), pick.tier().energyCapacity());
        }, ModItems.FLUX_PICKAXE.get(), ModItems.ENDER_PICKAXE.get());
    }

    public static int getStored(ItemStack stack) {
        EnergyHandler handler = handler(stack);
        return handler == null ? stack.getOrDefault(ModComponents.ENERGY.get(), 0) : handler.getAmountAsInt();
    }

    public static int extract(ItemStack stack, int amount) {
        EnergyHandler handler = handler(stack);
        if (handler == null || amount <= 0) {
            return 0;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = handler.extract(amount, transaction);
            transaction.commit();
            return extracted;
        }
    }

    /**
     * Same roll vanilla tools use for Unbreaking: {@code 1/(level+1)} chance to pay, implemented as
     * {@code random.nextInt(level + 1) > 0} meaning the cost is skipped.
     */
    public static boolean shouldSkipCost(ItemStack stack, Player player) {
        Holder<Enchantment> unbreaking = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);
        int level = EnchantmentHelper.getItemEnchantmentLevel(unbreaking, stack);
        if (level <= 0) {
            return false;
        }
        return player.getRandom().nextInt(level + 1) > 0;
    }

    private static EnergyHandler handler(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return ItemAccess.forStack(stack).getCapability(Capabilities.Energy.ITEM);
    }
}
