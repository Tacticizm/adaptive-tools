package com.tacticizm.adaptivetools.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.tacticizm.adaptivetools.AdaptiveTools;
import com.tacticizm.adaptivetools.ModBlocks;

@Mod(value = AdaptiveTools.MOD_ID, dist = Dist.CLIENT)
public class AdaptiveToolsClient {
    public AdaptiveToolsClient(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(AreaOutlineRenderer::onExtract);
        modEventBus.addListener(AdaptiveToolsClient::registerScreens);
        AdaptiveTools.LOGGER.info("Adaptive Tools client ready");
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModBlocks.CABLE_MENU.get(), CableScreen::new);
    }
}
