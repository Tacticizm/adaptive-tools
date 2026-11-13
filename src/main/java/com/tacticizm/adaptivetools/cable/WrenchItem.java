package com.tacticizm.adaptivetools.cable;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;

/**
 * One wrench for every cable tier and any later {@link Wrenchable} machine.
 * Sneak-right-click toggles the face that was clicked. A plain right-click opens config.
 */
public class WrenchItem extends Item {
    public WrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof Wrenchable wrenchable && context.getPlayer() != null) {
            BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside());
            return wrenchable.wrench(context.getItemInHand(), state, level, pos, context.getPlayer(), context.getHand(), hit);
        }
        return InteractionResult.PASS;
    }
}
