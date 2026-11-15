package com.tacticizm.adaptivetools.wireless;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.tacticizm.adaptivetools.ModBlocks;

/** Sits against a machine and delivers one channel to that face. */
public class ReceiverBlockEntity extends BlockEntity implements MenuProvider, MachineInventory {
    public enum LinkMode {
        OFF,
        SEND,
        RECEIVE;

        public LinkMode next() {
            return values()[Math.floorMod(ordinal() + 1, values().length)];
        }

        public static LinkMode byOrdinal(int ordinal) {
            return values()[Math.floorMod(ordinal, values().length)];
        }
    }

    private UUID channelId;
    private UUID owner = new UUID(0, 0);
    private boolean itemModule;
    private boolean fluidModule;
    private LinkMode itemMode = LinkMode.OFF;
    private LinkMode fluidMode = LinkMode.OFF;
    public int lastMoved;

    public ReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.RECEIVER_BE.get(), pos, state);
        WirelessDevices.add(this);
    }

    public Direction facing() {
        return getBlockState().getValue(ReceiverBlock.FACING);
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
        refreshMode(false);
    }

    public boolean itemModule() {
        return itemModule;
    }

    public boolean fluidModule() {
        return fluidModule;
    }

    public LinkMode itemMode() {
        return itemModule ? itemMode : LinkMode.OFF;
    }

    public LinkMode fluidMode() {
        return fluidModule ? fluidMode : LinkMode.OFF;
    }

    public void installItemModule() {
        itemModule = true;
        if (itemMode == LinkMode.OFF) {
            itemMode = LinkMode.RECEIVE;
        }
        setChanged();
    }

    public void installFluidModule() {
        fluidModule = true;
        if (fluidMode == LinkMode.OFF) {
            fluidMode = LinkMode.RECEIVE;
        }
        setChanged();
    }

    public void cycleItem() {
        if (itemModule) {
            itemMode = itemMode.next();
            setChanged();
        }
    }

    public void cycleFluid() {
        if (fluidModule) {
            fluidMode = fluidMode.next();
            setChanged();
        }
    }

    public void setItemMode(LinkMode mode) {
        itemModule = mode != LinkMode.OFF;
        itemMode = mode;
        setChanged();
    }

    public void setFluidMode(LinkMode mode) {
        fluidModule = mode != LinkMode.OFF;
        fluidMode = mode;
        setChanged();
    }

    public void markMoved(int amount) {
        lastMoved += amount;
        if (amount > 0) {
            refreshMode(true);
        }
    }

    public void refreshMode(boolean active) {
        if (level == null || level.isClientSide()) {
            return;
        }
        ReceiverBlock.Mode mode = channelId == null ? ReceiverBlock.Mode.UNASSIGNED : active || lastMoved > 0 ? ReceiverBlock.Mode.ACTIVE : ReceiverBlock.Mode.IDLE;
        BlockState state = getBlockState();
        if (state.getValue(ReceiverBlock.MODE) != mode) {
            level.setBlock(worldPosition, state.setValue(ReceiverBlock.MODE, mode), 3);
        }
    }

    public void beginTick() {
        lastMoved = 0;
    }

    public void endTick() {
        refreshMode(lastMoved > 0);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        WirelessDevices.add(this);
        if (channelId != null && level instanceof net.minecraft.server.level.ServerLevel server && ChannelDirectory.get(server).get(channelId) == null) {
            channelId = null;
            setChanged();
        }
        refreshMode(false);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        WirelessDevices.remove(this);
    }

    @Override
    public ItemStack toStack() {
        ItemStack stack = new ItemStack(ModBlocks.RECEIVER_ITEM.get());
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
        if (channelId != null) {
            output.putString("channel", channelId.toString());
        }
        output.putString("owner", owner.toString());
        output.putBoolean("item_module", itemModule);
        output.putBoolean("fluid_module", fluidModule);
        output.putInt("item_mode", itemMode.ordinal());
        output.putInt("fluid_mode", fluidMode.ordinal());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        channelId = input.getString("channel").map(UUID::fromString).orElse(null);
        owner = input.getString("owner").map(UUID::fromString).orElse(new UUID(0, 0));
        itemModule = input.getBooleanOr("item_module", false);
        fluidModule = input.getBooleanOr("fluid_module", false);
        itemMode = LinkMode.byOrdinal(input.getIntOr("item_mode", 0));
        fluidMode = LinkMode.byOrdinal(input.getIntOr("fluid_mode", 0));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.adaptivetools.machine_receiver");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ReceiverMenu(id, inventory, this);
    }
}
