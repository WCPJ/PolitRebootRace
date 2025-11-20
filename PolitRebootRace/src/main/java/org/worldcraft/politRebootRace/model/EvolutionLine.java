package org.worldcraft.politRebootRace.model;

import org.bukkit.Material;

import java.util.List;

public class EvolutionLine {
    private final String id;
    private final String displayName;
    private final String description;
    private final Material icon;
    private final List<EvolutionStep> steps;

    public EvolutionLine(String id, String displayName, String description, Material icon, List<EvolutionStep> steps) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.steps = steps;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Material getIcon() {
        return icon;
    }

    public List<EvolutionStep> getSteps() {
        return steps;
    }
}
