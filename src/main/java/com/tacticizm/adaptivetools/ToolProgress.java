package com.tacticizm.adaptivetools;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Tool XP and the abilities bought with it. Missing on an old stack means the stack is
 * grandfathered: every ability its tier already allowed stays unlocked.
 */
public record ToolProgress(
        int xp,
        int efficiency,
        int efficiencyOn,
        int fortune,
        int fortuneOn,
        int silk,
        int silkOn,
        int area,
        int trench,
        int fell,
        int unlocks,
        int cleave,
        int chain,
        int remainder,
        int grandfather) {

    public static final Codec<ToolProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("xp").forGetter(ToolProgress::xp),
            Codec.INT.fieldOf("efficiency").forGetter(ToolProgress::efficiency),
            Codec.INT.fieldOf("efficiency_on").forGetter(ToolProgress::efficiencyOn),
            Codec.INT.fieldOf("fortune").forGetter(ToolProgress::fortune),
            Codec.INT.fieldOf("fortune_on").forGetter(ToolProgress::fortuneOn),
            Codec.INT.fieldOf("silk").forGetter(ToolProgress::silk),
            Codec.INT.fieldOf("silk_on").forGetter(ToolProgress::silkOn),
            Codec.INT.fieldOf("area").forGetter(ToolProgress::area),
            Codec.INT.fieldOf("trench").forGetter(ToolProgress::trench),
            Codec.INT.fieldOf("fell").forGetter(ToolProgress::fell),
            Codec.INT.fieldOf("unlocks").forGetter(ToolProgress::unlocks),
            Codec.INT.fieldOf("cleave").forGetter(ToolProgress::cleave),
            Codec.INT.fieldOf("chain").forGetter(ToolProgress::chain),
            Codec.INT.fieldOf("remainder").forGetter(ToolProgress::remainder),
            Codec.INT.fieldOf("grandfather").forGetter(ToolProgress::grandfather)
    ).apply(instance, ToolProgress::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToolProgress> STREAM_CODEC = StreamCodec.of(
            (buf, progress) -> {
                buf.writeVarInt(progress.xp);
                buf.writeVarInt(progress.efficiency);
                buf.writeVarInt(progress.efficiencyOn);
                buf.writeVarInt(progress.fortune);
                buf.writeVarInt(progress.fortuneOn);
                buf.writeVarInt(progress.silk);
                buf.writeVarInt(progress.silkOn);
                buf.writeVarInt(progress.area);
                buf.writeVarInt(progress.trench);
                buf.writeVarInt(progress.fell);
                buf.writeVarInt(progress.unlocks);
                buf.writeVarInt(progress.cleave);
                buf.writeVarInt(progress.chain);
                buf.writeVarInt(progress.remainder);
                buf.writeVarInt(progress.grandfather);
            },
            buf -> new ToolProgress(
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));

    public static ToolProgress fresh() {
        return new ToolProgress(0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0);
    }

    public static ToolProgress grandfathered() {
        return fresh().withGrandfather(1);
    }

    public boolean isGrandfather() {
        return grandfather != 0;
    }

    public ToolProgress withXp(int value) {
        return new ToolProgress(value, efficiency, efficiencyOn, fortune, fortuneOn, silk, silkOn, area, trench, fell, unlocks, cleave, chain, remainder, grandfather);
    }

    public ToolProgress withRemainder(int value) {
        return new ToolProgress(xp, efficiency, efficiencyOn, fortune, fortuneOn, silk, silkOn, area, trench, fell, unlocks, cleave, chain, value, grandfather);
    }

    public ToolProgress withGrandfather(int value) {
        return new ToolProgress(xp, efficiency, efficiencyOn, fortune, fortuneOn, silk, silkOn, area, trench, fell, unlocks, cleave, chain, remainder, value);
    }

    public ToolProgress cleared() {
        return new ToolProgress(xp, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, remainder, grandfather);
    }
}
