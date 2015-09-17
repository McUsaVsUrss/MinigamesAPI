package com.comze_instancelabs.minigamesapi;

import com.comze_instancelabs.minigamesapi.config.ArenasConfig;
import com.comze_instancelabs.minigamesapi.util.Util;
import com.comze_instancelabs.minigamesapi.util.Validator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class ArenaSetup {

    // actually the most basic arena just needs a spawn and a lobby

    /**
     * Sets the spawn for a single-spawn arena
     *
     * @param arenaname
     * @param l         Location of the spawn
     */
    public void setSpawn(JavaPlugin plugin, String arenaname, Location l) {
        Util.saveComponentForArena(plugin, arenaname, "spawns.spawn0", l);
    }

    /**
     * Sets a new spawn for a multi-spawn arena without the need of a given index
     *
     * @param plugin
     * @param arenaname
     * @param l         Location of the spawn
     * @return the automatically used index
     */
    public int autoSetSpawn(JavaPlugin plugin, String arenaname, Location l) {
        int count = Util.getAllSpawns(plugin, arenaname).size();
        Util.saveComponentForArena(plugin, arenaname, "spawns.spawn" + Integer.toString(count), l);
        return count;
    }

    /**
     * Sets one of multiple spawns for a multi-spawn arena
     *
     * @param arenaname
     * @param l         Location of the spawn
     * @param count     Index of the spawn; if the given index is already set, the spawn location will be overwritten
     */
    public void setSpawn(JavaPlugin plugin, String arenaname, Location l, int count) {
        Util.saveComponentForArena(plugin, arenaname, "spawns.spawn" + Integer.toString(count), l);
    }

    /**
     * Removes a spawn at the given index
     *
     * @param plugin
     * @param arenaname
     * @param count     Index of the spawn
     */
    public boolean removeSpawn(JavaPlugin plugin, String arenaname, int count) {
        ArenasConfig config = MinigamesAPI.getAPI().getPluginInstance(plugin).getArenasConfig();
        String path = "arenas." + arenaname + ".spawns.spawn" + Integer.toString(count);
        boolean ret = false;
        if (config.getConfig().isSet(path)) {
            ret = true;
        }
        config.getConfig().set(path, null);
        config.saveConfig();
        return ret;
    }

    /**
     * Sets the waiting lobby for an arena
     *
     * @param arenaname
     * @param l         Location of the lobby
     */
    public void setLobby(JavaPlugin plugin, String arenaname, Location l) {
        Util.saveComponentForArena(plugin, arenaname, "lobby", l);
    }

    /**
     * Sets the main lobby
     *
     * @param l Location of the lobby
     */
    public void setMainLobby(JavaPlugin plugin, Location l) {
        Util.saveMainLobby(plugin, l);
    }

    /**
     * Sets low and high boundaries for later blocks resetting
     *
     * @param plugin
     * @param arenaname
     * @param l         Location to save
     * @param low       True if it's the low boundary, false if it's the high boundary
     */
    public void setBoundaries(JavaPlugin plugin, String arenaname, Location l, boolean low) {
        if (low) {
            Util.saveComponentForArena(plugin, arenaname, "bounds.low", l);
        } else {
            Util.saveComponentForArena(plugin, arenaname, "bounds.high", l);
        }
    }

    /**
     * Sets low and high boundaries for later blocks resetting for a sub component
     *
     * @param plugin
     * @param arenaname
     * @param l               Location to save
     * @param low             True if it's the low boundary, false if it's the high boundary
     * @param extra_component Sub component string
     */
    public void setBoundaries(JavaPlugin plugin, String arenaname, Location l, boolean low, String extra_component) {
        if (low) {
            Util.saveComponentForArena(plugin, arenaname, extra_component + ".bounds.low", l);
        } else {
            Util.saveComponentForArena(plugin, arenaname, extra_component + ".bounds.high", l);
        }
    }

    /**
     * Saves a given arena if it was set up properly.
     *
     * @return Arena or null if setup failed
     */
    public Arena saveArena(JavaPlugin plugin, String arenaname) {
        if (!Validator.isArenaValid(plugin, arenaname)) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Arena " + arenaname + " appears to be invalid.");
            return null;
        }
        PluginInstance pli = MinigamesAPI.getAPI().getPluginInstance(plugin);
        if (pli.getArenaByName(arenaname) != null) {
            pli.removeArenaByName(arenaname);
        }
        Arena a = Util.initArena(plugin, arenaname);
        if (a.getArenaType() == ArenaType.REGENERATION) {
            if (Util.isComponentForArenaValid(plugin, arenaname, "bounds")) {
                Util.saveArenaToFile(plugin, arenaname);
            } else {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Could not save arena to file because boundaries were not set up.");
            }
        }
        this.setArenaVIP(plugin, arenaname, false);
        pli.addArena(a);

        // experimental:
        Class clazz = plugin.getClass();
        try {
            Method method = clazz.getDeclaredMethod("loadArenas", JavaPlugin.class, pli.getArenasConfig().getClass());
            if (method != null) {
                method.setAccessible(true);
                Object ret = method.invoke(this, plugin, pli.getArenasConfig());
                System.out.println(ret);
                pli.clearArenas();
                pli.addLoadedArenas((ArrayList<Arena>) ret);
            }
        } catch (Exception e) {
            System.out.println("Failed to update Arena list, please reload the server.");
            e.printStackTrace();
        }

        String path = "arenas." + arenaname + ".displayname";
        if (!pli.getArenasConfig().getConfig().isSet(path)) {
            pli.getArenasConfig().getConfig().set(path, arenaname);
            pli.getArenasConfig().saveConfig();
        }

        return a;
    }

    public void setPlayerCount(JavaPlugin plugin, String arena, int count, boolean max) {
        String component = "max_players";
        if (!max) {
            component = "min_players";
        }
        String base = "arenas." + arena + "." + component;
        MinigamesAPI.getAPI().getPluginInstance(plugin).getArenasConfig().getConfig().set(base, count);
        MinigamesAPI.getAPI().getPluginInstance(plugin).getArenasConfig().saveConfig();
    }

    public int getPlayerCount(JavaPlugin plugin, String arena, boolean max) {
        if (!max) {
            if (!MinigamesAPI.getAPI().getPluginInstance(plugin).getArenasConfig().getConfig().isSet("arenas." + arena + ".min_players")) {
                setPlayerCount(plugin, arena, plugin.getConfig().getInt("config.defaults.default_min_players"), max);
                return plugin.getConfig().getInt("config.defaults.default_min_players");
            }
            return MinigamesAPI.getAPI().getPluginInstance(plugin).getArenasConfig().getConfig().getInt("arenas." + arena + ".min_players");
        }
        if (!MinigamesAPI.getAPI().getPluginInstance(plugin).getArenasConfig().getConfig().isSet("arenas." + arena + ".max_players")) {
            setPlayerCount(plugin, arena, plugin.getConfig().getInt("config.defaults.default_max_players"), max);
            return plugin.getConfig().getInt("config.defaults.default_max_players");
        }
        return MinigamesAPI.getAPI().getPluginInstance(plugin).getArenasConfig().getConfig().getInt("arenas." + arena + ".max_players");
    }

    public void setArenaVIP(JavaPlugin plugin, String arena, boolean vip) {
        MinigamesAPI.getAPI().getPluginInstance(plugin).getArenasConfig().getConfig().set("arenas." + arena + ".is_vip", vip);
        MinigamesAPI.getAPI().getPluginInstance(plugin).getArenasConfig().saveConfig();
    }

    public boolean getArenaVIP(JavaPlugin plugin, String arena) {
        return MinigamesAPI.getAPI().getPluginInstance(plugin).getArenasConfig().getConfig().getBoolean("arenas." + arena + ".is_vip");
    }

    public void setArenaEnabled(JavaPlugin plugin, String arena, boolean enabled) {
        MinigamesAPI.getAPI().getPluginInstance(plugin).getArenasConfig().getConfig().set("arenas." + arena + ".enabled", enabled);
        MinigamesAPI.getAPI().getPluginInstance(plugin).getArenasConfig().saveConfig();
    }

    public boolean getArenaEnabled(JavaPlugin plugin, String arena) {
        FileConfiguration config = MinigamesAPI.getAPI().getPluginInstance(plugin).getArenasConfig().getConfig();
        return config.isSet("arenas." + arena + ".enabled") ? config.getBoolean("arenas." + arena + ".enabled") : true;
    }

    public void setShowScoreboard(JavaPlugin plugin, String arena, boolean enabled) {
        MinigamesAPI.getAPI().getPluginInstance(plugin).getArenasConfig().getConfig().set("arenas." + arena + ".showscoreboard", enabled);
        MinigamesAPI.getAPI().getPluginInstance(plugin).getArenasConfig().saveConfig();
    }

    public boolean getShowScoreboard(JavaPlugin plugin, String arena) {
        FileConfiguration config = MinigamesAPI.getAPI().getPluginInstance(plugin).getArenasConfig().getConfig();
        return config.isSet("arenas." + arena + ".showscoreboard") ? config.getBoolean("arenas." + arena + ".showscoreboard") : true;
    }
}
