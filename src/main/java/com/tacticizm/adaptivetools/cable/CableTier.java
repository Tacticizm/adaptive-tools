package com.tacticizm.adaptivetools.cable;

/**
 * Cable throughput. FE is per tick. Items and millibuckets are per second, spread over 20 ticks.
 * A route cannot outrun the slowest cable on it. Rate modules raise only the item and fluid
 * budget of the cable they sit on, never the FE budget.
 *
 * <p>TODO(recipes): light cables are "craft 16 at the lowest tier", but the ingredients are still
 * unconfirmed, so there is no light-cable recipe. Medium and heavy recipes are registered.
 */
public enum CableTier {
    LIGHT("light_cable", "Light Cable", 1_000, 8, 1_000),
    MEDIUM("medium_cable", "Medium Cable", 8_000, 32, 4_000),
    HEAVY("heavy_cable", "Heavy Cable", 32_000, 128, 16_000);

    private final String id;
    private final String displayName;
    private final int fePerTick;
    private final int itemsPerSecond;
    private final int milliBucketsPerSecond;

    CableTier(String id, String displayName, int fePerTick, int itemsPerSecond, int milliBucketsPerSecond) {
        this.id = id;
        this.displayName = displayName;
        this.fePerTick = fePerTick;
        this.itemsPerSecond = itemsPerSecond;
        this.milliBucketsPerSecond = milliBucketsPerSecond;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public int fePerTick() {
        return fePerTick;
    }

    public int itemsPerSecond() {
        return itemsPerSecond;
    }

    public int milliBucketsPerSecond() {
        return milliBucketsPerSecond;
    }

    public int rank() {
        return ordinal();
    }
}
