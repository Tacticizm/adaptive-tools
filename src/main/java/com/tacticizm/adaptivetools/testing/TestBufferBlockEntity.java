package com.tacticizm.adaptivetools.testing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import com.tacticizm.adaptivetools.ModBlocks;

/** Exposes FE, items, and fluids on every side, which is what a connected machine does. */
public class TestBufferBlockEntity extends BlockEntity {
    public final SimpleEnergyHandler energy = new SimpleEnergyHandler(4_000_000, 4_000_000, 4_000_000, 0);
    public final ItemStacksResourceHandler items = new ItemStacksResourceHandler(9);
    public final FluidStacksResourceHandler fluids = new FluidStacksResourceHandler(1, 100_000);

    public TestBufferBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.TEST_BUFFER_BE.get(), pos, state);
    }
}
