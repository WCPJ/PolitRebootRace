package org.worldcraft.politRebootRace.system;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.worldcraft.politRebootRace.model.*;

import java.util.*;

public class EvolutionRegistry {

    private final Map<String, List<EvolutionLine>> lines = new HashMap<>();

    public EvolutionRegistry() {
        registerDefaults();
    }

    public List<EvolutionLine> getLinesForRace(String raceId) {
        return lines.getOrDefault(raceId.toLowerCase(), List.of());
    }

    private void registerDefaults() {
        registerHuman();
        registerOgre();
        registerAmphibian();
        registerPiglin();
        registerBlazeborn();
        registerWitherborn();
        registerEnderman();
        registerDragonborn();
        registerChorusian();
    }

    private void registerHuman() {
        List<EvolutionStep> adaptable = List.of(
                new EvolutionStep(
                        "human_scout",
                        "Разведчик",
                        List.of("+Скорость передвижения", "+Скорость копания"),
                        List.of(
                                new EvolutionRequirement(EvolutionRequirementType.MOB_KILL, 15, null, null, "Убейте 15 мобов"),
                                new EvolutionRequirement(EvolutionRequirementType.CRAFT_ITEM, 1, null, Material.MAP, "Создайте карту")),
                        8,
                        Map.of(Material.LEATHER, 8),
                        List.of(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, true, false, false),
                                new PotionEffect(PotionEffectType.FAST_DIGGING, Integer.MAX_VALUE, 0, true, false, false))
                ),
                new EvolutionStep(
                        "human_veteran",
                        "Ветеран",
                        List.of("Ускоренная регенерация", "Больше опыта с убийств"),
                        List.of(new EvolutionRequirement(EvolutionRequirementType.PLAYER_KILL, 5, null, null, "Победите 5 игроков")),
                        12,
                        Map.of(Material.GOLDEN_APPLE, 1),
                        List.of(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0, true, false, false))
                )
        );

        register("human", List.of(new EvolutionLine("human_growth", "Адаптация", "Лёгкий путь для гибких людей", Material.COMPASS, adaptable)));
    }

    private void registerOgre() {
        List<EvolutionStep> berserk = List.of(
                new EvolutionStep(
                        "ogre_berserk_1",
                        "Грубиян",
                        List.of("+Сила", "Ломайте 20 блоков камня"),
                        List.of(new EvolutionRequirement(EvolutionRequirementType.MOB_KILL, 20, null, null, "Убейте 20 мобов")),
                        10,
                        Map.of(Material.IRON_AXE, 1),
                        List.of(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 0, true, false, false))
                ),
                new EvolutionStep(
                        "ogre_berserk_2",
                        "Берсерк",
                        List.of("Сопротивление отбрасыванию", "Сильнее атака"),
                        List.of(new EvolutionRequirement(EvolutionRequirementType.PLAYER_KILL, 3, null, null, "Убейте 3 игроков")),
                        18,
                        Map.of(Material.NETHERITE_AXE, 1),
                        List.of(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 0, true, false, false))
                )
        );

        List<EvolutionStep> guardian = List.of(
                new EvolutionStep(
                        "ogre_guard_1",
                        "Глыба",
                        List.of("Больше здоровья", "Урон от падения снижен"),
                        List.of(new EvolutionRequirement(EvolutionRequirementType.MOB_KILL, 10, EntityType.ZOMBIE, null, "Убейте 10 зомби")),
                        10,
                        Map.of(Material.SHIELD, 1),
                        List.of(new PotionEffect(PotionEffectType.HEALTH_BOOST, Integer.MAX_VALUE, 1, true, false, false))
                ),
                new EvolutionStep(
                        "ogre_guard_2",
                        "Живой бастион",
                        List.of("Сопротивление огню", "Сопротивление урону"),
                        List.of(new EvolutionRequirement(EvolutionRequirementType.SPECIFIC_MOB_KILL, 1, EntityType.IRON_GOLEM, null, "Победите голема")),
                        16,
                        Map.of(Material.OBSIDIAN, 12),
                        List.of(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false, false),
                                new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 1, true, false, false))
                )
        );

        register("ogre", List.of(
                new EvolutionLine("ogre_berserk", "Берсерк", "Путь безрассудной силы", Material.ANVIL, berserk),
                new EvolutionLine("ogre_guard", "Защитник", "Тяжёлая броня и стойкость", Material.SHIELD, guardian)
        ));
    }

    private void registerAmphibian() {
        List<EvolutionStep> tide = List.of(
                new EvolutionStep(
                        "amp_wave",
                        "Волна",
                        List.of("+Скорость в воде", "Лучшее дыхание"),
                        List.of(new EvolutionRequirement(EvolutionRequirementType.SPECIFIC_MOB_KILL, 5, EntityType.DROWNED, null, "Победите 5 утопленников")),
                        8,
                        Map.of(Material.PRISMARINE_CRYSTALS, 12),
                        List.of(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, Integer.MAX_VALUE, 0, true, false, false),
                                new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0, true, false, false))
                ),
                new EvolutionStep(
                        "amp_depth",
                        "Глубина",
                        List.of("Подводное зрение", "Урон в воде выше"),
                        List.of(new EvolutionRequirement(EvolutionRequirementType.MOB_KILL, 15, null, null, "Убейте 15 морских мобов")),
                        12,
                        Map.of(Material.HEART_OF_THE_SEA, 1),
                        List.of(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false, false))
                )
        );
        register("amphibian", List.of(new EvolutionLine("amp_tide", "Глашатай глубин", "Укрепляет силу в воде", Material.TRIDENT, tide)));
    }

    private void registerPiglin() {
        List<EvolutionStep> trader = List.of(
                new EvolutionStep(
                        "piglin_broker",
                        "Брокер",
                        List.of("Скидки у пиглинов"),
                        List.of(new EvolutionRequirement(EvolutionRequirementType.CRAFT_ITEM, 5, null, Material.GOLD_INGOT, "Переплавьте золото")),
                        6,
                        Map.of(Material.GOLD_BLOCK, 2),
                        List.of(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, Integer.MAX_VALUE, 0, true, false, false))
                ),
                new EvolutionStep(
                        "piglin_magnate",
                        "Магнат",
                        List.of("Больше урона золотым оружием"),
                        List.of(new EvolutionRequirement(EvolutionRequirementType.PLAYER_KILL, 2, null, null, "Убейте 2 игроков, держа золото")),
                        14,
                        Map.of(Material.GOLDEN_CARROT, 8),
                        List.of(new PotionEffect(PotionEffectType.LUCK, Integer.MAX_VALUE, 1, true, false, false))
                )
        );
        register("piglin", List.of(new EvolutionLine("piglin_gold", "Золотой путь", "Экономика и выгода", Material.GOLD_BLOCK, trader)));
    }

    private void registerBlazeborn() {
        List<EvolutionStep> flame = List.of(
                new EvolutionStep(
                        "blaze_spark",
                        "Искра",
                        List.of("Быстрое передвижение в лаве"),
                        List.of(new EvolutionRequirement(EvolutionRequirementType.MOB_KILL, 20, null, null, "Убейте 20 адских мобов")),
                        10,
                        Map.of(Material.MAGMA_BLOCK, 8),
                        List.of(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false, false),
                                new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, true, false, false))
                ),
                new EvolutionStep(
                        "blaze_core",
                        "Пирокинетик",
                        List.of("Урон от огня повышен"),
                        List.of(new EvolutionRequirement(EvolutionRequirementType.SPECIFIC_MOB_KILL, 3, EntityType.BLAZE, null, "Убейте 3 ифрита")),
                        14,
                        Map.of(Material.BLAZE_ROD, 6),
                        List.of(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 0, true, false, false))
                )
        );
        register("blazeborn", List.of(new EvolutionLine("blaze_fire", "Пиромант", "Управление пламенем", Material.BLAZE_ROD, flame)));
    }

    private void registerWitherborn() {
        List<EvolutionStep> decay = List.of(
                new EvolutionStep(
                        "wither_spores",
                        "Споры",
                        List.of("Касание иссушения"),
                        List.of(new EvolutionRequirement(EvolutionRequirementType.MOB_KILL, 15, null, null, "Убейте 15 мобов в аду")),
                        10,
                        Map.of(Material.WITHER_ROSE, 2),
                        List.of(new PotionEffect(PotionEffectType.WITHER, Integer.MAX_VALUE, 0, true, false, false))
                ),
                new EvolutionStep(
                        "wither_avatar",
                        "Аватар", 
                        List.of("Иммунитет к взрывам"),
                        List.of(new EvolutionRequirement(EvolutionRequirementType.SPECIFIC_MOB_KILL, 1, EntityType.WITHER_SKELETON, null, "Убейте визер-скелета мечом")),
                        16,
                        Map.of(Material.NETHER_STAR, 1),
                        List.of(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 1, true, false, false))
                )
        );
        register("witherborn", List.of(new EvolutionLine("wither_decay", "Угасание", "Сила иссушения", Material.WITHER_ROSE, decay)));
    }

    private void registerEnderman() {
        List<EvolutionStep> voidWalk = List.of(
                new EvolutionStep(
                        "ender_shadow",
                        "Тень",
                        List.of("Тише при ходьбе"),
                        List.of(new EvolutionRequirement(EvolutionRequirementType.MOB_KILL, 25, null, null, "Убейте 25 мобов")),
                        10,
                        Map.of(Material.ENDER_PEARL, 8),
                        List.of(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 60 * 5, 0, true, false, false))
                ),
                new EvolutionStep(
                        "ender_scout",
                        "Ступень пустоты",
                        List.of("+Шанс телепортации"),
                        List.of(new EvolutionRequirement(EvolutionRequirementType.SPECIFIC_MOB_KILL, 5, EntityType.ENDERMITE, null, "Убейте 5 эндермитов")),
                        14,
                        Map.of(Material.CHORUS_FRUIT, 6),
                        List.of(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, true, false, false))
                )
        );
        register("enderman", List.of(new EvolutionLine("end_shadow", "Пустотник", "Укрепление связи с Эндом", Material.ENDER_PEARL, voidWalk)));
    }

    private void registerDragonborn() {
        List<EvolutionStep> fury = List.of(
                new EvolutionStep(
                        "dragon_roar",
                        "Рёв",
                        List.of("Сила дракона"),
                        List.of(new EvolutionRequirement(EvolutionRequirementType.PLAYER_KILL, 2, null, null, "Победите 2 игроков")),
                        12,
                        Map.of(Material.DRAGON_BREATH, 2),
                        List.of(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 1, true, false, false))
                ),
                new EvolutionStep(
                        "dragon_wings",
                        "Крылья",
                        List.of("Укреплённые элитры"),
                        List.of(new EvolutionRequirement(EvolutionRequirementType.SPECIFIC_MOB_KILL, 1, EntityType.ENDER_DRAGON, null, "Убейте дракона")),
                        24,
                        Map.of(Material.ELYTRA, 1),
                        List.of(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 0, true, false, false))
                )
        );
        register("dragonborn", List.of(new EvolutionLine("dragon_fury", "Ярость", "Путь наследников дракона", Material.DRAGON_BREATH, fury)));
    }

    private void registerChorusian() {
        List<EvolutionStep> trickster = List.of(
                new EvolutionStep(
                        "chorus_trick",
                        "Фокусник",
                        List.of("Случайные телепорты"),
                        List.of(new EvolutionRequirement(EvolutionRequirementType.CRAFT_ITEM, 3, null, Material.CHORUS_FRUIT, "Приготовьте 3 жареных хора")),
                        6,
                        Map.of(Material.CHORUS_FRUIT, 6),
                        List.of(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 2, true, false, false))
                ),
                new EvolutionStep(
                        "chorus_wanderer",
                        "Странник",
                        List.of("Плавное падение"),
                        List.of(new EvolutionRequirement(EvolutionRequirementType.MOB_KILL, 12, null, null, "Победите 12 мобов")),
                        10,
                        Map.of(Material.SHULKER_SHELL, 2),
                        List.of(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 0, true, false, false))
                )
        );
        register("chorusian", List.of(new EvolutionLine("chorus_travel", "Прыгун", "Лёгкость и удача", Material.CHORUS_FRUIT, trickster)));
    }

    private void register(String raceId, List<EvolutionLine> raceLines) {
        lines.put(raceId.toLowerCase(), raceLines);
    }
}
