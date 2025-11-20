package org.worldcraft.politRebootRace.system;

import org.bukkit.attribute.Attribute;
import org.worldcraft.politRebootRace.model.*;

import javax.smartcardio.ATR;
import java.util.*;

public class RaceRegistry {

    private final Map<String, RaceDefinition> races = new HashMap<>();

    public void register(RaceDefinition def) {
        races.put(def.id().toLowerCase(), def);
    }

    public RaceDefinition get(String id) {
        return races.get(id.toLowerCase());
    }

    public List<RaceDefinition> getByDimension(RaceDimension dim) {
        return races.values().stream()
                .filter(r -> r.dimension() == dim)
                .sorted(Comparator.comparing(RaceDefinition::id))
                .toList();
    }

    public void registerDefaultRaces() {
        // ===== ЗЕМЛЯ =====

        // 1) Человек — "как обычно"
        RaceStats humanStats = new RaceStats.Builder()
                .heightScale(1.0)
                // можно задать явно, а можно и не трогать, если полагаешься на ванильные значения
                .set(Attribute.MAX_HEALTH, 20.0)
                .set(Attribute.MOVEMENT_SPEED, 0.10)
                .set(Attribute.ATTACK_DAMAGE, 2.0)
                .set(Attribute.ENTITY_INTERACTION_RANGE, 3.0)
                .set(Attribute.BLOCK_INTERACTION_RANGE, 3.0)
                .build();

        register(new RaceDefinition(
                "human", "Человек", RaceDimension.EARTH,
                humanStats,
                List.of()
        ));

        // 2) Огр
        // 37 хп
        // Размер 1.9
        // Скорость на 25% меньше стандартной (0.10 -> 0.075) — через multiply(0.75)
        // Атака +4 к базовой (2 + 4 = 6) — через add(+4)
        // Чуть больше дистанции атаки и установки блоков -> 3.6
        RaceStats ogreStats = new RaceStats.Builder()
                .heightScale(2.0)
                .set(Attribute.MAX_HEALTH, 37.0)
                .add(Attribute.ATTACK_DAMAGE, 4.0)
                .multiply(Attribute.MOVEMENT_SPEED, 0.75)
                .set(Attribute.ENTITY_INTERACTION_RANGE, 4.0)
                .set(Attribute.BLOCK_INTERACTION_RANGE, 4.0)
                .set(Attribute.KNOCKBACK_RESISTANCE, 1.0)
                .set(Attribute.ATTACK_KNOCKBACK, 2)
                .set(Attribute.ARMOR_TOUGHNESS, 2)
                .build();

        register(new RaceDefinition(
                "ogre", "Огр", RaceDimension.EARTH,
                ogreStats,
                List.of(RaceFeature.LOVES_HEAT)
        ));

        // 3) Амфибия
        // 25 хп, чуть ниже рост, немного выше урон
        RaceStats amphibianStats = new RaceStats.Builder()
                .heightScale(0.95)
                .set(Attribute.MAX_HEALTH, 25.0)
                .set(Attribute.MOVEMENT_SPEED, 0.10)
                .set(Attribute.ATTACK_DAMAGE, 2.5)
                .set(Attribute.ENTITY_INTERACTION_RANGE, 3.0)
                .set(Attribute.BLOCK_INTERACTION_RANGE, 3.0)
                .set(Attribute.WATER_MOVEMENT_EFFICIENCY, 5.0)
                .set(Attribute.OXYGEN_BONUS, 999)
                .set(Attribute.BURNING_TIME, 0.5)
                .build();

        register(new RaceDefinition(
                "amphibian", "Амфибия", RaceDimension.EARTH,
                amphibianStats,
                List.of(RaceFeature.WATER_BREATHING)
        ));

        // ===== АД =====

        RaceStats piglinStats = new RaceStats.Builder()
                .heightScale(1.0)
                .set(Attribute.MAX_HEALTH, 22.0)
                .set(Attribute.MOVEMENT_SPEED, 0.150)
                .set(Attribute.ATTACK_DAMAGE, 3.0)
                .set(Attribute.ENTITY_INTERACTION_RANGE, 3.0)
                .set(Attribute.BLOCK_INTERACTION_RANGE, 3.0)
                .set(Attribute.BURNING_TIME, 0.5)

                .build();

        register(new RaceDefinition(
                "piglin", "Пиглин", RaceDimension.NETHER,
                piglinStats,
                List.of(RaceFeature.LOVES_HEAT)
        ));

        RaceStats blazebornStats = new RaceStats.Builder()
                .heightScale(1.05)
                .set(Attribute.MAX_HEALTH, 24.0)
                .set(Attribute.MOVEMENT_SPEED, 0.10)
                .set(Attribute.ATTACK_DAMAGE, 3.5)
                .set(Attribute.ENTITY_INTERACTION_RANGE, 3.0)
                .set(Attribute.BLOCK_INTERACTION_RANGE, 3.0)
                .set(Attribute.BURNING_TIME, 0)
                .set(Attribute.EXPLOSION_KNOCKBACK_RESISTANCE, 0)
                .build();

        register(new RaceDefinition(
                "blazeborn", "Блейзорождённый", RaceDimension.NETHER,
                blazebornStats,
                List.of(RaceFeature.FIRE_IMMUNE)
        ));

        RaceStats witherbornStats = new RaceStats.Builder()
                .heightScale(1.2)
                .set(Attribute.MAX_HEALTH, 28.0)
                .set(Attribute.MOVEMENT_SPEED, 0.095)
                .set(Attribute.ATTACK_DAMAGE, 4.0)
                .set(Attribute.ENTITY_INTERACTION_RANGE, 3.2)
                .set(Attribute.BLOCK_INTERACTION_RANGE, 3.2)
                .set(Attribute.BURNING_TIME, 0)

                .build();

        register(new RaceDefinition(
                "witherborn", "Визеро-рождённый", RaceDimension.NETHER,
                witherbornStats,
                List.of()
        ));

        // ===== ЭНД =====

        RaceStats endermanStats = new RaceStats.Builder()
                .heightScale(1.4)
                .set(Attribute.MAX_HEALTH, 40.0)
                .set(Attribute.MOVEMENT_SPEED, 0.123)
                .set(Attribute.ATTACK_DAMAGE, 4.0)
                .set(Attribute.ENTITY_INTERACTION_RANGE, 4.0)
                .set(Attribute.BLOCK_INTERACTION_RANGE, 4.0)

                .build();

        register(new RaceDefinition(
                "enderman", "Эндермен", RaceDimension.END,
                endermanStats,
                List.of(RaceFeature.HATES_WATER)
        ));

        RaceStats dragonbornStats = new RaceStats.Builder()
                .heightScale(1.15)
                .set(Attribute.MAX_HEALTH, 30.0)
                .set(Attribute.MOVEMENT_SPEED, 0.10)
                .set(Attribute.ATTACK_DAMAGE, 4.5)
                .set(Attribute.ENTITY_INTERACTION_RANGE, 3.2)
                .set(Attribute.BLOCK_INTERACTION_RANGE, 3.2)
                .build();

        register(new RaceDefinition(
                "dragonborn", "Драконорожденный", RaceDimension.END,
                dragonbornStats,
                List.of()
        ));

        RaceStats chorusianStats = new RaceStats.Builder()
                .heightScale(0.5)
                .set(Attribute.MAX_HEALTH, 10.0)
                .set(Attribute.MOVEMENT_SPEED, 0.15)
                .set(Attribute.ATTACK_DAMAGE, 1.0)
                .set(Attribute.ENTITY_INTERACTION_RANGE, 2.0)
                .set(Attribute.BLOCK_INTERACTION_RANGE, 2.0)
                .set(Attribute.SAFE_FALL_DISTANCE, 99999)
                .set(Attribute.OXYGEN_BONUS, 10)
                .set(Attribute.LUCK, 2)
                .build();

        register(new RaceDefinition(
                "chorusian", "Хорусианин", RaceDimension.END,
                chorusianStats,
                List.of()
        ));
    }
}
