package org.worldcraft.politRebootRace.system;

import io.papermc.paper.event.entity.EntityKnockbackEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.worldcraft.politRebootRace.PolitRebootRace;
import org.worldcraft.politRebootRace.model.*;

import java.util.*;

public class SelectionManager implements Listener {

    private static final String TITLE_DIMENSION = "Выбор мира";
    private static final String TITLE_RACE = "Выбор расы";
    private static final String TITLE_CONFIRM = "Подтверждение расы";

    private final Plugin plugin;
    private final PlayerRaceStorage storage;
    private final RaceRegistry registry;

    private final Map<UUID, SelectionSession> sessions = new HashMap<>();

    // слоты планет (3x3 блока каждая)
    private static final int[] EARTH_SLOTS  = {0, 1, 2, 9, 10, 11, 18, 19, 20};
    private static final int   EARTH_CENTER = 10;

    private static final int[] NETHER_SLOTS  = {3, 4, 5, 12, 13, 14, 21, 22, 23};
    private static final int   NETHER_CENTER = 13;

    private static final int[] END_SLOTS  = {6, 7, 8, 15, 16, 17, 24, 25, 26};
    private static final int   END_CENTER = 16;

    // слоты выбора рас (центр каждой "колонки")
    private static final int[] RACE_SLOTS = {10, 13, 16};

    // слоты подтверждения
    private static final int CONFIRM_SLOT = 13;
    private static final int BACK_SLOT    = 22;

    public SelectionManager(Plugin plugin, PlayerRaceStorage storage, RaceRegistry registry) {
        this.plugin = plugin;
        this.storage = storage;
        this.registry = registry;
    }

    // ----------------- ВСПОМОГАТЕЛЬНЫЕ -----------------

    private SelectionSession getSession(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), u -> new SelectionSession());
    }

    private PlayerRaceProfile getProfile(Player p) {
        return storage.get(p.getUniqueId());
    }

    private boolean isOgre(Player p) {
        PlayerRaceProfile profile = getProfile(p);
        return profile != null && profile.hasRace() && "ogre".equalsIgnoreCase(profile.getRaceId());
    }

    private boolean isAmphibian(Player p) {
        PlayerRaceProfile profile = getProfile(p);
        return profile != null && profile.hasRace() && "amphibian".equalsIgnoreCase(profile.getRaceId());
    }

    public void startSelection(Player player) {
        SelectionSession session = getSession(player);
        session.setStage(SelectionSession.Stage.DIMENSION);
        openDimensionGui(player);
    }

    /**
     * Пере-применяет расовые атрибуты/эффекты к игроку.
     */
    public void reapplyRace(Player player) {
        PlayerRaceProfile profile = storage.get(player.getUniqueId());
        if (profile == null || !profile.hasRace()) return;

        RaceDefinition race = registry.get(profile.getRaceId());
        if (race == null) return;

        applyRaceAttributes(player, race);
    }

    // ----------------- GUI УТИЛИТЫ -----------------

    private ItemStack glassPane(String name) {
        ItemStack stack = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void fillFrame(Inventory inv) {
        ItemStack filler = glassPane(" ");
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }

    // легенды измерений
    private List<String> dimensionLore(RaceDimension dim) {
        List<String> lore = new ArrayList<>();
        switch (dim) {
            case EARTH -> {
                lore.add("§7Живой мир, наполненный светом.");
                lore.add("§7Изобилие ресурсов и мягкий климат.");
                lore.add("§7Здесь рождаются цивилизации и легенды.");
            }
            case NETHER -> {
                lore.add("§7Осколок мира, выжженный пламенем.");
                lore.add("§7Пиглины и воины грызутся за золото.");
                lore.add("§7Каждый шаг даётся в обмен на кровь и жар.");
            }
            case END -> {
                lore.add("§7Мир пустоты и шёпота.");
                lore.add("§7Магия здесь — не инструмент, а воздух.");
                lore.add("§7Сущности синтезируют ресурсы из самой реальности.");
            }
        }
        return lore;
    }

    // легенды рас
    private List<String> raceLegend(String id) {
        List<String> lore = new ArrayList<>();
        switch (id.toLowerCase()) {
            case "human" -> {
                lore.add("§7Обычные смертные с сильной волей.");
                lore.add("§7Гибкие, приспосабливаются к любому миру.");
            }
            case "ogre" -> {
                lore.add("§7Громоздкие великаны, рождённые для боя.");
                lore.add("§7Медлительны, но почти не знают страха.");
            }
            case "amphibian" -> {
                lore.add("§7Существа, чья стихия — вода.");
                lore.add("§7На суше страдают, в глубинах — чувствуют себя как дома.");
            }
            case "piglin" -> {
                lore.add("§7Торговцы и воины ада.");
                lore.add("§7Почитают золото и презирают слабость.");
            }
            case "blazeborn" -> {
                lore.add("§7Порождения пламени и пепла.");
                lore.add("§7Ходят по лаве так же спокойно, как по камню.");
            }
            case "witherborn" -> {
                lore.add("§7Коснувшиеся силы иссушения.");
                lore.add("§7Гибель следует за ними по пятам.");
            }
            case "enderman" -> {
                lore.add("§7Тонкие тени между мирами.");
                lore.add("§7Молчат, но видят дальше других.");
            }
            case "dragonborn" -> {
                lore.add("§7Наследники силы дракона края.");
                lore.add("§7Гордые, сильные и очень опасные.");
            }
            case "chorusian" -> {
                lore.add("§7Маленькие странники, связанные с цветком хора.");
                lore.add("§7Лёгкие, везучие и почти невесомые.");
            }
            default -> lore.add("§7Таинственное создание неизвестного происхождения.");
        }
        return lore;
    }

    // ----------------- ПЛАНЕТЫ (ВЫБОР МИРА) -----------------

    private void buildPlanet(Inventory inv,
                             int centerSlot,
                             int[] slots,
                             Material surroundMat,
                             Material centerMat,
                             String name,
                             List<String> lore) {

        // один и тот же lore на весь "шар", чтобы при наведении везде была легенда
        ItemStack surround = item(surroundMat, name, lore);
        ItemStack center   = item(centerMat, name, lore);

        for (int slot : slots) {
            inv.setItem(slot, surround);
        }
        inv.setItem(centerSlot, center);
    }

    private RaceDimension dimensionForSlot(int slot) {
        for (int s : EARTH_SLOTS) {
            if (s == slot) return RaceDimension.EARTH;
        }
        for (int s : NETHER_SLOTS) {
            if (s == slot) return RaceDimension.NETHER;
        }
        for (int s : END_SLOTS) {
            if (s == slot) return RaceDimension.END;
        }
        return null;
    }

    private void openDimensionGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_DIMENSION);
        fillFrame(inv);

        // Земля — изобилие и баланс
        buildPlanet(
                inv,
                EARTH_CENTER,
                EARTH_SLOTS,
                Material.GRASS_BLOCK,
                Material.EMERALD_BLOCK,
                "§aЗемля",
                dimensionLore(RaceDimension.EARTH)
        );

        // Ад — осколок, выжженный войной
        buildPlanet(
                inv,
                NETHER_CENTER,
                NETHER_SLOTS,
                Material.NETHERRACK,
                Material.GOLD_BLOCK,
                "§cАд",
                dimensionLore(RaceDimension.NETHER)
        );

        // Энд — пустота и магия
        buildPlanet(
                inv,
                END_CENTER,
                END_SLOTS,
                Material.END_STONE,
                Material.ENDER_EYE,
                "§5Энд",
                dimensionLore(RaceDimension.END)
        );

        player.openInventory(inv);
    }

    // ----------------- ВЫБОР РАСЫ -----------------

    /**
     * Посчитать, каким будет атрибут после применения RaceStats,
     * исходя из ванильного базового значения.
     */
    private double previewStat(RaceStats stats, Attribute attribute, double vanillaBase) {
        RaceStats.AttributeChange change = stats.attributes().get(attribute);
        if (change == null) return vanillaBase;
        return change.apply(vanillaBase);
    }

    private String fmtDouble(double v) {
        if (Math.abs(v - Math.round(v)) < 1e-6) {
            return String.valueOf((int) Math.round(v));
        }
        return String.format(Locale.US, "%.2f", v);
    }

    private void openRaceGui(Player player, SelectionSession session) {
        List<RaceDefinition> races = registry.getByDimension(session.getDimension());
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_RACE);
        fillFrame(inv);

        List<String> ids = new ArrayList<>(Collections.nCopies(inv.getSize(), null));

        for (int i = 0; i < races.size() && i < RACE_SLOTS.length; i++) {
            RaceDefinition r = races.get(i);
            RaceStats s = r.stats();
            int slot = RACE_SLOTS[i];

            double hp    = previewStat(s, Attribute.MAX_HEALTH, 20.0);
            double dmg   = previewStat(s, Attribute.ATTACK_DAMAGE, 2.0);
            double speed = previewStat(s, Attribute.MOVEMENT_SPEED, 0.10);
            double height = s.heightScale();

            List<String> lore = new ArrayList<>();
            lore.addAll(raceLegend(r.id()));
            lore.add(" ");
            lore.add("§7Характеристики:");
            lore.add("§7HP: §c" + fmtDouble(hp));
            lore.add("§7Урон: §c" + fmtDouble(dmg));
            lore.add("§7Скорость: §e" + fmtDouble(speed));
            lore.add("§7Рост: §e" + fmtDouble(height));

            if (!r.features().isEmpty()) {
                lore.add(" ");
                lore.add("§7Особенности:");
                for (RaceFeature f : r.features()) {
                    // Было: lore.add("§8- §f" + f.name());
                    // Стало:
                    lore.add("§8- §f" + f.getDisplayName());
                    lore.add("   §7" + f.getDescription()); // Добавит описание на следующую строку
                }
            }

            Material icon = iconForRace(r.id());
            inv.setItem(slot, item(icon, "§f" + r.displayName(), lore));
            ids.set(slot, r.id());
        }

        session.setRaceOrder(ids);
        player.openInventory(inv);
    }

    private void openConfirmGui(Player player, SelectionSession session) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_CONFIRM);
        fillFrame(inv);

        RaceDefinition r = session.getRace();
        RaceStats s = r.stats();

        double hp    = previewStat(s, Attribute.MAX_HEALTH, 20.0);
        double dmg   = previewStat(s, Attribute.ATTACK_DAMAGE, 2.0);
        double speed = previewStat(s, Attribute.MOVEMENT_SPEED, 0.10);

        // большая "планета" по центру — финальное подтверждение
        List<String> lore = new ArrayList<>();
        lore.addAll(dimensionLore(session.getDimension()));
        lore.add(" ");
        lore.add("§7Вы выбрали расу: §f" + r.displayName());
        lore.add(" ");
        lore.add("§7Характеристики:");
        lore.add("§7HP: §c" + fmtDouble(hp));
        lore.add("§7Урон: §c" + fmtDouble(dmg));
        lore.add("§7Скорость: §e" + fmtDouble(speed));
        lore.add(" ");
        lore.add("§aНажмите, чтобы утвердить выбор.");

        Material centerMat = switch (session.getDimension()) {
            case EARTH -> Material.EMERALD_BLOCK;
            case NETHER -> Material.GOLD_BLOCK;
            case END -> Material.ENDER_EYE;
        };

        inv.setItem(CONFIRM_SLOT, item(centerMat, "§aПодтвердить мир и расу", lore));

        // маленькая кнопка назад
        inv.setItem(BACK_SLOT, item(
                Material.ARROW,
                "§cВернуться к планетам",
                List.of("§7Нажмите, чтобы изменить измерение.")
        ));

        player.openInventory(inv);
    }

    private Material iconForRace(String id) {
        return switch (id.toLowerCase()) {
            case "human" -> Material.PLAYER_HEAD;
            case "ogre" -> Material.IRON_AXE;
            case "amphibian" -> Material.TROPICAL_FISH;
            case "piglin" -> Material.GOLD_INGOT;
            case "blazeborn" -> Material.BLAZE_ROD;
            case "witherborn" -> Material.WITHER_SKELETON_SKULL;
            case "enderman" -> Material.ENDER_PEARL;
            case "dragonborn" -> Material.DRAGON_HEAD;
            case "chorusian" -> Material.CHORUS_FRUIT;
            default -> Material.PAPER;
        };
    }

    // ----------------- АТРИБУТЫ + ЭФФЕКТЫ РАС -----------------

    private void setAttr(Player p, Attribute attribute, double value) {
        AttributeInstance inst = p.getAttribute(attribute);
        if (inst != null) {
            inst.setBaseValue(value);
        }
    }

    public void applyRaceAttributes(Player player, RaceDefinition race) {
        RaceStats stats = race.stats();

        // применяем все статовые атрибуты
        stats.applyTo(player);
        // масштаб модели
        setAttr(player, Attribute.SCALE, stats.heightScale());

        // не даём здоровью быть выше максимума
        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            double max = maxHealthAttr.getValue();
            if (player.getHealth() > max) {
                player.setHealth(max);
            }
        }

        applyRacePotions(player, race);
        PolitRebootRace.get().getEvolutionManager().applyBonuses(player);
    }

    private void clearRacePotions(Player player) {
        PotionEffectType[] ours = {
                PotionEffectType.HASTE,
                PotionEffectType.JUMP_BOOST,
                PotionEffectType.WATER_BREATHING
        };
        for (PotionEffectType type : ours) {
            if (type != null) player.removePotionEffect(type);
        }
    }

    private void applyRacePotions(Player player, RaceDefinition race) {
        clearRacePotions(player);

        String id = race.id().toLowerCase();

        if ("ogre".equals(id)) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.HASTE, Integer.MAX_VALUE, 1, true, false, false));
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 3, true, false, false));
        }

        if ("amphibian".equals(id)) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0, true, false, false));
        }
    }

    // ----------------- СОБЫТИЯ ЖИЗНЕННОГО ЦИКЛА -----------------

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        reapplyRace(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> reapplyRace(player));
    }

    // ----------------- GUI КЛИКИ -----------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        SelectionSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        if (!(TITLE_DIMENSION.equals(title)
                || TITLE_RACE.equals(title)
                || TITLE_CONFIRM.equals(title))) {
            return;
        }

        if (event.getClickedInventory() == null ||
                !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        event.setCancelled(true);

        int slot = event.getSlot();

        if (TITLE_DIMENSION.equals(title)) {
            handleDimensionClick(player, session, slot);
        } else if (TITLE_RACE.equals(title)) {
            handleRaceClick(player, session, slot);
        } else if (TITLE_CONFIRM.equals(title)) {
            handleConfirmClick(player, session, slot);
        }
    }

    private void handleDimensionClick(Player player, SelectionSession session, int slot) {
        RaceDimension dim = dimensionForSlot(slot);
        if (dim == null) return;

        session.setDimension(dim);
        session.setStage(SelectionSession.Stage.RACE);
        openRaceGui(player, session);
    }

    private void handleRaceClick(Player player, SelectionSession session, int slot) {
        if (session.getDimension() == null) {
            openDimensionGui(player);
            return;
        }

        List<String> ids = session.getRaceOrder();
        if (ids == null || slot < 0 || slot >= ids.size()) return;

        String id = ids.get(slot);
        if (id == null) return;

        RaceDefinition def = registry.get(id);
        if (def == null) return;

        session.setRace(def);
        session.setStage(SelectionSession.Stage.CONFIRM);
        openConfirmGui(player, session);
    }

    private void handleConfirmClick(Player player, SelectionSession session, int slot) {
        if (slot == BACK_SLOT) {
            session.setStage(SelectionSession.Stage.DIMENSION);
            openDimensionGui(player);
            return;
        }
        if (slot != CONFIRM_SLOT) return;

        RaceDefinition race = session.getRace();
        if (race == null || session.getDimension() == null) return;

        PlayerRaceProfile profile = storage.get(player.getUniqueId());
        if (profile == null) return;

        profile.resetEvolution();
        profile.setRaceId(race.id());
        storage.save();

        PolitRebootRace.get().getEvolutionManager().clearBonuses(player);
        applyRaceAttributes(player, race);
        player.closeInventory();
        player.sendMessage("§aРаса установлена: §f" + race.displayName());

        session.setStage(SelectionSession.Stage.NONE);
        sessions.remove(player.getUniqueId());
    }

    // ----------------- СПЕЦ. МЕХАНИКИ РАС -----------------

    // Огры: иммун к откидыванию
    @EventHandler
    public void onKnockback(EntityKnockbackEvent event) {
        if (event.getEntity() instanceof Player p && isOgre(p)) {
            event.setCancelled(true);
        }
    }

    // Падения и огонь для огра/амфибии
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;

        DamageCause cause = event.getCause();

        // Огр: падение — порог с 3 до 7 блоков
        if (isOgre(p) && cause == DamageCause.FALL) {
            float dist = p.getFallDistance();
            if (dist <= 7.0f) {
                event.setCancelled(true);
            } else {
                event.setDamage(event.getDamage() * 0.8);
            }
        }

        // Амфибия: урон от падений вдвое меньше
        if (isAmphibian(p) && cause == DamageCause.FALL) {
            event.setDamage(event.getDamage() * 0.5);
        }

        // Амфибия: огонь наносит двойной урон
        if (isAmphibian(p) && (
                cause == DamageCause.FIRE ||
                        cause == DamageCause.FIRE_TICK ||
                        cause == DamageCause.LAVA
        )) {
            event.setDamage(event.getDamage() * 2.0);
        }
    }

    // Еда: Огр — постоянный голод + -20% эффективности
    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;

        if (isOgre(p)) {
            int oldLevel = p.getFoodLevel();
            int newLevel = event.getFoodLevel();
            int delta = newLevel - oldLevel;

            if (delta > 0) {
                int reduced = (int) Math.floor(delta * 0.8); // -20%
                newLevel = oldLevel + reduced;

                float sat = p.getSaturation();
                if (sat > 0) {
                    float newSat = sat * 0.8f;
                    if (newSat > newLevel) newSat = newLevel;
                    p.setSaturation(newSat);
                }
            }

            // постоянный "чуть голодный" — не выше 8
            if (newLevel > 8) {
                newLevel = 8;
            }

            event.setFoodLevel(newLevel);
        }
    }

    @EventHandler
    public void onItemEat(PlayerItemConsumeEvent event) {
        Player p = event.getPlayer();
        Material m = event.getItem().getType();

        // Амфибия питается только рыбой как ЕДОЙ, но может пить зелья/молоко
        if (isAmphibian(p) && m.isEdible() && !isFish(m)) {
            event.setCancelled(true);
            p.sendMessage("§cАмфибия может питаться только рыбой!");
            return;
        }

        // Молоко очищает эффекты — нужно вернуть расовые бафы
        if (m == Material.MILK_BUCKET) {
            Bukkit.getScheduler().runTask(plugin, () -> reapplyRace(p));
        }
    }
    /**
     * Сбрасывает игрока до "заводских настроек" Minecraft.
     */
    public void resetPlayerAttributes(Player player) {
        // 1. Сброс основных атрибутов до ванильных значений
        setAttr(player, Attribute.MAX_HEALTH, 20.0);
        setAttr(player, Attribute.MOVEMENT_SPEED, 0.10);
        setAttr(player, Attribute.ATTACK_DAMAGE, 1.0); // Ванильная база (1.0 + рука = 2.0)
        setAttr(player, Attribute.ATTACK_KNOCKBACK, 0.0);
        setAttr(player, Attribute.KNOCKBACK_RESISTANCE, 0.0);
        setAttr(player, Attribute.SCALE, 1.0); // Возвращаем нормальный рост

        // Дополнительные атрибуты (на всякий случай)
        setAttr(player, Attribute.ENTITY_INTERACTION_RANGE, 3.0);
        setAttr(player, Attribute.BLOCK_INTERACTION_RANGE, 4.5);

        // 2. Снимаем все эффекты зелий
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // 3. Корректируем здоровье (чтобы не осталось 30/20 хп)
        if (player.getHealth() > 20.0) {
            player.setHealth(20.0);
        }

        // 4. Сбрасываем еду/насыщение если нужно, но обычно это не требуется
    }

    private boolean isFish(Material m) {
        return switch (m) {
            case COD,
                 COOKED_COD,
                 SALMON,
                 COOKED_SALMON,
                 TROPICAL_FISH,
                 PUFFERFISH -> true;
            default -> false;
        };
    }
}
