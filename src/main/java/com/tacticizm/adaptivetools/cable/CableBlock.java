package com.tacticizm.adaptivetools.cable;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CableBlock extends BaseEntityBlock implements Wrenchable {
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final EnumProperty<CableAppearance> MODULES = EnumProperty.create("modules", CableAppearance.class);
    private static final BooleanProperty[] CONNECTED = {DOWN, UP, NORTH, SOUTH, WEST, EAST};
    private static final VoxelShape[] SHAPES = buildShapes();

    private final CableTier tier;

    public CableBlock(CableTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any()
                .setValue(DOWN, false)
                .setValue(UP, false)
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(EAST, false)
                .setValue(MODULES, CableAppearance.OFF));
    }

    public CableTier tier() {
        return tier;
    }

    public static void refresh(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CableBlock cable)) {
            return;
        }
        BlockState next = cable.withConnections(level, pos, state);
        if (!next.equals(state)) {
            level.setBlock(pos, next, Block.UPDATE_ALL);
        }
    }

    public BlockState withConnections(LevelReader level, BlockPos pos, BlockState state) {
        CableBlockEntity cable = level.getBlockEntity(pos) instanceof CableBlockEntity found ? found : null;
        BlockState next = state.setValue(MODULES, CableAppearance.of(cable));
        for (Direction direction : Direction.values()) {
            next = next.setValue(CONNECTED[direction.ordinal()], connects(level, pos, direction, cable));
        }
        return next;
    }

    @Override
    protected MapCodec<CableBlock> codec() {
        return Block.simpleCodec(props -> new CableBlock(tier, props));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST, MODULES);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(context.getLevel(), context.getClickedPos(), defaultBlockState());
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
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[mask(state)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[mask(state)];
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return SHAPES[mask(state)];
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) {
            BlockState connected = withConnections(level, pos, state);
            if (!connected.equals(state)) {
                level.setBlock(pos, connected, Block.UPDATE_ALL);
            }
            if (level instanceof net.minecraft.server.level.ServerLevel server) {
                CableNetworks.get(server).addCable(pos);
            }
        }
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighbor,
            RandomSource random) {
        return withConnections(level, pos, state);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, Orientation orientation, boolean movedByPiston) {
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
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
                refresh(level, pos);
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

    private boolean connects(LevelReader level, BlockPos pos, Direction direction, CableBlockEntity self) {
        if (self != null && self.face(direction).disconnected) {
            return false;
        }
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighbor = level.getBlockState(neighborPos);
        if (neighbor.getBlock() instanceof CableBlock) {
            return !(level.getBlockEntity(neighborPos) instanceof CableBlockEntity other) || !other.face(direction.getOpposite()).disconnected;
        }
        return Block.isFaceFull(neighbor.getCollisionShape(level, neighborPos), direction.getOpposite());
    }

    private static int mask(BlockState state) {
        int mask = 0;
        for (int i = 0; i < CONNECTED.length; i++) {
            if (state.getValue(CONNECTED[i])) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    private static VoxelShape[] buildShapes() {
        VoxelShape core = box(5, 5, 5, 11, 11, 11);
        VoxelShape[] arms = {
                box(5, 0, 5, 11, 5, 11),
                box(5, 11, 5, 11, 16, 11),
                box(5, 5, 0, 11, 11, 5),
                box(5, 5, 11, 11, 11, 16),
                box(0, 5, 5, 5, 11, 11),
                box(11, 5, 5, 16, 11, 11)
        };
        VoxelShape[] shapes = new VoxelShape[64];
        for (int mask = 0; mask < shapes.length; mask++) {
            VoxelShape shape = core;
            for (int arm = 0; arm < arms.length; arm++) {
                if ((mask & (1 << arm)) != 0) {
                    shape = Shapes.or(shape, arms[arm]);
                }
            }
            shapes[mask] = shape.optimize();
        }
        return shapes;
    }
}
