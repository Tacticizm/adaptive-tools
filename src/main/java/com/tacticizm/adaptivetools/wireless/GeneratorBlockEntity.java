package com.tacticizm.adaptivetools.wireless;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import com.tacticizm.adaptivetools.ModBlocks;

/** Burns furnace fuel into a 10M buffer and pushes 1,000 FE/t to neighbors. */
public class GeneratorBlockEntity extends BlockEntity implements MenuProvider, MachineInventory {
    public static final int CAPACITY = 10_000_000;
    public static final int FE_PER_TICK = 1_000;

    public final SimpleContainer fuel = new SimpleContainer(1);
    private int energy;
    private int burnTime;
    private int burnTotal;
    private int outputBudget = FE_PER_TICK;
    public final EnergyHandler energyHandler = new EnergyHandler() {
        @Override
        public long getAmountAsLong() {
            return energy;
        }

        @Override
        public long getCapacityAsLong() {
            return CAPACITY;
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            int moved = Math.min(amount, Math.min(energy, Math.max(0, outputBudget)));
            if (moved <= 0) {
                return 0;
            }
            energy -= moved;
            outputBudget -= moved;
            setChanged();
            return moved;
        }
    };

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.GENERATOR_BE.get(), pos, state);
    }

    public int energy() {
        return energy;
    }

    public int burnTime() {
        return burnTime;
    }

    /** Test setup. Clears the fuel slot so a later tick cannot start a new burn. */
    public void prepare(int energy, int burnTime) {
        this.energy = Math.max(0, Math.min(CAPACITY, energy));
        this.burnTime = Math.max(0, burnTime);
        fuel.setItem(0, ItemStack.EMPTY);
        setChanged();
    }

    public void serverTick() {
        outputBudget = FE_PER_TICK;
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (burnTime <= 0) {
            ItemStack stack = fuel.getItem(0);
            int duration = stack.isEmpty() ? 0 : server.fuelValues().burnDuration(stack);
            if (duration > 0 && energy < CAPACITY) {
                stack.shrink(1);
                burnTime = duration;
                burnTotal = duration;
                setChanged();
            }
        }
        if (burnTime > 0) {
            burnTime--;
            energy = Math.min(CAPACITY, energy + FE_PER_TICK);
            setChanged();
        }
        push(server);
    }

    private void push(ServerLevel server) {
        for (Direction direction : Direction.values()) {
            if (outputBudget <= 0 || energy <= 0) {
                return;
            }
            EnergyHandler neighbor = server.getCapability(Capabilities.Energy.BLOCK, worldPosition.relative(direction), direction.getOpposite());
            EnergyMove.move(energyHandler, neighbor, Math.min(outputBudget, energy));
        }
    }

    @Override
    public ItemStack toStack() {
        ItemStack stack = new ItemStack(ModBlocks.GENERATOR_ITEM.get());
        saveToItem(stack);
        return stack;
    }

    public void saveToItem(ItemStack stack) {
        net.minecraft.world.level.storage.TagValueOutput output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
        saveAdditional(output);
        net.minecraft.world.item.BlockItem.setBlockEntityData(stack, getType(), output);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("energy", energy);
        output.putInt("burn", burnTime);
        output.putInt("burn_total", burnTotal);
        output.store("fuel", ItemStack.CODEC, fuel.getItem(0));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy = input.getIntOr("energy", 0);
        burnTime = input.getIntOr("burn", 0);
        burnTotal = input.getIntOr("burn_total", 0);
        fuel.setItem(0, input.read("fuel", ItemStack.CODEC).orElse(ItemStack.EMPTY));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.adaptivetools.base_generator");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new GeneratorMenu(id, inventory, this);
    }
}
