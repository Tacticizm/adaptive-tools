package com.tacticizm.adaptivetools;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Armor abilities. The shield runs after vanilla armor, on {@link LivingDamageEvent.Pre}.
 * Elytra and flight bill only while that movement is happening. Water walking is free.
 */
public final class ArmorAbilities {
    /** Provisional. One damage point costs 100 FE, and only whole points are absorbed. */
    public static final int FE_PER_DAMAGE = 100;
    public static final int ELYTRA_FE_PER_TICK = 12;
    public static final int FLIGHT_FE_PER_TICK = 100;
    private static final int LOW_TICKS = 40;
    private static final int SLOW_FALL_TICKS = 40;
    private static final List<EquipmentSlot> ARMOR = List.of(EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private static final Set<UUID> FLIGHT_GRANTED = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> WAS_FLYING = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> ELYTRA_WARNED = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> FLIGHT_WARNED = ConcurrentHashMap.newKeySet();

    private ArmorAbilities() {}

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            water(player);
            return;
        }
        water(player);
        elytra(player);
        flight(player);
    }

    public static void onDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof Player player) {
            event.setNewDamage(absorb(player, event.getNewDamage()));
        }
    }

    /**
     * Absorbs whole damage points from the single shield piece with the most charge.
     * A tie prefers chest, then helmet, leggings, boots. Leftover damage is returned
     * and no second piece is billed.
     */
    public static float absorb(Player player, float damage) {
        if (damage <= 0.0F || player.getAbilities().instabuild) {
            return damage;
        }
        ItemStack piece = shieldPiece(player);
        if (piece.isEmpty()) {
            return damage;
        }
        int stored = ModEnergy.getStored(piece);
        int cover = Math.min((int) Math.floor(damage), stored / FE_PER_DAMAGE);
        if (cover <= 0) {
            return damage;
        }
        ModEnergy.extract(piece, cover * FE_PER_DAMAGE);
        return damage - cover;
    }

    public static void water(Player player) {
        if (player.isShiftKeyDown() || player.isSpectator() || player.isFallFlying() || player.getAbilities().flying || player.isPassenger()) {
            return;
        }
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!(boots.getItem() instanceof AdaptiveArmorItem armor) || !armor.canWaterWalk()) {
            return;
        }
        if (!ToolSettings.of(boots, armor.tier()).waterWalking()) {
            return;
        }
        BlockPos feet = BlockPos.containing(player.getX(), player.getY() - 0.05, player.getZ());
        for (BlockPos pos : List.of(feet, feet.below())) {
            FluidState fluid = player.level().getFluidState(pos);
            if (!fluid.is(FluidTags.WATER) || fluid.getHeight(player.level(), pos) <= 0.0F) {
                continue;
            }
            // A source block's walkable top is the top of the block. getHeight is only the visual fill.
            double surface = fluid.isSource() ? pos.getY() + 1.0 : pos.getY() + fluid.getHeight(player.level(), pos);
            double y = player.getY();
            if (y <= surface && y > surface - 1.05 && player.getDeltaMovement().y <= 0.1) {
                player.setPos(player.getX(), surface, player.getZ());
                player.setDeltaMovement(player.getDeltaMovement().x, 0.0, player.getDeltaMovement().z);
                player.setOnGround(true);
                player.resetFallDistance();
                return;
            }
        }
    }

    public static void elytra(Player player) {
        tickElytra(player, player.isFallFlying());
    }

    static void tickElytra(Player player, boolean gliding) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof AdaptiveArmorItem armor) || !armor.canElytra()) {
            return;
        }
        ToolSettings settings = ToolSettings.of(chest, armor.tier());
        int stored = ModEnergy.getStored(chest);
        boolean powered = settings.elytra() && (player.getAbilities().instabuild || stored >= ELYTRA_FE_PER_TICK);
        if (gliding && powered && !player.getAbilities().instabuild) {
            ModEnergy.extract(chest, ELYTRA_FE_PER_TICK);
            stored = ModEnergy.getStored(chest);
            powered = stored >= ELYTRA_FE_PER_TICK;
            if (stored > 0 && stored < ELYTRA_FE_PER_TICK * LOW_TICKS && ELYTRA_WARNED.add(player.getUUID())) {
                tell(player, "adaptivetools.status.low_energy");
            }
        }
        if (powered) {
            chest.set(DataComponents.GLIDER, Unit.INSTANCE);
        } else {
            chest.remove(DataComponents.GLIDER);
        }
        if (gliding && !powered) {
            if (!player.getAbilities().instabuild && stored > 0 && stored < ELYTRA_FE_PER_TICK) {
                ModEnergy.extract(chest, stored);
            }
            player.stopFallFlying();
            slowFall(player);
            ELYTRA_WARNED.remove(player.getUUID());
        }
        if (!gliding) {
            ELYTRA_WARNED.remove(player.getUUID());
        }
    }

    public static void flight(Player player) {
        if (player.isSpectator() || player.getAbilities().instabuild) {
            return;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean piece = chest.getItem() instanceof AdaptiveArmorItem armor && armor.canFly();
        ToolSettings settings = piece ? ToolSettings.of(chest, ((AdaptiveArmorItem) chest.getItem()).tier()) : null;
        int stored = piece ? ModEnergy.getStored(chest) : 0;
        boolean enabled = settings != null && settings.flight();
        boolean powered = enabled && (player.getAbilities().instabuild || stored >= FLIGHT_FE_PER_TICK);
        UUID id = player.getUUID();

        if (!powered) {
            boolean wasFlying = player.getAbilities().flying || WAS_FLYING.remove(id);
            if (FLIGHT_GRANTED.remove(id) || wasFlying) {
                player.getAbilities().flying = false;
                player.getAbilities().mayfly = false;
                syncAbilities(player);
                if (wasFlying) {
                    if (piece && !player.getAbilities().instabuild && stored > 0 && stored < FLIGHT_FE_PER_TICK) {
                        ModEnergy.extract(chest, stored);
                    }
                    slowFall(player);
                }
            }
            FLIGHT_WARNED.remove(id);
            return;
        }

        if (FLIGHT_GRANTED.add(id)) {
            player.getAbilities().mayfly = true;
            syncAbilities(player);
        }
        if (!player.getAbilities().flying) {
            if (WAS_FLYING.remove(id) && player.onGround()) {
                chest.set(ModComponents.TOOL_SETTINGS.get(), settings.withFlight(false));
                player.setItemSlot(EquipmentSlot.CHEST, chest);
                FLIGHT_GRANTED.remove(id);
                player.getAbilities().mayfly = false;
                syncAbilities(player);
            }
            FLIGHT_WARNED.remove(id);
            return;
        }

        WAS_FLYING.add(id);
        if (!player.getAbilities().instabuild) {
            ModEnergy.extract(chest, FLIGHT_FE_PER_TICK);
            stored = ModEnergy.getStored(chest);
            if (stored > 0 && stored < FLIGHT_FE_PER_TICK * LOW_TICKS && FLIGHT_WARNED.add(id)) {
                tell(player, "adaptivetools.status.low_energy");
            }
            if (stored < FLIGHT_FE_PER_TICK) {
                player.getAbilities().flying = false;
                player.getAbilities().mayfly = false;
                FLIGHT_GRANTED.remove(id);
                WAS_FLYING.remove(id);
                syncAbilities(player);
                slowFall(player);
            }
        }
    }

    public static void toggleWorn(Player player, int bit) {
        if (bit != ToolSettings.WATER_WALKING && bit != ToolSettings.ELYTRA_GLIDE && bit != ToolSettings.CREATIVE_FLIGHT && bit != ToolSettings.ENERGY_SHIELD) {
            return;
        }
        boolean anyOn = false;
        boolean changed = false;
        List<EquipmentSlot> slots = bit == ToolSettings.ENERGY_SHIELD ? ARMOR : List.of(bit == ToolSettings.WATER_WALKING ? EquipmentSlot.FEET : EquipmentSlot.CHEST);
        for (EquipmentSlot slot : slots) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof AdaptiveArmorItem armor) || !supports(armor, bit)) {
                continue;
            }
            ToolSettings next = ToolSettings.of(stack, armor.tier()).toggle(bit);
            stack.set(ModComponents.TOOL_SETTINGS.get(), next);
            changed = true;
            anyOn |= next.enabled(bit);
        }
        if (!changed) {
            return;
        }
        String key = switch (bit) {
            case ToolSettings.WATER_WALKING -> anyOn ? "adaptivetools.status.water_on" : "adaptivetools.status.water_off";
            case ToolSettings.ELYTRA_GLIDE -> anyOn ? "adaptivetools.status.elytra_on" : "adaptivetools.status.elytra_off";
            case ToolSettings.CREATIVE_FLIGHT -> anyOn ? "adaptivetools.status.flight_on" : "adaptivetools.status.flight_off";
            default -> anyOn ? "adaptivetools.status.shield_on" : "adaptivetools.status.shield_off";
        };
        tell(player, key);
    }

    private static boolean supports(AdaptiveArmorItem armor, int bit) {
        if (bit == ToolSettings.WATER_WALKING) {
            return armor.canWaterWalk();
        }
        if (bit == ToolSettings.ELYTRA_GLIDE) {
            return armor.canElytra();
        }
        if (bit == ToolSettings.CREATIVE_FLIGHT) {
            return armor.canFly();
        }
        return armor.canShield();
    }

    private static ItemStack shieldPiece(Player player) {
        ItemStack best = ItemStack.EMPTY;
        int bestStored = 0;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof AdaptiveArmorItem armor) || !armor.canShield()) {
                continue;
            }
            if (!ToolSettings.of(stack, armor.tier()).energyShield()) {
                continue;
            }
            int stored = ModEnergy.getStored(stack);
            if (stored > bestStored) {
                best = stack;
                bestStored = stored;
            }
        }
        return best;
    }

    private static void slowFall(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALL_TICKS));
    }

    private static void syncAbilities(Player player) {
        if (player instanceof ServerPlayer server && server.connection != null) {
            server.onUpdateAbilities();
        }
    }

    private static void tell(Player player, String key) {
        if (player instanceof ServerPlayer server && server.connection != null) {
            server.sendOverlayMessage(Component.translatable(key));
        }
    }
}
