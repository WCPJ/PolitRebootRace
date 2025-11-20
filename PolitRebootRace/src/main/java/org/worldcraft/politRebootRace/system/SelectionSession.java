package org.worldcraft.politRebootRace.system;

import org.worldcraft.politRebootRace.model.RaceDefinition;
import org.worldcraft.politRebootRace.model.RaceDimension;

import java.util.List;

public class SelectionSession {

    public enum Stage {
        NONE,
        DIMENSION,
        RACE,
        CONFIRM
    }

    private Stage stage = Stage.NONE;
    private RaceDimension dimension;
    private RaceDefinition race;
    private List<String> raceOrder; // id рас по слотам GUI выбора расы

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public RaceDimension getDimension() {
        return dimension;
    }

    public void setDimension(RaceDimension dimension) {
        this.dimension = dimension;
    }

    public RaceDefinition getRace() {
        return race;
    }

    public void setRace(RaceDefinition race) {
        this.race = race;
    }

    public List<String> getRaceOrder() {
        return raceOrder;
    }

    public void setRaceOrder(List<String> raceOrder) {
        this.raceOrder = raceOrder;
    }
}
