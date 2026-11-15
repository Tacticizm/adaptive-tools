package com.tacticizm.adaptivetools;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Embersteel durability items float in a lava source and repair. Flux and Ender are energy items,
 * so they are left alone. One item occupies a source. The lava becomes obsidian only after the
 * item is back in its owner's inventory.
 */
public final class LavaRepair {
    /** Provisional. Ten percent of the item's max durability, applied across one second. */
    public static final int PERCENT_PER_SECOND = 10;
    static final String TAG = "adaptivetools_lava_repair";
    private static final int THRESHOLD = 20 * (100 / PERCENT_PER_SECOND);
    private static final Map<UUID, Integer> BANK = new HashMap<>();

    private LavaRepair() {}

    public static void onTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof ItemEntity item && !item.level().isClientSide()) {
            tick(item);
        }
    }

    public static boolean repairs(ItemStack stack) {
        if (!(stack.getItem() instanceof TieredTool tool) || tool.tier().usesEnergy()) {
            return false;
        }
        return tool.tier().ordinal() >= ToolTier.EMBERSTEEL.ordinal();
    }

    public static void tick(ItemEntity item) {
        Level level = item.level();
        BlockPos pos = BlockPos.containing(item.getX(), item.getY(), item.getZ());
        FluidState fluid = level.getFluidState(pos);
        if (!fluid.is(FluidTags.LAVA) || !fluid.isSource() || !repairs(item.getItem())) {
            release(item);
            return;
        }
        if (!item.entityTags().contains(TAG)) {
            boolean taken = !level.getEntitiesOfClass(ItemEntity.class, new AABB(pos), other -> other != item && other.entityTags().contains(TAG)).isEmpty();
            if (taken) {
                return;
            }
            item.addTag(TAG);
        }
        item.setInvulnerable(true);
        item.setNoGravity(true);
        item.setUnlimitedLifetime();
        item.setRemainingFireTicks(0);
        item.setPos(pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5);
        item.setDeltaMovement(0.0, 0.0, 0.0);

        ItemStack stack = item.getItem();
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return;
        }
        int bank = BANK.getOrDefault(item.getUUID(), 0) + max;
        int heal = bank / THRESHOLD;
        BANK.put(item.getUUID(), bank % THRESHOLD);
        if (heal > 0 && stack.getDamageValue() > 0) {
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - heal));
            item.setItem(stack);
        }
        if (stack.getDamageValue() > 0) {
            return;
        }
        if (giveBack(item, stack)) {
            level.setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
            BANK.remove(item.getUUID());
            item.discard();
        }
    }

    private static boolean giveBack(ItemEntity item, ItemStack stack) {
        if (!(item.getOwner() instanceof Player player) || !player.isAlive() || player.level() != item.level()) {
            return false;
        }
        int slot = player.getInventory().getFreeSlot();
        if (slot < 0) {
            return false;
        }
        player.getInventory().setItem(slot, stack.copy());
        return true;
    }

    private static void release(ItemEntity item) {
        if (!item.entityTags().contains(TAG)) {
            return;
        }
        item.removeTag(TAG);
        item.setInvulnerable(false);
        item.setNoGravity(false);
        BANK.remove(item.getUUID());
    }
}
