package com.tacticizm.adaptivetools.testing;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Stand-in for a neighbor mod's machine. Not added to the creative tab. */
public class TestBufferBlock extends BaseEntityBlock {
    public static final MapCodec<TestBufferBlock> CODEC = Block.simpleCodec(TestBufferBlock::new);

    public TestBufferBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<TestBufferBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TestBufferBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
