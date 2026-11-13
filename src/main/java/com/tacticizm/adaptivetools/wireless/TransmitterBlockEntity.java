package com.tacticizm.adaptivetools.wireless;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import com.tacticizm.adaptivetools.ModBlocks;
import com.tacticizm.adaptivetools.ModItems;

/** FE buffer for one channel. Capacity modules add 1M each, up to three. */
public class TransmitterBlockEntity extends BlockEntity implements MenuProvider, MachineInventory {
    public static final int BASE_CAPACITY = 1_000_000;
    public static final int MODULE_CAPACITY = 1_000_000;
    public static final int MAX_MODULES = 3;
    /** Provisional send rate onto a channel. */
    public static final int FE_PER_TICK = 1_000;

    public final SimpleContainer modules = new SimpleContainer(2);
    private int energy;
    private UUID channelId;
    private UUID owner = new UUID(0, 0);
    private boolean allowRemoval;
    public int sentThisTick;
    public int lastSent;

    public final EnergyHandler energyHandler = new EnergyHandler() {
        @Override
        public long getAmountAsLong() {
            return energy;
        }

        @Override
        public long getCapacityAsLong() {
            return capacity();
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            int moved = Math.min(amount, capacity() - energy);
            if (moved <= 0) {
                return 0;
            }
            energy += moved;
            setChanged();
            return moved;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            int moved = Math.min(amount, energy);
            if (moved <= 0) {
                return 0;
            }
            energy -= moved;
            setChanged();
            return moved;
        }
    };

    public TransmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.TRANSMITTER_BE.get(), pos, state);
        WirelessDevices.add(this);
    }

    public int energy() {
        return energy;
    }

    public int capacity() {
        return BASE_CAPACITY + capacityModules() * MODULE_CAPACITY;
    }

    public int capacityModules() {
        ItemStack stack = modules.getItem(0);
        if (!stack.is(ModItems.CAPACITY_MODULE.get())) {
            return 0;
        }
        return Math.min(MAX_MODULES, stack.getCount());
    }

    public boolean hasCrossDimension() {
        return modules.getItem(1).is(ModItems.CROSS_DIMENSION_MODULE.get());
    }

    public UUID channelId() {
        return channelId;
    }

    public UUID owner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner == null ? new UUID(0, 0) : owner;
        setChanged();
    }

    public void assign(UUID channel) {
        this.channelId = channel;
        setChanged();
    }

    /** FE that would be deleted by taking one capacity module off. Zero when nothing is lost. */
    public int energyLostIfRemoved() {
        int next = BASE_CAPACITY + Math.max(0, capacityModules() - 1) * MODULE_CAPACITY;
        return Math.max(0, energy - next);
    }

    public boolean canTakeCapacity() {
        return allowRemoval || energyLostIfRemoved() <= 0;
    }

    public void armRemoval() {
        allowRemoval = true;
        setChanged();
    }

    public void onCapacityTaken() {
        if (energy > capacity()) {
            energy = capacity();
        }
        allowRemoval = false;
        setChanged();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        WirelessDevices.add(this);
        forgetMissingChannel();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        WirelessDevices.remove(this);
    }

    public void forgetMissingChannel() {
        if (channelId != null && level instanceof net.minecraft.server.level.ServerLevel server && ChannelDirectory.get(server).get(channelId) == null) {
            channelId = null;
            setChanged();
        }
    }

    @Override
    public ItemStack toStack() {
        ItemStack stack = new ItemStack(ModBlocks.TRANSMITTER_ITEM.get());
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
        if (channelId != null) {
            output.putString("channel", channelId.toString());
        }
        output.putString("owner", owner.toString());
        output.store("capacity", ItemStack.CODEC, modules.getItem(0));
        output.store("cross", ItemStack.CODEC, modules.getItem(1));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy = input.getIntOr("energy", 0);
        channelId = input.getString("channel").map(UUID::fromString).orElse(null);
        owner = input.getString("owner").map(UUID::fromString).orElse(new UUID(0, 0));
        modules.setItem(0, input.read("capacity", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        modules.setItem(1, input.read("cross", ItemStack.CODEC).orElse(ItemStack.EMPTY));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.adaptivetools.wireless_energy_transmitter");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new TransmitterMenu(id, inventory, this);
    }
}
