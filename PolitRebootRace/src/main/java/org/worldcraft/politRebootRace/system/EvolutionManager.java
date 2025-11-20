package org.worldcraft.politRebootRace.system;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.worldcraft.politRebootRace.model.*;

import java.util.*;

public class EvolutionManager implements Listener {

    private static final String GUI_TITLE = "Эволюция расы";

    private final PlayerRaceStorage storage;
    private final EvolutionRegistry registry;

    public EvolutionManager(PlayerRaceStorage storage, EvolutionRegistry registry) {
        this.storage = storage;
        this.registry = registry;
    }

    public void openEvolution(Player player) {
        PlayerRaceProfile profile = storage.get(player.getUniqueId());
        if (profile == null || !profile.hasRace()) {
            player.sendMessage("§cСначала выберите расу через /nationality.");
            return;
        }

        List<EvolutionLine> lines = registry.getLinesForRace(profile.getRaceId());
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
        fill(inv);

        int slot = 10;
        for (EvolutionLine line : lines) {
            inv.setItem(slot, buildLineItem(line, profile));
            slot += 3;
        }

        inv.setItem(22, buildResetItem());
        player.openInventory(inv);
    }

    private ItemStack buildResetItem() {
        ItemStack stack = new ItemStack(Material.BARRIER);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cСбросить прогресс");
            meta.setLore(List.of(
                    "§7Возвращает развитие на ноль.",
                    "§7Можно выбрать другую линию."));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void fill(Inventory inv) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, pane);
        }
    }

    private ItemStack buildLineItem(EvolutionLine line, PlayerRaceProfile profile) {
        ItemStack stack = new ItemStack(line.getIcon());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e" + line.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add("§7" + line.getDescription());
            lore.add(" ");
            boolean active = line.getId().equalsIgnoreCase(profile.getEvolutionLineId());
            if (active) {
                lore.add("§aТекущая линия развития");
            } else if (profile.getEvolutionLineId() != null) {
                lore.add("§cВы уже выбрали другую линию");
            } else {
                lore.add("§7Нажмите, чтобы выбрать");
            }

            int step = active ? profile.getEvolutionStep() : 0;
            if (step >= line.getSteps().size()) {
                lore.add("§bЛиния завершена");
            } else {
                EvolutionStep next = line.getSteps().get(step);
                lore.add("§7Следующий этап: §f" + next.getName());
                lore.addAll(formatStep(next, profile, line.getId()));
            }

            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private List<String> formatStep(EvolutionStep step, PlayerRaceProfile profile, String lineId) {
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add("§7Бонусы:");
        for (String bonus : step.getBonuses()) {
            lore.add("§8- §f" + bonus);
        }
        lore.add(" ");
        lore.add("§7Требования:");
        int idx = 0;
        for (EvolutionRequirement req : step.getRequirements()) {
            String key = key(lineId, step, idx);
            int progress = profile.getRequirementProgress().getOrDefault(key, 0);
            lore.add("§8- §f" + req.getDescription() + " §7(" + progress + "/" + req.getAmount() + ")");
            idx++;
        }
        if (step.getExperienceCost() > 0) {
            lore.add("§8- §f" + step.getExperienceCost() + " уровней опыта");
        }
        if (!step.getResourceCosts().isEmpty()) {
            for (Map.Entry<Material, Integer> e : step.getResourceCosts().entrySet()) {
                lore.add("§8- §f" + e.getValue() + "x " + e.getKey().name());
            }
        }
        return lore;
    }

    private String key(String lineId, EvolutionStep step, int reqIndex) {
        return lineId + ":" + step.getId() + ":" + reqIndex;
    }

    private void tryUpgrade(Player player, EvolutionLine line, PlayerRaceProfile profile) {
        int stepIndex = profile.getEvolutionStep();
        if (stepIndex >= line.getSteps().size()) {
            player.sendMessage("§aВы уже завершили эту ветку.");
            return;
        }

        EvolutionStep step = line.getSteps().get(stepIndex);
        int idx = 0;
        for (EvolutionRequirement req : step.getRequirements()) {
            String key = key(line.getId(), step, idx);
            int progress = profile.getRequirementProgress().getOrDefault(key, 0);
            if (progress < req.getAmount()) {
                player.sendMessage("§cТребование не выполнено: " + req.getDescription());
                return;
            }
            idx++;
        }

        if (player.getLevel() < step.getExperienceCost()) {
            player.sendMessage("§cНе хватает опыта. Нужно " + step.getExperienceCost() + " уровней.");
            return;
        }

        for (Map.Entry<Material, Integer> entry : step.getResourceCosts().entrySet()) {
            if (!player.getInventory().containsAtLeast(new ItemStack(entry.getKey()), entry.getValue())) {
                player.sendMessage("§cНедостаточно ресурсов: " + entry.getKey().name());
                return;
            }
        }

        player.giveExpLevels(-step.getExperienceCost());
        for (Map.Entry<Material, Integer> entry : step.getResourceCosts().entrySet()) {
            player.getInventory().removeItem(new ItemStack(entry.getKey(), entry.getValue()));
        }

        profile.setEvolutionStep(stepIndex + 1);
        applyRewards(player, step);
        storage.save();
        player.sendMessage("§aЭтап завершён: " + step.getName());
        openEvolution(player);
    }

    private void applyRewards(Player player, EvolutionStep step) {
        for (PotionEffect effect : step.getRewards()) {
            player.addPotionEffect(effect);
        }
    }

    public void applyBonuses(Player player) {
        PlayerRaceProfile profile = storage.get(player.getUniqueId());
        if (profile == null) return;
        if (profile.getEvolutionLineId() == null) return;
        List<EvolutionLine> lines = registry.getLinesForRace(profile.getRaceId());
        for (EvolutionLine line : lines) {
            if (line.getId().equalsIgnoreCase(profile.getEvolutionLineId())) {
                int stepIdx = Math.min(profile.getEvolutionStep(), line.getSteps().size());
                for (int i = 0; i < stepIdx; i++) {
                    applyRewards(player, line.getSteps().get(i));
                }
                break;
            }
        }
    }

    public void clearBonuses(Player player) {
        PlayerRaceProfile profile = storage.get(player.getUniqueId());
        if (profile == null || profile.getRaceId() == null) return;
        player.getActivePotionEffects().forEach(effect -> {
            PotionEffectType type = effect.getType();
            for (EvolutionLine line : registry.getLinesForRace(profile.getRaceId())) {
                for (EvolutionStep step : line.getSteps()) {
                    for (PotionEffect reward : step.getRewards()) {
                        if (reward.getType().equals(type)) {
                            player.removePotionEffect(type);
                        }
                    }
                }
            }
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!GUI_TITLE.equals(event.getView().getTitle())) return;
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) return;
        event.setCancelled(true);

        PlayerRaceProfile profile = storage.get(player.getUniqueId());
        if (profile == null) return;

        if (event.getSlot() == 22) {
            clearBonuses(player);
            profile.resetEvolution();
            storage.save();
            player.sendMessage("§eПрогресс эволюции сброшен.");
            openEvolution(player);
            return;
        }

        List<EvolutionLine> lines = registry.getLinesForRace(profile.getRaceId());
        int index = (event.getSlot() - 10) / 3;
        if (index < 0 || index >= lines.size()) return;
        EvolutionLine line = lines.get(index);

        if (profile.getEvolutionLineId() == null) {
            profile.setEvolutionLineId(line.getId());
            profile.setEvolutionStep(0);
            storage.save();
            player.sendMessage("§aВы выбрали путь: " + line.getDisplayName());
            openEvolution(player);
            return;
        }

        if (!line.getId().equalsIgnoreCase(profile.getEvolutionLineId())) {
            player.sendMessage("§cСменить путь можно только после сброса прогресса.");
            return;
        }

        tryUpgrade(player, line, profile);
    }

    // --- Прогресс требований ---
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        Entity victim = event.getEntity();
        if (killer == null) return;

        if (victim instanceof Player) {
            bumpRequirement(killer, EvolutionRequirementType.PLAYER_KILL, null, null);
        } else {
            bumpRequirement(killer, EvolutionRequirementType.MOB_KILL, victim.getType(), null);
            bumpRequirement(killer, EvolutionRequirementType.SPECIFIC_MOB_KILL, victim.getType(), null);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack result = event.getRecipe().getResult();
        bumpRequirement(player, EvolutionRequirementType.CRAFT_ITEM, null, result.getType());
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;
        bumpRequirement(player, EvolutionRequirementType.USE_ITEM, null, item.getType());
    }

    private void bumpRequirement(Player player, EvolutionRequirementType type, EntityType entityType, Material material) {
        PlayerRaceProfile profile = storage.get(player.getUniqueId());
        if (profile == null || profile.getEvolutionLineId() == null) return;
        List<EvolutionLine> lines = registry.getLinesForRace(profile.getRaceId());
        EvolutionLine line = lines.stream()
                .filter(l -> l.getId().equalsIgnoreCase(profile.getEvolutionLineId()))
                .findFirst().orElse(null);
        if (line == null) return;
        if (profile.getEvolutionStep() >= line.getSteps().size()) return;
        int stepIdx = Math.min(profile.getEvolutionStep(), line.getSteps().size() - 1);
        if (stepIdx < 0 || stepIdx >= line.getSteps().size()) return;
        EvolutionStep step = line.getSteps().get(stepIdx);

        int idx = 0;
        for (EvolutionRequirement req : step.getRequirements()) {
            if (req.getType() != type) {
                idx++;
                continue;
            }
            if (req.getEntityTarget() != null && entityType != req.getEntityTarget()) {
                idx++;
                continue;
            }
            if (req.getMaterialTarget() != null && material != req.getMaterialTarget()) {
                idx++;
                continue;
            }
            String key = key(line.getId(), step, idx);
            int current = profile.getRequirementProgress().getOrDefault(key, 0);
            if (current < req.getAmount()) {
                profile.incrementRequirement(key, 1);
                storage.save();
            }
            idx++;
        }
    }
}
