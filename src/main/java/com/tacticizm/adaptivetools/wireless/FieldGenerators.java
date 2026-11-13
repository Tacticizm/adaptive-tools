package com.tacticizm.adaptivetools.wireless;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

import com.tacticizm.adaptivetools.ModComponents;
import com.tacticizm.adaptivetools.ToolSettings;

/** One carried field generator burns fuel and charges FE items. The first one in the inventory wins. */
public final class FieldGenerators {
    private static final int DEFAULTS = ToolSettings.FIELD_ON | ToolSettings.FIELD_HOTBAR | ToolSettings.FIELD_MAIN | ToolSettings.FIELD_ARMOR;

    private FieldGenerators() {}

    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            tick(player);
        }
    }

    public static ToolSettings settings(ItemStack stack) {
        ToolSettings stored = stack.get(ModComponents.TOOL_SETTINGS.get());
        if (stored == null) {
            return new ToolSettings(DEFAULTS, 1, 1, Integer.MIN_VALUE, 1, 3, 3, 4, 4);
        }
        return stored;
    }

    public static boolean enabled(ItemStack stack) {
        return settings(stack).fieldOn();
    }

    public static void toggle(ServerPlayer player) {
        int slot = findFirst(player);
        if (slot < 0) {
            return;
        }
        ItemStack stack = player.getInventory().getItem(slot);
        ToolSettings next = settings(stack).toggle(ToolSettings.FIELD_ON);
        stack.set(ModComponents.TOOL_SETTINGS.get(), next);
        player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(next.fieldOn()
                ? "adaptivetools.status.field_on"
                : "adaptivetools.status.field_off"));
    }

    public static void tick(ServerPlayer player) {
        int slot = findFirst(player);
        if (slot < 0) {
            return;
        }
        ItemStack stack = player.getInventory().getItem(slot);
        if (!(stack.getItem() instanceof FieldGeneratorItem generator) || !enabled(stack)) {
            return;
        }
        FieldGeneratorMenu menu = openMenu(player, slot);
        burn(player, stack, generator, menu);
        charge(player, stack, generator, slot, menu);
    }

    private static void burn(ServerPlayer player, ItemStack stack, FieldGeneratorItem generator, FieldGeneratorMenu menu) {
        int energy = stack.getOrDefault(ModComponents.ENERGY.get(), 0);
        int burn = stack.getOrDefault(ModComponents.FIELD_BURN.get(), 0);
        ItemStack fuel = menu != null ? menu.fuel.getItem(0).copy() : copy(fuel(stack));
        if (burn <= 0 && energy < generator.capacity()) {
            int duration = fuel.isEmpty() ? 0 : player.level().fuelValues().burnDuration(fuel);
            if (duration > 0) {
                fuel.shrink(1);
                burn = duration;
            }
        }
        if (burn > 0) {
            burn--;
            energy = Math.min(generator.capacity(), energy + generator.generationPerTick());
        }
        stack.set(ModComponents.ENERGY.get(), energy);
        stack.set(ModComponents.FIELD_BURN.get(), burn);
        store(stack, fuel);
        if (menu != null) {
            menu.fuel.setItem(0, fuel.copy());
        }
    }

    private static void charge(ServerPlayer player, ItemStack stack, FieldGeneratorItem generator, int generatorSlot, FieldGeneratorMenu menu) {
        int energy = stack.getOrDefault(ModComponents.ENERGY.get(), 0);
        if (energy <= 0) {
            return;
        }
        List<Target> targets = targets(player, stack, generator, generatorSlot, menu);
        if (targets.isEmpty()) {
            return;
        }
        int budget = Math.min(energy, generator.chargePerTick());
        if (!generator.advanced()) {
            Target lowest = targets.get(0);
            for (Target target : targets) {
                if (target.ratio < lowest.ratio) {
                    lowest = target;
                }
            }
            budget -= give(lowest, budget);
        } else {
            while (budget > 0) {
                double min = Double.MAX_VALUE;
                for (Target target : targets) {
                    if (target.room() > 0) {
                        min = Math.min(min, target.ratio());
                    }
                }
                if (min == Double.MAX_VALUE) {
                    break;
                }
                List<Target> tied = new ArrayList<>();
                double next = 1.0;
                for (Target target : targets) {
                    if (target.room() <= 0) {
                        continue;
                    }
                    if (Math.abs(target.ratio() - min) < 0.0000001) {
                        tied.add(target);
                    } else if (target.ratio() > min) {
                        next = Math.min(next, target.ratio());
                    }
                }
                if (tied.isEmpty()) {
                    break;
                }
                int share = Math.max(1, budget / tied.size());
                boolean moved = false;
                for (Target target : tied) {
                    long step = (long) Math.ceil(next * target.capacity) - target.stored;
                    if (step < 1) {
                        step = 1;
                    }
                    int give = (int) Math.min(share, Math.min(step, target.room()));
                    int taken = give(target, give);
                    budget -= taken;
                    if (taken > 0) {
                        moved = true;
                    }
                    if (budget <= 0) {
                        break;
                    }
                }
                if (!moved) {
                    break;
                }
            }
        }
        stack.set(ModComponents.ENERGY.get(), energy - (Math.min(energy, generator.chargePerTick()) - budget));
    }

    private static int give(Target target, int amount) {
        if (amount <= 0 || target.room() <= 0) {
            return 0;
        }
        int moved = EnergyMove.move(new Pocket(target), target.handler, Math.min(amount, target.room()));
        target.addStored(moved);
        return moved;
    }

    private static List<Target> targets(ServerPlayer player, ItemStack generatorStack, FieldGeneratorItem generator, int generatorSlot, FieldGeneratorMenu menu) {
        List<Target> targets = new ArrayList<>();
        add(targets, menu == null ? charge(generatorStack) : menu.charge.getItem(0));
        Inventory inventory = player.getInventory();
        ToolSettings settings = settings(generatorStack);
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (slot == generatorSlot) {
                continue;
            }
            if (!area(generator, settings, slot)) {
                continue;
            }
            add(targets, inventory.getItem(slot));
        }
        return targets;
    }

    private static void add(List<Target> targets, ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() instanceof FieldGeneratorItem) {
            return;
        }
        EnergyHandler handler = EnergyMove.item(stack);
        if (handler == null || handler.getCapacityAsLong() <= 0 || handler.getAmountAsLong() >= handler.getCapacityAsLong()) {
            return;
        }
        targets.add(new Target(handler, handler.getAmountAsLong(), handler.getCapacityAsLong()));
    }

    private static boolean area(FieldGeneratorItem generator, ToolSettings settings, int slot) {
        if (!generator.advanced()) {
            return slot != Inventory.SLOT_OFFHAND;
        }
        if (slot < 9) {
            return settings.fieldHotbar();
        }
        if (slot < Inventory.INVENTORY_SIZE) {
            return settings.fieldMain();
        }
        if (slot == Inventory.SLOT_OFFHAND) {
            return false;
        }
        EquipmentSlot equipment = Inventory.EQUIPMENT_SLOT_MAPPING.get(slot);
        return equipment != null && equipment.isArmor() && settings.fieldArmor();
    }

    static int findFirst(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).getItem() instanceof FieldGeneratorItem) {
                return slot;
            }
        }
        return -1;
    }

    private static FieldGeneratorMenu openMenu(ServerPlayer player, int slot) {
        if (player.containerMenu instanceof FieldGeneratorMenu menu && menu.slot() == slot) {
            return menu;
        }
        return null;
    }

    private static ItemStack copy(ItemStack stack) {
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    private static ItemStack fuel(ItemStack stack) {
        FrozenStack saved = stack.get(ModComponents.FIELD_FUEL.get());
        return saved == null ? ItemStack.EMPTY : saved.copy();
    }

    private static ItemStack charge(ItemStack stack) {
        FrozenStack saved = stack.get(ModComponents.FIELD_CHARGE.get());
        return saved == null ? ItemStack.EMPTY : saved.copy();
    }

    private static void store(ItemStack stack, ItemStack value) {
        if (value.isEmpty()) {
            stack.remove(ModComponents.FIELD_FUEL.get());
        } else {
            stack.set(ModComponents.FIELD_FUEL.get(), new FrozenStack(value));
        }
    }

    private static final class Target {
        private final EnergyHandler handler;
        private long stored;
        private final long capacity;
        private double ratio;

        private Target(EnergyHandler handler, long stored, long capacity) {
            this.handler = handler;
            this.stored = stored;
            this.capacity = capacity;
            this.ratio = stored / (double) capacity;
        }

        private int room() {
            return (int) Math.min(Integer.MAX_VALUE, capacity - stored);
        }

        private double ratio() {
            return ratio;
        }

        private void addStored(int amount) {
            stored += amount;
            ratio = stored / (double) capacity;
        }
    }

    /** A tiny source so {@link EnergyMove} can pull a counted amount of FE. */
    private static final class Pocket implements EnergyHandler {
        private int left;

        private Pocket(Target target) {
            this.left = target.room();
        }

        @Override
        public long getAmountAsLong() {
            return left;
        }

        @Override
        public long getCapacityAsLong() {
            return left;
        }

        @Override
        public int insert(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            return 0;
        }

        @Override
        public int extract(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            int moved = Math.min(amount, left);
            left -= moved;
            return moved;
        }
    }
}
