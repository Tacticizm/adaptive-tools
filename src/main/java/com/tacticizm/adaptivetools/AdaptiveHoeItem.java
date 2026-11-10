package com.tacticizm.adaptivetools;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Hoe. Till and harvest are separate toggles and share the area caps. A seed is reserved in the
 * inventory before any replant, so the last seed of a type is never spent. Only age-based crops
 * are harvested. Tilling does not grant experience.
 */
public class AdaptiveHoeItem extends AdaptiveToolItem {
    public AdaptiveHoeItem(ToolTier tier, Properties properties) {
        super(tier, properties.hoe(tier.toolMaterial(), tier.hoeDamageBaseline(), tier.hoeAttackSpeed()));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        ToolSettings settings = ToolSettings.of(stack, tier);
        BlockPos origin = context.getClickedPos();
        boolean ability = settings.effectiveSide(tier) > 1;
        int acted = 0;
        for (BlockPos pos : AreaMining.positions(origin, net.minecraft.core.Direction.UP, settings.effectiveSide(tier))) {
            if (!ToolBilling.canAfford(player.getMainHandItem(), player, tier, ability)) {
                break;
            }
            BlockState state = level.getBlockState(pos);
            boolean did = false;
            if (settings.harvest() && isMature(state)) {
                did = harvest(player, (ServerLevel) level, pos, state, settings);
            } else if (settings.till() && canTill(level, pos, state)) {
                level.setBlock(pos, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
                did = true;
            }
            if (did) {
                ToolBilling.pay(player.getMainHandItem(), player, tier, ability);
                acted++;
            }
        }
        return acted > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    private boolean harvest(Player player, ServerLevel level, BlockPos pos, BlockState state, ToolSettings settings) {
        ItemStack seed = seedFor(state);
        boolean replant = settings.replant() && !seed.isEmpty() && count(player.getInventory(), seed) > 1;
        BlockState replanted = replant ? planted(state) : null;
        level.destroyBlock(pos, true, player);
        if (replant && replanted != null && level.getBlockState(pos).isAir()) {
            if (consumeOne(player.getInventory(), seed)) {
                level.setBlock(pos, replanted, Block.UPDATE_ALL);
            }
        }
        return true;
    }

    public static boolean isMature(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (state.getBlock() instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
        }
        return false;
    }

    private static boolean canTill(Level level, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos.above()).isAir()) {
            return false;
        }
        return state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT_PATH) || state.is(Blocks.ROOTED_DIRT);
    }

    private static ItemStack seedFor(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.WHEAT) {
            return new ItemStack(Items.WHEAT_SEEDS);
        }
        if (block == Blocks.BEETROOTS) {
            return new ItemStack(Items.BEETROOT_SEEDS);
        }
        if (block == Blocks.CARROTS) {
            return new ItemStack(Items.CARROT);
        }
        if (block == Blocks.POTATOES) {
            return new ItemStack(Items.POTATO);
        }
        if (block == Blocks.NETHER_WART) {
            return new ItemStack(Items.NETHER_WART);
        }
        if (block instanceof CropBlock) {
            return new ItemStack(block);
        }
        return ItemStack.EMPTY;
    }

    private static BlockState planted(BlockState state) {
        IntegerProperty age = null;
        if (state.getBlock() instanceof CropBlock) {
            age = CropBlock.AGE;
        } else if (state.getBlock() instanceof NetherWartBlock) {
            age = NetherWartBlock.AGE;
        }
        if (age == null) {
            return state.getBlock().defaultBlockState();
        }
        return state.getBlock().defaultBlockState().setValue(age, 0);
    }

    private static int count(Inventory inventory, ItemStack seed) {
        int total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (ItemStack.isSameItem(stack, seed)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static boolean consumeOne(Inventory inventory, ItemStack seed) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (ItemStack.isSameItem(stack, seed) && !stack.isEmpty()) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }
}
