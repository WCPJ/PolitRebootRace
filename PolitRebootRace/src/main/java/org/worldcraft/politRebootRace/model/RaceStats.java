package org.worldcraft.politRebootRace.model;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Конфиг статов расы:
 * - heightScale — масштаб модели (рост)
 * - arbitrary attributes — любые атрибуты игрока как у команды /attribute
 */
public final class RaceStats {

    /** Масштаб высоты модели (1.0 = без изменений) */
    private final double heightScale;

    /** Изменения атрибутов игрока */
    private final Map<Attribute, AttributeChange> attributes;

    public RaceStats(double heightScale, Map<Attribute, AttributeChange> attributes) {
        this.heightScale = heightScale;
        // делаем копию, чтобы извне не могли мутировать наш внутренний map
        this.attributes = new HashMap<>(attributes);
    }

    public double heightScale() {
        return heightScale;
    }

    public Map<Attribute, AttributeChange> attributes() {
        return attributes;
    }

    /**
     * Применить все изменения атрибутов к игроку.
     * Высоту (heightScale) ты применяешь отдельно своим механизмом (через SCALE, NMS или свой рендер).
     */
    public void applyTo(Player player) {
        for (Map.Entry<Attribute, AttributeChange> entry : attributes.entrySet()) {
            Attribute attribute = entry.getKey();
            AttributeChange change = entry.getValue();

            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                // например, не у всех есть PLAYER_*-атрибуты
                continue;
            }

            double base = instance.getBaseValue();
            double result = change.apply(base);
            instance.setBaseValue(result);
        }
    }

    /**
     * Описание изменения одного атрибута:
     * - операция (SET / ADD / MULTIPLY)
     * - значение
     *
     * Имитация логики /attribute ... base set / add / умножение.
     */
    public static final class AttributeChange {

        public enum Operation {
            /** /attribute ... base set X */
            SET,
            /** /attribute ... base add X */
            ADD,
            /** условное умножение base * X */
            MULTIPLY
        }

        private final Operation operation;
        private final double value;

        public AttributeChange(Operation operation, double value) {
            this.operation = operation;
            this.value = value;
        }

        public Operation operation() {
            return operation;
        }

        public double value() {
            return value;
        }

        public double apply(double base) {
            return switch (operation) {
                case SET      -> value;
                case ADD      -> base + value;
                case MULTIPLY -> base * value;
            };
        }
    }

    /**
     * Удобный билдер, чтобы собирать статы расы в одном месте.
     */
    public static final class Builder {

        private double heightScale = 1.0;
        private final Map<Attribute, AttributeChange> attributes = new HashMap<>();

        public Builder heightScale(double scale) {
            this.heightScale = scale;
            return this;
        }

        public Builder set(Attribute attribute, double value) {
            attributes.put(attribute,
                    new AttributeChange(AttributeChange.Operation.SET, value));
            return this;
        }

        public Builder add(Attribute attribute, double value) {
            attributes.put(attribute,
                    new AttributeChange(AttributeChange.Operation.ADD, value));
            return this;
        }

        public Builder multiply(Attribute attribute, double factor) {
            attributes.put(attribute,
                    new AttributeChange(AttributeChange.Operation.MULTIPLY, factor));
            return this;
        }

        public RaceStats build() {
            return new RaceStats(heightScale, attributes);
        }
    }
}
