package com.tacticizm.adaptivetools.cable;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

/**
 * Which indicator texture a cable shows on its connectors. One value for the whole cable, because
 * a separate value on every face would be hundreds of thousands of blockstates.
 */
public enum CableAppearance implements StringRepresentable {
    OFF("off"),
    POWER("power"),
    ITEMS("items"),
    ALL("all");

    private final String name;

    CableAppearance(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static CableAppearance of(CableBlockEntity cable) {
        if (cable == null) {
            return OFF;
        }
        boolean power = false;
        boolean transport = false;
        for (Direction direction : Direction.values()) {
            FaceConfig face = cable.face(direction);
            if (face.powerModule) {
                power = true;
            }
            if (face.itemModule || face.fluidModule) {
                transport = true;
            }
        }
        if (power && transport) {
            return ALL;
        }
        if (power) {
            return POWER;
        }
        if (transport) {
            return ITEMS;
        }
        return OFF;
    }
}
