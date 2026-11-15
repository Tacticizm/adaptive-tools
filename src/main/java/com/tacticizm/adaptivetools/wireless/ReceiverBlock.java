package com.tacticizm.adaptivetools.wireless;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ReceiverBlock extends MachineBlock {
    public static final MapCodec<ReceiverBlock> CODEC = simpleCodec(ReceiverBlock::new);
    public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class);
    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);

    public enum Mode implements StringRepresentable {
        UNASSIGNED("unassigned"),
        IDLE("idle"),
        ACTIVE("active");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public ReceiverBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MODE, Mode.UNASSIGNED));
    }

    @Override
    protected MapCodec<ReceiverBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReceiverBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MODE);
    }

    @Override
    protected boolean shouldChangedStateKeepBlockEntity(BlockState state) {
        return state.getBlock() instanceof ReceiverBlock;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ReceiverBlockEntity receiver) {
            if (receiver.owner().getLeastSignificantBits() == 0 && receiver.owner().getMostSignificantBits() == 0) {
                receiver.setOwner(player.getUUID());
            }
            player.openMenu(receiver, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult rotate(BlockState state, Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            level.setBlock(pos, state.cycle(FACING), Block.UPDATE_ALL);
        }
        return InteractionResult.SUCCESS;
    }
}
