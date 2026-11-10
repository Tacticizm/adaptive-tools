package com.tacticizm.adaptivetools;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@Mod(AdaptiveTools.MOD_ID)
public class AdaptiveTools {
    public static final String MOD_ID = "adaptivetools";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AdaptiveTools(IEventBus modEventBus) {
        ModComponents.COMPONENTS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModItems.TABS.register(modEventBus);
        PickaxeGameTests.FUNCTIONS.register(modEventBus);

        modEventBus.addListener(ModEnergy::registerCapabilities);
        modEventBus.addListener(ModNetworking::register);
        modEventBus.addListener(AdaptiveToolsData::gatherClient);
        modEventBus.addListener(AdaptiveToolsData::gatherServer);
        modEventBus.addListener(PickaxeGameTests::registerTests);

        NeoForge.EVENT_BUS.addListener(ToolBreakEvents::onBreak);
        NeoForge.EVENT_BUS.addListener(AdaptiveTools::onTooltip);

        LOGGER.info("Adaptive Tools loading");
    }

    private static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
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
}
