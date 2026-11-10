package com.tacticizm.adaptivetools;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Axe, shovel, hoe, and sword behavior. */
public final class ToolGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, AdaptiveTools.MOD_ID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FELL = function("axe_fell_limit", ToolGameTests::fellLimit);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DIG = function("shovel_area_dig", ToolGameTests::areaDig);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HOE = function("hoe_till_and_replant", ToolGameTests::tillAndReplant);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CLEAVE = function("sword_cleave_count", ToolGameTests::cleaveCount);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CHAIN = function("sword_chain_count", ToolGameTests::chainCount);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ENERGY = function("shovel_energy_stops", ToolGameTests::energyStops);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DENIED = function("shovel_denied_not_charged", ToolGameTests::deniedNotCharged);

    private static final BlockPos CENTER = new BlockPos(8, 3, 8);

    private ToolGameTests() {}

    public static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "tools"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        Identifier structure = Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "platform");
        for (var test : List.of(FELL, DIG, HOE, CLEAVE, CHAIN, ENERGY, DENIED)) {
            event.registerTest(test.getId(), new FunctionGameTestInstance(
                    ResourceKey.create(BuiltInRegistries.TEST_FUNCTION.key(), test.getId()),
                    new TestData<>(environment, structure, 100, 0, true)));
        }
    }

    public static void fellLimit(GameTestHelper helper) {
        for (int y = 3; y <= 6; y++) {
            helper.setBlock(new BlockPos(8, y, 8), Blocks.OAK_LOG);
        }
        ItemStack axe = new ItemStack(ModItems.RESONANT_AXE.get());
        axe.set(ModComponents.TOOL_SETTINGS.get(), ToolSettings.defaults(ToolTier.RESONANT_ALLOY).withFell(true, 2, false, false));
        ServerPlayer player = player(helper, axe, true);
        player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(8, 3, 8)));
        int air = 0;
        for (int y = 3; y <= 6; y++) {
            if (helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(8, y, 8))).isAir()) {
                air++;
            }
        }
        helper.assertValueEqual(air, 2, "fell limit of two logs");
        helper.succeed();
    }

    public static void areaDig(GameTestHelper helper) {
        fillDirt(helper, 7, 9);
        helper.setBlock(new BlockPos(6, 3, 8), Blocks.DIRT);
        ItemStack shovel = new ItemStack(ModItems.RESONANT_SHOVEL.get());
        shovel.set(ModComponents.TOOL_SETTINGS.get(), ToolSettings.area(true, 3));
        ServerPlayer player = player(helper, shovel, true);
        AdaptiveShovelItem.FORCED_FACE.set(net.minecraft.core.Direction.UP);
        try {
            player.gameMode.destroyBlock(helper.absolutePos(CENTER));
        } finally {
            AdaptiveShovelItem.FORCED_FACE.remove();
        }
        helper.assertValueEqual(airCount(helper, 7, 9, 3), 9, "3x3 dirt dug");
        helper.assertFalse(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(6, 3, 8))).isAir(), "outside the square");
        helper.succeed();
    }

    public static void tillAndReplant(GameTestHelper helper) {
        helper.setBlock(new BlockPos(8, 2, 8), Blocks.FARMLAND);
        helper.setBlock(CENTER, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE));
        helper.setBlock(new BlockPos(9, 2, 8), Blocks.DIRT);
        helper.setBlock(new BlockPos(9, 3, 8), Blocks.DIRT);
        ItemStack hoe = new ItemStack(ModItems.RESONANT_HOE.get());
        hoe.set(ModComponents.TOOL_SETTINGS.get(), ToolSettings.area(true, 3).withHoe(true, true, true));
        ServerPlayer player = player(helper, hoe, true);
        player.getInventory().setItem(1, new ItemStack(Items.WHEAT_SEEDS, 2));
        BlockPos absolute = helper.absolutePos(CENTER);
        hoe.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(absolute), net.minecraft.core.Direction.UP, absolute, false)));
        var wheat = helper.getLevel().getBlockState(absolute);
        helper.assertTrue(wheat.is(Blocks.WHEAT), "wheat was replanted, found " + wheat);
        helper.assertValueEqual(wheat.getValue(CropBlock.AGE), 0, "replanted wheat is young");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(9, 3, 8))).is(Blocks.FARMLAND), "neighbor dirt was tilled");
        helper.assertValueEqual(seedCount(player), 1, "one seed stayed in reserve");
        helper.succeed();
    }

    public static void cleaveCount(GameTestHelper helper) {
        ServerPlayer player = swordPlayer(helper, true, false);
        Zombie primary = zombie(helper, 8, 3, 8);
        List<Zombie> extras = List.of(zombie(helper, 8, 3, 9), zombie(helper, 7, 3, 9), zombie(helper, 9, 3, 9));
        int hit = SwordCombat.strike(player, primary, 10.0F);
        helper.assertValueEqual(hit, 3, "cleave hit three extras");
        for (Zombie zombie : extras) {
            helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "cleave damaged an extra");
        }
        helper.succeed();
    }

    public static void chainCount(GameTestHelper helper) {
        ServerPlayer player = swordPlayer(helper, false, true);
        Zombie primary = zombie(helper, 8, 3, 4);
        List<Zombie> extras = List.of(zombie(helper, 8, 3, 6), zombie(helper, 8, 3, 8), zombie(helper, 8, 3, 10), zombie(helper, 8, 3, 12));
        int hit = SwordCombat.strike(player, primary, 10.0F);
        helper.assertValueEqual(hit, 4, "chain jumped four times");
        for (Zombie zombie : extras) {
            helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "chain damaged a link");
        }
        helper.succeed();
    }

    public static void energyStops(GameTestHelper helper) {
        fillDirt(helper, 7, 9);
        ItemStack shovel = new ItemStack(ModItems.FLUX_SHOVEL.get());
        shovel.set(ModComponents.TOOL_SETTINGS.get(), ToolSettings.area(true, 3));
        shovel.set(ModComponents.ENERGY.get(), 250);
        ServerPlayer player = player(helper, shovel, true);
        AdaptiveShovelItem.FORCED_FACE.set(net.minecraft.core.Direction.UP);
        try {
            player.gameMode.destroyBlock(helper.absolutePos(CENTER));
        } finally {
            AdaptiveShovelItem.FORCED_FACE.remove();
        }
        helper.assertValueEqual(airCount(helper, 7, 9, 3), 2, "two ability actions fit in 250 FE");
        helper.assertValueEqual(ModEnergy.getStored(player.getMainHandItem()), 0, "energy was spent");
        helper.succeed();
    }

    public static void deniedNotCharged(GameTestHelper helper) {
        fillDirt(helper, 7, 9);
        BlockPos denied = helper.absolutePos(new BlockPos(7, 3, 7));
        ToolBreakEvents.TEST_DENIED_POSITIONS.add(denied);
        try {
            ItemStack shovel = new ItemStack(ModItems.FLUX_SHOVEL.get());
            shovel.set(ModComponents.TOOL_SETTINGS.get(), ToolSettings.area(true, 3));
            shovel.set(ModComponents.ENERGY.get(), 2_000);
            ServerPlayer player = player(helper, shovel, true);
            AdaptiveShovelItem.FORCED_FACE.set(net.minecraft.core.Direction.UP);
            try {
                player.gameMode.destroyBlock(helper.absolutePos(CENTER));
            } finally {
                AdaptiveShovelItem.FORCED_FACE.remove();
            }
            helper.assertFalse(helper.getLevel().getBlockState(denied).isAir(), "denied dirt remains");
            helper.assertValueEqual(airCount(helper, 7, 9, 3), 8, "the other eight blocks dug");
            helper.assertValueEqual(ModEnergy.getStored(player.getMainHandItem()), 2_000 - 8 * ActionCosts.ABILITY_FE, "denied block was free");
        } finally {
            ToolBreakEvents.TEST_DENIED_POSITIONS.remove(denied);
        }
        helper.succeed();
    }

    private static ServerPlayer swordPlayer(GameTestHelper helper, boolean cleave, boolean chain) {
        ItemStack sword = new ItemStack(ModItems.ENDER_SWORD.get());
        sword.set(ModComponents.TOOL_SETTINGS.get(), ToolSettings.defaults(ToolTier.ENDER_ALLOY)
                .withSword(cleave, 3, 3, chain, 4, 4, false, false, false, false, false));
        sword.set(ModComponents.ENERGY.get(), 100_000);
        ServerPlayer player = player(helper, sword, false);
        player.resetAttackStrengthTicker();
        return player;
    }

    private static Zombie zombie(GameTestHelper helper, int x, int y, int z) {
        Zombie zombie = new Zombie(EntityType.ZOMBIE, helper.getLevel());
        BlockPos pos = helper.absolutePos(new BlockPos(x, y, z));
        zombie.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        helper.getLevel().addFreshEntity(zombie);
        return zombie;
    }

    private static ServerPlayer player(GameTestHelper helper, ItemStack tool, boolean above) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().instabuild = false;
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        if (above) {
            player.setPos(origin.getX() + 8.5, origin.getY() + 5.0, origin.getZ() + 8.5);
            player.setXRot(90.0F);
        } else {
            player.setPos(origin.getX() + 8.5, origin.getY() + 3.0, origin.getZ() + 2.5);
            player.setYRot(0.0F);
            player.setXRot(0.0F);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return player;
    }

    private static void fillDirt(GameTestHelper helper, int min, int max) {
        for (int x = min; x <= max; x++) {
            for (int z = min; z <= max; z++) {
                helper.setBlock(new BlockPos(x, 3, z), Blocks.DIRT);
            }
        }
    }

    private static int airCount(GameTestHelper helper, int min, int max, int y) {
        int air = 0;
        for (int x = min; x <= max; x++) {
            for (int z = min; z <= max; z++) {
                if (helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(x, y, z))).isAir()) {
                    air++;
                }
            }
        }
        return air;
    }

    private static int seedCount(ServerPlayer player) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(Items.WHEAT_SEEDS)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> function(String name, Consumer<GameTestHelper> test) {
        return FUNCTIONS.register(name, () -> test);
    }

    @SuppressWarnings("unused")
    private static List<BlockPos> keep(ArrayList<BlockPos> positions) {
        return positions;
    }
}
