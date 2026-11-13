package com.tacticizm.adaptivetools;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
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
import com.tacticizm.adaptivetools.wireless.FieldGeneratorMenu;
import com.tacticizm.adaptivetools.wireless.GeneratorBlock;
import com.tacticizm.adaptivetools.wireless.GeneratorBlockEntity;
import com.tacticizm.adaptivetools.wireless.GeneratorMenu;
import com.tacticizm.adaptivetools.wireless.ReceiverBlock;
import com.tacticizm.adaptivetools.wireless.ReceiverBlockEntity;
import com.tacticizm.adaptivetools.wireless.ReceiverMenu;
import com.tacticizm.adaptivetools.wireless.TransmitterBlock;
import com.tacticizm.adaptivetools.wireless.TransmitterBlockEntity;
import com.tacticizm.adaptivetools.wireless.TransmitterMenu;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AdaptiveTools.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AdaptiveTools.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, AdaptiveTools.MOD_ID);

    public static final DeferredBlock<CableBlock> LIGHT_CABLE = cable(CableTier.LIGHT);
    public static final DeferredBlock<CableBlock> MEDIUM_CABLE = cable(CableTier.MEDIUM);
    public static final DeferredBlock<CableBlock> HEAVY_CABLE = cable(CableTier.HEAVY);
    public static final DeferredBlock<TestBufferBlock> TEST_BUFFER = BLOCKS.registerBlock("test_buffer", TestBufferBlock::new, props -> props.strength(0.5F).noLootTable());
    private static final Map<ToolTier, DeferredBlock<Block>> STORAGE = new EnumMap<>(ToolTier.class);
    private static final Map<ToolTier, DeferredItem<BlockItem>> STORAGE_ITEMS = new EnumMap<>(ToolTier.class);

    static {
        for (ToolTier tier : ToolTier.values()) {
            DeferredBlock<Block> block = BLOCKS.registerBlock(tier.id() + "_block", Block::new, props -> props.strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
            STORAGE.put(tier, block);
            STORAGE_ITEMS.put(tier, ModItems.ITEMS.registerSimpleBlockItem(block));
        }
    }

    public static final DeferredItem<BlockItem> LIGHT_CABLE_ITEM = ModItems.ITEMS.registerSimpleBlockItem(LIGHT_CABLE);
    public static final DeferredItem<BlockItem> MEDIUM_CABLE_ITEM = ModItems.ITEMS.registerSimpleBlockItem(MEDIUM_CABLE);
    public static final DeferredItem<BlockItem> HEAVY_CABLE_ITEM = ModItems.ITEMS.registerSimpleBlockItem(HEAVY_CABLE);

    public static final DeferredBlock<GeneratorBlock> GENERATOR = BLOCKS.registerBlock("base_generator", GeneratorBlock::new, props -> props.strength(3.5F).sound(SoundType.METAL).noLootTable());
    public static final DeferredBlock<TransmitterBlock> TRANSMITTER = BLOCKS.registerBlock("wireless_energy_transmitter", TransmitterBlock::new, props -> props.strength(3.5F).sound(SoundType.METAL).noLootTable());
    public static final DeferredBlock<ReceiverBlock> RECEIVER = BLOCKS.registerBlock("machine_receiver", ReceiverBlock::new, props -> props.strength(3.5F).sound(SoundType.METAL).noLootTable());
    public static final DeferredItem<BlockItem> GENERATOR_ITEM = ModItems.ITEMS.registerSimpleBlockItem(GENERATOR);
    public static final DeferredItem<BlockItem> TRANSMITTER_ITEM = ModItems.ITEMS.registerSimpleBlockItem(TRANSMITTER);
    public static final DeferredItem<BlockItem> RECEIVER_ITEM = ModItems.ITEMS.registerSimpleBlockItem(RECEIVER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CableBlockEntity>> CABLE_BE = BLOCK_ENTITIES.register("cable",
            () -> new BlockEntityType<>(CableBlockEntity::new, LIGHT_CABLE.get(), MEDIUM_CABLE.get(), HEAVY_CABLE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TestBufferBlockEntity>> TEST_BUFFER_BE = BLOCK_ENTITIES.register("test_buffer",
            () -> new BlockEntityType<>(TestBufferBlockEntity::new, TEST_BUFFER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeneratorBlockEntity>> GENERATOR_BE = BLOCK_ENTITIES.register("base_generator",
            () -> new BlockEntityType<>(GeneratorBlockEntity::new, GENERATOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TransmitterBlockEntity>> TRANSMITTER_BE = BLOCK_ENTITIES.register("wireless_energy_transmitter",
            () -> new BlockEntityType<>(TransmitterBlockEntity::new, TRANSMITTER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReceiverBlockEntity>> RECEIVER_BE = BLOCK_ENTITIES.register("machine_receiver",
            () -> new BlockEntityType<>(ReceiverBlockEntity::new, RECEIVER.get()));

    public static final DeferredHolder<MenuType<?>, MenuType<CableMenu>> CABLE_MENU = MENUS.register("cable", () -> IMenuTypeExtension.create((id, inventory, buf) -> {
        var pos = buf.readBlockPos();
        var face = buf.readEnum(net.minecraft.core.Direction.class);
        var blockEntity = inventory.player.level().getBlockEntity(pos);
        CableBlockEntity cable = blockEntity instanceof CableBlockEntity found ? found : null;
        return new CableMenu(id, inventory, cable, face);
    }));
    public static final DeferredHolder<MenuType<?>, MenuType<GeneratorMenu>> GENERATOR_MENU = MENUS.register("base_generator", () -> IMenuTypeExtension.create((id, inventory, buf) -> {
        var pos = buf.readBlockPos();
        var blockEntity = inventory.player.level().getBlockEntity(pos);
        return new GeneratorMenu(id, inventory, blockEntity instanceof GeneratorBlockEntity found ? found : null);
    }));
    public static final DeferredHolder<MenuType<?>, MenuType<TransmitterMenu>> TRANSMITTER_MENU = MENUS.register("wireless_energy_transmitter", () -> IMenuTypeExtension.create((id, inventory, buf) -> {
        var pos = buf.readBlockPos();
        var blockEntity = inventory.player.level().getBlockEntity(pos);
        return new TransmitterMenu(id, inventory, blockEntity instanceof TransmitterBlockEntity found ? found : null);
    }));
    public static final DeferredHolder<MenuType<?>, MenuType<ReceiverMenu>> RECEIVER_MENU = MENUS.register("machine_receiver", () -> IMenuTypeExtension.create((id, inventory, buf) -> {
        var pos = buf.readBlockPos();
        var blockEntity = inventory.player.level().getBlockEntity(pos);
        return new ReceiverMenu(id, inventory, blockEntity instanceof ReceiverBlockEntity found ? found : null);
    }));
    public static final DeferredHolder<MenuType<?>, MenuType<FieldGeneratorMenu>> FIELD_MENU = MENUS.register("field_generator", () -> IMenuTypeExtension.create((id, inventory, buf) -> new FieldGeneratorMenu(id, inventory, buf.readVarInt())));

    private ModBlocks() {}

    public static DeferredBlock<Block> storage(ToolTier tier) {
        return STORAGE.get(tier);
    }

    public static DeferredItem<BlockItem> storageItem(ToolTier tier) {
        return STORAGE_ITEMS.get(tier);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, TEST_BUFFER_BE.get(), (be, side) -> be.energy);
        event.registerBlockEntity(Capabilities.Item.BLOCK, TEST_BUFFER_BE.get(), (be, side) -> be.items);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, TEST_BUFFER_BE.get(), (be, side) -> be.fluids);
        event.registerBlockEntity(Capabilities.Energy.BLOCK, GENERATOR_BE.get(), (be, side) -> be.energyHandler);
        event.registerBlockEntity(Capabilities.Energy.BLOCK, TRANSMITTER_BE.get(), (be, side) -> be.energyHandler);
    }

    private static DeferredBlock<CableBlock> cable(CableTier tier) {
        return BLOCKS.registerBlock(tier.id(), props -> new CableBlock(tier, props), cableProperties());
    }

    private static java.util.function.UnaryOperator<BlockBehaviour.Properties> cableProperties() {
        return props -> props.strength(0.4F).sound(SoundType.METAL).noOcclusion().noLootTable();
    }
}
