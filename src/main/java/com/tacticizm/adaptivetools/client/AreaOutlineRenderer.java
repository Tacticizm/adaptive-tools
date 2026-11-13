package com.tacticizm.adaptivetools.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.ARGB;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.CustomBlockOutlineRenderer;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

import com.tacticizm.adaptivetools.AdaptivePickaxeItem;
import com.tacticizm.adaptivetools.AreaMining;
import com.tacticizm.adaptivetools.ToolSettings;

/**
 * Draws the vanilla block outline on every extra block the held pickaxe would break.
 * The targeted block keeps the outline vanilla already submits.
 */
public final class AreaOutlineRenderer implements CustomBlockOutlineRenderer {
    private final List<VoxelShape> shapes;
    private final boolean highContrast;

    private AreaOutlineRenderer(List<VoxelShape> shapes, boolean highContrast) {
        this.shapes = shapes;
        this.highContrast = highContrast;
    }

    public static void onExtract(ExtractBlockOutlineRenderStateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof com.tacticizm.adaptivetools.TieredTool held)) {
            return;
        }
        ToolSettings settings = ToolSettings.of(stack, held.tier());
        BlockPos origin = event.getBlockPos();
        List<BlockPos> positions;
        if (held instanceof com.tacticizm.adaptivetools.AdaptiveShovelItem shovel) {
            positions = shovel.targets(player, origin, settings);
        } else if (held instanceof AdaptivePickaxeItem || held instanceof com.tacticizm.adaptivetools.AdaptiveHoeItem) {
            int side = settings.effectiveSide(held.tier());
            if (side <= 1) {
                return;
            }
            positions = AreaMining.positions(origin, event.getHitResult().getDirection(), side);
        } else if (held instanceof com.tacticizm.adaptivetools.AdaptiveAxeItem && settings.fell()) {
            positions = com.tacticizm.adaptivetools.TreeFelling.logs(player.level(), origin, settings.effectiveFell(held.tier()), held.tier().includesNearbyTrees());
        } else {
            return;
        }
        if (positions.size() <= 1) {
            return;
        }
        GameType gameType = gameType(minecraft);
        List<VoxelShape> shapes = new ArrayList<>();
        for (BlockPos pos : positions) {
            if (pos.equals(origin)) {
                continue;
            }
            BlockState state = event.getLevel().getBlockState(pos);
            if (!AreaMining.canMine(event.getLevel(), pos, state)) {
                continue;
            }
            if (player.blockActionRestricted(event.getLevel(), pos, gameType)) {
                continue;
            }
            shapes.add(state.getShape(event.getLevel(), pos, event.getCollisionContext()).move(pos.getX(), pos.getY(), pos.getZ()));
        }
        if (!shapes.isEmpty()) {
            event.addCustomRenderer(new AreaOutlineRenderer(shapes, event.isHighContrast()));
        }
    }

    @Override
    public boolean render(BlockOutlineRenderState renderState, MultiBufferSource.BufferSource buffer, PoseStack poseStack, boolean translucentPass, LevelRenderState levelRenderState) {
        if (translucentPass) {
            return false;
        }
        Vec3 camera = levelRenderState.cameraRenderState.pos;
        // Same line width and black tint vanilla uses for the block the crosshair is on.
        float width = Minecraft.getInstance().gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth;
        int color = highContrast ? 0xFF57FFE1 : ARGB.black(102);
        var consumer = buffer.getBuffer(RenderTypes.lines());
        for (VoxelShape shape : shapes) {
            ShapeRenderer.renderShape(poseStack, consumer, shape, -camera.x, -camera.y, -camera.z, color, width);
        }
        return false;
    }

    private static GameType gameType(Minecraft minecraft) {
        MultiPlayerGameMode gameMode = minecraft.gameMode;
        return gameMode == null ? GameType.SURVIVAL : gameMode.getPlayerMode();
    }
}
