package org.worldcraft.politRebootRace.model;

import java.util.List;

public record RaceDefinition(
        String id,
        String displayName,
        RaceDimension dimension,
        RaceStats stats,
        List<RaceFeature> features
) {}
