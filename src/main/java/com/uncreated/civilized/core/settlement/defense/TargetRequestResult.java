package com.uncreated.civilized.core.settlement.defense;

public enum TargetRequestResult {
    ACQUIRED_NEW_TARGET,
    REACQUIRED_SAME_TARGET,
    NO_TARGET;

    public boolean targetFound() {
        return this == ACQUIRED_NEW_TARGET || this == REACQUIRED_SAME_TARGET;
    }
}
