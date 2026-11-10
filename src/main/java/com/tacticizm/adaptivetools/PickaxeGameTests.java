package com.tacticizm.adaptivetools;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game tests for area size, energy billing, denied blocks, and settings persistence.
 * The structure {@code adaptivetools:platform} is a 16×8×16 empty volume; each test fills its own blocks.
 */
public final class PickaxeGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, AdaptiveTools.MOD_ID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SHAPE = function("area_shape", PickaxeGameTests::areaShape);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TEMPERED = function("tempered_copper_only_target", PickaxeGameTests::temperedCopperOnlyTarget);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EMBERSTEEL = function("embersteel_only_target", PickaxeGameTests::embersteelOnlyTarget);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RESONANT_3 = function("resonant_3x3_one_layer", PickaxeGameTests::resonant3x3OneLayer);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RESONANT_1 = function("resonant_smaller_size", PickaxeGameTests::resonantSmallerSize);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SIDE = function("side_face_one_layer", PickaxeGameTests::sideFaceOneLayer);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FLUX_5 = function("flux_5x5", PickaxeGameTests::flux5x5);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FLUX_3 = function("flux_selectable_3x3", PickaxeGameTests::fluxSelectable3);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ENDER_7 = function("ender_7x7", PickaxeGameTests::ender7x7);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ENDER_5 = function("ender_selectable_5x5", PickaxeGameTests::enderSelectable5);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AREA_OFF = function("area_off_mines_one", PickaxeGameTests::areaOffMinesOne);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ENERGY_BILL = function("energy_billed_per_block", PickaxeGameTests::energyBilledPerBlock);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ENERGY_STOP = function("energy_stops_when_empty", PickaxeGameTests::energyStopsWhenEmpty);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ENERGY_EMPTY = function("energy_empty_breaks_nothing", PickaxeGameTests::energyEmptyBreaksNothing);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DURABILITY_STOP = function("durability_stops_when_empty", PickaxeGameTests::durabilityStopsWhenEmpty);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DENIED = function("denied_block_not_charged", PickaxeGameTests::deniedBlockNotCharged);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SETTINGS = function("settings_persist", PickaxeGameTests::settingsPersist);

    private static final BlockPos CENTER = new BlockPos(8, 3, 8);

    private PickaxeGameTests() {}

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "default"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        Identifier structure = Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "platform");
        for (var test : List.of(SHAPE, TEMPERED, EMBERSTEEL, RESONANT_3, RESONANT_1, SIDE, FLUX_5, FLUX_3, ENDER_7, ENDER_5, AREA_OFF, ENERGY_BILL, ENERGY_STOP, ENERGY_EMPTY, DURABILITY_STOP, DENIED, SETTINGS)) {
            event.registerTest(test.getId(), testInstance(test, environment, structure));
        }
    }

    public static void areaShape(GameTestHelper helper) {
        BlockPos origin = BlockPos.ZERO;
        List<BlockPos> flat = AreaMining.positions(origin, net.minecraft.core.Direction.UP, 3);
        helper.assertValueEqual(flat.size(), 9, "3x3 count");
        helper.assertTrue(flat.getFirst().equals(origin), "target is first");
        for (BlockPos pos : flat) {
            helper.assertValueEqual(pos.getY(), 0, "horizontal layer");
        }
        List<BlockPos> side = AreaMining.positions(origin, net.minecraft.core.Direction.NORTH, 5);
        helper.assertValueEqual(side.size(), 25, "5x5 count");
        for (BlockPos pos : side) {
            helper.assertValueEqual(pos.getZ(), 0, "side face stays one layer deep");
        }
        helper.assertValueEqual(AreaMining.positions(origin, net.minecraft.core.Direction.UP, 7).size(), 49, "7x7 count");
        helper.assertValueEqual(AreaMining.positions(origin, net.minecraft.core.Direction.UP, 1).size(), 1, "1x1 count");
        helper.succeed();
    }

    public static void temperedCopperOnlyTarget(GameTestHelper helper) {
        assertOnlyTarget(helper, stack(ModItems.TEMPERED_COPPER_PICKAXE, true, 3));
    }

    public static void embersteelOnlyTarget(GameTestHelper helper) {
        assertOnlyTarget(helper, stack(ModItems.EMBERSTEEL_PICKAXE, true, 3));
    }

    public static void resonant3x3OneLayer(GameTestHelper helper) {
        fill(helper, 6, 2, 6, 10, 4, 10, Blocks.STONE);
        ServerPlayer player = breaker(helper, stack(ModItems.RESONANT_PICKAXE, true, 3), true);
        breakCenter(helper, player);
        assertSquare(helper, 8, 3, 8, 1, true);
        assertLayerIntact(helper, 2);
        assertLayerIntact(helper, 4);
        helper.succeed();
    }

    public static void resonantSmallerSize(GameTestHelper helper) {
        assertOnlyTarget(helper, stack(ModItems.RESONANT_PICKAXE, true, 1));
    }

    public static void sideFaceOneLayer(GameTestHelper helper) {
        // 3×3 centered on y=3 is y=2..4. y=5 sits outside the square and must stay.
        fill(helper, 7, 2, 7, 9, 5, 9, Blocks.STONE);
        AdaptivePickaxeItem.FORCED_FACE.set(net.minecraft.core.Direction.NORTH);
        ServerPlayer player = breaker(helper, stack(ModItems.RESONANT_PICKAXE, true, 3), false);
        try {
            breakCenter(helper, player);
        } finally {
            AdaptivePickaxeItem.FORCED_FACE.remove();
        }
        for (int x = 7; x <= 9; x++) {
            for (int y = 2; y <= 4; y++) {
                helper.assertTrue(isAir(helper, x, y, 8), "side plane " + x + "," + y);
                helper.assertFalse(isAir(helper, x, y, 7), "deeper layer should stay");
                helper.assertFalse(isAir(helper, x, y, 9), "front layer should stay");
            }
        }
        helper.assertFalse(isAir(helper, 8, 5, 8), "above the square should stay");
        helper.succeed();
    }

    public static void flux5x5(GameTestHelper helper) {
        assertSquareBreak(helper, ModItems.FLUX_PICKAXE, 5, 10_000, 5 * 5 * ActionCosts.BASE_FE);
    }

    public static void fluxSelectable3(GameTestHelper helper) {
        assertSquareBreak(helper, ModItems.FLUX_PICKAXE, 3, 10_000, 3 * 3 * ActionCosts.BASE_FE);
    }

    public static void ender7x7(GameTestHelper helper) {
        assertSquareBreak(helper, ModItems.ENDER_PICKAXE, 7, 100_000, 7 * 7 * ActionCosts.BASE_FE);
    }

    public static void enderSelectable5(GameTestHelper helper) {
        assertSquareBreak(helper, ModItems.ENDER_PICKAXE, 5, 100_000, 5 * 5 * ActionCosts.BASE_FE);
    }

    public static void areaOffMinesOne(GameTestHelper helper) {
        assertOnlyTarget(helper, energyStack(ModItems.FLUX_PICKAXE, 10_000, false, 5));
    }

    public static void energyBilledPerBlock(GameTestHelper helper) {
        int start = 10_000;
        fill(helper, 7, 3, 7, 9, 3, 9, Blocks.STONE);
        ServerPlayer player = breaker(helper, energyStack(ModItems.FLUX_PICKAXE, start, true, 3), true);
        breakCenter(helper, player);
        helper.assertValueEqual(ModEnergy.getStored(player.getMainHandItem()), start - 9 * ActionCosts.BASE_FE, "FE spent");
        assertSquare(helper, 8, 3, 8, 1, true);
        helper.succeed();
    }

    public static void energyStopsWhenEmpty(GameTestHelper helper) {
        int start = 250;
        fill(helper, 7, 3, 7, 9, 3, 9, Blocks.STONE);
        ServerPlayer player = breaker(helper, energyStack(ModItems.FLUX_PICKAXE, start, true, 3), true);
        breakCenter(helper, player);
        helper.assertTrue(isAir(helper, 8, 3, 8), "target paid");
        helper.assertTrue(isAir(helper, 7, 3, 7), "first outward block paid");
        helper.assertFalse(isAir(helper, 7, 3, 8), "unpaid block stays");
        helper.assertFalse(isAir(helper, 9, 3, 9), "later block stays");
        helper.assertValueEqual(ModEnergy.getStored(player.getMainHandItem()), start - 2 * ActionCosts.BASE_FE, "stopped with leftover FE");
        helper.succeed();
    }

    public static void energyEmptyBreaksNothing(GameTestHelper helper) {
        fill(helper, 8, 3, 8, 8, 3, 8, Blocks.STONE);
        ServerPlayer player = breaker(helper, energyStack(ModItems.FLUX_PICKAXE, 0, true, 3), true);
        breakCenter(helper, player);
        helper.assertFalse(isAir(helper, 8, 3, 8), "empty tool breaks nothing");
        helper.assertValueEqual(ModEnergy.getStored(player.getMainHandItem()), 0, "still empty");
        helper.succeed();
    }

    public static void durabilityStopsWhenEmpty(GameTestHelper helper) {
        fill(helper, 7, 3, 7, 9, 3, 9, Blocks.STONE);
        ItemStack tool = stack(ModItems.RESONANT_PICKAXE, true, 3);
        tool.setDamageValue(tool.getMaxDamage() - 2);
        ServerPlayer player = breaker(helper, tool, true);
        breakCenter(helper, player);
        helper.assertTrue(isAir(helper, 8, 3, 8), "first durability");
        helper.assertTrue(isAir(helper, 7, 3, 7), "second durability");
        helper.assertFalse(isAir(helper, 7, 3, 8), "no durability left");
        helper.succeed();
    }

    public static void deniedBlockNotCharged(GameTestHelper helper) {
        int start = 10_000;
        fill(helper, 7, 3, 7, 9, 3, 9, Blocks.STONE);
        helper.setBlock(new BlockPos(7, 3, 7), Blocks.DIAMOND_BLOCK);
        BlockPos denied = helper.absolutePos(new BlockPos(7, 3, 7));
        ToolBreakEvents.TEST_DENIED_POSITIONS.add(denied);
        try {
            ServerPlayer player = breaker(helper, energyStack(ModItems.FLUX_PICKAXE, start, true, 3), true);
            breakCenter(helper, player);
            helper.assertFalse(isAir(helper, 7, 3, 7), "denied block remains");
            helper.assertTrue(isAir(helper, 8, 3, 8), "target broke");
            helper.assertTrue(isAir(helper, 9, 3, 9), "other blocks broke");
            helper.assertValueEqual(ModEnergy.getStored(player.getMainHandItem()), start - 8 * ActionCosts.BASE_FE, "denied block was free");
        } finally {
            ToolBreakEvents.TEST_DENIED_POSITIONS.remove(denied);
        }
        helper.succeed();
    }

    public static void settingsPersist(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(ModItems.FLUX_PICKAXE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        int slot = player.getInventory().getSelectedSlot();
        ModNetworking.applySettings(player, slot, true, 9);
        ItemStack applied = player.getMainHandItem();
        ToolSettings settings = ToolSettings.of(applied, ToolTier.FLUX_ALLOY);
        helper.assertTrue(settings.areaMining(), "area mining saved");
        helper.assertValueEqual(settings.areaSize(), 5, "size clamped to the Flux cap");

        var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        Tag saved = ItemStack.CODEC.encodeStart(ops, applied).getOrThrow();
        ItemStack loaded = ItemStack.CODEC.parse(ops, saved).getOrThrow();
        ToolSettings roundTrip = ToolSettings.of(loaded, ToolTier.FLUX_ALLOY);
        helper.assertTrue(roundTrip.areaMining(), "area mining survived save");
        helper.assertValueEqual(roundTrip.areaSize(), 5, "size survived save");

        player.setShiftKeyDown(true);
        InteractionResult shifted = loaded.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(shifted.consumesAction(), "shift-use opens settings");
        player.setShiftKeyDown(false);
        InteractionResult plain = loaded.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertFalse(plain.consumesAction(), "use without shift does nothing");
        helper.succeed();
    }

    private static void assertOnlyTarget(GameTestHelper helper, ItemStack tool) {
        fill(helper, 7, 3, 7, 9, 3, 9, Blocks.STONE);
        ServerPlayer player = breaker(helper, tool, true);
        breakCenter(helper, player);
        helper.assertTrue(isAir(helper, 8, 3, 8), "target broke");
        helper.assertFalse(isAir(helper, 7, 3, 8), "neighbor stayed");
        helper.assertFalse(isAir(helper, 9, 3, 9), "corner stayed");
        helper.succeed();
    }

    private static void assertSquareBreak(GameTestHelper helper, net.neoforged.neoforge.registries.DeferredItem<AdaptivePickaxeItem> item, int side, int startEnergy, int expectedCost) {
        int radius = (side - 1) / 2;
        fill(helper, 8 - radius - 1, 2, 8 - radius - 1, 8 + radius + 1, 4, 8 + radius + 1, Blocks.STONE);
        ServerPlayer player = breaker(helper, energyStack(item, startEnergy, true, side), true);
        breakCenter(helper, player);
        assertSquare(helper, 8, 3, 8, radius, true);
        helper.assertFalse(isAir(helper, 8 - radius - 1, 3, 8), "outside the square");
        helper.assertFalse(isAir(helper, 8, 2, 8), "layer below");
        helper.assertFalse(isAir(helper, 8, 4, 8), "layer above");
        helper.assertValueEqual(ModEnergy.getStored(player.getMainHandItem()), startEnergy - expectedCost, "FE for " + side + "x" + side);
        helper.succeed();
    }

    private static void assertSquare(GameTestHelper helper, int cx, int cy, int cz, int radius, boolean broken) {
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                if (broken) {
                    helper.assertTrue(isAir(helper, x, cy, z), "broken " + x + "," + cy + "," + z);
                } else {
                    helper.assertFalse(isAir(helper, x, cy, z), "intact " + x + "," + cy + "," + z);
                }
            }
        }
    }

    private static void assertLayerIntact(GameTestHelper helper, int y) {
        for (int x = 7; x <= 9; x++) {
            for (int z = 7; z <= 9; z++) {
                helper.assertFalse(isAir(helper, x, y, z), "layer " + y + " at " + x + "," + z);
            }
        }
    }

    private static void fill(GameTestHelper helper, int x0, int y0, int z0, int x1, int y1, int z1, net.minecraft.world.level.block.Block block) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    helper.setBlock(new BlockPos(x, y, z), block);
                }
            }
        }
    }

    private static boolean isAir(GameTestHelper helper, int x, int y, int z) {
        BlockState state = helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(x, y, z)));
        return state.isAir();
    }

    private static void breakCenter(GameTestHelper helper, ServerPlayer player) {
        player.gameMode.destroyBlock(helper.absolutePos(CENTER));
    }

    private static ServerPlayer breaker(GameTestHelper helper, ItemStack tool, boolean lookDown) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().instabuild = false;
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        if (lookDown) {
            player.setPos(origin.getX() + 8.5, origin.getY() + 5.0, origin.getZ() + 8.5);
            player.setYRot(0.0F);
            player.setXRot(90.0F);
        } else {
            player.setPos(origin.getX() + 8.5, origin.getY() + 1.88, origin.getZ() + 6.2);
            player.setYRot(0.0F);
            player.setXRot(0.0F);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return player;
    }

    private static ItemStack stack(net.neoforged.neoforge.registries.DeferredItem<AdaptivePickaxeItem> item, boolean area, int size) {
        ItemStack stack = new ItemStack(item.get());
        stack.set(ModComponents.TOOL_SETTINGS.get(), new ToolSettings(area, size));
        return stack;
    }

    private static ItemStack energyStack(net.neoforged.neoforge.registries.DeferredItem<AdaptivePickaxeItem> item, int energy, boolean area, int size) {
        ItemStack stack = stack(item, area, size);
        stack.set(ModComponents.ENERGY.get(), energy);
        return stack;
    }

    private static DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> function(String name, Consumer<GameTestHelper> test) {
        return FUNCTIONS.register(name, () -> test);
    }

    private static GameTestInstance testInstance(DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> test, Holder<TestEnvironmentDefinition<?>> environment, Identifier structure) {
        ResourceKey<Consumer<GameTestHelper>> key = ResourceKey.create(BuiltInRegistries.TEST_FUNCTION.key(), test.getId());
        return new FunctionGameTestInstance(key, new TestData<>(environment, structure, 100, 0, true));
    }
}
