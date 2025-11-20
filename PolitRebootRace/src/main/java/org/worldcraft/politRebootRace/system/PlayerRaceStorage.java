package org.worldcraft.politRebootRace.system;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.worldcraft.politRebootRace.model.PlayerRaceProfile;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerRaceStorage {

    private final File file;
    private FileConfiguration config;

    private final Map<UUID, PlayerRaceProfile> profiles = new HashMap<>();

    public PlayerRaceStorage(Plugin plugin) {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        this.file = new File(plugin.getDataFolder(), "players.yml");
    }

    public void load() {
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(file);

        if (config.isConfigurationSection("players")) {
            for (String key : config.getConfigurationSection("players").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                String base = "players." + key;

                String race = config.getString(base + ".race", null);

                PlayerRaceProfile p = new PlayerRaceProfile(uuid);
                p.setRaceId(race);
                p.setHeat(config.getInt(base + ".heat", 0));
                p.setFreeze(config.getInt(base + ".freeze", 0));
                p.setWater(config.getInt(base + ".water", 0));
                p.setEndDistortion(config.getInt(base + ".endDistortion", 0));

                p.setEvolutionLineId(config.getString(base + ".evolution.line", null));
                p.setEvolutionStep(config.getInt(base + ".evolution.step", 0));
                if (config.isConfigurationSection(base + ".evolution.progress")) {
                    for (String keyReq : config.getConfigurationSection(base + ".evolution.progress").getKeys(false)) {
                        int value = config.getInt(base + ".evolution.progress." + keyReq, 0);
                        p.getRequirementProgress().put(keyReq, value);
                    }
                }

                profiles.put(uuid, p);
            }
        }
    }

    public void save() {
        if (config == null) {
            config = new YamlConfiguration();
        }

        for (PlayerRaceProfile p : profiles.values()) {
            String base = "players." + p.uuid();
            config.set(base + ".race", p.getRaceId());
            config.set(base + ".heat", p.getHeat());
            config.set(base + ".freeze", p.getFreeze());
            config.set(base + ".water", p.getWater());
            config.set(base + ".endDistortion", p.getEndDistortion());

            config.set(base + ".evolution.line", p.getEvolutionLineId());
            config.set(base + ".evolution.step", p.getEvolutionStep());
            for (String key : p.getRequirementProgress().keySet()) {
                config.set(base + ".evolution.progress." + key, p.getRequirementProgress().get(key));
            }
        }

        try {
            config.save(file);
        } catch (IOException ignored) {}
    }

    public PlayerRaceProfile get(UUID uuid) {
        return profiles.computeIfAbsent(uuid, PlayerRaceProfile::new);
    }
}
