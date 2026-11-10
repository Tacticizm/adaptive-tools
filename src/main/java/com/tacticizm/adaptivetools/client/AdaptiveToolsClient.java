package com.tacticizm.adaptivetools.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

import com.tacticizm.adaptivetools.AdaptiveTools;

@Mod(value = AdaptiveTools.MOD_ID, dist = Dist.CLIENT)
public class AdaptiveToolsClient {
    public AdaptiveToolsClient(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(AreaOutlineRenderer::onExtract);
        AdaptiveTools.LOGGER.info("Adaptive Tools client ready");
    }
}
