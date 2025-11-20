package org.worldcraft.politRebootRace;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.worldcraft.politRebootRace.system.*;

public class PolitRebootRace extends JavaPlugin {

    private static PolitRebootRace instance;

    private RaceRegistry raceRegistry;
    public PlayerRaceStorage storage;
    private SelectionManager selectionManager;
    private StatusManager statusManager;
    private EvolutionRegistry evolutionRegistry;
    private EvolutionManager evolutionManager;

    public static PolitRebootRace get() {
        return instance;
    }

    public EvolutionManager getEvolutionManager() {
        return evolutionManager;
    }

    @Override
    public void onEnable() {
        instance = this;

        raceRegistry = new RaceRegistry();
        raceRegistry.registerDefaultRaces();

        storage = new PlayerRaceStorage(this);
        storage.load();

        selectionManager = new SelectionManager(this, storage, raceRegistry);
        statusManager = new StatusManager(this, storage, raceRegistry);
        evolutionRegistry = new EvolutionRegistry();
        evolutionManager = new EvolutionManager(storage, evolutionRegistry);

        if (getCommand("nationality") != null) {
            // Передаем this.raceRegistry третьим аргументом!
            getCommand("nationality").setExecutor(new NationalityCommand(selectionManager, storage, raceRegistry));
        } else {
            getLogger().severe("Команда /nationality не найдена в plugin.yml!");
        }

        if (getCommand("evolution") != null) {
            getCommand("evolution").setExecutor((sender, command, label, args) -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cКоманда доступна только игрокам.");
                    return true;
                }
                evolutionManager.openEvolution(player);
                return true;
            });
        } else {
            getLogger().severe("Команда /evolution не найдена в plugin.yml!");
        }

        Bukkit.getPluginManager().registerEvents(selectionManager, this);
        Bukkit.getPluginManager().registerEvents(statusManager, this);
        Bukkit.getPluginManager().registerEvents(evolutionManager, this);

        // тик статов раз в секунду
        Bukkit.getScheduler().runTaskTimer(this, statusManager, 20L, 20L);

        // перекинуть расовые атрибуты онлайн-игрокам
        for (Player player : Bukkit.getOnlinePlayers()) {
            selectionManager.reapplyRace(player);
        }

        getLogger().info("PolitRebootRace enabled.");
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            storage.save();
        }
        getLogger().info("PolitRebootRace disabled.");
    }
}
