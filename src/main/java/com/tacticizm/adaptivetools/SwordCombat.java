package com.tacticizm.adaptivetools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.SweepAttackEvent;

/**
 * Sword abilities. Extra hits replace vanilla sweeping. They only start from a landed hit, never
 * call themselves, and share one "already hit" set. Passive mobs and players are off unless the
 * sword's settings say otherwise. Tamed pets are never targets.
 */
public final class SwordCombat {
    private static final ThreadLocal<Boolean> STRIKING = ThreadLocal.withInitial(() -> false);
    private static final java.util.Map<java.util.UUID, Long> GUARD_BROKEN_UNTIL = new java.util.concurrent.ConcurrentHashMap<>();

    private SwordCombat() {}

    public static void onSweep(SweepAttackEvent event) {
        if (AdaptiveSwordItem.isOurSword(event.getEntity().getMainHandItem())) {
            event.setSweeping(false);
        }
    }

    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof AdaptiveSwordItem sword)) {
            return;
        }
        ToolSettings settings = ToolSettings.of(stack, sword.tier());
        if (!settings.momentum()) {
            return;
        }
        long now = player.level().getGameTime();
        long since = now - stack.getOrDefault(ModComponents.MOMENTUM_TIME.get(), 0L);
        int stacks = since > 60 ? 0 : stack.getOrDefault(ModComponents.MOMENTUM_STACKS.get(), 0);
        if (stacks > 0) {
            event.setAmount(event.getAmount() * (1.0F + 0.05F * stacks));
        }
    }

    public static void onDamaged(LivingDamageEvent.Post event) {
        if (STRIKING.get()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof AdaptiveSwordItem sword)) {
            return;
        }
        if (sword.tier().usesEnergy()) {
            ToolBilling.pay(stack, player, sword.tier(), false);
        }
        rememberMomentum(player, stack);
        guardBreak(player, stack, target, sword.tier());
        strike(player, target, event.getOriginalDamage());
    }

    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof AdaptiveSwordItem sword)) {
            return;
        }
        ToolSettings settings = ToolSettings.of(stack, sword.tier());
        if (!settings.siphon()) {
            return;
        }
        long now = player.level().getGameTime();
        long ready = stack.getOrDefault(ModComponents.SIPHON_READY.get(), 0L);
        if (now < ready) {
            return;
        }
        player.heal(1.0F);
        stack.set(ModComponents.SIPHON_READY.get(), now + 40);
    }

    public static void onShield(LivingShieldBlockEvent event) {
        Long until = GUARD_BROKEN_UNTIL.get(event.getEntity().getUUID());
        if (until != null && event.getEntity().level().getGameTime() < until) {
            event.setBlocked(false);
        }
    }

    /** Extra targets actually hit. Tests call this directly. */
    public static int strike(ServerPlayer player, LivingEntity primary, float originalDamage) {
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof AdaptiveSwordItem sword)) {
            return 0;
        }
        ToolSettings settings = ToolSettings.of(stack, sword.tier());
        Set<LivingEntity> hit = new HashSet<>();
        hit.add(primary);
        int count = 0;
        if (settings.cleave()) {
            count += cleave(player, primary, originalDamage, settings, hit);
        }
        if (settings.chain()) {
            count += chain(player, primary, originalDamage, settings, hit);
        }
        return count;
    }

    private static int cleave(ServerPlayer player, LivingEntity primary, float originalDamage, ToolSettings settings, Set<LivingEntity> hit) {
        Vec3 look = player.getLookAngle();
        List<LivingEntity> candidates = nearby(player, primary.position(), settings.cleaveRange(), settings, hit);
        candidates.removeIf(entity -> {
            Vec3 toward = entity.position().subtract(player.position());
            return toward.lengthSqr() < 1.0E-4 || toward.normalize().dot(look) <= 0.0;
        });
        candidates.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)));
        return hurtSome(player, originalDamage, settings.cleaveTargets(), candidates);
    }

    private static int chain(ServerPlayer player, LivingEntity primary, float originalDamage, ToolSettings settings, Set<LivingEntity> hit) {
        int count = 0;
        LivingEntity last = primary;
        for (int jump = 0; jump < settings.chainTargets(); jump++) {
            List<LivingEntity> candidates = nearby(player, last.position(), settings.chainRange(), settings, hit);
            if (candidates.isEmpty()) {
                break;
            }
            LivingEntity cursor = last;
            LivingEntity next = candidates.stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(cursor))).orElse(null);
            if (next == null || !hurtOne(player, next, originalDamage)) {
                break;
            }
            hit.add(next);
            last = next;
            count++;
        }
        return count;
    }

    private static int hurtSome(ServerPlayer player, float originalDamage, int limit, List<LivingEntity> candidates) {
        int count = 0;
        for (LivingEntity entity : candidates) {
            if (count >= limit) {
                break;
            }
            if (hurtOne(player, entity, originalDamage)) {
                count++;
            }
        }
        return count;
    }

    private static boolean hurtOne(ServerPlayer player, LivingEntity entity, float originalDamage) {
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof AdaptiveSwordItem sword)) {
            return false;
        }
        if (!ToolBilling.canAfford(stack, player, sword.tier(), true)) {
            return false;
        }
        if (!player.hasLineOfSight(entity)) {
            return false;
        }
        float charge = player.getAttackStrengthScale(0.5F);
        float amount = originalDamage * 0.5F * Math.max(0.2F, charge);
        ToolBilling.pay(stack, player, sword.tier(), true);
        STRIKING.set(true);
        try {
            entity.hurt(player.damageSources().playerAttack(player), amount);
        } finally {
            STRIKING.set(false);
        }
        return true;
    }

    private static List<LivingEntity> nearby(ServerPlayer player, Vec3 center, int range, ToolSettings settings, Set<LivingEntity> hit) {
        AABB box = new AABB(center, center).inflate(range);
        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, box, entity -> entity.isAlive() && !hit.contains(entity))) {
            if (entity.distanceToSqr(center) > (double) range * range) {
                continue;
            }
            if (!allowed(player, entity, settings)) {
                continue;
            }
            found.add(entity);
        }
        return found;
    }

    public static boolean allowed(Player player, LivingEntity entity, ToolSettings settings) {
        if (entity == player || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof TamableAnimal pet && pet.isTame()) {
            return false;
        }
        if (entity instanceof Player) {
            return settings.targetPlayers();
        }
        if (!(entity instanceof Enemy) && !settings.targetPassives()) {
            return false;
        }
        return true;
    }

    private static void rememberMomentum(ServerPlayer player, net.minecraft.world.item.ItemStack stack) {
        if (!(stack.getItem() instanceof AdaptiveSwordItem sword)) {
            return;
        }
        ToolSettings settings = ToolSettings.of(stack, sword.tier());
        if (!settings.momentum()) {
            return;
        }
        long now = player.level().getGameTime();
        long since = now - stack.getOrDefault(ModComponents.MOMENTUM_TIME.get(), 0L);
        int stacks = since > 60 ? 0 : stack.getOrDefault(ModComponents.MOMENTUM_STACKS.get(), 0);
        stack.set(ModComponents.MOMENTUM_STACKS.get(), Math.min(4, stacks + 1));
        stack.set(ModComponents.MOMENTUM_TIME.get(), now);
    }

    private static void guardBreak(ServerPlayer player, net.minecraft.world.item.ItemStack stack, LivingEntity target, ToolTier tier) {
        ToolSettings settings = ToolSettings.of(stack, tier);
        if (!settings.guardBreak()) {
            return;
        }
        long now = player.level().getGameTime();
        long ready = stack.getOrDefault(ModComponents.GUARD_READY.get(), 0L);
        if (now < ready) {
            return;
        }
        GUARD_BROKEN_UNTIL.put(target.getUUID(), now + 40);
        stack.set(ModComponents.GUARD_READY.get(), now + 100);
        target.stopUsingItem();
    }
}
