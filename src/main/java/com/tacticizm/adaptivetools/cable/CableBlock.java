package com.tacticizm.adaptivetools.cable;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

public class CableBlock extends BaseEntityBlock implements Wrenchable {
    private final CableTier tier;

    public CableBlock(CableTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public CableTier tier() {
        return tier;
    }

    @Override
    protected MapCodec<CableBlock> codec() {
        return Block.simpleCodec(props -> new CableBlock(tier, props));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this)) {
            CableNetworks.get(server).addCable(pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, Orientation orientation, boolean movedByPiston) {
        if (level instanceof ServerLevel server) {
            CableNetworks.get(server).invalidate(pos);
        }
    }

    @Override
    protected boolean shouldChangedStateKeepBlockEntity(BlockState state) {
        return state.getBlock() instanceof CableBlock;
    }

    @Override
    public InteractionResult wrench(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CableBlockEntity cable)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                cable.toggleDisconnected(hit.getDirection());
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            cable.setOpeningFace(hit.getDirection());
            player.openMenu(cable, buf -> {
                buf.writeBlockPos(pos);
                buf.writeEnum(hit.getDirection());
            });
        }
        return InteractionResult.SUCCESS;
    }
}
