package com.tacticizm.adaptivetools;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Pickaxe for one {@link ToolTier}. Durability tiers lose durability. Flux and Ender spend FE
 * through the energy capability. Area breaks are billed one block at a time and stop before an
 * unpaid block is broken.
 */
public class AdaptivePickaxeItem extends Item implements TieredTool {
    private static final ThreadLocal<Boolean> BREAKING_AREA = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Direction> PENDING_FACE = new ThreadLocal<>();
    /** Tests pin the mined face so a mock player does not have to land a perfect ray. */
    static final ThreadLocal<Direction> FORCED_FACE = new ThreadLocal<>();

    private final ToolTier tier;

    public AdaptivePickaxeItem(ToolTier tier, Properties properties) {
        super(properties.stacksTo(1).pickaxe(tier.toolMaterial(), ToolTier.PICKAXE_ATTACK_DAMAGE, ToolTier.PICKAXE_ATTACK_SPEED));
        this.tier = tier;
    }

    public ToolTier tier() {
        return tier;
    }

    public static boolean isBreakingArea() {
        return BREAKING_AREA.get();
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return !tier.usesEnergy() && super.isDamageable(stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        if (!tier.usesEnergy()) {
            return super.isBarVisible(stack);
        }
        int capacity = tier.energyCapacity();
        int stored = ModEnergy.getStored(stack);
        return stored < capacity;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        if (!tier.usesEnergy()) {
            return super.getBarWidth(stack);
        }
        int capacity = tier.energyCapacity();
        if (capacity <= 0) {
            return 0;
        }
        return Math.round(13.0F * ModEnergy.getStored(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (!tier.usesEnergy()) {
            return super.getBarColor(stack);
        }
        return tier == ToolTier.ENDER_ALLOY ? 0xFFB060FF : 0xFF4FD0FF;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);
        if (!canAfford(stack, null)) {
            return 0.0F;
        }
        return speed;
    }

    /**
     * Shift-right-click opens Tool Settings only for a miss (air or a block past reach). A block
     * inside reach arrives through {@link #useOn} and is left alone.
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            com.tacticizm.adaptivetools.client.ToolSettingsScreen.open(player, hand);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (level.isClientSide() || !(miner instanceof Player player)) {
            return true;
        }
        pay(stack, player);
        // Survival reaches this after the break is accepted and before the targeted block is removed.
        // Its cost is paid above, then the rest of the square. Creative never calls mineBlock
        // (instabuild skips drops) and starts the square from the break event instead.
        if (!BREAKING_AREA.get() && player instanceof ServerPlayer serverPlayer && !player.getAbilities().instabuild) {
            breakArea(serverPlayer, pos);
        }
        return true;
    }

    /** Creative (and any other instabuild break) starts the square here, because mineBlock will not run. */
    public void breakAreaIfCreative(ServerPlayer player, BlockPos origin) {
        if (player.getAbilities().instabuild) {
            breakArea(player, origin);
        }
    }

    public boolean canAfford(ItemStack stack, Player player) {
        if (player != null && player.getAbilities().instabuild) {
            return true;
        }
        if (tier.usesEnergy()) {
            return ModEnergy.getStored(stack) >= ActionCosts.BASE_FE;
        }
        int maxDamage = stack.getMaxDamage();
        return maxDamage <= 0 || stack.getDamageValue() < maxDamage;
    }

    public void pay(ItemStack stack, Player player) {
        if (player != null && player.getAbilities().instabuild) {
            return;
        }
        if (tier.usesEnergy()) {
            if (player != null && ModEnergy.shouldSkipCost(stack, player)) {
                return;
            }
            ModEnergy.extract(stack, ActionCosts.BASE_FE);
            return;
        }
        if (player != null) {
            stack.hurtAndBreak(ActionCosts.BASE_DURABILITY, player, EquipmentSlot.MAINHAND);
        }
    }

    public static void captureFace(Player player, BlockPos pos) {
        Direction forced = FORCED_FACE.get();
        if (forced != null) {
            PENDING_FACE.set(forced);
            return;
        }
        Direction face = Direction.UP;
        HitResult hit = player.pick(player.blockInteractionRange(), 0.0F, false);
        if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK && blockHit.getBlockPos().equals(pos)) {
            face = blockHit.getDirection();
        }
        PENDING_FACE.set(face);
    }

    private void breakArea(ServerPlayer player, BlockPos origin) {
        Direction face = PENDING_FACE.get();
        PENDING_FACE.remove();
        if (face == null) {
            face = Direction.UP;
        }
        ItemStack stack = player.getMainHandItem();
        ToolSettings settings = ToolSettings.of(stack, tier);
        int side = settings.effectiveSide(tier);
        if (side <= 1) {
            return;
        }
        List<BlockPos> positions = AreaMining.positions(origin, face, side);
        Level level = player.level();
        BREAKING_AREA.set(true);
        try {
            for (BlockPos pos : positions) {
                if (pos.equals(origin)) {
                    continue;
                }
                ItemStack current = player.getMainHandItem();
                if (!(current.getItem() instanceof AdaptivePickaxeItem tool) || !tool.canAfford(current, player)) {
                    break;
                }
                BlockState state = level.getBlockState(pos);
                if (!AreaMining.canMine(level, pos, state)) {
                    continue;
                }
                // destroyBlock fires BreakBlockEvent. A cancelled event does not break and does not reach mineBlock, so it is not charged.
                player.gameMode.destroyBlock(pos);
            }
        } finally {
            BREAKING_AREA.set(false);
        }
    }
}
