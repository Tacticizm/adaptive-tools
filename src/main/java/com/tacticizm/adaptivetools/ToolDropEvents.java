package com.tacticizm.adaptivetools;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/** Charcoal processing and the hoe's "no experience for tilling or harvesting". */
public final class ToolDropEvents {
    private ToolDropEvents() {}

    public static void onDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (tool.getItem() instanceof AdaptiveHoeItem) {
            event.setDroppedExperience(0);
        }
        if (!(tool.getItem() instanceof AdaptiveAxeItem axe) || !axe.tier().canCharcoal()) {
            return;
        }
        ToolSettings settings = ToolSettings.of(tool, axe.tier());
        if (!settings.charcoal() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = event.getState();
        if (!state.is(net.minecraft.tags.BlockTags.LOGS) || !smeltsToCharcoal(level, state)) {
            return;
        }
        event.getDrops().clear();
        double x = event.getPos().getX() + 0.5;
        double y = event.getPos().getY() + 0.5;
        double z = event.getPos().getZ() + 0.5;
        event.getDrops().add(new ItemEntity(level, x, y, z, new ItemStack(Items.CHARCOAL)));
    }

    private static boolean smeltsToCharcoal(ServerLevel level, BlockState state) {
        var found = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(new ItemStack(state.getBlock())), level);
        return found.isPresent() && found.get().value().assemble(new SingleRecipeInput(new ItemStack(state.getBlock()))).is(Items.CHARCOAL);
    }
}
