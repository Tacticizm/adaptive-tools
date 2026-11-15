package com.tacticizm.adaptivetools;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.Holder;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Smithing keeps components, lava repair, and a recipe for every item. */
public final class ProgressionGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, AdaptiveTools.MOD_ID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SMITHING = function("smithing_keeps_components", ProgressionGameTests::smithingKeepsComponents);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FRESH = function("direct_craft_is_fresh", ProgressionGameTests::directCraftIsFresh);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LAVA = function("lava_repair_returns_item", ProgressionGameTests::lavaRepairReturns);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WAIT = function("lava_waits_when_inventory_full", ProgressionGameTests::lavaWaits);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ONE = function("lava_one_item_per_source", ProgressionGameTests::oneItem);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RECIPES = function("every_item_has_recipe", ProgressionGameTests::everyItemHasRecipe);

    private ProgressionGameTests() {}

    public static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "progression"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        Identifier structure = Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "platform");
        for (var test : List.of(SMITHING, FRESH, LAVA, WAIT, ONE, RECIPES)) {
            event.registerTest(test.getId(), new FunctionGameTestInstance(
                    ResourceKey.create(BuiltInRegistries.TEST_FUNCTION.key(), test.getId()),
                    new TestData<>(environment, structure, 100, 0, true)));
        }
    }

    public static void smithingKeepsComponents(GameTestHelper helper) {
        Holder<Enchantment> unbreaking = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);
        ItemStack base = new ItemStack(ModItems.TEMPERED_COPPER_PICKAXE.get());
        ToolSettings settings = ToolSettings.of(base, ToolTier.TEMPERED_COPPER).withArea(true, 1);
        base.set(ModComponents.TOOL_SETTINGS.get(), settings);
        base.setDamageValue(40);
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(unbreaking, 1);
        base.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());

        ItemStack upgraded = smith(helper, ModItems.RESONANT_UPGRADE.get(), base, new ItemStack(ModItems.RESONANT_ALLOY_INGOT.get()));
        helper.assertTrue(upgraded.is(ModItems.RESONANT_PICKAXE.get()), "smithing made a resonant pickaxe");
        helper.assertTrue(ToolSettings.of(upgraded, ToolTier.RESONANT_ALLOY).areaMining(), "settings survived the upgrade");
        helper.assertValueEqual(upgraded.getDamageValue(), 40, "damage survived the upgrade");
        helper.assertValueEqual(upgraded.get(DataComponents.ENCHANTMENTS).getLevel(unbreaking), 1, "unbreaking survived the upgrade");

        ItemStack ember = new ItemStack(ModItems.EMBERSTEEL_PICKAXE.get());
        ember.set(ModComponents.TOOL_SETTINGS.get(), ToolSettings.of(ember, ToolTier.EMBERSTEEL).withGrading(true, 12));
        ember.setDamageValue(20);
        ItemStack flux = smith(helper, ModItems.FLUX_UPGRADE.get(), ember, new ItemStack(ModItems.FLUX_ALLOY_INGOT.get()));
        helper.assertTrue(flux.is(ModItems.FLUX_PICKAXE.get()), "embersteel upgraded to flux");
        helper.assertValueEqual(ModEnergy.getStored(flux), 0, "a durability upgrade starts with no FE");
        helper.assertTrue(ToolSettings.of(flux, ToolTier.FLUX_ALLOY).grading(), "flux kept grading");

        ItemStack charged = new ItemStack(ModItems.FLUX_PICKAXE.get());
        charged.set(ModComponents.ENERGY.get(), 1234);
        charged.set(ModComponents.TOOL_SETTINGS.get(), ToolSettings.of(charged, ToolTier.FLUX_ALLOY).withArea(true, 5));
        ItemStack ender = smith(helper, ModItems.ENDER_UPGRADE.get(), charged, new ItemStack(ModItems.ENDER_ALLOY_INGOT.get()));
        helper.assertValueEqual(ModEnergy.getStored(ender), 1234, "an energy upgrade keeps its FE");
        helper.assertTrue(ToolSettings.of(ender, ToolTier.ENDER_ALLOY).areaMining(), "ender kept area mining");
        helper.succeed();
    }

    public static void directCraftIsFresh(GameTestHelper helper) {
        RecipeManager recipes = (RecipeManager) helper.getLevel().recipeAccess();
        RecipeHolder<?> holder = recipes.byKey(ResourceKey.create(Registries.RECIPE, ModItems.RESONANT_PICKAXE.getId())).orElseThrow();
        ItemStack fresh = ((ShapedRecipe) holder.value()).assemble(CraftingInput.EMPTY);
        helper.assertTrue(fresh.is(ModItems.RESONANT_PICKAXE.get()), "the shaped recipe makes the pickaxe");
        helper.assertTrue(fresh.get(ModComponents.TOOL_SETTINGS.get()) == null, "a crafted pickaxe has no saved settings");
        helper.assertValueEqual(fresh.getDamageValue(), 0, "a crafted pickaxe is undamaged");
        helper.succeed();
    }

    public static void lavaRepairReturns(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        BlockPos lava = helper.absolutePos(new BlockPos(8, 2, 8));
        helper.setBlock(new BlockPos(8, 2, 8), Blocks.LAVA.defaultBlockState());
        ItemStack stack = new ItemStack(ModItems.EMBERSTEEL_PICKAXE.get());
        stack.setDamageValue(stack.getMaxDamage() / 10);
        stack.set(ModComponents.TOOL_SETTINGS.get(), ToolSettings.of(stack, ToolTier.EMBERSTEEL).withFell(true, 4, false, false));
        ItemEntity entity = spawn(helper, lava, stack, player);
        for (int tick = 0; tick < 20; tick++) {
            LavaRepair.tick(entity);
        }
        helper.assertTrue(helper.getLevel().getBlockState(lava).is(Blocks.OBSIDIAN), "the source became obsidian");
        helper.assertTrue(entity.isRemoved(), "the floating item was picked up");
        ItemStack returned = find(player, ModItems.EMBERSTEEL_PICKAXE.get());
        helper.assertTrue(!returned.isEmpty(), "the pickaxe is in the inventory");
        helper.assertValueEqual(returned.getDamageValue(), 0, "ten percent per second finished a ten percent chip");
        helper.assertTrue(ToolSettings.of(returned, ToolTier.EMBERSTEEL).fell(), "lava repair kept the settings");
        helper.succeed();
    }

    public static void lavaWaits(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE));
        }
        BlockPos lava = helper.absolutePos(new BlockPos(8, 2, 8));
        helper.setBlock(new BlockPos(8, 2, 8), Blocks.LAVA.defaultBlockState());
        ItemStack stack = new ItemStack(ModItems.EMBERSTEEL_AXE.get());
        stack.setDamageValue(stack.getMaxDamage() / 10);
        ItemEntity entity = spawn(helper, lava, stack, null);
        for (int tick = 0; tick < 20; tick++) {
            LavaRepair.tick(entity);
        }
        helper.assertTrue(helper.getLevel().getFluidState(lava).is(FluidTags.LAVA), "lava stays until the item is returned");
        helper.assertTrue(entity.isAlive(), "an ownerless item keeps floating");
        helper.assertValueEqual(entity.getItem().getDamageValue(), 0, "it is fully repaired while it waits");

        entity.setThrower(player);
        LavaRepair.tick(entity);
        helper.assertTrue(helper.getLevel().getFluidState(lava).is(FluidTags.LAVA), "a full inventory does not consume the lava");
        helper.assertTrue(entity.isAlive(), "the item stays until a slot is free");
        player.getInventory().setItem(0, ItemStack.EMPTY);
        LavaRepair.tick(entity);
        helper.assertTrue(helper.getLevel().getBlockState(lava).is(Blocks.OBSIDIAN), "lava is consumed once the item fits");
        helper.assertTrue(!find(player, ModItems.EMBERSTEEL_AXE.get()).isEmpty(), "the axe went into the free slot");
        helper.succeed();
    }

    public static void oneItem(GameTestHelper helper) {
        BlockPos lava = helper.absolutePos(new BlockPos(8, 2, 8));
        helper.setBlock(new BlockPos(8, 2, 8), Blocks.LAVA.defaultBlockState());
        ItemStack firstStack = new ItemStack(ModItems.EMBERSTEEL_SHOVEL.get());
        firstStack.setDamageValue(firstStack.getMaxDamage());
        ItemStack secondStack = new ItemStack(ModItems.EMBERSTEEL_HOE.get());
        secondStack.setDamageValue(secondStack.getMaxDamage());
        ItemEntity first = spawn(helper, lava, firstStack, null);
        LavaRepair.tick(first);
        ItemEntity second = spawn(helper, lava, secondStack, null);
        int firstBefore = first.getItem().getDamageValue();
        int secondBefore = second.getItem().getDamageValue();
        for (int tick = 0; tick < 20; tick++) {
            LavaRepair.tick(first);
            LavaRepair.tick(second);
        }
        helper.assertTrue(first.getItem().getDamageValue() < firstBefore, "the first item repaired");
        helper.assertValueEqual(second.getItem().getDamageValue(), secondBefore, "the second item was not repaired");
        helper.assertTrue(helper.getLevel().getFluidState(lava).is(FluidTags.LAVA), "the source is still lava");
        helper.succeed();
    }

    public static void everyItemHasRecipe(GameTestHelper helper) {
        RecipeManager recipes = (RecipeManager) helper.getLevel().recipeAccess();
        Set<Item> results = new HashSet<>();
        for (RecipeHolder<?> holder : recipes.getRecipes()) {
            if (!AdaptiveTools.MOD_ID.equals(holder.id().identifier().getNamespace())) {
                continue;
            }
            if (holder.value() instanceof CraftingRecipe crafting) {
                ItemStack stack = crafting.assemble(CraftingInput.EMPTY);
                if (!stack.isEmpty()) {
                    results.add(stack.getItem());
                }
            } else if (holder.value() instanceof SmithingTransformRecipe smithing) {
                ItemStack stack = smithing.assemble(new SmithingRecipeInput(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
                if (!stack.isEmpty()) {
                    results.add(stack.getItem());
                }
            }
        }
        List<String> missing = new java.util.ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null || !AdaptiveTools.MOD_ID.equals(id.getNamespace()) || "test_buffer".equals(id.getPath())) {
                continue;
            }
            if (!results.contains(item)) {
                missing.add(id.toString());
            }
        }
        helper.assertTrue(missing.isEmpty(), "items without a recipe: " + missing);
        helper.succeed();
    }

    private static ItemStack smith(GameTestHelper helper, Item template, ItemStack base, ItemStack ingot) {
        RecipeManager recipes = (RecipeManager) helper.getLevel().recipeAccess();
        SmithingRecipeInput input = new SmithingRecipeInput(new ItemStack(template), base, ingot);
        var found = recipes.getRecipeFor(RecipeType.SMITHING, input, helper.getLevel());
        helper.assertTrue(found.isPresent(), "smithing recipe for " + base.getItem());
        return found.get().value().assemble(input);
    }

    private static ItemEntity spawn(GameTestHelper helper, BlockPos pos, ItemStack stack, ServerPlayer thrower) {
        ItemEntity entity = new ItemEntity(helper.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        if (thrower != null) {
            entity.setThrower(thrower);
        }
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }

    private static ItemStack find(ServerPlayer player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
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
