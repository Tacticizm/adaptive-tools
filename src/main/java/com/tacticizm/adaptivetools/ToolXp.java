package com.tacticizm.adaptivetools;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Tool XP, levels, and ability points. Tier still sets the maximum. Points unlock options inside it.
 * A stack with no progress component is grandfathered, so an existing playtest item keeps working.
 */
public final class ToolXp {
    public static final int LEAVES = 1;
    public static final int CHARCOAL = 1 << 1;
    public static final int TILL = 1 << 2;
    public static final int HARVEST = 1 << 3;
    public static final int REPLANT = 1 << 4;
    public static final int GRADE = 1 << 5;
    public static final int MOMENTUM = 1 << 6;
    public static final int GUARD = 1 << 7;
    public static final int SIPHON = 1 << 8;
    public static final int WATER = 1 << 9;
    public static final int ELYTRA = 1 << 10;
    public static final int FLIGHT = 1 << 11;
    public static final int SHIELD = 1 << 12;

    public static final int EFF_UP = 1;
    public static final int EFF_DOWN = 2;
    public static final int FORT_UP = 3;
    public static final int FORT_DOWN = 4;
    public static final int SILK = 5;
    public static final int RESET = 6;

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private ToolXp() {}

    public static void onCraftedEvent(PlayerEvent.ItemCraftedEvent event) {
        onCrafted(event.getCrafting());
    }

    /** A shaped craft starts at zero. Smithing already copied progress, so it is left alone. */
    public static void onCrafted(ItemStack stack) {
        if (stack.getItem() instanceof TieredTool && stack.get(ModComponents.TOOL_PROGRESS.get()) == null) {
            stack.set(ModComponents.TOOL_PROGRESS.get(), ToolProgress.fresh());
        }
    }

    public static void onBlockBroken(ServerPlayer player, BlockState state) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof AdaptiveHoeItem) {
            return;
        }
        if (stack.getItem() instanceof AdaptiveAxeItem) {
            if (state.is(BlockTags.LOGS)) {
                award(stack, 1);
            }
            return;
        }
        if (stack.getItem() instanceof AdaptivePickaxeItem || stack.getItem() instanceof AdaptiveShovelItem) {
            award(stack, 1);
        }
    }

    public static void onKill(ServerPlayer player, LivingEntity dead) {
        int amount = killXp(dead);
        ItemStack weapon = player.getMainHandItem();
        if (weapon.getItem() instanceof AdaptiveSwordItem) {
            award(weapon, amount);
        }
        if (dead instanceof Enemy) {
            for (EquipmentSlot slot : ARMOR) {
                ItemStack piece = player.getItemBySlot(slot);
                if (piece.getItem() instanceof AdaptiveArmorItem) {
                    award(piece, amount);
                }
            }
        }
    }

    public static int killXp(LivingEntity dead) {
        return Math.max(1, (int) (dead.getMaxHealth() / 10.0F));
    }

    /** One XP per ten damage absorbed. The leftover stays on the piece. */
    public static void noteShield(ItemStack stack, int damage) {
        if (damage <= 0 || !(stack.getItem() instanceof AdaptiveArmorItem)) {
            return;
        }
        ToolProgress progress = ensure(stack);
        int pool = progress.remainder() + damage;
        stack.set(ModComponents.TOOL_PROGRESS.get(), progress.withXp(progress.xp() + pool / 10).withRemainder(pool % 10));
    }

    public static void award(ItemStack stack, int amount) {
        if (amount <= 0 || !(stack.getItem() instanceof TieredTool)) {
            return;
        }
        ToolProgress progress = ensure(stack);
        stack.set(ModComponents.TOOL_PROGRESS.get(), progress.withXp(progress.xp() + amount));
    }

    public static int levelFor(int xp) {
        int level = 0;
        int remaining = Math.max(0, xp);
        int step = 50;
        while (remaining >= step) {
            remaining -= step;
            level++;
            step += 50;
        }
        return level;
    }

    public static int xpIntoLevel(int xp) {
        int level = levelFor(xp);
        return xp - xpToReach(level);
    }

    public static int xpToReach(int level) {
        return 50 * level * (level + 1) / 2;
    }

    public static int xpForNext(int level) {
        return 50 * (level + 1);
    }

    public static int cap(ItemStack stack) {
        if (!(stack.getItem() instanceof TieredTool tool)) {
            return 0;
        }
        return spent(maxed(stack.getItem(), tool.tier()));
    }

    public static int earned(ItemStack stack) {
        ToolProgress progress = stack.get(ModComponents.TOOL_PROGRESS.get());
        int xp = progress == null ? 0 : progress.xp();
        return Math.min(levelFor(xp), cap(stack));
    }

    public static int unspent(ItemStack stack) {
        ToolProgress progress = stack.get(ModComponents.TOOL_PROGRESS.get());
        if (progress == null || progress.isGrandfather()) {
            return earned(stack);
        }
        return Math.max(0, earned(stack) - spent(progress));
    }

    public static int percent(ItemStack stack) {
        if (cap(stack) > 0 && earned(stack) >= cap(stack)) {
            return 100;
        }
        ToolProgress progress = stack.get(ModComponents.TOOL_PROGRESS.get());
        int xp = progress == null ? 0 : progress.xp();
        int level = levelFor(xp);
        int need = xpForNext(level);
        return need <= 0 ? 100 : xpIntoLevel(xp) * 100 / need;
    }

    public static ToolSettings limit(ItemStack stack, ToolSettings settings, ToolTier tier) {
        ToolProgress progress = stack.get(ModComponents.TOOL_PROGRESS.get());
        if (progress == null || progress.isGrandfather() || !(stack.getItem() instanceof TieredTool)) {
            return settings;
        }
        return apply(stack.getItem(), settings, tier, progress);
    }

    /**
     * Turns requested settings on when they are already bought or the stack can pay the difference.
     * Lowering an active level does not refund the purchase.
     */
    public static ToolSettings purchase(ItemStack stack, ToolSettings before, ToolSettings requested, ToolTier tier) {
        if (!(stack.getItem() instanceof TieredTool)) {
            return requested;
        }
        ToolProgress existing = stack.get(ModComponents.TOOL_PROGRESS.get());
        if (existing == null || existing.isGrandfather()) {
            return requested.clamped(tier);
        }
        Item item = stack.getItem();
        int points = Math.max(0, earned(stack) - spent(existing));
        int efficiency = existing.efficiency();
        int efficiencyOn = existing.efficiencyOn();
        int fortune = existing.fortune();
        int fortuneOn = existing.fortuneOn();
        int silk = existing.silk();
        int silkOn = existing.silkOn();
        int area = Math.max(1, existing.area());
        int trench = Math.max(1, existing.trench());
        int fell = existing.fell();
        int unlocks = existing.unlocks();
        int cleave = existing.cleave();
        int chain = existing.chain();

        if (item instanceof AdaptivePickaxeItem || item instanceof AdaptiveShovelItem || item instanceof AdaptiveHoeItem) {
            int want = requested.areaMining() ? ToolSettings.clampSize(requested.areaSize(), tier.maxAreaSide()) : area;
            int bought = buySteps(areaSteps(area), areaSteps(want), points);
            points -= bought - areaSteps(area);
            area = sideFor(bought);
        }
        if (item instanceof AdaptiveShovelItem && tier.canTrench()) {
            int want = requested.trench() ? Math.min(requested.configuredTrench(tier), tier.maxTrench()) : trench;
            int bought = buySteps(areaSteps(trench), areaSteps(ToolSettings.clampSize(want, tier.maxTrench())), points);
            points -= bought - areaSteps(trench);
            trench = sideFor(bought);
        }
        if (item instanceof AdaptiveAxeItem && tier.maxFellLogs() > 0 && requested.fell()) {
            int want = Math.min(requested.configuredFell(tier), tier.maxFellLogs());
            while (points > 0 && fell < want) {
                int next = fell <= 0 ? Math.min(16, tier.maxFellLogs()) : Math.min(fell * 2, tier.maxFellLogs());
                if (next <= fell) {
                    break;
                }
                points--;
                fell = next;
            }
        }
        unlocks = buyBit(unlocks, LEAVES, requested.leafClear() && item instanceof AdaptiveAxeItem && fell > 0, points);
        points = budget(existing, stack, efficiency, fortune, silk, area, trench, fell, unlocks, cleave, chain);
        unlocks = buyBit(unlocks, CHARCOAL, requested.charcoal() && item instanceof AdaptiveAxeItem && tier.canCharcoal(), points);
        points = budget(existing, stack, efficiency, fortune, silk, area, trench, fell, unlocks, cleave, chain);
        unlocks = buyBit(unlocks, GRADE, requested.grading() && item instanceof AdaptiveShovelItem && tier.canGrade(), points);
        points = budget(existing, stack, efficiency, fortune, silk, area, trench, fell, unlocks, cleave, chain);
        unlocks = buyBit(unlocks, TILL, requested.till() && item instanceof AdaptiveHoeItem, points);
        points = budget(existing, stack, efficiency, fortune, silk, area, trench, fell, unlocks, cleave, chain);
        unlocks = buyBit(unlocks, HARVEST, requested.harvest() && item instanceof AdaptiveHoeItem, points);
        points = budget(existing, stack, efficiency, fortune, silk, area, trench, fell, unlocks, cleave, chain);
        unlocks = buyBit(unlocks, REPLANT, requested.replant() && item instanceof AdaptiveHoeItem, points);
        points = budget(existing, stack, efficiency, fortune, silk, area, trench, fell, unlocks, cleave, chain);
        if (item instanceof AdaptiveSwordItem && tier.canCleave() && requested.cleave()) {
            int want = Math.min(Math.max(1, requested.cleaveTargets()), tier.maxCleaveTargets());
            int gain = Math.min(points, Math.max(0, want - cleave));
            cleave += gain;
            points -= gain;
        }
        if (item instanceof AdaptiveSwordItem && tier.canChain() && requested.chain()) {
            int want = Math.min(Math.max(1, requested.chainTargets()), tier.maxChainTargets());
            int gain = Math.min(points, Math.max(0, want - chain));
            chain += gain;
            points -= gain;
        }
        unlocks = buyBit(unlocks, MOMENTUM, requested.momentum() && item instanceof AdaptiveSwordItem && tier.canMomentum(), points);
        points = budget(existing, stack, efficiency, fortune, silk, area, trench, fell, unlocks, cleave, chain);
        unlocks = buyBit(unlocks, GUARD, requested.guardBreak() && item instanceof AdaptiveSwordItem && tier.canGuardBreak(), points);
        points = budget(existing, stack, efficiency, fortune, silk, area, trench, fell, unlocks, cleave, chain);
        unlocks = buyBit(unlocks, SIPHON, requested.siphon() && item instanceof AdaptiveSwordItem && tier.canSiphon(), points);
        points = budget(existing, stack, efficiency, fortune, silk, area, trench, fell, unlocks, cleave, chain);
        if (item instanceof AdaptiveArmorItem armor) {
            unlocks = buyBit(unlocks, WATER, requested.waterWalking() && armor.canWaterWalk(), points);
            points = budget(existing, stack, efficiency, fortune, silk, area, trench, fell, unlocks, cleave, chain);
            unlocks = buyBit(unlocks, ELYTRA, requested.elytra() && armor.canElytra(), points);
            points = budget(existing, stack, efficiency, fortune, silk, area, trench, fell, unlocks, cleave, chain);
            unlocks = buyBit(unlocks, FLIGHT, requested.flight() && armor.canFly(), points);
            points = budget(existing, stack, efficiency, fortune, silk, area, trench, fell, unlocks, cleave, chain);
            unlocks = buyBit(unlocks, SHIELD, requested.energyShield() && armor.canShield(), points);
        }
        ToolProgress updated = new ToolProgress(
                existing.xp(), efficiency, efficiencyOn, fortune, fortuneOn, silk, silkOn,
                area, trench, fell, unlocks, cleave, chain, existing.remainder(), existing.grandfather());
        stack.set(ModComponents.TOOL_PROGRESS.get(), updated);
        return apply(item, requested, tier, updated);
    }

    public static void act(ServerPlayer player, int slot, int action) {
        if (slot < 0 || slot >= player.getInventory().getContainerSize()) {
            return;
        }
        ItemStack stack = player.getInventory().getItem(slot);
        if (!(stack.getItem() instanceof TieredTool tool)) {
            return;
        }
        if (action == RESET) {
            reset(stack, tool.tier());
            syncEnchantments(stack, player.level().registryAccess());
            return;
        }
        ToolProgress existing = stack.get(ModComponents.TOOL_PROGRESS.get());
        if (existing == null) {
            existing = ToolProgress.grandfathered();
        }
        boolean free = existing.isGrandfather();
        int points = free ? 1 : Math.max(0, Math.min(levelFor(existing.xp()), cap(stack)) - spent(existing));
        int efficiency = existing.efficiency();
        int efficiencyOn = existing.efficiencyOn();
        int fortune = existing.fortune();
        int fortuneOn = existing.fortuneOn();
        int silk = existing.silk();
        int silkOn = existing.silkOn();
        boolean enchantable = !(stack.getItem() instanceof AdaptiveArmorItem);
        if (enchantable && action == EFF_UP && efficiencyOn < 5 && (free || (efficiency < 5 && points > 0))) {
            if (!free) {
                efficiency++;
            }
            efficiencyOn = free ? efficiencyOn + 1 : efficiency;
        } else if (action == EFF_DOWN) {
            efficiencyOn = Math.max(0, efficiencyOn - 1);
        } else if (enchantable && action == FORT_UP && fortuneOn < 3 && (free || (fortune < 3 && points > 0))) {
            if (!free) {
                fortune++;
            }
            fortuneOn = free ? fortuneOn + 1 : fortune;
            silkOn = 0;
        } else if (action == FORT_DOWN) {
            fortuneOn = Math.max(0, fortuneOn - 1);
        } else if (enchantable && action == SILK) {
            if (silkOn > 0) {
                silkOn = 0;
            } else if (free || silk > 0 || points > 0) {
                if (!free && silk == 0) {
                    silk = 1;
                }
                silkOn = 1;
                fortuneOn = 0;
            }
        }
        stack.set(ModComponents.TOOL_PROGRESS.get(), new ToolProgress(
                existing.xp(), efficiency, efficiencyOn, fortune, fortuneOn, silk, silkOn,
                existing.area(), existing.trench(), existing.fell(), existing.unlocks(),
                existing.cleave(), existing.chain(), existing.remainder(), free ? 1 : 0));
        syncEnchantments(stack, player.level().registryAccess());
    }

    public static void reset(ItemStack stack, ToolTier tier) {
        ToolProgress existing = stack.get(ModComponents.TOOL_PROGRESS.get());
        if (existing == null) {
            existing = ToolProgress.grandfathered();
        }
        stack.set(ModComponents.TOOL_PROGRESS.get(), existing.cleared());
        stack.set(ModComponents.TOOL_SETTINGS.get(), ToolSettings.defaults(tier));
    }

    public static void syncEnchantments(ItemStack stack, RegistryAccess access) {
        ToolProgress progress = stack.get(ModComponents.TOOL_PROGRESS.get());
        if (progress == null || access == null || stack.getItem() instanceof AdaptiveArmorItem) {
            return;
        }
        var lookup = access.lookupOrThrow(Registries.ENCHANTMENT);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        ItemEnchantments current = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        current.entrySet().forEach(entry -> {
            var key = entry.getKey();
            if (!key.is(Enchantments.EFFICIENCY) && !key.is(Enchantments.FORTUNE) && !key.is(Enchantments.SILK_TOUCH)) {
                mutable.set(key, entry.getIntValue());
            }
        });
        if (progress.efficiencyOn() > 0) {
            mutable.set(lookup.getOrThrow(Enchantments.EFFICIENCY), progress.efficiencyOn());
        }
        if (progress.silkOn() > 0) {
            mutable.set(lookup.getOrThrow(Enchantments.SILK_TOUCH), 1);
        } else if (progress.fortuneOn() > 0) {
            mutable.set(lookup.getOrThrow(Enchantments.FORTUNE), progress.fortuneOn());
        }
        stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
    }

    private static ToolProgress ensure(ItemStack stack) {
        ToolProgress progress = stack.get(ModComponents.TOOL_PROGRESS.get());
        return progress == null ? ToolProgress.grandfathered() : progress;
    }

    private static int budget(ToolProgress existing, ItemStack stack, int efficiency, int fortune, int silk, int area, int trench, int fell, int unlocks, int cleave, int chain) {
        ToolProgress draft = new ToolProgress(existing.xp(), efficiency, 0, fortune, 0, silk, 0, area, trench, fell, unlocks, cleave, chain, 0, 0);
        return Math.max(0, Math.min(levelFor(existing.xp()), cap(stack)) - spent(draft));
    }

    private static int buyBit(int unlocks, int bit, boolean want, int points) {
        if (!want || (unlocks & bit) != 0 || points <= 0) {
            return unlocks;
        }
        return unlocks | bit;
    }

    private static int buySteps(int have, int want, int points) {
        return have + Math.min(points, Math.max(0, want - have));
    }

    private static ToolSettings apply(Item item, ToolSettings requested, ToolTier tier, ToolProgress progress) {
        ToolSettings result = requested.clamped(tier);
        int side = Math.min(result.areaSize(), Math.max(1, progress.area()));
        boolean digger = item instanceof AdaptivePickaxeItem || item instanceof AdaptiveShovelItem || item instanceof AdaptiveHoeItem;
        result = result.withArea(digger && result.areaMining() && progress.area() > 1, digger ? side : result.areaSize());
        if (item instanceof AdaptiveShovelItem) {
            int length = Math.min(result.configuredTrench(tier), Math.max(1, progress.trench()));
            result = result.withTrench(result.trench() && tier.canTrench() && progress.trench() > 1, length);
            result = result.withGrading(result.grading() && has(progress, GRADE), result.gradeHeight());
        }
        if (item instanceof AdaptiveAxeItem) {
            int limit = progress.fell() <= 0 ? result.fellLimit() : Math.min(result.configuredFell(tier), progress.fell());
            result = result.withFell(result.fell() && progress.fell() > 0, limit, result.leafClear() && has(progress, LEAVES), result.charcoal() && has(progress, CHARCOAL));
        }
        if (item instanceof AdaptiveHoeItem) {
            result = result.withHoe(result.till() && has(progress, TILL), result.harvest() && has(progress, HARVEST), result.replant() && has(progress, REPLANT));
        }
        if (item instanceof AdaptiveSwordItem) {
            int cleave = Math.min(result.cleaveTargets(), Math.max(1, progress.cleave()));
            int chain = Math.min(result.chainTargets(), Math.max(1, progress.chain()));
            result = result.withSword(
                    result.cleave() && progress.cleave() > 0, cleave, result.cleaveRange(),
                    result.chain() && progress.chain() > 0, chain, result.chainRange(),
                    result.momentum() && has(progress, MOMENTUM),
                    result.guardBreak() && has(progress, GUARD),
                    result.siphon() && has(progress, SIPHON),
                    result.targetPassives(), result.targetPlayers());
        }
        if (item instanceof AdaptiveArmorItem) {
            result = result.withWaterWalking(result.waterWalking() && has(progress, WATER));
            result = result.withElytra(result.elytra() && has(progress, ELYTRA));
            result = result.withFlight(result.flight() && has(progress, FLIGHT));
            result = result.withShield(result.energyShield() && has(progress, SHIELD));
        }
        return result;
    }

    private static boolean has(ToolProgress progress, int bit) {
        return (progress.unlocks() & bit) != 0;
    }

    static ToolProgress maxed(Item item, ToolTier tier) {
        int efficiency = 0;
        int fortune = 0;
        int silk = 0;
        int area = 1;
        int trench = 1;
        int fell = 0;
        int unlocks = 0;
        int cleave = 0;
        int chain = 0;
        if (!(item instanceof AdaptiveArmorItem)) {
            efficiency = 5;
            fortune = 3;
            silk = 1;
        }
        if (item instanceof AdaptivePickaxeItem || item instanceof AdaptiveShovelItem || item instanceof AdaptiveHoeItem) {
            area = Math.max(1, tier.maxAreaSide());
        }
        if (item instanceof AdaptiveShovelItem && tier.canTrench()) {
            trench = Math.max(1, tier.maxTrench());
        }
        if (item instanceof AdaptiveShovelItem && tier.canGrade()) {
            unlocks |= GRADE;
        }
        if (item instanceof AdaptiveAxeItem && tier.maxFellLogs() > 0) {
            fell = tier.maxFellLogs();
            unlocks |= LEAVES;
            if (tier.canCharcoal()) {
                unlocks |= CHARCOAL;
            }
        }
        if (item instanceof AdaptiveHoeItem) {
            unlocks |= TILL | HARVEST | REPLANT;
        }
        if (item instanceof AdaptiveSwordItem) {
            if (tier.canCleave()) {
                cleave = tier.maxCleaveTargets();
            }
            if (tier.canChain()) {
                chain = tier.maxChainTargets();
            }
            if (tier.canMomentum()) {
                unlocks |= MOMENTUM;
            }
            if (tier.canGuardBreak()) {
                unlocks |= GUARD;
            }
            if (tier.canSiphon()) {
                unlocks |= SIPHON;
            }
        }
        if (item instanceof AdaptiveArmorItem armor) {
            if (armor.canWaterWalk()) {
                unlocks |= WATER;
            }
            if (armor.canElytra()) {
                unlocks |= ELYTRA;
            }
            if (armor.canFly()) {
                unlocks |= FLIGHT;
            }
            if (armor.canShield()) {
                unlocks |= SHIELD;
            }
        }
        return new ToolProgress(0, efficiency, efficiency, fortune, fortune, silk, 0, area, trench, fell, unlocks, cleave, chain, 0, 0);
    }

    static int spent(ToolProgress progress) {
        int total = progress.efficiency() + progress.fortune() + progress.silk();
        total += areaSteps(progress.area());
        total += areaSteps(progress.trench());
        total += fellSteps(progress.fell());
        total += Integer.bitCount(progress.unlocks());
        total += progress.cleave();
        total += progress.chain();
        return total;
    }

    private static int areaSteps(int side) {
        return Math.max(0, (Math.max(1, side) - 1) / 2);
    }

    private static int sideFor(int steps) {
        return steps <= 0 ? 1 : 1 + steps * 2;
    }

    private static int fellSteps(int limit) {
        if (limit <= 0) {
            return 0;
        }
        int steps = 1;
        int cursor = 16;
        while (cursor < limit) {
            cursor *= 2;
            steps++;
        }
        return steps;
    }
}
