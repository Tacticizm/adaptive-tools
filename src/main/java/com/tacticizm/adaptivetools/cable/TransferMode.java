package com.tacticizm.adaptivetools.cable;

/** Item and fluid modules stay disabled until the player picks Send or Receive. */
public enum TransferMode {
    DISABLED,
    SEND,
    RECEIVE;

    public static TransferMode byOrdinal(int ordinal) {
        TransferMode[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return DISABLED;
        }
        return values[ordinal];
    }
}
