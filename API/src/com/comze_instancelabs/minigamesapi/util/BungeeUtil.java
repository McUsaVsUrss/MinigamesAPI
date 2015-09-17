package com.comze_instancelabs.minigamesapi.util;

import com.comze_instancelabs.minigamesapi.Arena;
import com.comze_instancelabs.minigamesapi.MinigamesAPI;
import com.comze_instancelabs.minigamesapi.PluginInstance;
import com.comze_instancelabs.minigamesapi.bungee.BungeeSocket;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BungeeUtil {

    public static void connectToServer(JavaPlugin plugin, String player, String server) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);
        try {
            out.writeUTF("Connect");
            out.writeUTF(server);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bukkit.getPlayer(player).sendPluginMessage(plugin, "BungeeCord", stream.toByteArray());
    }

    public static void sendSignUpdateRequest(JavaPlugin plugin, String minigame, Arena arena) {
        PluginInstance pli = MinigamesAPI.getAPI().getPluginInstance(plugin);
        BungeeSocket.sendSignUpdate(pli, arena);
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        try {
            out.writeUTF("Forward");
            out.writeUTF("ALL");
            out.writeUTF("MinigamesLib");

            ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
            DataOutputStream msgout = new DataOutputStream(msgbytes);
            msgout.writeUTF(minigame + ":" + arena.getInternalName() + ":" + arena.getArenaState().toString() + ":" + Integer.toString(arena.getAllPlayers().size()) + ":" + Integer.toString(arena.getMaxPlayers()));

            out.writeShort(msgbytes.toByteArray().length);
            out.write(msgbytes.toByteArray());

            Bukkit.getServer().sendPluginMessage(MinigamesAPI.getAPI(), "BungeeCord", out.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
