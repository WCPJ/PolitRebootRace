package org.worldcraft.politRebootRace.model;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public class EvolutionRequirement {

    private final EvolutionRequirementType type;
    private final int amount;
    private final EntityType entityTarget;
    private final Material materialTarget;
    private final String description;

    public EvolutionRequirement(EvolutionRequirementType type,
                                int amount,
                                EntityType entityTarget,
                                Material materialTarget,
                                String description) {
        this.type = type;
        this.amount = amount;
        this.entityTarget = entityTarget;
        this.materialTarget = materialTarget;
        this.description = description;
    }

    public EvolutionRequirementType getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public EntityType getEntityTarget() {
        return entityTarget;
    }

    public Material getMaterialTarget() {
        return materialTarget;
    }

    public String getDescription() {
        return description;
    }
}
