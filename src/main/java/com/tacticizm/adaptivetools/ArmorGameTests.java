package com.tacticizm.adaptivetools;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Shield math, energy drain, water walking, and the armor recipes. */
public final class ArmorGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, AdaptiveTools.MOD_ID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SHIELD = function("shield_no_double_charge", ArmorGameTests::shieldOnce);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LEFTOVER = function("shield_leftover_passes", ArmorGameTests::shieldLeftover);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ELYTRA = function("elytra_fe_drain", ArmorGameTests::elytraDrain);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FLIGHT = function("flight_fe_drain", ArmorGameTests::flightDrain);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WATER = function("water_walking", ArmorGameTests::waterWalking);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RECIPES = function("armor_recipes", ArmorGameTests::recipes);

    private ArmorGameTests() {}

    public static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "armor"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        Identifier structure = Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "platform");
        for (var test : List.of(SHIELD, LEFTOVER, ELYTRA, FLIGHT, WATER, RECIPES)) {
            event.registerTest(test.getId(), new FunctionGameTestInstance(
                    ResourceKey.create(BuiltInRegistries.TEST_FUNCTION.key(), test.getId()),
                    new TestData<>(environment, structure, 100, 0, true)));
        }
    }

    public static void shieldOnce(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.setItemSlot(EquipmentSlot.HEAD, charged(ToolTier.FLUX_ALLOY, ArmorType.HELMET, 500, true));
        player.setItemSlot(EquipmentSlot.CHEST, charged(ToolTier.FLUX_ALLOY, ArmorType.CHESTPLATE, 500, true));
        float left = ArmorAbilities.absorb(player, 4.0F);
        helper.assertValueEqual((int) left, 0, "four damage was covered");
        helper.assertValueEqual(ModEnergy.getStored(player.getItemBySlot(EquipmentSlot.CHEST)), 100, "chest paid 400 FE");
        helper.assertValueEqual(ModEnergy.getStored(player.getItemBySlot(EquipmentSlot.HEAD)), 500, "helmet was not charged");
        helper.succeed();
    }

    public static void shieldLeftover(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.setItemSlot(EquipmentSlot.HEAD, charged(ToolTier.ENDER_ALLOY, ArmorType.HELMET, 200, true));
        player.setItemSlot(EquipmentSlot.CHEST, charged(ToolTier.ENDER_ALLOY, ArmorType.CHESTPLATE, 500, true));
        float left = ArmorAbilities.absorb(player, 8.0F);
        helper.assertValueEqual((int) left, 3, "three damage passed through");
        helper.assertValueEqual(ModEnergy.getStored(player.getItemBySlot(EquipmentSlot.CHEST)), 0, "chest spent the 500 FE it had");
        helper.assertValueEqual(ModEnergy.getStored(player.getItemBySlot(EquipmentSlot.HEAD)), 200, "the other shield was not charged");
        helper.succeed();
    }

    public static void elytraDrain(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ItemStack chest = charged(ToolTier.FLUX_ALLOY, ArmorType.CHESTPLATE, 1000, false);
        chest.set(ModComponents.TOOL_SETTINGS.get(), ToolSettings.of(chest, ToolTier.FLUX_ALLOY).withElytra(true));
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        ArmorAbilities.tickElytra(player, false);
        helper.assertValueEqual(ModEnergy.getStored(player.getItemBySlot(EquipmentSlot.CHEST)), 1000, "standing still is free");
        ArmorAbilities.tickElytra(player, true);
        helper.assertValueEqual(ModEnergy.getStored(player.getItemBySlot(EquipmentSlot.CHEST)), 988, "one glide tick costs 12 FE");
        player.getItemBySlot(EquipmentSlot.CHEST).set(ModComponents.ENERGY.get(), 10);
        ArmorAbilities.tickElytra(player, true);
        helper.assertValueEqual(ModEnergy.getStored(player.getItemBySlot(EquipmentSlot.CHEST)), 0, "the last charge was spent");
        helper.assertTrue(player.hasEffect(MobEffects.SLOW_FALLING), "an empty chest starts the slow fall");
        helper.succeed();
    }

    public static void flightDrain(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ItemStack chest = charged(ToolTier.ENDER_ALLOY, ArmorType.CHESTPLATE, 500, false);
        chest.set(ModComponents.TOOL_SETTINGS.get(), ToolSettings.of(chest, ToolTier.ENDER_ALLOY).withFlight(true));
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        player.getAbilities().flying = false;
        ArmorAbilities.flight(player);
        helper.assertValueEqual(ModEnergy.getStored(player.getItemBySlot(EquipmentSlot.CHEST)), 500, "flight costs nothing until the player is flying");
        helper.assertTrue(player.getAbilities().mayfly, "the toggle grants flight");
        player.getAbilities().flying = true;
        ArmorAbilities.flight(player);
        helper.assertValueEqual(ModEnergy.getStored(player.getItemBySlot(EquipmentSlot.CHEST)), 400, "one flying tick costs 100 FE");
        player.getAbilities().flying = false;
        player.setOnGround(true);
        ArmorAbilities.flight(player);
        helper.assertFalse(ToolSettings.of(player.getItemBySlot(EquipmentSlot.CHEST), ToolTier.ENDER_ALLOY).flight(), "landing switches flight off");
        helper.assertFalse(player.getAbilities().mayfly, "landing clears the flight ability");

        ItemStack dry = charged(ToolTier.ENDER_ALLOY, ArmorType.CHESTPLATE, 50, false);
        dry.set(ModComponents.TOOL_SETTINGS.get(), ToolSettings.of(dry, ToolTier.ENDER_ALLOY).withFlight(true));
        player.setItemSlot(EquipmentSlot.CHEST, dry);
        player.getAbilities().flying = true;
        ArmorAbilities.flight(player);
        helper.assertValueEqual(ModEnergy.getStored(player.getItemBySlot(EquipmentSlot.CHEST)), 0, "a short charge is spent");
        helper.assertFalse(player.getAbilities().flying, "flight stops when the chest is empty");
        helper.assertTrue(player.hasEffect(MobEffects.SLOW_FALLING), "an empty chest slow-falls");
        helper.succeed();
    }

    public static void waterWalking(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        BlockPos water = helper.absolutePos(new BlockPos(8, 2, 8));
        helper.setBlock(new BlockPos(8, 2, 8), Blocks.WATER.defaultBlockState());
        ItemStack boots = new ItemStack(ModItems.armor(ToolTier.RESONANT_ALLOY, ArmorType.BOOTS).get());
        boots.set(ModComponents.TOOL_SETTINGS.get(), ToolSettings.of(boots, ToolTier.RESONANT_ALLOY).withWaterWalking(true));
        player.setItemSlot(EquipmentSlot.FEET, boots);
        player.setPos(water.getX() + 0.5, water.getY() + 0.2, water.getZ() + 0.5);
        ArmorAbilities.water(player);
        helper.assertTrue(Math.abs(player.getY() - (water.getY() + 1.0)) < 0.01, "feet stay on the surface, y=" + player.getY());
        helper.assertTrue(helper.getLevel().getFluidState(water).is(FluidTags.WATER), "the water was not frozen");
        helper.assertTrue(helper.getLevel().getBlockState(water).is(Blocks.WATER), "the water block is still water");
        player.setShiftKeyDown(true);
        player.setPos(water.getX() + 0.5, water.getY() + 0.2, water.getZ() + 0.5);
        ArmorAbilities.water(player);
        helper.assertTrue(Math.abs(player.getY() - (water.getY() + 0.2)) < 0.01, "sneak sinks instead of standing");
        helper.succeed();
    }

    public static void recipes(GameTestHelper helper) {
        RecipeManager recipes = (RecipeManager) helper.getLevel().recipeAccess();
        for (ToolTier tier : ToolTier.values()) {
            for (ArmorType type : List.of(ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS)) {
                Identifier id = ModItems.armor(tier, type).getId();
                Optional<RecipeHolder<?>> recipe = recipes.byKey(ResourceKey.create(Registries.RECIPE, id));
                helper.assertTrue(recipe.isPresent(), "recipe " + id);
            }
        }
        ItemStack ingot = new ItemStack(ModItems.TEMPERED_COPPER_INGOT.get());
        ItemStack empty = ItemStack.EMPTY;
        assertCraft(helper, recipes, CraftingInput.of(3, 2, List.of(ingot, ingot, ingot, ingot, empty, ingot)), ModItems.armor(ToolTier.TEMPERED_COPPER, ArmorType.HELMET).get().getDefaultInstance());
        assertCraft(helper, recipes, CraftingInput.of(3, 3, List.of(ingot, empty, ingot, ingot, ingot, ingot, ingot, ingot, ingot)), ModItems.armor(ToolTier.TEMPERED_COPPER, ArmorType.CHESTPLATE).get().getDefaultInstance());
        assertCraft(helper, recipes, CraftingInput.of(3, 3, List.of(ingot, ingot, ingot, ingot, empty, ingot, ingot, empty, ingot)), ModItems.armor(ToolTier.TEMPERED_COPPER, ArmorType.LEGGINGS).get().getDefaultInstance());
        assertCraft(helper, recipes, CraftingInput.of(3, 2, List.of(ingot, empty, ingot, ingot, empty, ingot)), ModItems.armor(ToolTier.TEMPERED_COPPER, ArmorType.BOOTS).get().getDefaultInstance());

        ItemStack resonant = new ItemStack(ModItems.RESONANT_ALLOY_INGOT.get());
        ItemStack crystal = new ItemStack(Items.PRISMARINE_CRYSTALS);
        CraftingInput module = CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.WATER_BUCKET), resonant, resonant, crystal, crystal, empty, empty, empty, empty));
        Optional<RecipeHolder<CraftingRecipe>> found = recipes.getRecipeFor(RecipeType.CRAFTING, module, helper.getLevel());
        helper.assertTrue(found.isPresent(), "water walking module recipe");
        helper.assertTrue(found.get().value().assemble(module).is(ModItems.WATER_WALKING_MODULE.get()), "recipe makes the module");
        boolean bucket = false;
        for (ItemStack remain : found.get().value().getRemainingItems(module)) {
            if (remain.is(Items.BUCKET)) {
                bucket = true;
            }
        }
        helper.assertTrue(bucket, "the water bucket comes back empty");
        helper.succeed();
    }

    private static void assertCraft(GameTestHelper helper, RecipeManager recipes, CraftingInput input, ItemStack expected) {
        Optional<RecipeHolder<CraftingRecipe>> found = recipes.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(found.isPresent() && found.get().value().assemble(input).is(expected.getItem()), "craft " + expected.getItem());
    }

    private static ItemStack charged(ToolTier tier, ArmorType type, int energy, boolean shield) {
        ItemStack stack = new ItemStack(ModItems.armor(tier, type).get());
        stack.set(ModComponents.ENERGY.get(), energy);
        if (shield) {
            stack.set(ModComponents.TOOL_SETTINGS.get(), ToolSettings.of(stack, tier).withShield(true));
        }
        return stack;
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().instabuild = false;
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        return player;
    }

    private static DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> function(String name, Consumer<GameTestHelper> test) {
        return FUNCTIONS.register(name, () -> test);
    }
}
