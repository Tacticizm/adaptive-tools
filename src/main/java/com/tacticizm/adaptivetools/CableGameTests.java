package com.tacticizm.adaptivetools;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import com.tacticizm.adaptivetools.cable.CableBlockEntity;
import com.tacticizm.adaptivetools.cable.CableNetworks;
import com.tacticizm.adaptivetools.cable.CableTier;
import com.tacticizm.adaptivetools.cable.TransferMode;
import com.tacticizm.adaptivetools.testing.TestBufferBlockEntity;

/** Rates, limits, filters, modules, and the wrench. */
public final class CableGameTests {
    public static final net.neoforged.neoforge.registries.DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            net.neoforged.neoforge.registries.DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.TEST_FUNCTION, AdaptiveTools.MOD_ID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LIGHT = function("cable_light_rates", CableGameTests::lightRates);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MEDIUM = function("cable_medium_rates", CableGameTests::mediumRates);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HEAVY = function("cable_heavy_rates", CableGameTests::heavyRates);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SLOWEST = function("cable_slowest_limits_route", CableGameTests::slowestLimitsRoute);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROUND = function("cable_round_robin", CableGameTests::roundRobin);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PRIORITY = function("cable_priority", CableGameTests::priority);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FILTER = function("cable_filter", CableGameTests::filter);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SEND = function("cable_send_receive", CableGameTests::sendReceive);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RATE = function("cable_rate_module_doubles_items_not_fe", CableGameTests::rateModule);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TWO_RATE = function("cable_two_rate_modules", CableGameTests::twoRateModules);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WRENCH = function("cable_wrench_disconnect", CableGameTests::wrenchDisconnect);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DROPS = function("cable_break_returns_modules", CableGameTests::breakReturnsModules);

    private static final BlockPos SOURCE = new BlockPos(2, 2, 2);
    private static final BlockPos CABLE = new BlockPos(3, 2, 2);
    private static final BlockPos SINK = new BlockPos(4, 2, 2);

    private CableGameTests() {}

    public static void registerTests(net.neoforged.neoforge.event.RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "cables"),
                new net.minecraft.gametest.framework.TestEnvironmentDefinition.AllOf(java.util.List.of()));
        var structure = net.minecraft.resources.Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "platform");
        for (var test : java.util.List.of(LIGHT, MEDIUM, HEAVY, SLOWEST, ROUND, PRIORITY, FILTER, SEND, RATE, TWO_RATE, WRENCH, DROPS)) {
            event.registerTest(test.getId(), new net.minecraft.gametest.framework.FunctionGameTestInstance(
                    net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.BuiltInRegistries.TEST_FUNCTION.key(), test.getId()),
                    new net.minecraft.gametest.framework.TestData<>(environment, structure, 100, 0, true)));
        }
    }

    private static DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> function(String name, Consumer<GameTestHelper> test) {
        return FUNCTIONS.register(name, () -> test);
    }

    public static void lightRates(GameTestHelper helper) {
        assertTierRates(helper, ModBlocks.LIGHT_CABLE.get());
        helper.succeed();
    }

    public static void mediumRates(GameTestHelper helper) {
        assertTierRates(helper, ModBlocks.MEDIUM_CABLE.get());
        helper.succeed();
    }

    public static void heavyRates(GameTestHelper helper) {
        assertTierRates(helper, ModBlocks.HEAVY_CABLE.get());
        helper.succeed();
    }

    public static void slowestLimitsRoute(GameTestHelper helper) {
        helper.setBlock(SOURCE, ModBlocks.TEST_BUFFER.get());
        helper.setBlock(new BlockPos(3, 2, 2), ModBlocks.HEAVY_CABLE.get());
        helper.setBlock(new BlockPos(4, 2, 2), ModBlocks.LIGHT_CABLE.get());
        helper.setBlock(new BlockPos(5, 2, 2), ModBlocks.HEAVY_CABLE.get());
        helper.setBlock(new BlockPos(6, 2, 2), ModBlocks.TEST_BUFFER.get());
        wire(helper, new BlockPos(3, 2, 2), Direction.WEST, Direction.EAST, true, true, true);
        wire(helper, new BlockPos(5, 2, 2), Direction.WEST, Direction.EAST, true, true, true);
        fillSource(helper, SOURCE);
        tick(helper, new BlockPos(4, 2, 2), 20);
        TestBufferBlockEntity sink = buffer(helper, new BlockPos(6, 2, 2));
        helper.assertValueEqual(sink.energy.getAmountAsInt(), CableTier.LIGHT.fePerTick() * 20, "FE follows the light cable");
        helper.assertValueEqual(sink.items.getAmountAsInt(0), CableTier.LIGHT.itemsPerSecond(), "items follow the light cable");
        helper.assertValueEqual(sink.fluids.getAmountAsInt(0), CableTier.LIGHT.milliBucketsPerSecond(), "fluid follows the light cable");
        helper.succeed();
    }

    public static void roundRobin(GameTestHelper helper) {
        helper.setBlock(SOURCE, ModBlocks.TEST_BUFFER.get());
        helper.setBlock(CABLE, ModBlocks.LIGHT_CABLE.get());
        helper.setBlock(SINK, ModBlocks.TEST_BUFFER.get());
        helper.setBlock(new BlockPos(3, 3, 2), ModBlocks.TEST_BUFFER.get());
        CableBlockEntity cable = cable(helper, CABLE);
        cable.installItem(Direction.WEST);
        cable.setItemMode(Direction.WEST, TransferMode.SEND);
        cable.installItem(Direction.EAST);
        cable.setItemMode(Direction.EAST, TransferMode.RECEIVE);
        cable.installItem(Direction.UP);
        cable.setItemMode(Direction.UP, TransferMode.RECEIVE);
        buffer(helper, SOURCE).items.set(0, ItemResource.of(Items.COBBLESTONE), 8);
        tick(helper, CABLE, 20);
        int east = buffer(helper, SINK).items.getAmountAsInt(0);
        int up = buffer(helper, new BlockPos(3, 3, 2)).items.getAmountAsInt(0);
        helper.assertValueEqual(east + up, 8, "all eight items moved");
        helper.assertValueEqual(Math.abs(east - up), 0, "round-robin split the items evenly");
        helper.succeed();
    }

    public static void priority(GameTestHelper helper) {
        helper.setBlock(SOURCE, ModBlocks.TEST_BUFFER.get());
        helper.setBlock(CABLE, ModBlocks.LIGHT_CABLE.get());
        helper.setBlock(SINK, ModBlocks.TEST_BUFFER.get());
        helper.setBlock(new BlockPos(3, 3, 2), ModBlocks.TEST_BUFFER.get());
        CableBlockEntity cable = cable(helper, CABLE);
        cable.installItem(Direction.WEST);
        cable.setItemMode(Direction.WEST, TransferMode.SEND);
        cable.installItem(Direction.EAST);
        cable.setItemMode(Direction.EAST, TransferMode.RECEIVE);
        cable.setItemPriority(Direction.EAST, 0);
        cable.installItem(Direction.UP);
        cable.setItemMode(Direction.UP, TransferMode.RECEIVE);
        cable.setItemPriority(Direction.UP, 5);
        buffer(helper, SOURCE).items.set(0, ItemResource.of(Items.COBBLESTONE), 8);
        tick(helper, CABLE, 20);
        helper.assertValueEqual(buffer(helper, new BlockPos(3, 3, 2)).items.getAmountAsInt(0), 8, "higher priority sink takes the items");
        helper.assertValueEqual(buffer(helper, SINK).items.getAmountAsInt(0), 0, "lower priority sink waits");
        helper.succeed();
    }

    public static void filter(GameTestHelper helper) {
        link(helper, ModBlocks.LIGHT_CABLE.get());
        CableBlockEntity cable = cable(helper, CABLE);
        cable.setItemFilter(Direction.WEST, 0, new ItemStack(Items.COBBLESTONE));
        TestBufferBlockEntity source = buffer(helper, SOURCE);
        source.items.set(0, ItemResource.of(Items.COBBLESTONE), 64);
        source.items.set(1, ItemResource.of(Items.DIRT), 64);
        tick(helper, CABLE, 20);
        TestBufferBlockEntity sink = buffer(helper, SINK);
        helper.assertValueEqual(sink.items.getAmountAsInt(0), 8, "filtered cobble moved");
        helper.assertTrue(sink.items.getResource(0).isEmpty() || sink.items.getResource(0).is(Items.COBBLESTONE), "sink did not receive dirt");
        helper.assertValueEqual(source.items.getAmountAsInt(1), 64, "dirt stayed in the source");
        helper.succeed();
    }

    public static void sendReceive(GameTestHelper helper) {
        helper.setBlock(SOURCE, ModBlocks.TEST_BUFFER.get());
        helper.setBlock(CABLE, ModBlocks.LIGHT_CABLE.get());
        helper.setBlock(SINK, ModBlocks.TEST_BUFFER.get());
        CableBlockEntity cable = cable(helper, CABLE);
        cable.installItem(Direction.WEST);
        cable.setItemMode(Direction.WEST, TransferMode.SEND);
        cable.installItem(Direction.EAST);
        cable.setItemMode(Direction.EAST, TransferMode.SEND);
        buffer(helper, SOURCE).items.set(0, ItemResource.of(Items.COBBLESTONE), 16);
        tick(helper, CABLE, 20);
        helper.assertValueEqual(buffer(helper, SINK).items.getAmountAsInt(0), 0, "two Send faces do not move items");
        cable.setItemMode(Direction.EAST, TransferMode.RECEIVE);
        tick(helper, CABLE, 20);
        helper.assertValueEqual(buffer(helper, SINK).items.getAmountAsInt(0), 8, "Send into Receive moves items");
        helper.succeed();
    }

    public static void rateModule(GameTestHelper helper) {
        link(helper, ModBlocks.LIGHT_CABLE.get());
        cable(helper, CABLE).addRate(Direction.WEST);
        fillSource(helper, SOURCE);
        tick(helper, CABLE, 20);
        TestBufferBlockEntity sink = buffer(helper, SINK);
        helper.assertValueEqual(sink.items.getAmountAsInt(0), CableTier.LIGHT.itemsPerSecond() * 2, "one rate module doubles items");
        helper.assertValueEqual(sink.fluids.getAmountAsInt(0), CableTier.LIGHT.milliBucketsPerSecond() * 2, "one rate module doubles fluid");
        helper.assertValueEqual(sink.energy.getAmountAsInt(), CableTier.LIGHT.fePerTick() * 20, "rate module does not change FE");
        helper.succeed();
    }

    public static void twoRateModules(GameTestHelper helper) {
        link(helper, ModBlocks.LIGHT_CABLE.get());
        CableBlockEntity cable = cable(helper, CABLE);
        helper.assertTrue(cable.addRate(Direction.EAST), "first rate module");
        helper.assertTrue(cable.addRate(Direction.EAST), "second rate module");
        helper.assertFalse(cable.addRate(Direction.EAST), "third rate module is refused");
        buffer(helper, SOURCE).items.set(0, ItemResource.of(Items.COBBLESTONE), 64);
        tick(helper, CABLE, 20);
        helper.assertValueEqual(buffer(helper, SINK).items.getAmountAsInt(0), CableTier.LIGHT.itemsPerSecond() * 4, "two rate modules quadruple items");
        helper.succeed();
    }

    public static void wrenchDisconnect(GameTestHelper helper) {
        link(helper, ModBlocks.LIGHT_CABLE.get());
        buffer(helper, SOURCE).items.set(0, ItemResource.of(Items.COBBLESTONE), 64);
        tick(helper, CABLE, 5);
        int moved = buffer(helper, SINK).items.getAmountAsInt(0);
        helper.assertTrue(moved > 0, "items moved before the wrench");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().mayBuild = true;
        player.setShiftKeyDown(true);
        BlockPos absolute = helper.absolutePos(CABLE);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.WEST, absolute, false);
        ItemStack wrench = new ItemStack(ModItems.WRENCH.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        helper.assertTrue(player.isShiftKeyDown(), "test player is sneaking");
        wrench.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(cable(helper, CABLE).face(Direction.WEST).disconnected, "wrench disconnected the west face");
        tick(helper, CABLE, 20);
        helper.assertValueEqual(buffer(helper, SINK).items.getAmountAsInt(0), moved, "disconnected face stopped the route");
        player.setShiftKeyDown(true);
        wrench.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertFalse(cable(helper, CABLE).face(Direction.WEST).disconnected, "wrench reconnected the face");
        helper.succeed();
    }

    public static void breakReturnsModules(GameTestHelper helper) {
        helper.setBlock(CABLE, ModBlocks.LIGHT_CABLE.get());
        CableBlockEntity cable = cable(helper, CABLE);
        cable.installPower(Direction.NORTH);
        cable.installItem(Direction.NORTH);
        cable.installFluid(Direction.NORTH);
        cable.addRate(Direction.NORTH);
        cable.addRate(Direction.NORTH);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().instabuild = false;
        player.gameMode.destroyBlock(helper.absolutePos(CABLE));
        helper.assertValueEqual(count(helper, CABLE, ModBlocks.LIGHT_CABLE_ITEM.get()), 1, "cable item");
        helper.assertValueEqual(count(helper, CABLE, ModItems.POWER_MODULE.get()), 1, "power module");
        helper.assertValueEqual(count(helper, CABLE, ModItems.ITEM_MODULE.get()), 1, "item module");
        helper.assertValueEqual(count(helper, CABLE, ModItems.FLUID_MODULE.get()), 1, "fluid module");
        helper.assertValueEqual(count(helper, CABLE, ModItems.RATE_MODULE.get()), 2, "both rate modules");
        helper.succeed();
    }

    private static void assertTierRates(GameTestHelper helper, net.minecraft.world.level.block.Block block) {
        com.tacticizm.adaptivetools.cable.CableBlock cable = (com.tacticizm.adaptivetools.cable.CableBlock) block;
        link(helper, block);
        fillSource(helper, SOURCE);
        tick(helper, CABLE, 20);
        CableTier tier = cable.tier();
        TestBufferBlockEntity sink = buffer(helper, SINK);
        helper.assertValueEqual(sink.energy.getAmountAsInt(), tier.fePerTick() * 20, tier.id() + " FE");
        helper.assertValueEqual(itemCount(sink), tier.itemsPerSecond(), tier.id() + " items");
        helper.assertValueEqual(sink.fluids.getAmountAsInt(0), tier.milliBucketsPerSecond(), tier.id() + " fluid");
    }

    private static void link(GameTestHelper helper, net.minecraft.world.level.block.Block cableBlock) {
        helper.setBlock(SOURCE, ModBlocks.TEST_BUFFER.get());
        helper.setBlock(CABLE, cableBlock);
        helper.setBlock(SINK, ModBlocks.TEST_BUFFER.get());
        wire(helper, CABLE, Direction.WEST, Direction.EAST, true, true, true);
    }

    private static void wire(GameTestHelper helper, BlockPos pos, Direction from, Direction to, boolean power, boolean items, boolean fluid) {
        CableBlockEntity cable = cable(helper, pos);
        if (power) {
            cable.installPower(from);
            cable.installPower(to);
        }
        if (items) {
            cable.installItem(from);
            cable.setItemMode(from, TransferMode.SEND);
            cable.installItem(to);
            cable.setItemMode(to, TransferMode.RECEIVE);
        }
        if (fluid) {
            cable.installFluid(from);
            cable.setFluidMode(from, TransferMode.SEND);
            cable.installFluid(to);
            cable.setFluidMode(to, TransferMode.RECEIVE);
        }
    }

    private static void fillSource(GameTestHelper helper, BlockPos pos) {
        TestBufferBlockEntity source = buffer(helper, pos);
        source.energy.set(4_000_000);
        source.items.set(0, ItemResource.of(Items.COBBLESTONE), 64);
        source.items.set(1, ItemResource.of(Items.COBBLESTONE), 64);
        source.fluids.set(0, FluidResource.of(Fluids.WATER), 80_000);
    }

    private static void tick(GameTestHelper helper, BlockPos cable, int times) {
        ServerLevel level = helper.getLevel();
        BlockPos absolute = helper.absolutePos(cable);
        for (int i = 0; i < times; i++) {
            CableNetworks.get(level).tickAt(level, absolute);
        }
    }

    private static CableBlockEntity cable(GameTestHelper helper, BlockPos pos) {
        return (CableBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    private static int itemCount(TestBufferBlockEntity buffer) {
        int total = 0;
        for (int slot = 0; slot < buffer.items.size(); slot++) {
            total += buffer.items.getAmountAsInt(slot);
        }
        return total;
    }

    private static TestBufferBlockEntity buffer(GameTestHelper helper, BlockPos pos) {
        return (TestBufferBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    private static int count(GameTestHelper helper, BlockPos pos, net.minecraft.world.item.Item item) {
        int total = 0;
        AABB box = new AABB(helper.absolutePos(pos)).inflate(4);
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, box)) {
            if (entity.getItem().is(item)) {
                total += entity.getItem().getCount();
            }
        }
        return total;
    }
}
