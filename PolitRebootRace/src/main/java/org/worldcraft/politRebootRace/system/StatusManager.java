package org.worldcraft.politRebootRace.system;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.worldcraft.politRebootRace.model.*;

import java.util.*;

public class StatusManager implements Listener, Runnable {

    private final Plugin plugin;
    private final PlayerRaceStorage storage;
    private final RaceRegistry registry;

    // таймер раз в 10 секунд для энд-искажения
    private final Map<UUID, Integer> endTickTimer = new HashMap<>();
    // детерминированное накопление жара
    private final Map<UUID, Double> heatAccumulator = new HashMap<>();

    private final Random random = new Random();

    // BossBar'ы на игрока
    private static class PlayerBars {
        BossBar heat;
        BossBar freeze;
        BossBar humidity;
        BossBar end;
    }

    private final Map<UUID, PlayerBars> bossBars = new HashMap<>();

    // максимум для прогресса (используем для процентов и стадий)
    private static final int MAX_STAT_VALUE = 20;

    // инфа по стадии опасности
    private static class StageInfo {
        final int stage;
        final ChatColor numeralColor;
        final BarColor barColor;

        StageInfo(int stage, ChatColor numeralColor, BarColor barColor) {
            this.stage = stage;
            this.numeralColor = numeralColor;
            this.barColor = barColor;
        }
    }

    public StatusManager(Plugin plugin, PlayerRaceStorage storage, RaceRegistry registry) {
        this.plugin = plugin;
        this.storage = storage;
        this.registry = registry;
    }

    // основной тик – вызывается раз в секунду
    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            tickPlayer(player);
        }
    }

    private void tickPlayer(Player player) {
        PlayerRaceProfile profile = storage.get(player.getUniqueId());
        if (profile == null || !profile.hasRace()) {
            hideAllBars(player);
            return;
        }

        RaceDefinition race = registry.get(profile.getRaceId());
        if (race == null) {
            hideAllBars(player);
            return;
        }

        boolean isNetherRace = race.dimension() == RaceDimension.NETHER;
        boolean isEndRace = race.dimension() == RaceDimension.END;
        boolean isAmphibian = "amphibian".equalsIgnoreCase(race.id());

        // обновляем статы
        updateHeatAndFreeze(player, profile, isNetherRace, isAmphibian);
        updateHumidity(player, profile, isAmphibian);
        updateEndDistortion(player, profile, race);

        // накладываем эффекты
        applyStatusEffects(player, profile, isNetherRace, isAmphibian);

        // обновляем боссбары — видимость только по значениям статов
        updateBossBars(player, profile, race, isAmphibian);
    }

    // --- события жизни, чтобы таймеры были аккуратными ---

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // специально ничего не делаем, бары создаются при первом тике
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        // при респауне просто на следующем тике всё пересчитается
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        endTickTimer.remove(id);
        heatAccumulator.remove(id);

        PlayerBars bars = bossBars.remove(id);
        if (bars != null) {
            bars.heat.removeAll();
            bars.freeze.removeAll();
            bars.humidity.removeAll();
            bars.end.removeAll();
        }
    }

    // ----------------- BOSS BAR УТИЛИТЫ -----------------

    private PlayerBars getBars(Player p) {
        UUID id = p.getUniqueId();
        PlayerBars bars = bossBars.get(id);
        if (bars != null) return bars;

        bars = new PlayerBars();

        bars.heat = Bukkit.createBossBar(ChatColor.RED + "Жар", BarColor.WHITE, BarStyle.SEGMENTED_20);
        bars.freeze = Bukkit.createBossBar(ChatColor.AQUA + "Заморозка", BarColor.WHITE, BarStyle.SEGMENTED_20);
        bars.humidity = Bukkit.createBossBar(ChatColor.BLUE + "Влажность тела", BarColor.WHITE, BarStyle.SEGMENTED_20);
        bars.end = Bukkit.createBossBar(ChatColor.DARK_PURPLE + "Искажение энда", BarColor.WHITE, BarStyle.SEGMENTED_20);

        for (BossBar bar : Arrays.asList(bars.heat, bars.freeze, bars.humidity, bars.end)) {
            bar.setVisible(false);
            bar.setProgress(0.0);
            bar.addPlayer(p);
        }

        bossBars.put(id, bars);
        return bars;
    }

    private void hideAllBars(Player p) {
        PlayerBars bars = bossBars.get(p.getUniqueId());
        if (bars == null) return;

        bars.heat.setVisible(false);
        bars.freeze.setVisible(false);
        bars.humidity.setVisible(false);
        bars.end.setVisible(false);
    }

    private double progressFromValue(int value) {
        if (value <= 0) return 0.0;
        if (value >= MAX_STAT_VALUE) return 1.0;
        return value / (double) MAX_STAT_VALUE;
    }

    private StageInfo computeStage(double dangerPercent) {
        // dangerPercent: 0..100, где 0 = безопасно, 100 = почти смерть
        if (dangerPercent <= 25.0) {
            return new StageInfo(1, ChatColor.WHITE, BarColor.WHITE);
        } else if (dangerPercent <= 50.0) {
            return new StageInfo(2, ChatColor.GREEN, BarColor.GREEN);
        } else if (dangerPercent <= 90.0) {
            return new StageInfo(3, ChatColor.GOLD, BarColor.YELLOW);
        } else {
            return new StageInfo(4, ChatColor.RED, BarColor.RED);
        }
    }

    private String romanStage(int stage) {
        return switch (stage) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> "";
        };
    }

    private void updateBossBars(Player player, PlayerRaceProfile profile,
                                RaceDefinition race, boolean isAmphibian) {

        PlayerBars bars = getBars(player);

        int heat = profile.getHeat();
        int freeze = profile.getFreeze();
        int humidity = profile.getWater();
        int end = profile.getEndDistortion();

        boolean isEndRace = race.dimension() == RaceDimension.END;

        // ЖАР (чем выше %, тем опаснее)
        if (heat > 0) {
            double progress = progressFromValue(heat);
            double percent = progress * 100.0;
            StageInfo st = computeStage(percent);
            int pct = (int) Math.round(percent);

            bars.heat.setColor(st.barColor);
            bars.heat.setTitle(ChatColor.RED + "Жар: " + pct + "% " + st.numeralColor + romanStage(st.stage));
            bars.heat.setProgress(progress);
            bars.heat.setVisible(true);
        } else {
            bars.heat.setVisible(false);
        }

        // ЗАМОРОЗКА (чем выше %, тем опаснее)
        if (freeze > 0) {
            double progress = progressFromValue(freeze);
            double percent = progress * 100.0;
            StageInfo st = computeStage(percent);
            int pct = (int) Math.round(percent);

            bars.freeze.setColor(st.barColor);
            bars.freeze.setTitle(ChatColor.AQUA + "Заморозка: " + pct + "% " + st.numeralColor + romanStage(st.stage));
            bars.freeze.setProgress(progress);
            bars.freeze.setVisible(true);
        } else {
            bars.freeze.setVisible(false);
        }

        // ВЛАЖНОСТЬ ТЕЛА (с жаждой наоборот):
        //  - высокий % влажности = безопасно
        //  - опасность растёт при снижении % влажности
        if (isAmphibian) {
            double progress = progressFromValue(humidity);
            double percent = progress * 100.0;
            if (percent < 0) percent = 0;
            if (percent > 100) percent = 100;

            double dryness = 100.0 - percent; // "опасность жажды"
            StageInfo st = computeStage(dryness);
            int pct = (int) Math.round(percent);

            bars.humidity.setColor(st.barColor);
            bars.humidity.setTitle(ChatColor.BLUE + "Влажность тела: " + pct + "% " + st.numeralColor + romanStage(st.stage));
            bars.humidity.setProgress(progress);
            bars.humidity.setVisible(true);
        } else {
            bars.humidity.setVisible(false);
        }

        // ИСКАЖЕНИЕ ЭНДА (чем выше %, тем хуже, энд-расы не видят)
        if (!isEndRace && end > 0) {
            double progress = progressFromValue(end);
            double percent = progress * 100.0;
            StageInfo st = computeStage(percent);
            int pct = (int) Math.round(percent);

            bars.end.setColor(st.barColor);
            bars.end.setTitle(ChatColor.DARK_PURPLE + "Искажение энда: " + pct + "% " + st.numeralColor + romanStage(st.stage));
            bars.end.setProgress(progress);
            bars.end.setVisible(true);
        } else {
            bars.end.setVisible(false);
        }
    }

    // ----------------- HEAT + FREEZE -----------------

    private boolean updateHeatAndFreeze(Player p,
                                        PlayerRaceProfile profile,
                                        boolean isNetherRace,
                                        boolean isAmphibian) {
        int oldHeat = profile.getHeat();
        int oldFreeze = profile.getFreeze();

        World.Environment env = p.getWorld().getEnvironment();
        boolean nearStrongHeat = isNearBlock(p, 3, Set.of(
                Material.LAVA, Material.MAGMA_BLOCK
        ));
        boolean nearWeakHeat = isNearBlock(p, 3, Set.of(
                Material.FIRE, Material.SOUL_FIRE,
                Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
                Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER
        ));
        boolean coldGround = isOnColdBlock(p);

        UUID id = p.getUniqueId();

        // ---- ЖАР: детерминированное накопление через аккумулятор ----
        double acc = heatAccumulator.getOrDefault(id, 0.0);
        double rate = 0.0; // изменение жара в "пунктах в секунду"

        int currentHeat = profile.getHeat();

        if (env == World.Environment.NETHER) {
            if (!isNetherRace) {
                // обычные расы в аду
                if (nearStrongHeat) {
                    rate = 1.0 / 30.0;   // ~1 пункт / 30 сек
                } else if (nearWeakHeat) {
                    rate = 1.0 / 60.0;   // ~1 пункт / 1 мин
                } else {
                    rate = 1.0 / 120.0;  // ~1 пункт / 2 мин просто за нахождение в аду
                }
            } else {
                // адские расы – лёгкий комфортный жар до 10
                if (currentHeat < 10) {
                    if (nearStrongHeat) {
                        rate = 1.0 / 60.0;   // ~1 пункт / 1 мин
                    } else if (nearWeakHeat) {
                        rate = 1.0 / 120.0;  // ~1 пункт / 2 мин
                    } else {
                        rate = 1.0 / 180.0;  // ~1 пункт / 3 мин
                    }
                }
            }
        } else {
            // вне ада
            if (nearStrongHeat) {
                // немного греемся у сильного источника
                rate = 1.0 / 180.0; // ~1 пункт / 3 мин
            } else if (currentHeat > 0) {
                // постепенно остываем
                rate = -1.0 / 90.0; // ~-1 пункт / 1.5 мин
            }
        }

        // прямая зависимость: пока заморозка > 0, жар не РАСТЁТ
        if (profile.getFreeze() > 0 && rate > 0) {
            rate = 0.0;
        }

        // обновляем аккумулятор жара
        acc += rate;
        int deltaHeat = 0;
        if (acc >= 1.0) {
            deltaHeat = (int) acc;
            acc -= deltaHeat;
        } else if (acc <= -1.0) {
            deltaHeat = (int) acc;
            acc -= deltaHeat;
        }

        if (deltaHeat != 0) {
            profile.addHeat(deltaHeat);
            if (profile.getHeat() < 0) profile.setHeat(0);
            if (profile.getHeat() > MAX_STAT_VALUE) profile.setHeat(MAX_STAT_VALUE);
        }

        if (Math.abs(acc) < 1e-6) {
            heatAccumulator.remove(id);
        } else {
            heatAccumulator.put(id, acc);
        }

        // ---- ЗАМОРОЗКА ----
        //  - в воде не мёрзнем (проверяется в applyStatusEffects через влажность, но тут уменьшаем рост)
        //  - в Энде не мёрзнем
        //  - пока жар > 0, заморозка не растёт

        if (!nearStrongHeat && !nearWeakHeat) {
            boolean shouldFreeze = false;
            int baseChance = 90; // ~1.5 мин/пункт

            if (env != World.Environment.NETHER && env != World.Environment.THE_END) {
                if (coldGround) {
                    shouldFreeze = true;
                    baseChance = 60; // ~1 мин/пункт
                }
                if (isNetherRace && env != World.Environment.NETHER) {
                    // адская раса вне ада мёрзнет быстрее
                    shouldFreeze = true;
                    baseChance = Math.min(baseChance, 45);
                }
                if (isAmphibian && coldGround) {
                    baseChance = 30; // амфибия на холодных блоках мёрзнет максимально быстро
                }
            }

            if (shouldFreeze && profile.getHeat() == 0) {
                if (random.nextInt(baseChance) == 0) {
                    profile.addFreeze(1);
                    if (profile.getFreeze() > MAX_STAT_VALUE) profile.setFreeze(MAX_STAT_VALUE);
                }
            }
        }

        // отогреваемся при сильном тепле или в аду
        if (profile.getFreeze() > 0 && (nearStrongHeat || env == World.Environment.NETHER)) {
            int chance = 60; // немного быстрее
            if (random.nextInt(chance) == 0) {
                profile.addFreeze(-1);
                if (profile.getFreeze() < 0) profile.setFreeze(0);
            }
        }

        return oldHeat != profile.getHeat() || oldFreeze != profile.getFreeze();
    }

    private boolean isOnColdBlock(Player p) {
        Block b = p.getLocation().getBlock();
        Material m = b.getType();
        return switch (m) {
            case SNOW, SNOW_BLOCK, POWDER_SNOW, ICE, PACKED_ICE, BLUE_ICE, FROSTED_ICE -> true;
            default -> false;
        };
    }

    private boolean isNearBlock(Player p, int radius, Set<Material> mats) {
        Block center = p.getLocation().getBlock();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Block b = center.getRelative(dx, dy, dz);
                    if (mats.contains(b.getType())) return true;
                }
            }
        }
        return false;
    }

    // ----------------- ОБЩИЕ ЭФФЕКТЫ (ЖАР + ХОЛОД + ВЛАЖНОСТЬ) -----------------

    private void applyStatusEffects(Player p,
                                    PlayerRaceProfile profile,
                                    boolean isNetherRace,
                                    boolean isAmphibian) {
        int heat = profile.getHeat();
        int freeze = profile.getFreeze();
        int humidity = profile.getWater();

        int slownessLevel = -1;
        int fatigueLevel = -1;

        // --- ЗАМОРОЗКА ---
        if (freeze >= 5 && freeze < 10) {
            slownessLevel = Math.max(slownessLevel, 0); // медлительность I
        } else if (freeze >= 10 && freeze < 20) {
            slownessLevel = Math.max(slownessLevel, 1); // медлительность II
            fatigueLevel = Math.max(fatigueLevel, 1);   // усталость II
        } else if (freeze >= 20) {
            slownessLevel = Math.max(slownessLevel, 2); // медлительность III
            fatigueLevel = Math.max(fatigueLevel, 2);   // усталость III
            p.damage(2.0);
            profile.setFreeze(10); // откат до 50%, чтобы не убивало насмерть
        }

        // --- ВЛАЖНОСТЬ ТЕЛА (только амфибии, при 0% влажности) ---
        if (isAmphibian && humidity == 0) {
            slownessLevel = Math.max(slownessLevel, 1); // минимум медлительность II
            fatigueLevel = Math.max(fatigueLevel, 1);   // минимум усталость II
            if (random.nextInt(10) == 0) { // примерно раз в 10 сек по сердцу
                p.damage(2.0);
            }
        }

        if (slownessLevel >= 0) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, slownessLevel, true, false, false));
        }
        if (fatigueLevel >= 0) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 40, fatigueLevel, true, false, false));
        }

        // --- ПЕРЕГРЕВ (НЕ ДЛЯ АДСКИХ РАС) ---
        if (!isNetherRace) {
            if (heat >= 10 && heat < 15) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 0, true, false, false));
            } else if (heat >= 15 && heat < 20) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 1, true, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 40, 0, true, false, false));
            } else if (heat >= 20) {
                p.damage(isAmphibian ? 2.0 : 1.0);
            }
        }
    }

    // ----------------- ВЛАЖНОСТЬ ТЕЛА (бывшая вода) -----------------

    private boolean updateHumidity(Player p, PlayerRaceProfile profile, boolean isAmphibian) {
        int old = profile.getWater();

        if (!isAmphibian) {
            // для не-амфибий влажность тела/жажда отключена
            return false;
        }

        boolean inWater = p.getLocation().getBlock().getType() == Material.WATER;

        if (inWater) {
            // ~1 пункт каждые 5 секунд
            if (random.nextInt(5) == 0) profile.addWater(+1);
        } else {
            // ~1 пункт каждые 10 секунд
            if (random.nextInt(10) == 0) profile.addWater(-1);
        }

        if (profile.getWater() < 0) profile.setWater(0);
        if (profile.getWater() > MAX_STAT_VALUE) profile.setWater(MAX_STAT_VALUE);

        return old != profile.getWater();
    }

    // ----------------- ЭНД-ИСКАЖЕНИЕ -----------------

    private boolean updateEndDistortion(Player p, PlayerRaceProfile profile, RaceDefinition race) {
        int old = profile.getEndDistortion();

        boolean isEndRace = race.dimension() == RaceDimension.END;
        UUID id = p.getUniqueId();

        if (isEndRace) {
            profile.setEndDistortion(0);
            endTickTimer.remove(id);
            return old != 0;
        }

        World.Environment env = p.getWorld().getEnvironment();

        if (env == World.Environment.THE_END) {
            // ~1 пункт/мин в Энде
            if (random.nextInt(60) == 0) profile.addEndDistortion(+1);
        } else {
            // потихоньку спад
            if (profile.getEndDistortion() > 0 && random.nextInt(60) == 0) {
                profile.addEndDistortion(-1);
            }
        }

        if (profile.getEndDistortion() < 0) profile.setEndDistortion(0);
        if (profile.getEndDistortion() > MAX_STAT_VALUE) profile.setEndDistortion(MAX_STAT_VALUE);

        int level = profile.getEndDistortion();
        int timer = endTickTimer.getOrDefault(id, 0) + 1;

        if (timer >= 10) { // раз в 10 секунд
            if (level >= 5 && level < 10) {
                if (random.nextInt(100) < 5) {
                    randomTeleportLikeChorus(p);
                }
            } else if (level >= 10 && level < 20) {
                if (random.nextInt(100) < 15) {
                    randomTeleportLikeChorus(p);
                    p.damage(1.5);
                }
            } else if (level >= 20) {
                spawnAggressiveEndermen(p);
                profile.setEndDistortion(8); // 40%
            }
            timer = 0;
        }

        endTickTimer.put(id, timer);

        return old != profile.getEndDistortion();
    }

    private void randomTeleportLikeChorus(Player p) {
        Location loc = p.getLocation();
        double radius = 8;
        double dx = (random.nextDouble() * 2 - 1) * radius;
        double dz = (random.nextDouble() * 2 - 1) * radius;
        Location target = loc.clone().add(dx, 0, dz);
        target = p.getWorld().getHighestBlockAt(target).getLocation().add(0.5, 1, 0.5);
        p.teleport(target);
        p.getWorld().playSound(target, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1f, 1f);
    }

    private void spawnAggressiveEndermen(Player p) {
        for (int i = 0; i < 6; i++) {
            Location around = p.getLocation().clone().add(
                    random.nextInt(7) - 3,
                    0,
                    random.nextInt(7) - 3
            );
            around = p.getWorld().getHighestBlockAt(around).getLocation().add(0.5, 1, 0.5);
            Enderman e = (Enderman) p.getWorld().spawnEntity(around, EntityType.ENDERMAN);
            e.setTarget(p);
            e.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1, true, false, false));
        }
    }

    // --- потребление предметов: влажность для амфибий + снижение искажения энда ---

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        // если кто-то отменил ивент (например, запретил еду амфибии), ничего не делаем
        if (event.isCancelled()) return;

        Player p = event.getPlayer();
        PlayerRaceProfile profile = storage.get(p.getUniqueId());
        if (profile == null || !profile.hasRace()) return;

        RaceDefinition race = registry.get(profile.getRaceId());
        boolean isAmphibian = race != null && "amphibian".equalsIgnoreCase(race.id());

        Material m = event.getItem().getType();

        // зелья / мед – жидкость → влажность +2 только для амфибий
        if (isAmphibian && (m == Material.POTION || m == Material.HONEY_BOTTLE)) {
            profile.addWater(+2);
            if (profile.getWater() > MAX_STAT_VALUE) profile.setWater(MAX_STAT_VALUE);
        }

        // продукты, снижающие искажение энда – ТОЛЬКО для не-амфибий
        if (!isAmphibian) {
            switch (m) {
                case MILK_BUCKET -> profile.addEndDistortion(-1);  // ≈5%
                case GOLDEN_APPLE -> profile.addEndDistortion(-2); // ≈10%
                case GOLDEN_CARROT -> profile.addEndDistortion(-1);
                case HONEY_BOTTLE -> profile.addEndDistortion(-1);
                default -> {
                }
            }
            if (profile.getEndDistortion() < 0) profile.setEndDistortion(0);
        }
    }
}
