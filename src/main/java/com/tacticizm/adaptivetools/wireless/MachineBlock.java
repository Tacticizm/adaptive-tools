package com.tacticizm.adaptivetools.wireless;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.tacticizm.adaptivetools.cable.Wrenchable;

/** Wrench rotates with a plain click and picks the block up when sneaking. */
public abstract class MachineBlock extends BaseEntityBlock implements Wrenchable {
    protected MachineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected abstract MapCodec<? extends MachineBlock> codec();

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public static void onDrops(net.neoforged.neoforge.event.level.BlockDropsEvent event) {
        if (!(event.getBlockEntity() instanceof MachineInventory machine) || !(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        event.getDrops().clear();
        event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(level, event.getPos().getX() + 0.5, event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5, machine.toStack()));
    }

    @Override
    public InteractionResult wrench(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                dropMachine(level, pos);
                level.removeBlock(pos, false);
            }
            return InteractionResult.SUCCESS;
        }
        return rotate(state, level, pos);
    }

    protected abstract InteractionResult rotate(BlockState state, Level level, BlockPos pos);

    protected void dropMachine(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MachineInventory machine && !blockEntity.isRemoved()) {
            popResource(level, pos, machine.toStack());
        }
    }
}
