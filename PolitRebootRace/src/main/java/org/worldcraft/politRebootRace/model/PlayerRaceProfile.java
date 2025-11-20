package org.worldcraft.politRebootRace.model;

import java.util.UUID;

public class PlayerRaceProfile {

    private final UUID uuid;
    private String raceId;

    // 0..20
    private int heat;          // Жар
    private int freeze;        // Заморозка
    private int water = 20;    // Вода (По умолчанию 20, чтобы амфибия не умирала сразу)
    private int endDistortion; // Энд-искажение

    public PlayerRaceProfile(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Сбрасывает все временные статусы выживания.
     * Вызывать ПЕРЕД сменой расы.
     */
    public void wipeProgress() {
        this.heat = 0;
        this.freeze = 0;
        this.water = 20; // Сбрасываем на максимум влажности
        this.endDistortion = 0;
    }

    public UUID uuid() {
        return uuid;
    }

    public String getRaceId() {
        return raceId;
    }

    public void setRaceId(String raceId) {
        this.raceId = raceId;
    }

    public boolean hasRace() {
        return raceId != null && !raceId.isEmpty();
    }

    // --- Жар ---
    public int getHeat() { return heat; }
    public void setHeat(int heat) { this.heat = clamp20(heat); }
    public void addHeat(int delta) { setHeat(this.heat + delta); }

    // --- Заморозка ---
    public int getFreeze() { return freeze; }
    public void setFreeze(int freeze) { this.freeze = clamp20(freeze); }
    public void addFreeze(int delta) { setFreeze(this.freeze + delta); }

    // --- Вода ---
    public int getWater() { return water; }
    public void setWater(int water) { this.water = clamp20(water); }
    public void addWater(int delta) { setWater(this.water + delta); }

    // --- Энд-искажение ---
    public int getEndDistortion() { return endDistortion; }
    public void setEndDistortion(int endDistortion) { this.endDistortion = clamp20(endDistortion); }
    public void addEndDistortion(int delta) { setEndDistortion(this.endDistortion + delta); }

    private int clamp20(int v) {
        if (v < 0) return 0;
        if (v > 20) return 20;
        return v;
    }
}