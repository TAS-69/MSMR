package dev.msmr;

import java.util.UUID;

/** Persisted with the physical storage block, including while no player has it open. */
public interface ChestData {
    ChestProfile msmr$profile();
    UUID msmr$owner();
    long msmr$version();
    void msmr$update(ChestProfile profile,UUID owner);
}
