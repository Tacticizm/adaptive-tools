package com.tacticizm.adaptivetools;

/**
 * Starting action costs from the design spec. Area and multi-target actions bill once per block
 * (or per target). An ability-enhanced action costs {@link #ABILITY_FE} total, which is 125% of
 * {@link #BASE_FE}, not an extra 125 on top.
 *
 * <p>This slice has no learned abilities, so every pickaxe break — including each block of an area
 * — bills the base cost. {@link #ABILITY_FE} is here so the next slice does not invent a second number.
 */
public final class ActionCosts {
    public static final int BASE_FE = 100;
    public static final int ABILITY_FE = 125;
    public static final int BASE_DURABILITY = 1;

    private ActionCosts() {}
}
