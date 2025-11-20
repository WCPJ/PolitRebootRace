package org.worldcraft.politRebootRace.system;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.worldcraft.politRebootRace.PolitRebootRace;
import org.worldcraft.politRebootRace.model.PlayerRaceProfile;
import org.worldcraft.politRebootRace.model.RaceDefinition;
import org.worldcraft.politRebootRace.model.RaceDimension;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NationalityCommand implements CommandExecutor, TabCompleter {

    private final SelectionManager selectionManager;
    private final PlayerRaceStorage storage;
    private final RaceRegistry registry;

    public NationalityCommand(SelectionManager selectionManager, PlayerRaceStorage storage, RaceRegistry registry) {
        this.selectionManager = selectionManager;
        this.storage = storage;
        this.registry = registry;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        // 1. Если аргументов нет — логика для обычного игрока (открыть меню)
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cКонсоль может использовать только: /nationality set/reset ...");
                return true;
            }

            PlayerRaceProfile profile = storage.get(player.getUniqueId());
            if (profile.hasRace()) {
                // Если хочешь разрешить игрокам менять расу самим, убери эту проверку
                player.sendMessage("§cВы уже выбрали свою расу. Обратитесь к администратору для смены.");
                return true;
            }

            selectionManager.startSelection(player);
            return true;
        }

        // 2. Проверка прав администратора для аргументов
        if (!sender.hasPermission("politrace.admin")) {
            sender.sendMessage("§cУ вас нет прав на использование команд управления расами.");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // === СБРОС РАСЫ ===
        // /nationality reset <player>
        if (subCommand.equals("reset")) {
            if (args.length < 2) {
                sender.sendMessage("§cИспользование: /nationality reset <player>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cИгрок не найден.");
                return true;
            }

            // Сбрасываем профиль
            PlayerRaceProfile profile = storage.get(target.getUniqueId());
            profile.setRaceId(null); // Удаляем ID расы
            // Сбрасываем статы
            profile.setHeat(0);
            profile.setFreeze(0);
            profile.setWater(20);
            profile.setEndDistortion(0);
            profile.resetEvolution();

            storage.save(); // Сохраняем изменения

            // Сбрасываем физические атрибуты
            selectionManager.resetPlayerAttributes(target);
            PolitRebootRace.get().getEvolutionManager().clearBonuses(target);

            sender.sendMessage("§aРаса игрока " + target.getName() + " успешно сброшена.");
            target.sendMessage("§eВаша раса была сброшена администратором.");
            return true;
        }

        // === ВЫДАЧА РАСЫ ===
        // /nationality set <player> <race_id>
        if (subCommand.equals("set")) {
            if (args.length < 3) {
                sender.sendMessage("§cИспользование: /nationality set <player> <race_id>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cИгрок не найден.");
                return true;
            }

            String raceId = args[2];
            RaceDefinition race = registry.get(raceId);

            if (race == null) {
                sender.sendMessage("§cРаса с ID '" + raceId + "' не найдена.");
                return true;
            }

            // Обновляем профиль
            PlayerRaceProfile profile = storage.get(target.getUniqueId());
            profile.resetEvolution();
            profile.setRaceId(race.id());
            storage.save();

            // Сначала сбрасываем старое, потом накладываем новое
            selectionManager.resetPlayerAttributes(target);
            selectionManager.applyRaceAttributes(target, race);

            sender.sendMessage("§aИгроку " + target.getName() + " установлена раса: " + race.displayName());
            target.sendMessage("§aАдминистратор установил вам расу: " + race.displayName());
            return true;
        }

        sender.sendMessage("§cНеизвестная подкоманда. Используйте set или reset.");
        return true;
    }

    // --- TAB COMPLETION (Подсказки) ---
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("politrace.admin")) {
            return List.of();
        }

        // Аргумент 1: set / reset
        if (args.length == 1) {
            return List.of("set", "reset");
        }

        // Аргумент 2: Ник игрока
        if (args.length == 2) {
            return null; // null автоматически покажет список онлайн игроков
        }

        // Аргумент 3: ID расы (только для set)
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            // Собираем все ID рас из всех измерений
            List<String> raceIds = new ArrayList<>();
            raceIds.addAll(getIdsFromDim(RaceDimension.EARTH));
            raceIds.addAll(getIdsFromDim(RaceDimension.NETHER));
            raceIds.addAll(getIdsFromDim(RaceDimension.END));
            return raceIds;
        }

        return List.of();
    }

    private List<String> getIdsFromDim(RaceDimension dim) {
        return registry.getByDimension(dim).stream()
                .map(RaceDefinition::id)
                .collect(Collectors.toList());
    }
}