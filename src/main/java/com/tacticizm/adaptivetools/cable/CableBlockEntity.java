package com.tacticizm.adaptivetools.cable;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.tacticizm.adaptivetools.ModBlocks;
import com.tacticizm.adaptivetools.ModItems;

public class CableBlockEntity extends BlockEntity implements MenuProvider {
    private final FaceConfig[] faces = new FaceConfig[6];
    private Direction openingFace = Direction.NORTH;
    private boolean dropped;

    public CableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.CABLE_BE.get(), pos, state);
        for (int i = 0; i < faces.length; i++) {
            faces[i] = new FaceConfig();
        }
    }

    public CableTier tier() {
        return getBlockState().getBlock() instanceof CableBlock cable ? cable.tier() : CableTier.LIGHT;
    }

    public FaceConfig face(Direction direction) {
        return faces[direction.ordinal()];
    }

    public void setOpeningFace(Direction face) {
        this.openingFace = face;
    }

    public boolean toggleDisconnected(Direction direction) {
        FaceConfig face = face(direction);
        face.disconnected = !face.disconnected;
        setChanged();
        invalidateNetwork();
        syncAppearance();
        return face.disconnected;
    }

    public boolean installPower(Direction direction) {
        return install(face(direction).powerModule, () -> face(direction).powerModule = true);
    }

    public boolean removePower(Direction direction) {
        return remove(face(direction).powerModule, () -> face(direction).powerModule = false);
    }

    public boolean installItem(Direction direction) {
        if (face(direction).itemModule) {
            return false;
        }
        face(direction).itemModule = true;
        face(direction).itemMode = TransferMode.DISABLED;
        setChanged();
        syncAppearance();
        return true;
    }

    public boolean removeItem(Direction direction) {
        if (!face(direction).itemModule) {
            return false;
        }
        face(direction).itemModule = false;
        face(direction).itemMode = TransferMode.DISABLED;
        setChanged();
        syncAppearance();
        return true;
    }

    public boolean installFluid(Direction direction) {
        if (face(direction).fluidModule) {
            return false;
        }
        face(direction).fluidModule = true;
        face(direction).fluidMode = TransferMode.DISABLED;
        setChanged();
        syncAppearance();
        return true;
    }

    public boolean removeFluid(Direction direction) {
        if (!face(direction).fluidModule) {
            return false;
        }
        face(direction).fluidModule = false;
        face(direction).fluidMode = TransferMode.DISABLED;
        setChanged();
        syncAppearance();
        return true;
    }

    public boolean addRate(Direction direction) {
        if (face(direction).rateModules >= 2) {
            return false;
        }
        face(direction).rateModules++;
        setChanged();
        invalidateNetwork();
        return true;
    }

    public boolean removeRate(Direction direction) {
        if (face(direction).rateModules <= 0) {
            return false;
        }
        face(direction).rateModules--;
        setChanged();
        invalidateNetwork();
        return true;
    }

    public void setItemMode(Direction direction, TransferMode mode) {
        if (!face(direction).itemModule) {
            return;
        }
        face(direction).itemMode = mode;
        setChanged();
    }

    public void setFluidMode(Direction direction, TransferMode mode) {
        if (!face(direction).fluidModule) {
            return;
        }
        face(direction).fluidMode = mode;
        setChanged();
    }

    public void setItemPriority(Direction direction, int priority) {
        face(direction).itemPriority = priority;
        setChanged();
    }

    public void setFluidPriority(Direction direction, int priority) {
        face(direction).fluidPriority = priority;
        setChanged();
    }

    public void setItemFilter(Direction direction, int slot, ItemStack stack) {
        if (slot < 0 || slot >= FaceConfig.FILTER_SIZE) {
            return;
        }
        face(direction).itemFilters[slot] = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        setChanged();
    }

    public void setFluidFilter(Direction direction, int slot, ItemStack stack) {
        if (slot < 0 || slot >= FaceConfig.FILTER_SIZE) {
            return;
        }
        face(direction).fluidFilters[slot] = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        setChanged();
    }

    public void resetFace(Direction direction) {
        face(direction).resetSettings();
        setChanged();
    }

    public List<ItemStack> moduleStacks() {
        List<ItemStack> drops = new ArrayList<>();
        for (FaceConfig face : faces) {
            if (face.powerModule) {
                drops.add(new ItemStack(ModItems.POWER_MODULE.get()));
            }
            if (face.itemModule) {
                drops.add(new ItemStack(ModItems.ITEM_MODULE.get()));
            }
            if (face.fluidModule) {
                drops.add(new ItemStack(ModItems.FLUID_MODULE.get()));
            }
            if (face.rateModules > 0) {
                drops.add(new ItemStack(ModItems.RATE_MODULE.get(), face.rateModules));
            }
        }
        return drops;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (dropped || level == null || level.isClientSide()) {
            return;
        }
        dropped = true;
        Block.popResource(level, pos, new ItemStack(state.getBlock()));
        for (ItemStack module : moduleStacks()) {
            Block.popResource(level, pos, module);
        }
        if (level instanceof ServerLevel server) {
            CableNetworks.get(server).removeCable(pos);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) {
            CableNetworks.get(server).addCable(worldPosition);
        }
        syncAppearance();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ValueOutput.ValueOutputList list = output.childrenList("faces");
        for (FaceConfig face : faces) {
            face.save(list.addChild());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int index = 0;
        for (ValueInput child : input.childrenListOrEmpty("faces")) {
            if (index >= faces.length) {
                break;
            }
            faces[index++].load(child);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.adaptivetools." + tier().id());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CableMenu(id, inventory, this, openingFace);
    }

    private boolean install(boolean already, Runnable action) {
        if (already) {
            return false;
        }
        action.run();
        setChanged();
        syncAppearance();
        return true;
    }

    private boolean remove(boolean present, Runnable action) {
        if (!present) {
            return false;
        }
        action.run();
        setChanged();
        syncAppearance();
        return true;
    }

    private void syncAppearance() {
        if (level != null && !level.isClientSide()) {
            CableBlock.refresh(level, worldPosition);
        }
    }

    private void invalidateNetwork() {
        if (level instanceof ServerLevel server) {
            CableNetworks.get(server).invalidate(worldPosition);
        }
    }
}
