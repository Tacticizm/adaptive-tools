package com.tacticizm.adaptivetools;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Per-stack tool settings. Area mining is off until the player turns it on. The stored size is the
 * odd side length the player picked; the tier cap is applied whenever the value is read or saved.
 */
public record ToolSettings(boolean areaMining, int areaSize) {
    public static final Codec<ToolSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("area_mining").forGetter(ToolSettings::areaMining),
            Codec.INT.fieldOf("area_size").forGetter(ToolSettings::areaSize)
    ).apply(instance, ToolSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToolSettings> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ToolSettings::areaMining,
            ByteBufCodecs.VAR_INT, ToolSettings::areaSize,
            ToolSettings::new);

    public static int clampSize(int requested, int cap) {
        int size = Math.max(1, Math.min(requested, Math.max(1, cap)));
        if ((size & 1) == 0) {
            size--;
        }
        return Math.max(1, size);
    }

    public static ToolSettings of(ItemStack stack, ToolTier tier) {
        ToolSettings stored = stack.get(ModComponents.TOOL_SETTINGS.get());
        if (stored == null) {
            return new ToolSettings(false, tier.maxAreaSide());
        }
        return new ToolSettings(stored.areaMining, clampSize(stored.areaSize, tier.maxAreaSide()));
    }

    /** Side length actually mined. Area mining off, or a 1×1 cap, mines only the targeted block. */
    public int effectiveSide(ToolTier tier) {
        if (!areaMining) {
            return 1;
        }
        return clampSize(areaSize, tier.maxAreaSide());
    }

    public ToolSettings clamped(ToolTier tier) {
        return new ToolSettings(areaMining, clampSize(areaSize, tier.maxAreaSide()));
    }
}
