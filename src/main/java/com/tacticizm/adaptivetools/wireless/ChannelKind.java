package com.tacticizm.adaptivetools.wireless;

public enum ChannelKind {
    POWER,
    ITEM,
    FLUID;

    public static ChannelKind byOrdinal(int ordinal) {
        ChannelKind[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }
}
