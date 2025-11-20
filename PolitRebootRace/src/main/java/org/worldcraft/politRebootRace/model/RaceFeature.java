package org.worldcraft.politRebootRace.model;

public enum RaceFeature {
    LOVES_HEAT("Теплолюбивость", "Получает баффы от жара и огня"),
    HATES_WATER("Гидрофобия", "Получает урон от воды"),
    WATER_BREATHING("Жабры", "Может дышать под водой"),
    FIRE_IMMUNE("Огнестойкость", "Полный иммунитет к огню и лаве"),
    NIGHT_VISION("Ночное зрение", "Видит в темноте постоянно");

    private final String displayName;
    private final String description;

    RaceFeature(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}