package com.tacticizm.adaptivetools;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import com.tacticizm.adaptivetools.cable.CableNetworks;

@Mod(AdaptiveTools.MOD_ID)
public class AdaptiveTools {
    public static final String MOD_ID = "adaptivetools";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AdaptiveTools(IEventBus modEventBus) {
        ModComponents.COMPONENTS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.BLOCK_ENTITIES.register(modEventBus);
        ModBlocks.MENUS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModItems.TABS.register(modEventBus);
        PickaxeGameTests.FUNCTIONS.register(modEventBus);
        CableGameTests.FUNCTIONS.register(modEventBus);
        ToolGameTests.FUNCTIONS.register(modEventBus);
        ArmorGameTests.FUNCTIONS.register(modEventBus);
        com.tacticizm.adaptivetools.wireless.WirelessGameTests.FUNCTIONS.register(modEventBus);
        ProgressionGameTests.FUNCTIONS.register(modEventBus);

        modEventBus.addListener(ModEnergy::registerCapabilities);
        modEventBus.addListener(ModBlocks::registerCapabilities);
        modEventBus.addListener(ModNetworking::register);
        modEventBus.addListener(com.tacticizm.adaptivetools.wireless.WirelessMessages::register);
        modEventBus.addListener(AdaptiveToolsData::gatherClient);
        modEventBus.addListener(AdaptiveToolsData::gatherServer);
        modEventBus.addListener(PickaxeGameTests::registerTests);
        modEventBus.addListener(CableGameTests::registerTests);
        modEventBus.addListener(ToolGameTests::registerTests);
        modEventBus.addListener(ArmorGameTests::registerTests);
        modEventBus.addListener(com.tacticizm.adaptivetools.wireless.WirelessGameTests::registerTests);
        modEventBus.addListener(ProgressionGameTests::registerTests);

        NeoForge.EVENT_BUS.addListener(ToolBreakEvents::onBreak);
        NeoForge.EVENT_BUS.addListener(ToolDropEvents::onDrops);
        NeoForge.EVENT_BUS.addListener(com.tacticizm.adaptivetools.wireless.MachineBlock::onDrops);
        NeoForge.EVENT_BUS.addListener(com.tacticizm.adaptivetools.wireless.FieldGenerators::onTick);
        NeoForge.EVENT_BUS.addListener(SwordCombat::onSweep);
        NeoForge.EVENT_BUS.addListener(SwordCombat::onIncoming);
        NeoForge.EVENT_BUS.addListener(SwordCombat::onDamaged);
        NeoForge.EVENT_BUS.addListener(SwordCombat::onDeath);
        NeoForge.EVENT_BUS.addListener(SwordCombat::onShield);
        NeoForge.EVENT_BUS.addListener(ArmorAbilities::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ArmorAbilities::onDamage);
        NeoForge.EVENT_BUS.addListener(LavaRepair::onTick);
        NeoForge.EVENT_BUS.addListener(AdaptiveTools::onServerTick);
        NeoForge.EVENT_BUS.addListener(AdaptiveTools::onTooltip);
        NeoForge.EVENT_BUS.addListener(AdaptiveTools::onLevelTick);

        LOGGER.info("Adaptive Tools loading");
    }

    private static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof AdaptiveArmorItem armor && armor.tier().usesEnergy()) {
            event.getToolTip().add(Component.translatable("adaptivetools.tooltip.energy", ModEnergy.getStored(stack), armor.tier().energyCapacity()));
        }
        if (!(stack.getItem() instanceof AdaptivePickaxeItem pick)) {
            return;
        }
        ToolSettings settings = ToolSettings.of(stack, pick.tier());
        if (pick.tier().maxAreaSide() > 1) {
            event.getToolTip().add(Component.translatable(
                    settings.areaMining() ? "adaptivetools.tooltip.area_on" : "adaptivetools.tooltip.area_off",
                    settings.areaSize(),
                    settings.areaSize()));
        }
        if (pick.tier().usesEnergy()) {
            event.getToolTip().add(Component.translatable("adaptivetools.tooltip.energy", ModEnergy.getStored(stack), pick.tier().energyCapacity()));
        }
    }

    private static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        com.tacticizm.adaptivetools.wireless.WirelessNetwork.tick(event.getServer());
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel server) {
            CableNetworks.tick(server);
        }
    }
}
