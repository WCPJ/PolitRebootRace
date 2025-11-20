package org.worldcraft.politRebootRace.model;

import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;

public class EvolutionStep {

    private final String id;
    private final String name;
    private final List<String> bonuses;
    private final List<EvolutionRequirement> requirements;
    private final int experienceCost;
    private final Map<Material, Integer> resourceCosts;
    private final List<PotionEffect> rewards;

    public EvolutionStep(String id,
                         String name,
                         List<String> bonuses,
                         List<EvolutionRequirement> requirements,
                         int experienceCost,
                         Map<Material, Integer> resourceCosts,
                         List<PotionEffect> rewards) {
        this.id = id;
        this.name = name;
        this.bonuses = bonuses;
        this.requirements = requirements;
        this.experienceCost = experienceCost;
        this.resourceCosts = resourceCosts;
        this.rewards = rewards;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getBonuses() {
        return bonuses;
    }

    public List<EvolutionRequirement> getRequirements() {
        return requirements;
    }

    public int getExperienceCost() {
        return experienceCost;
    }

    public Map<Material, Integer> getResourceCosts() {
        return resourceCosts;
    }

    public List<PotionEffect> getRewards() {
        return rewards;
    }
}
