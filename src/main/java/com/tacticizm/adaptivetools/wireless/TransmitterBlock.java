package com.tacticizm.adaptivetools.wireless;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class TransmitterBlock extends MachineBlock {
    public static final MapCodec<TransmitterBlock> CODEC = simpleCodec(TransmitterBlock::new);

    public TransmitterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<TransmitterBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TransmitterBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TransmitterBlockEntity transmitter) {
            if (transmitter.owner().getLeastSignificantBits() == 0 && transmitter.owner().getMostSignificantBits() == 0) {
                transmitter.setOwner(player.getUUID());
            }
            player.openMenu(transmitter, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult rotate(BlockState state, Level level, BlockPos pos) {
        return InteractionResult.SUCCESS;
    }
}
