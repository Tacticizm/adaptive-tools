package com.tacticizm.adaptivetools;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.tacticizm.adaptivetools.cable.CableBlock;
import com.tacticizm.adaptivetools.cable.CableBlockEntity;
import com.tacticizm.adaptivetools.cable.CableMenu;
import com.tacticizm.adaptivetools.cable.CableTier;
import com.tacticizm.adaptivetools.testing.TestBufferBlock;
import com.tacticizm.adaptivetools.testing.TestBufferBlockEntity;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AdaptiveTools.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AdaptiveTools.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, AdaptiveTools.MOD_ID);

    public static final DeferredBlock<CableBlock> LIGHT_CABLE = cable(CableTier.LIGHT);
    public static final DeferredBlock<CableBlock> MEDIUM_CABLE = cable(CableTier.MEDIUM);
    public static final DeferredBlock<CableBlock> HEAVY_CABLE = cable(CableTier.HEAVY);
    public static final DeferredBlock<TestBufferBlock> TEST_BUFFER = BLOCKS.registerBlock("test_buffer", TestBufferBlock::new, props -> props.strength(0.5F).noLootTable());

    public static final DeferredItem<BlockItem> LIGHT_CABLE_ITEM = ModItems.ITEMS.registerSimpleBlockItem(LIGHT_CABLE);
    public static final DeferredItem<BlockItem> MEDIUM_CABLE_ITEM = ModItems.ITEMS.registerSimpleBlockItem(MEDIUM_CABLE);
    public static final DeferredItem<BlockItem> HEAVY_CABLE_ITEM = ModItems.ITEMS.registerSimpleBlockItem(HEAVY_CABLE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CableBlockEntity>> CABLE_BE = BLOCK_ENTITIES.register("cable",
            () -> new BlockEntityType<>(CableBlockEntity::new, LIGHT_CABLE.get(), MEDIUM_CABLE.get(), HEAVY_CABLE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TestBufferBlockEntity>> TEST_BUFFER_BE = BLOCK_ENTITIES.register("test_buffer",
            () -> new BlockEntityType<>(TestBufferBlockEntity::new, TEST_BUFFER.get()));

    public static final DeferredHolder<MenuType<?>, MenuType<CableMenu>> CABLE_MENU = MENUS.register("cable", () -> IMenuTypeExtension.create((id, inventory, buf) -> {
        var pos = buf.readBlockPos();
        var face = buf.readEnum(net.minecraft.core.Direction.class);
        var blockEntity = inventory.player.level().getBlockEntity(pos);
        CableBlockEntity cable = blockEntity instanceof CableBlockEntity found ? found : null;
        return new CableMenu(id, inventory, cable, face);
    }));

    private ModBlocks() {}

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, TEST_BUFFER_BE.get(), (be, side) -> be.energy);
        event.registerBlockEntity(Capabilities.Item.BLOCK, TEST_BUFFER_BE.get(), (be, side) -> be.items);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, TEST_BUFFER_BE.get(), (be, side) -> be.fluids);
    }

    private static DeferredBlock<CableBlock> cable(CableTier tier) {
        return BLOCKS.registerBlock(tier.id(), props -> new CableBlock(tier, props), cableProperties());
    }

    private static java.util.function.UnaryOperator<BlockBehaviour.Properties> cableProperties() {
        return props -> props.strength(0.4F).sound(SoundType.METAL).noOcclusion().noLootTable();
    }
}
