package org.worldcraft.politRebootRace.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerRaceProfile {

    private final UUID uuid;
    private String raceId;

    // --- Прогресс эволюции ---
    private String evolutionLineId;
    private int evolutionStep;
    private final Map<String, Integer> requirementProgress = new HashMap<>();

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
        resetEvolution();
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

    // --- Эволюция ---
    public String getEvolutionLineId() {
        return evolutionLineId;
    }

    public void setEvolutionLineId(String evolutionLineId) {
        this.evolutionLineId = evolutionLineId;
    }

    public int getEvolutionStep() {
        return evolutionStep;
    }

    public void setEvolutionStep(int evolutionStep) {
        this.evolutionStep = Math.max(0, evolutionStep);
    }

    public Map<String, Integer> getRequirementProgress() {
        return requirementProgress;
    }

    public void incrementRequirement(String key, int delta) {
        requirementProgress.put(key, requirementProgress.getOrDefault(key, 0) + delta);
    }

    public void resetEvolution() {
        this.evolutionLineId = null;
        this.evolutionStep = 0;
        this.requirementProgress.clear();
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