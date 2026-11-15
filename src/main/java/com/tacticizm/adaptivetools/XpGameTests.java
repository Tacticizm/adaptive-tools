package com.tacticizm.adaptivetools;

import java.util.List;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Tool XP, points, and the grandfather rule for items that predate the component. */
public final class XpGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, AdaptiveTools.MOD_ID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SOURCES = function("xp_from_work", XpGameTests::sources);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> POINTS = function("xp_levels_points_and_cap", XpGameTests::points);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> KEEP = function("xp_smithing_fresh_and_grandfather", XpGameTests::keep);

    private XpGameTests() {}

    public static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "xp"), new TestEnvironmentDefinition.AllOf(List.of()));
        Identifier structure = Identifier.fromNamespaceAndPath(AdaptiveTools.MOD_ID, "platform");
        for (var test : List.of(SOURCES, POINTS, KEEP)) {
            event.registerTest(test.getId(), new FunctionGameTestInstance(
                    ResourceKey.create(BuiltInRegistries.TEST_FUNCTION.key(), test.getId()),
                    new TestData<>(environment, structure, 100, 0, true)));
        }
    }

    public static void sources(GameTestHelper helper) {
        for (int x = 7; x <= 9; x++) {
            for (int z = 7; z <= 9; z++) {
                helper.setBlock(new BlockPos(x, 3, z), Blocks.STONE);
            }
        }
        ItemStack pick = new ItemStack(ModItems.RESONANT_PICKAXE.get());
        pick.set(ModComponents.TOOL_SETTINGS.get(), ToolSettings.area(true, 3));
        ServerPlayer miner = player(helper, pick);
        AdaptivePickaxeItem.FORCED_FACE.set(net.minecraft.core.Direction.UP);
        try {
            miner.gameMode.destroyBlock(helper.absolutePos(new BlockPos(8, 3, 8)));
        } finally {
            AdaptivePickaxeItem.FORCED_FACE.remove();
        }
        int broken = 0;
        for (int x = 7; x <= 9; x++) {
            for (int z = 7; z <= 9; z++) {
                if (helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(x, 3, z))).isAir()) {
                    broken++;
                }
            }
        }
        helper.assertValueEqual(broken, 9, "the 3x3 came out");
        helper.assertValueEqual(xp(miner.getMainHandItem()), 9, "each block in the area gave 1 XP");

        for (int y = 3; y <= 6; y++) {
            helper.setBlock(new BlockPos(8, y, 8), Blocks.OAK_LOG);
        }
        ItemStack axe = new ItemStack(ModItems.RESONANT_AXE.get());
        axe.set(ModComponents.TOOL_SETTINGS.get(), ToolSettings.defaults(ToolTier.RESONANT_ALLOY).withFell(true, 4, true, false));
        miner.setItemInHand(InteractionHand.MAIN_HAND, axe);
        miner.gameMode.destroyBlock(helper.absolutePos(new BlockPos(8, 3, 8)));
        helper.assertValueEqual(xp(miner.getMainHandItem()), 4, "each felled log gave 1 XP and leaves did not");

        ItemStack hoe = new ItemStack(ModItems.RESONANT_HOE.get());
        hoe.set(ModComponents.TOOL_SETTINGS.get(), ToolSettings.defaults(ToolTier.RESONANT_ALLOY).withHoe(true, false, false));
        miner.setItemInHand(InteractionHand.MAIN_HAND, hoe);
        helper.setBlock(new BlockPos(8, 4, 8), Blocks.AIR);
        helper.setBlock(new BlockPos(8, 3, 8), Blocks.DIRT);
        use(miner, helper.absolutePos(new BlockPos(8, 3, 8)));
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(8, 3, 8))).is(Blocks.FARMLAND), "dirt was tilled");
        helper.assertValueEqual(xp(miner.getMainHandItem()), 1, "tilling gave 1 XP");
        use(miner, helper.absolutePos(new BlockPos(8, 3, 8)));
        helper.assertValueEqual(xp(miner.getMainHandItem()), 1, "tilling farmland again gave nothing");
        hoe.set(ModComponents.TOOL_SETTINGS.get(), ToolSettings.defaults(ToolTier.RESONANT_ALLOY).withHoe(false, true, false));
        miner.setItemInHand(InteractionHand.MAIN_HAND, hoe);
        helper.setBlock(new BlockPos(8, 3, 8), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE));
        use(miner, helper.absolutePos(new BlockPos(8, 3, 8)));
        helper.assertValueEqual(xp(miner.getMainHandItem()), 2, "harvesting gave 1 XP");

        ItemStack sword = new ItemStack(ModItems.RESONANT_SWORD.get());
        miner.setItemInHand(InteractionHand.MAIN_HAND, sword);
        Zombie zombie = new Zombie(EntityType.ZOMBIE, helper.getLevel());
        BlockPos feet = helper.absolutePos(new BlockPos(4, 3, 4));
        zombie.setPos(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
        helper.getLevel().addFreshEntity(zombie);
        int expected = ToolXp.killXp(zombie);
        helper.assertValueEqual(expected, 2, "a zombie is worth 2 XP");
        zombie.hurt(helper.getLevel().damageSources().playerAttack(miner), 100.0F);
        helper.assertValueEqual(xp(miner.getMainHandItem()), expected, "the sword gained health-based XP");

        ItemStack helmet = new ItemStack(ModItems.armor(ToolTier.FLUX_ALLOY, ArmorType.HELMET).get());
        miner.setItemSlot(EquipmentSlot.HEAD, helmet);
        Zombie second = new Zombie(EntityType.ZOMBIE, helper.getLevel());
        second.setPos(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
        helper.getLevel().addFreshEntity(second);
        second.hurt(helper.getLevel().damageSources().playerAttack(miner), 100.0F);
        helper.assertValueEqual(xp(miner.getItemBySlot(EquipmentSlot.HEAD)), ToolXp.killXp(second), "worn armor gained the same kill XP");

        ItemStack chest = new ItemStack(ModItems.armor(ToolTier.FLUX_ALLOY, ArmorType.CHESTPLATE).get());
        chest.set(ModComponents.ENERGY.get(), 10_000);
        chest.set(ModComponents.TOOL_SETTINGS.get(), ToolSettings.of(chest, ToolTier.FLUX_ALLOY).withShield(true));
        miner.setItemSlot(EquipmentSlot.CHEST, chest);
        ArmorAbilities.absorb(miner, 25.0F);
        ToolProgress shield = miner.getItemBySlot(EquipmentSlot.CHEST).get(ModComponents.TOOL_PROGRESS.get());
        helper.assertValueEqual(shield.xp(), 2, "25 absorbed damage is 2 XP");
        helper.assertValueEqual(shield.remainder(), 5, "the leftover damage is remembered");
        ArmorAbilities.absorb(miner, 5.0F);
        shield = miner.getItemBySlot(EquipmentSlot.CHEST).get(ModComponents.TOOL_PROGRESS.get());
        helper.assertValueEqual(shield.xp(), 3, "the remainder completed another XP");
        helper.assertValueEqual(shield.remainder(), 0, "the remainder was spent");
        helper.succeed();
    }

    public static void points(GameTestHelper helper) {
        helper.assertValueEqual(ToolXp.levelFor(49), 0, "49 XP is still level 0");
        helper.assertValueEqual(ToolXp.levelFor(50), 1, "level 1 costs 50");
        helper.assertValueEqual(ToolXp.levelFor(149), 1, "level 2 costs 100 more");
        helper.assertValueEqual(ToolXp.levelFor(150), 2, "150 XP is level 2");
        helper.assertValueEqual(ToolXp.xpForNext(2), 150, "the next step is 150");

        ItemStack pick = new ItemStack(ModItems.RESONANT_PICKAXE.get());
        ToolXp.onCrafted(pick);
        helper.assertValueEqual(ToolXp.cap(pick), 10, "resonant pickaxe cap is efficiency, fortune, silk, and 3x3");
        ToolXp.award(pick, 10_000);
        helper.assertTrue(ToolXp.levelFor(xp(pick)) > ToolXp.cap(pick), "XP can pass the point cap");
        helper.assertValueEqual(ToolXp.earned(pick), ToolXp.cap(pick), "points stop at the cap");
        helper.assertValueEqual(ToolXp.percent(pick), 100, "a capped bar reads full");

        ItemStack fresh = new ItemStack(ModItems.RESONANT_PICKAXE.get());
        ToolXp.onCrafted(fresh);
        ToolXp.award(fresh, 300);
        helper.assertValueEqual(ToolXp.levelFor(xp(fresh)), 3, "300 XP is level 3");
        helper.assertValueEqual(ToolXp.unspent(fresh), 3, "three levels granted three points");
        ToolSettings bought = ToolXp.purchase(fresh, ToolSettings.of(fresh, ToolTier.RESONANT_ALLOY), ToolSettings.area(true, 3), ToolTier.RESONANT_ALLOY);
        helper.assertTrue(bought.areaMining(), "a point turned 3x3 on");
        helper.assertValueEqual(ToolXp.unspent(fresh), 2, "one point was spent");
        ToolSettings tooBig = ToolXp.purchase(fresh, bought, ToolSettings.area(true, 7), ToolTier.RESONANT_ALLOY);
        helper.assertValueEqual(tooBig.areaSize(), 3, "the tier cap still stops at 3x3");
        ToolXp.award(fresh, 10_000);
        ServerPlayer owner = player(helper, fresh);
        owner.getInventory().setItem(0, fresh);
        ToolXp.act(owner, 0, ToolXp.FORT_UP);
        ToolXp.act(owner, 0, ToolXp.FORT_UP);
        ToolXp.act(owner, 0, ToolXp.SILK);
        ToolProgress enchanted = owner.getInventory().getItem(0).get(ModComponents.TOOL_PROGRESS.get());
        helper.assertValueEqual(enchanted.fortune(), 2, "fortune was bought");
        helper.assertValueEqual(enchanted.fortuneOn(), 0, "silk turned fortune off");
        helper.assertValueEqual(enchanted.silkOn(), 1, "silk is the active enchant");
        ToolXp.act(owner, 0, ToolXp.FORT_DOWN);
        ToolXp.reset(owner.getInventory().getItem(0), ToolTier.RESONANT_ALLOY);
        ItemStack reset = owner.getInventory().getItem(0);
        helper.assertValueEqual(ToolXp.unspent(reset), ToolXp.earned(reset), "reset refunded every spent point");
        helper.assertTrue(!ToolSettings.of(reset, ToolTier.RESONANT_ALLOY).areaMining(), "reset turned the bought area off");
        helper.assertValueEqual(reset.get(ModComponents.TOOL_PROGRESS.get()).fortune(), 0, "reset cleared the fortune purchase");
        helper.assertValueEqual(xp(reset), 10_300, "reset did not create XP");
        helper.succeed();
    }

    public static void keep(GameTestHelper helper) {
        ItemStack base = new ItemStack(ModItems.TEMPERED_COPPER_PICKAXE.get());
        ToolXp.onCrafted(base);
        ToolXp.award(base, 80);
        base.set(ModComponents.TOOL_SETTINGS.get(), ToolSettings.area(true, 1));
        var recipes = (net.minecraft.world.item.crafting.RecipeManager) helper.getLevel().recipeAccess();
        var found = recipes.getRecipeFor(RecipeType.SMITHING, new SmithingRecipeInput(new ItemStack(ModItems.RESONANT_UPGRADE.get()), base, new ItemStack(ModItems.RESONANT_ALLOY_INGOT.get())), helper.getLevel());
        helper.assertTrue(found.isPresent(), "upgrade recipe");
        ItemStack upgraded = found.get().value().assemble(new SmithingRecipeInput(new ItemStack(ModItems.RESONANT_UPGRADE.get()), base, new ItemStack(ModItems.RESONANT_ALLOY_INGOT.get())));
        helper.assertValueEqual(xp(upgraded), 80, "smithing kept the XP");
        helper.assertTrue(!upgraded.get(ModComponents.TOOL_PROGRESS.get()).isGrandfather(), "smithing kept the progress record");
        helper.assertTrue(ToolXp.cap(upgraded) > ToolXp.cap(base), "the new tier has a higher point cap");

        var crafted = recipes.byKey(ResourceKey.create(Registries.RECIPE, ModItems.RESONANT_PICKAXE.getId())).orElseThrow();
        ItemStack shaped = ((ShapedRecipe) crafted.value()).assemble(CraftingInput.EMPTY);
        ToolXp.onCrafted(shaped);
        helper.assertValueEqual(xp(shaped), 0, "a fresh craft starts at 0 XP");
        helper.assertTrue(!shaped.get(ModComponents.TOOL_PROGRESS.get()).isGrandfather(), "a fresh craft is not grandfathered");
        shaped.set(ModComponents.TOOL_SETTINGS.get(), ToolSettings.area(true, 3));
        helper.assertTrue(!ToolSettings.of(shaped, ToolTier.RESONANT_ALLOY).areaMining(), "unbought area stays off");

        ItemStack old = new ItemStack(ModItems.RESONANT_PICKAXE.get());
        old.set(ModComponents.TOOL_SETTINGS.get(), ToolSettings.area(true, 3));
        helper.assertTrue(old.get(ModComponents.TOOL_PROGRESS.get()) == null, "an old stack has no XP component");
        helper.assertTrue(ToolSettings.of(old, ToolTier.RESONANT_ALLOY).areaMining(), "grandfathered area still works");
        ToolXp.award(old, 50);
        helper.assertTrue(old.get(ModComponents.TOOL_PROGRESS.get()).isGrandfather(), "earning XP does not lock an old stack");
        helper.assertTrue(ToolSettings.of(old, ToolTier.RESONANT_ALLOY).areaMining(), "the free unlock survived the migration");
        helper.succeed();
    }

    private static void use(ServerPlayer player, BlockPos pos) {
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false)));
    }

    private static int xp(ItemStack stack) {
        ToolProgress progress = stack.get(ModComponents.TOOL_PROGRESS.get());
        return progress == null ? 0 : progress.xp();
    }

    private static ServerPlayer player(GameTestHelper helper, ItemStack tool) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        player.setPos(origin.getX() + 8.5, origin.getY() + 5.0, origin.getZ() + 8.5);
        player.setYRot(0.0F);
        player.setXRot(90.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return player;
    }

    private static DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> function(String name, Consumer<GameTestHelper> test) {
        return FUNCTIONS.register(name, () -> test);
    }
}
