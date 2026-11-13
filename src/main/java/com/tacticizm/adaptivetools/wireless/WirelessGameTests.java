package com.tacticizm.adaptivetools.wireless;

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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.tacticizm.adaptivetools.AdaptiveTools;
import com.tacticizm.adaptivetools.ModBlocks;
import com.tacticizm.adaptivetools.ModComponents;
import com.tacticizm.adaptivetools.ModItems;
import com.tacticizm.adaptivetools.ToolSettings;
import com.tacticizm.adaptivetools.testing.TestBufferBlockEntity;

/** Generator, transmitter, channels, receivers, and block drops. */
public final class WirelessGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, AdaptiveTools.MOD_ID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FUEL = function("generator_fuel_and_output", WirelessGameTests::fuelAndOutput);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CAP = function("transmitter_capacity_cap", WirelessGameTests::capacityCap);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CHANNELS = function("channel_assign_revoke_delete", WirelessGameTests::channels);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DELIVER = function("receiver_delivers_fe", WirelessGameTests::deliversFe);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CARGO = function("channel_item_and_fluid", WirelessGameTests::itemAndFluid);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CROSS = function("cross_dimension_needs_module", WirelessGameTests::crossDimension);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CARRIED = function("carried_charges_lowest_percent", WirelessGameTests::carriedLowest);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DROP = function("block_keeps_energy_when_broken", WirelessGameTests::keepsEnergy);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FIELD_FUEL = function("field_fuel_rate_and_cap", WirelessGameTests::fieldFuel);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FIELD_BASIC = function("field_basic_charges_lowest", WirelessGameTests::fieldBasic);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FIELD_ADVANCED = function("field_advanced_splits_and_areas", WirelessGameTests::fieldAdvanced);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FIELD_OFF = function("field_toggle_off_skips_generators", WirelessGameTests::fieldOff);

    private WirelessGameTests() {}

    public static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "wireless"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        Identifier structure = Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "platform");
        for (var test : List.of(FUEL, CAP, CHANNELS, DELIVER, CARGO, CROSS, CARRIED, DROP, FIELD_FUEL, FIELD_BASIC, FIELD_ADVANCED, FIELD_OFF)) {
            event.registerTest(test.getId(), new FunctionGameTestInstance(
                    ResourceKey.create(BuiltInRegistries.TEST_FUNCTION.key(), test.getId()),
                    new TestData<>(environment, structure, 100, 0, true)));
        }
    }

    public static void fuelAndOutput(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = new BlockPos(8, 2, 8);
        helper.setBlock(origin, ModBlocks.GENERATOR.get().defaultBlockState());
        GeneratorBlockEntity generator = generator(helper, origin);
        generator.fuel.setItem(0, new ItemStack(Items.COAL));
        generator.serverTick();
        helper.assertValueEqual(generator.energy(), GeneratorBlockEntity.FE_PER_TICK, "one burn tick stores 1000 FE");
        helper.assertTrue(generator.fuel.getItem(0).isEmpty(), "one coal was consumed");
        helper.assertTrue(generator.burnTime() > 0, "the coal keeps burning");
        int stored = generator.energy();
        generator.fuel.setItem(0, ItemStack.EMPTY);
        generator.prepare(stored, 0);
        generator.serverTick();
        helper.assertValueEqual(generator.energy(), stored, "stored FE stays when the fuel is gone");

        helper.setBlock(origin.east(), ModBlocks.TEST_BUFFER.get().defaultBlockState());
        generator.prepare(5_000, 0);
        generator.serverTick();
        helper.assertValueEqual(generator.energy(), 4_000, "the generator pushed 1000 FE");
        helper.assertValueEqual(buffer(level, helper.absolutePos(origin.east())).energy.getAmountAsInt(), 1_000, "the neighbor received 1000 FE");
        helper.succeed();
    }

    public static void capacityCap(GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 2, 4), ModBlocks.TRANSMITTER.get().defaultBlockState());
        TransmitterBlockEntity transmitter = transmitter(helper, new BlockPos(4, 2, 4));
        helper.assertValueEqual(transmitter.capacity(), TransmitterBlockEntity.BASE_CAPACITY, "base buffer is 1M");
        transmitter.modules.setItem(0, new ItemStack(ModItems.CAPACITY_MODULE.get(), 3));
        helper.assertValueEqual(transmitter.capacity(), 4_000_000, "three modules raise the cap to 4M");
        transmitter.modules.setItem(0, new ItemStack(ModItems.CAPACITY_MODULE.get(), 4));
        helper.assertValueEqual(transmitter.capacity(), 4_000_000, "a fourth module does not raise the cap");
        transmitter.modules.setItem(0, new ItemStack(ModItems.CAPACITY_MODULE.get(), 3));
        insert(transmitter, 4_000_000);
        helper.assertValueEqual(transmitter.energyLostIfRemoved(), 1_000_000, "removing one module would drop 1M FE");
        helper.assertTrue(!transmitter.canTakeCapacity(), "the module stays until the player confirms");
        transmitter.armRemoval();
        transmitter.modules.setItem(0, new ItemStack(ModItems.CAPACITY_MODULE.get(), 2));
        transmitter.onCapacityTaken();
        helper.assertValueEqual(transmitter.energy(), 3_000_000, "confirmed removal keeps what still fits");
        helper.succeed();
    }

    public static void channels(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        ServerPlayer other = player(helper);
        ServerLevel level = helper.getLevel();
        ChannelDirectory directory = ChannelDirectory.get(level);
        WirelessChannel channel = directory.create(owner, "Mill", ChannelKind.POWER);
        helper.setBlock(new BlockPos(2, 2, 2), ModBlocks.TRANSMITTER.get().defaultBlockState());
        helper.setBlock(new BlockPos(4, 2, 2), ModBlocks.TEST_BUFFER.get().defaultBlockState());
        helper.setBlock(new BlockPos(4, 2, 3), ModBlocks.RECEIVER.get().defaultBlockState().setValue(ReceiverBlock.FACING, net.minecraft.core.Direction.NORTH));
        TransmitterBlockEntity transmitter = transmitter(helper, new BlockPos(2, 2, 2));
        ReceiverBlockEntity receiver = receiver(helper, new BlockPos(4, 2, 3));
        transmitter.setOwner(owner.getUUID());
        transmitter.assign(channel.id);
        receiver.setOwner(other.getUUID());
        receiver.assign(channel.id);
        directory.permit(channel, other.getUUID());
        insert(transmitter, 5_000);
        WirelessNetwork.tick(level.getServer());
        helper.assertValueEqual(buffer(level, helper.absolutePos(new BlockPos(4, 2, 2))).energy.getAmountAsInt(), 1_000, "a permitted player receives power");
        directory.revoke(channel, other.getUUID());
        WirelessNetwork.tick(level.getServer());
        helper.assertValueEqual(buffer(level, helper.absolutePos(new BlockPos(4, 2, 2))).energy.getAmountAsInt(), 1_000, "revoking stops the transfer");
        int stored = transmitter.energy();
        int assigned = WirelessDevices.count(level.getServer(), channel.id);
        helper.assertValueEqual(assigned, 2, "delete reports both devices");
        directory.delete(level, channel);
        helper.assertTrue(transmitter.channelId() == null && receiver.channelId() == null, "devices return to Unassigned");
        helper.assertValueEqual(transmitter.energy(), stored, "deleting a channel keeps the buffer");
        helper.succeed();
    }

    public static void deliversFe(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        ServerLevel level = helper.getLevel();
        WirelessChannel channel = ChannelDirectory.get(level).create(owner, "Feed", ChannelKind.POWER);
        helper.setBlock(new BlockPos(6, 2, 6), ModBlocks.TRANSMITTER.get().defaultBlockState());
        helper.setBlock(new BlockPos(8, 2, 6), ModBlocks.TEST_BUFFER.get().defaultBlockState());
        helper.setBlock(new BlockPos(8, 2, 7), ModBlocks.RECEIVER.get().defaultBlockState().setValue(ReceiverBlock.FACING, net.minecraft.core.Direction.NORTH));
        TransmitterBlockEntity transmitter = transmitter(helper, new BlockPos(6, 2, 6));
        ReceiverBlockEntity receiver = receiver(helper, new BlockPos(8, 2, 7));
        transmitter.setOwner(owner.getUUID());
        receiver.setOwner(owner.getUUID());
        transmitter.assign(channel.id);
        receiver.assign(channel.id);
        insert(transmitter, 8_000);
        WirelessNetwork.tick(level.getServer());
        helper.assertValueEqual(buffer(level, helper.absolutePos(new BlockPos(8, 2, 6))).energy.getAmountAsInt(), 1_000, "the receiver delivered 1000 FE");
        helper.succeed();
    }

    public static void itemAndFluid(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        ServerLevel level = helper.getLevel();
        ChannelDirectory directory = ChannelDirectory.get(level);
        WirelessChannel items = directory.create(owner, "Items", ChannelKind.ITEM);
        WirelessChannel fluids = directory.create(owner, "Fluids", ChannelKind.FLUID);
        placePair(helper, new BlockPos(2, 2, 6), new BlockPos(5, 2, 6), items.id, owner);
        placePair(helper, new BlockPos(2, 2, 10), new BlockPos(5, 2, 10), fluids.id, owner);
        ReceiverBlockEntity itemSend = receiver(helper, new BlockPos(2, 2, 7));
        ReceiverBlockEntity itemRecv = receiver(helper, new BlockPos(5, 2, 7));
        itemSend.setItemMode(ReceiverBlockEntity.LinkMode.SEND);
        itemRecv.setItemMode(ReceiverBlockEntity.LinkMode.RECEIVE);
        ReceiverBlockEntity fluidSend = receiver(helper, new BlockPos(2, 2, 11));
        ReceiverBlockEntity fluidRecv = receiver(helper, new BlockPos(5, 2, 11));
        fluidSend.setFluidMode(ReceiverBlockEntity.LinkMode.SEND);
        fluidRecv.setFluidMode(ReceiverBlockEntity.LinkMode.RECEIVE);
        buffer(level, helper.absolutePos(new BlockPos(2, 2, 6))).items.set(0, ItemResource.of(Items.COBBLESTONE), 8);
        buffer(level, helper.absolutePos(new BlockPos(2, 2, 10))).fluids.set(0, FluidResource.of(Fluids.WATER), 8_000);
        WirelessNetwork.tick(level.getServer());
        helper.assertValueEqual(buffer(level, helper.absolutePos(new BlockPos(5, 2, 6))).items.getAmountAsInt(0), 1, "one item moved across the channel");
        helper.assertValueEqual(buffer(level, helper.absolutePos(new BlockPos(5, 2, 10))).fluids.getAmountAsInt(0), WirelessNetwork.MB_PER_TICK, "fluid moved across the channel");
        helper.succeed();
    }

    public static void crossDimension(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        ServerLevel overworld = helper.getLevel();
        ServerLevel nether = overworld.getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "the nether is loaded");
        WirelessChannel channel = ChannelDirectory.get(overworld).create(owner, "Far", ChannelKind.ITEM);
        helper.setBlock(new BlockPos(8, 2, 2), ModBlocks.TEST_BUFFER.get().defaultBlockState());
        helper.setBlock(new BlockPos(8, 2, 3), ModBlocks.RECEIVER.get().defaultBlockState().setValue(ReceiverBlock.FACING, net.minecraft.core.Direction.NORTH));
        BlockPos farMachine = new BlockPos(8, 80, 8);
        BlockPos farReceiver = new BlockPos(8, 80, 9);
        nether.setBlockAndUpdate(farMachine, ModBlocks.TEST_BUFFER.get().defaultBlockState());
        nether.setBlockAndUpdate(farReceiver, ModBlocks.RECEIVER.get().defaultBlockState().setValue(ReceiverBlock.FACING, net.minecraft.core.Direction.NORTH));
        ReceiverBlockEntity send = receiver(helper, new BlockPos(8, 2, 3));
        ReceiverBlockEntity recv = (ReceiverBlockEntity) nether.getBlockEntity(farReceiver);
        send.setOwner(owner.getUUID());
        recv.setOwner(owner.getUUID());
        send.assign(channel.id);
        recv.assign(channel.id);
        send.setItemMode(ReceiverBlockEntity.LinkMode.SEND);
        recv.setItemMode(ReceiverBlockEntity.LinkMode.RECEIVE);
        buffer(overworld, helper.absolutePos(new BlockPos(8, 2, 2))).items.set(0, ItemResource.of(Items.COBBLESTONE), 4);
        helper.setBlock(new BlockPos(6, 2, 2), ModBlocks.TRANSMITTER.get().defaultBlockState());
        TransmitterBlockEntity transmitter = transmitter(helper, new BlockPos(6, 2, 2));
        transmitter.setOwner(owner.getUUID());
        transmitter.assign(channel.id);
        WirelessNetwork.tick(overworld.getServer());
        helper.assertValueEqual(netherBuffer(nether, farMachine).items.getAmountAsInt(0), 0, "cross-dimension is blocked without the module");
        transmitter.modules.setItem(1, new ItemStack(ModItems.CROSS_DIMENSION_MODULE.get()));
        WirelessNetwork.tick(overworld.getServer());
        helper.assertValueEqual(netherBuffer(nether, farMachine).items.getAmountAsInt(0), 1, "the module lets the item cross");
        helper.succeed();
    }

    public static void carriedLowest(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        ServerLevel level = helper.getLevel();
        WirelessChannel channel = ChannelDirectory.get(level).create(owner, "Pack", ChannelKind.POWER);
        helper.setBlock(new BlockPos(3, 2, 8), ModBlocks.TRANSMITTER.get().defaultBlockState());
        TransmitterBlockEntity transmitter = transmitter(helper, new BlockPos(3, 2, 8));
        transmitter.setOwner(owner.getUUID());
        transmitter.assign(channel.id);
        insert(transmitter, 50_000);
        ItemStack carried = new ItemStack(ModItems.CARRIED_RECEIVER.get());
        CarriedReceiverItem.assign(carried, channel.id);
        ItemStack empty = new ItemStack(ModItems.FLUX_PICKAXE.get());
        empty.set(ModComponents.ENERGY.get(), 0);
        ItemStack half = new ItemStack(ModItems.FLUX_PICKAXE.get());
        half.set(ModComponents.ENERGY.get(), 50_000);
        owner.getInventory().setItem(0, carried);
        owner.getInventory().setItem(1, empty);
        owner.getInventory().setItem(2, half);
        WirelessNetwork.tick(level.getServer());
        helper.assertValueEqual(owner.getInventory().getItem(1).getOrDefault(ModComponents.ENERGY.get(), 0), 1_000, "the emptier item was charged");
        helper.assertValueEqual(owner.getInventory().getItem(2).getOrDefault(ModComponents.ENERGY.get(), 0), 50_000, "the fuller item waited");
        helper.succeed();
    }

    public static void keepsEnergy(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        BlockPos origin = new BlockPos(8, 2, 12);
        helper.setBlock(origin, ModBlocks.GENERATOR.get().defaultBlockState());
        GeneratorBlockEntity generator = generator(helper, origin);
        generator.prepare(12_345, 0);
        ServerLevel level = helper.getLevel();
        BlockPos absolute = helper.absolutePos(origin);
        level.destroyBlock(absolute, true, owner);
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(absolute).inflate(2), entity -> entity.getItem().is(ModBlocks.GENERATOR_ITEM.get()));
        helper.assertTrue(!drops.isEmpty(), "breaking the generator dropped it");
        owner.getInventory().setItem(0, drops.get(0).getItem().copy());
        drops.get(0).discard();
        helper.setBlock(origin, Blocks.STONE.defaultBlockState());
        BlockPos place = helper.absolutePos(origin.above());
        owner.getInventory().getItem(0).useOn(new net.minecraft.world.item.context.UseOnContext(owner, net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(helper.absolutePos(origin).getCenter(), net.minecraft.core.Direction.UP, helper.absolutePos(origin), false)));
        GeneratorBlockEntity restored = (GeneratorBlockEntity) level.getBlockEntity(place);
        helper.assertTrue(restored != null && restored.energy() == 12_345, "the placed generator kept its FE, found " + (restored == null ? "none" : restored.energy()));
        helper.succeed();
    }

    public static void fieldFuel(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        ItemStack basic = new ItemStack(ModItems.BASIC_FIELD.get());
        basic.set(ModComponents.FIELD_FUEL.get(), new FrozenStack(new ItemStack(Items.COAL)));
        owner.getInventory().setItem(0, basic);
        FieldGenerators.tick(owner);
        helper.assertValueEqual(energy(owner.getInventory().getItem(0)), 200, "basic generates 200 FE per burn tick");
        helper.assertTrue(owner.getInventory().getItem(0).getOrDefault(ModComponents.FIELD_BURN.get(), 0) > 0, "the coal is burning");
        helper.assertTrue(owner.getInventory().getItem(0).get(ModComponents.FIELD_FUEL.get()) == null, "one coal was consumed");

        ItemStack advanced = new ItemStack(ModItems.ADVANCED_FIELD.get());
        advanced.set(ModComponents.FIELD_FUEL.get(), new FrozenStack(new ItemStack(Items.COAL)));
        owner.getInventory().setItem(0, advanced);
        FieldGenerators.tick(owner);
        helper.assertValueEqual(energy(owner.getInventory().getItem(0)), 1_000, "advanced generates 1000 FE per burn tick");

        ItemStack capped = new ItemStack(ModItems.BASIC_FIELD.get());
        capped.set(ModComponents.ENERGY.get(), 1_000_000 - 50);
        capped.set(ModComponents.FIELD_BURN.get(), 3);
        owner.getInventory().setItem(0, capped);
        FieldGenerators.tick(owner);
        helper.assertValueEqual(energy(owner.getInventory().getItem(0)), 1_000_000, "generation stops at the buffer cap");
        helper.succeed();
    }

    public static void fieldBasic(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        ItemStack generator = new ItemStack(ModItems.BASIC_FIELD.get());
        generator.set(ModComponents.ENERGY.get(), 10_000);
        owner.getInventory().setItem(0, generator);
        owner.getInventory().setItem(1, flux(0));
        owner.getInventory().setItem(2, flux(50_000));
        FieldGenerators.tick(owner);
        helper.assertValueEqual(energy(owner.getInventory().getItem(1)), 1_000, "only the lowest item was charged");
        helper.assertValueEqual(energy(owner.getInventory().getItem(2)), 50_000, "the fuller item was left alone");
        helper.assertValueEqual(energy(owner.getInventory().getItem(0)), 9_000, "the generator spent 1000 FE");
        helper.succeed();
    }

    public static void fieldAdvanced(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        ItemStack generator = new ItemStack(ModItems.ADVANCED_FIELD.get());
        generator.set(ModComponents.ENERGY.get(), 4_000);
        owner.getInventory().setItem(0, generator);
        owner.getInventory().setItem(9, flux(0));
        owner.getInventory().setItem(10, flux(0));
        FieldGenerators.tick(owner);
        helper.assertValueEqual(energy(owner.getInventory().getItem(9)) + energy(owner.getInventory().getItem(10)), 4_000, "advanced spends at most 4000 FE");
        helper.assertValueEqual(energy(owner.getInventory().getItem(9)), 2_000, "the first empty item got half");
        helper.assertValueEqual(energy(owner.getInventory().getItem(10)), 2_000, "the second empty item got half");

        ItemStack limited = new ItemStack(ModItems.ADVANCED_FIELD.get());
        limited.set(ModComponents.ENERGY.get(), 4_000);
        limited.set(ModComponents.TOOL_SETTINGS.get(), FieldGenerators.settings(limited).toggle(ToolSettings.FIELD_HOTBAR));
        owner.getInventory().setItem(0, limited);
        owner.getInventory().setItem(1, flux(0));
        owner.getInventory().setItem(9, flux(0));
        owner.getInventory().setItem(10, ItemStack.EMPTY);
        FieldGenerators.tick(owner);
        helper.assertValueEqual(energy(owner.getInventory().getItem(1)), 0, "a disabled hotbar is not charged");
        helper.assertValueEqual(energy(owner.getInventory().getItem(9)), 4_000, "the main-inventory item took the whole budget");
        helper.succeed();
    }

    public static void fieldOff(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        ItemStack generator = new ItemStack(ModItems.BASIC_FIELD.get());
        generator.set(ModComponents.ENERGY.get(), 10_000);
        generator.set(ModComponents.TOOL_SETTINGS.get(), FieldGenerators.settings(generator).toggle(ToolSettings.FIELD_ON));
        owner.getInventory().setItem(0, generator);
        owner.getInventory().setItem(1, flux(0));
        FieldGenerators.tick(owner);
        helper.assertValueEqual(energy(owner.getInventory().getItem(1)), 0, "toggle off does not charge");
        helper.assertValueEqual(energy(owner.getInventory().getItem(0)), 10_000, "toggle off does not spend FE");

        ItemStack active = new ItemStack(ModItems.BASIC_FIELD.get());
        active.set(ModComponents.ENERGY.get(), 10_000);
        ItemStack other = new ItemStack(ModItems.ADVANCED_FIELD.get());
        owner.getInventory().setItem(0, active);
        owner.getInventory().setItem(1, other);
        owner.getInventory().setItem(2, flux(0));
        FieldGenerators.tick(owner);
        helper.assertValueEqual(energy(owner.getInventory().getItem(1)), 0, "another field generator is not charged");
        helper.assertValueEqual(energy(owner.getInventory().getItem(2)), 1_000, "a normal FE item still charges");
        helper.succeed();
    }

    private static ItemStack flux(int stored) {
        ItemStack stack = new ItemStack(ModItems.FLUX_PICKAXE.get());
        stack.set(ModComponents.ENERGY.get(), stored);
        return stack;
    }

    private static int energy(ItemStack stack) {
        return stack.getOrDefault(ModComponents.ENERGY.get(), 0);
    }

    private static void placePair(GameTestHelper helper, BlockPos machine, BlockPos otherMachine, java.util.UUID channel, ServerPlayer owner) {
        helper.setBlock(machine, ModBlocks.TEST_BUFFER.get().defaultBlockState());
        helper.setBlock(machine.south(), ModBlocks.RECEIVER.get().defaultBlockState().setValue(ReceiverBlock.FACING, net.minecraft.core.Direction.NORTH));
        helper.setBlock(otherMachine, ModBlocks.TEST_BUFFER.get().defaultBlockState());
        helper.setBlock(otherMachine.south(), ModBlocks.RECEIVER.get().defaultBlockState().setValue(ReceiverBlock.FACING, net.minecraft.core.Direction.NORTH));
        ReceiverBlockEntity send = receiver(helper, machine.south());
        ReceiverBlockEntity recv = receiver(helper, otherMachine.south());
        send.setOwner(owner.getUUID());
        recv.setOwner(owner.getUUID());
        send.assign(channel);
        recv.assign(channel);
    }

    private static void insert(TransmitterBlockEntity transmitter, int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            transmitter.energyHandler.insert(amount, transaction);
            transaction.commit();
        }
    }

    private static GeneratorBlockEntity generator(GameTestHelper helper, BlockPos pos) {
        return (GeneratorBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    private static TransmitterBlockEntity transmitter(GameTestHelper helper, BlockPos pos) {
        return (TransmitterBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    private static ReceiverBlockEntity receiver(GameTestHelper helper, BlockPos pos) {
        return (ReceiverBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    private static TestBufferBlockEntity buffer(ServerLevel level, BlockPos absolute) {
        return (TestBufferBlockEntity) level.getBlockEntity(absolute);
    }

    private static TestBufferBlockEntity netherBuffer(ServerLevel nether, BlockPos pos) {
        return (TestBufferBlockEntity) nether.getBlockEntity(pos);
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> function(String name, Consumer<GameTestHelper> test) {
        return FUNCTIONS.register(name, () -> test);
    }
}
