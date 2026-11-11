package com.tacticizm.adaptivetools;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Per-stack tool settings. Abilities start off. Sizes are clamped to the tier whenever they are
 * read. Tool XP is not built yet, so a tier that cannot learn an ability has that toggle forced off.
 *
 * <p>Toggles live in one bit field so the codec stays small enough for existing stacks to load.
 */
public record ToolSettings(
        int flags,
        int areaSize,
        int trenchLength,
        int gradeHeight,
        int fellLimit,
        int cleaveTargets,
        int cleaveRange,
        int chainTargets,
        int chainRange) {

    private static final int AREA = 1;
    private static final int TRENCH = 1 << 1;
    private static final int GRADING = 1 << 2;
    private static final int FELL = 1 << 3;
    private static final int LEAVES = 1 << 4;
    private static final int CHARCOAL = 1 << 5;
    private static final int TILL = 1 << 6;
    private static final int HARVEST = 1 << 7;
    private static final int REPLANT = 1 << 8;
    private static final int CLEAVE = 1 << 9;
    private static final int CHAIN = 1 << 10;
    private static final int MOMENTUM = 1 << 11;
    private static final int GUARD = 1 << 12;
    private static final int SIPHON = 1 << 13;
    private static final int PASSIVES = 1 << 14;
    private static final int PLAYERS = 1 << 15;

    private static final Codec<ToolSettings> COMPACT = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("flags").forGetter(ToolSettings::flags),
            Codec.INT.fieldOf("area_size").forGetter(ToolSettings::areaSize),
            Codec.INT.fieldOf("trench_length").forGetter(ToolSettings::trenchLength),
            Codec.INT.fieldOf("grade_height").forGetter(ToolSettings::gradeHeight),
            Codec.INT.fieldOf("fell_limit").forGetter(ToolSettings::fellLimit),
            Codec.INT.fieldOf("cleave_targets").forGetter(ToolSettings::cleaveTargets),
            Codec.INT.fieldOf("cleave_range").forGetter(ToolSettings::cleaveRange),
            Codec.INT.fieldOf("chain_targets").forGetter(ToolSettings::chainTargets),
            Codec.INT.fieldOf("chain_range").forGetter(ToolSettings::chainRange)
    ).apply(instance, ToolSettings::new));

    private static final Codec<ToolSettings> LEGACY = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("area_mining").forGetter(ToolSettings::areaMining),
            Codec.INT.fieldOf("area_size").forGetter(ToolSettings::areaSize)
    ).apply(instance, ToolSettings::area));

    public static final Codec<ToolSettings> CODEC = Codec.either(COMPACT, LEGACY).xmap(
            either -> either.map(settings -> settings, settings -> settings),
            settings -> com.mojang.datafixers.util.Either.left(settings));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToolSettings> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ToolSettings::flags,
            ByteBufCodecs.VAR_INT, ToolSettings::areaSize,
            ByteBufCodecs.VAR_INT, ToolSettings::trenchLength,
            ByteBufCodecs.VAR_INT, ToolSettings::gradeHeight,
            ByteBufCodecs.VAR_INT, ToolSettings::fellLimit,
            ByteBufCodecs.VAR_INT, ToolSettings::cleaveTargets,
            ByteBufCodecs.VAR_INT, ToolSettings::cleaveRange,
            ByteBufCodecs.VAR_INT, ToolSettings::chainTargets,
            ByteBufCodecs.VAR_INT, ToolSettings::chainRange,
            ToolSettings::new);

    public static ToolSettings area(boolean areaMining, int areaSize) {
        return new ToolSettings(areaMining ? AREA : 0, areaSize, 1, Integer.MIN_VALUE, 1, 3, 3, 4, 4);
    }

    public static ToolSettings defaults(ToolTier tier) {
        ToolSettings base = area(false, tier.maxAreaSide());
        return new ToolSettings(
                base.flags(),
                base.areaSize(),
                Math.max(1, tier.maxTrench()),
                base.gradeHeight(),
                Math.max(1, tier.maxFellLogs()),
                Math.max(1, tier.maxCleaveTargets()),
                Math.max(1, tier.maxCleaveRange()),
                Math.max(1, tier.maxChainTargets()),
                Math.max(1, tier.maxChainRange())).clamped(tier);
    }

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
            return defaults(tier);
        }
        return stored.clamped(tier);
    }

    public boolean areaMining() { return has(AREA); }
    public boolean trench() { return has(TRENCH); }
    public boolean grading() { return has(GRADING); }
    public boolean fell() { return has(FELL); }
    public boolean leafClear() { return has(LEAVES); }
    public boolean charcoal() { return has(CHARCOAL); }
    public boolean till() { return has(TILL); }
    public boolean harvest() { return has(HARVEST); }
    public boolean replant() { return has(REPLANT); }
    public boolean cleave() { return has(CLEAVE); }
    public boolean chain() { return has(CHAIN); }
    public boolean momentum() { return has(MOMENTUM); }
    public boolean guardBreak() { return has(GUARD); }
    public boolean siphon() { return has(SIPHON); }
    public boolean targetPassives() { return has(PASSIVES); }
    public boolean targetPlayers() { return has(PLAYERS); }

    public int effectiveSide(ToolTier tier) {
        return areaMining() ? clampSize(areaSize, tier.maxAreaSide()) : 1;
    }

    /** Chosen trench length, even while the trench toggle is off. */
    public int configuredTrench(ToolTier tier) {
        int cap = Math.max(1, tier.maxTrench());
        return Math.max(1, Math.min(trenchLength <= 0 ? cap : trenchLength, cap));
    }

    /** Chosen log limit, even while felling is off. Zero when the tier cannot fell. */
    public int configuredFell(ToolTier tier) {
        if (tier.maxFellLogs() <= 0) {
            return 0;
        }
        return Math.max(1, Math.min(fellLimit <= 0 ? tier.maxFellLogs() : fellLimit, tier.maxFellLogs()));
    }

    public int effectiveTrench(ToolTier tier) {
        if (!trench() || !tier.canTrench()) {
            return 1;
        }
        return configuredTrench(tier);
    }

    public int effectiveFell(ToolTier tier) {
        if (!fell() || tier.maxFellLogs() <= 0) {
            return 1;
        }
        return configuredFell(tier);
    }

    public ToolSettings clamped(ToolTier tier) {
        int next = flags;
        if (!tier.canTrench()) next &= ~TRENCH;
        if (!tier.canGrade()) next &= ~GRADING;
        if (tier.maxFellLogs() <= 0) next &= ~(FELL | LEAVES | CHARCOAL);
        if (!tier.canCharcoal()) next &= ~CHARCOAL;
        if (!tier.canCleave()) next &= ~CLEAVE;
        if (!tier.canChain()) next &= ~CHAIN;
        if (!tier.canMomentum()) next &= ~MOMENTUM;
        if (!tier.canGuardBreak()) next &= ~GUARD;
        if (!tier.canSiphon()) next &= ~SIPHON;
        return new ToolSettings(
                next,
                clampSize(areaSize, tier.maxAreaSide()),
                Math.max(1, Math.min(trenchLength <= 0 ? tier.maxTrench() : trenchLength, tier.maxTrench())),
                gradeHeight,
                Math.max(1, Math.min(fellLimit <= 0 ? Math.max(1, tier.maxFellLogs()) : fellLimit, Math.max(1, tier.maxFellLogs()))),
                Math.max(1, Math.min(cleaveTargets <= 0 ? Math.max(1, tier.maxCleaveTargets()) : cleaveTargets, Math.max(1, tier.maxCleaveTargets()))),
                Math.max(1, Math.min(cleaveRange <= 0 ? Math.max(1, tier.maxCleaveRange()) : cleaveRange, Math.max(1, tier.maxCleaveRange()))),
                Math.max(1, Math.min(chainTargets <= 0 ? Math.max(1, tier.maxChainTargets()) : chainTargets, Math.max(1, tier.maxChainTargets()))),
                Math.max(1, Math.min(chainRange <= 0 ? Math.max(1, tier.maxChainRange()) : chainRange, Math.max(1, tier.maxChainRange()))));
    }

    public ToolSettings withArea(boolean on, int size) {
        return copy(set(AREA, on), size, trenchLength, gradeHeight, fellLimit, cleaveTargets, cleaveRange, chainTargets, chainRange);
    }

    public ToolSettings withTrench(boolean on, int length) {
        return copy(set(TRENCH, on), areaSize, length, gradeHeight, fellLimit, cleaveTargets, cleaveRange, chainTargets, chainRange);
    }

    public ToolSettings withGrading(boolean on, int height) {
        return copy(set(GRADING, on), areaSize, trenchLength, height, fellLimit, cleaveTargets, cleaveRange, chainTargets, chainRange);
    }

    public ToolSettings withFell(boolean on, int limit, boolean leaves, boolean burn) {
        int next = set(FELL, on);
        next = (leaves ? next | LEAVES : next & ~LEAVES);
        next = (burn ? next | CHARCOAL : next & ~CHARCOAL);
        return copy(next, areaSize, trenchLength, gradeHeight, limit, cleaveTargets, cleaveRange, chainTargets, chainRange);
    }

    public ToolSettings withHoe(boolean tillOn, boolean harvestOn, boolean replantOn) {
        int next = set(TILL, tillOn);
        next = harvestOn ? next | HARVEST : next & ~HARVEST;
        next = replantOn ? next | REPLANT : next & ~REPLANT;
        return copy(next, areaSize, trenchLength, gradeHeight, fellLimit, cleaveTargets, cleaveRange, chainTargets, chainRange);
    }

    public ToolSettings withSword(boolean cleaveOn, int cleaveCount, int cleaveBlocks, boolean chainOn, int chainCount, int chainBlocks,
            boolean momentumOn, boolean guardOn, boolean siphonOn, boolean passives, boolean players) {
        int next = set(CLEAVE, cleaveOn);
        next = chainOn ? next | CHAIN : next & ~CHAIN;
        next = momentumOn ? next | MOMENTUM : next & ~MOMENTUM;
        next = guardOn ? next | GUARD : next & ~GUARD;
        next = siphonOn ? next | SIPHON : next & ~SIPHON;
        next = passives ? next | PASSIVES : next & ~PASSIVES;
        next = players ? next | PLAYERS : next & ~PLAYERS;
        return copy(next, areaSize, trenchLength, gradeHeight, fellLimit, cleaveCount, cleaveBlocks, chainCount, chainBlocks);
    }

    private ToolSettings copy(int nextFlags, int area, int trench, int height, int fell, int cleaveCount, int cleaveBlocks, int chainCount, int chainBlocks) {
        return new ToolSettings(nextFlags, area, trench, height, fell, cleaveCount, cleaveBlocks, chainCount, chainBlocks);
    }

    private boolean has(int bit) {
        return (flags & bit) != 0;
    }

    private int set(int bit, boolean on) {
        return on ? flags | bit : flags & ~bit;
    }

}
