package com.comze_instancelabs.minigamesapi;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.comze_instancelabs.minigamesapi.arcade.ArcadeInstance;
import com.comze_instancelabs.minigamesapi.util.BungeeUtil;
import com.comze_instancelabs.minigamesapi.util.Util;
import com.comze_instancelabs.minigamesapi.util.Validator;

public class Arena {

	// Plugin the arena belongs to
	JavaPlugin plugin;
	PluginInstance pli;
	private ArcadeInstance ai;

	private ArrayList<Location> spawns = new ArrayList<Location>();
	HashMap<String, ItemStack[]> pinv = new HashMap<String, ItemStack[]>();
	HashMap<String, ItemStack[]> pinv_armor = new HashMap<String, ItemStack[]>();
	private HashMap<String, GameMode> pgamemode = new HashMap<String, GameMode>();

	private Location mainlobby;
	private Location waitinglobby;
	private Location signloc;

	private int max_players;
	private int min_players;

	private boolean viparena;
	private String permission_node;

	private ArrayList<String> players = new ArrayList<String>();

	private ArenaType type = ArenaType.DEFAULT;
	private ArenaState currentstate = ArenaState.JOIN;
	String name = "mainarena";

	private boolean shouldClearInventoryOnJoin = true;

	private Arena currentarena;

	boolean started = false;

	private boolean showArenascoreboard = true;

	SmartReset sr = null;

	/**
	 * Creates a normal singlespawn arena
	 * 
	 * @param plugin
	 *            JavaPlugin the arena belongs to
	 * @param name
	 *            name of the arena
	 */
	public Arena(JavaPlugin plugin, String name) {
		currentarena = this;
		this.plugin = plugin;
		this.name = name;
		sr = new SmartReset(this);
		this.pli = MinigamesAPI.getAPI().pinstances.get(plugin);
	}

	/**
	 * Creates an arena of given arenatype
	 * 
	 * @param name
	 *            name of the arena
	 * @param type
	 *            arena type
	 */
	public Arena(JavaPlugin plugin, String name, ArenaType type) {
		this(plugin, name);
		this.type = type;
	}

	// This is for loading existing arenas
	public void init(Location signloc, ArrayList<Location> spawns, Location mainlobby, Location waitinglobby, int max_players, int min_players, boolean viparena) {
		this.signloc = signloc;
		this.spawns = spawns;
		this.mainlobby = mainlobby;
		this.waitinglobby = waitinglobby;
		this.viparena = viparena;
		this.min_players = min_players;
		this.max_players = max_players;
		this.showArenascoreboard = pli.arenaSetup.getShowScoreboard(plugin, this.getName());
	}

	// This is for loading existing arenas
	public Arena initArena(Location signloc, ArrayList<Location> spawn, Location mainlobby, Location waitinglobby, int max_players, int min_players, boolean viparena) {
		this.init(signloc, spawn, mainlobby, waitinglobby, max_players, min_players, viparena);
		return this;
	}

	public Arena getArena() {
		return this;
	}

	public SmartReset getSmartReset() {
		return this.sr;
	}

	public boolean getShowScoreboard() {
		return this.showArenascoreboard;
	}

	public Location getSignLocation() {
		return this.signloc;
	}

	public void setSignLocation(Location l) {
		this.signloc = l;
	}

	public ArrayList<Location> getSpawns() {
		return this.spawns;
	}

	public String getName() {
		return name;
	}

	public int getMaxPlayers() {
		return this.max_players;
	}

	public int getMinPlayers() {
		return this.min_players;
	}

	public void setMinPlayers(int i) {
		this.min_players = i;
	}

	public void setMaxPlayers(int i) {
		this.max_players = i;
	}

	public boolean isVIPArena() {
		return this.viparena;
	}

	public void setVIPArena(boolean t) {
		this.viparena = t;
	}

	public ArrayList<String> getAllPlayers() {
		return this.players;
	}

	public boolean containsPlayer(String playername) {
		return players.contains(playername);
	}

	public ArenaState getArenaState() {
		return this.currentstate;
	}

	public void setArenaState(ArenaState s) {
		this.currentstate = s;
	}

	public ArenaType getArenaType() {
		return this.type;
	}

	/**
	 * Joins the waiting lobby of an arena
	 * 
	 * @param playername
	 *            the playername
	 */
	public void joinPlayerLobby(String playername) {
		if (this.getArenaState() != ArenaState.JOIN && this.getArenaState() != ArenaState.STARTING) {
			// arena ingame or restarting
			return;
		}
		if (!pli.arenaSetup.getArenaEnabled(plugin, this.getName())) {
			Bukkit.getPlayer(playername).sendMessage(pli.getMessagesConfig().arena_disabled);
			return;
		}
		if (ai == null && this.isVIPArena()) {
			if (Validator.isPlayerOnline(playername)) {
				if (!Bukkit.getPlayer(playername).hasPermission("arenas." + this.getName())) {
					Bukkit.getPlayer(playername).sendMessage(pli.getMessagesConfig().no_perm_to_join_arena.replaceAll("<arena>", this.getName()));
					return;
				}
			}
		}
		if (this.getAllPlayers().size() > this.max_players - 1) {
			// arena full
			return;
		}
		if (this.getAllPlayers().size() == this.max_players - 1) {
			if (currentlobbycount > 16 && this.getArenaState() == ArenaState.STARTING) {
				currentlobbycount = 16;
			}
		}
		pli.global_players.put(playername, this);
		this.players.add(playername);
		if (Validator.isPlayerValid(plugin, playername, this)) {
			final Player p = Bukkit.getPlayer(playername);
			p.sendMessage(pli.getMessagesConfig().you_joined_arena.replaceAll("<arena>", this.getName()));
			for (String p_ : this.getAllPlayers()) {
				if (Validator.isPlayerOnline(p_) && !p_.equalsIgnoreCase(p.getName())) {
					Player p__ = Bukkit.getPlayer(p_);
					int count = this.getAllPlayers().size();
					int maxcount = this.getMaxPlayers();
					p__.sendMessage(pli.getMessagesConfig().broadcast_player_joined.replaceAll("<player>", p.getName()).replace("<count>", Integer.toString(count)).replace("<maxcount>", Integer.toString(maxcount)));
				}
			}
			Util.updateSign(plugin, this);
			if (shouldClearInventoryOnJoin) {
				pinv.put(playername, p.getInventory().getContents());
				pinv_armor.put(playername, p.getInventory().getArmorContents());
				if (this.getArenaType() == ArenaType.JUMPNRUN) {
					Util.teleportPlayerFixed(p, this.spawns.get(0));
					return;
				} else {
					Util.teleportPlayerFixed(p, this.waitinglobby);
				}
				Bukkit.getScheduler().runTaskLater(MinigamesAPI.getAPI(), new Runnable() {
					public void run() {
						Util.clearInv(p);
						ItemStack classes_item = new ItemStack(plugin.getConfig().getInt("config.classes_selection_item"));
						ItemMeta cimeta = classes_item.getItemMeta();
						cimeta.setDisplayName("Classes");
						classes_item.setItemMeta(cimeta);
						p.getInventory().addItem(classes_item);
						p.updateInventory();
						pgamemode.put(p.getName(), p.getGameMode());
						p.setGameMode(GameMode.SURVIVAL);
					}
				}, 10L);
				if (this.getAllPlayers().size() > this.min_players - 1) {
					this.startLobby(true);
				}
			}
		}
	}

	/**
	 * Primarily used for ArcadeInstance to join a waiting lobby without countdown
	 * 
	 * @param playername
	 * @param countdown
	 */
	public void joinPlayerLobby(String playername, boolean countdown) {
		if (this.getArenaState() != ArenaState.JOIN && this.getArenaState() != ArenaState.STARTING) {
			return;
		}
		if (this.getAllPlayers().size() > this.max_players - 1) {
			return;
		}
		pli.global_players.put(playername, this);
		this.players.add(playername);
		if (Validator.isPlayerValid(plugin, playername, this)) {
			final Player p = Bukkit.getPlayer(playername);
			// TODO possibly remove join message in arcade?
			p.sendMessage(pli.getMessagesConfig().you_joined_arena.replaceAll("<arena>", this.getName()));
			Util.updateSign(plugin, this);
			if (shouldClearInventoryOnJoin) {
				pinv.put(playername, p.getInventory().getContents());
				pinv_armor.put(playername, p.getInventory().getArmorContents());
				if (this.getArenaType() == ArenaType.JUMPNRUN) {
					Util.teleportPlayerFixed(p, this.spawns.get(0));
					return;
				} else {
					Util.teleportPlayerFixed(p, this.waitinglobby);
				}
				Bukkit.getScheduler().runTaskLater(MinigamesAPI.getAPI(), new Runnable() {
					public void run() {
						Util.clearInv(p);
						ItemStack classes_item = new ItemStack(plugin.getConfig().getInt("config.classes_selection_item"));
						ItemMeta cimeta = classes_item.getItemMeta();
						cimeta.setDisplayName("Classes");
						classes_item.setItemMeta(cimeta);
						p.getInventory().addItem(classes_item);
						p.updateInventory();
						pgamemode.put(p.getName(), p.getGameMode());
						p.setGameMode(GameMode.SURVIVAL);
					}
				}, 10L);
				if (this.getAllPlayers().size() > this.min_players - 1) {
					this.startLobby(countdown);
				}
			}
		}
	}

	/**
	 * Joins the waiting lobby of an arena
	 * 
	 * @param playername
	 *            the playername
	 * @param ai
	 *            the ArcadeInstance
	 */
	public void joinPlayerLobby(String playername, ArcadeInstance ai) {
		this.ai = ai;
		joinPlayerLobby(playername, false); // join playerlobby without lobby countdown
	}

	/**
	 * Leaves the current arena, won't do anything if not present in any arena
	 * 
	 * @param playername
	 * @param fullLeave
	 *            Determines if player left only minigame or the server
	 */
	public void leavePlayer(final String playername, boolean fullLeave) {
		this.leavePlayerRaw(playername, fullLeave);
	}

	public void leavePlayer(final String playername, boolean fullLeave, boolean endofGame) {
		this.leavePlayer(playername, fullLeave);

		if (!endofGame) {
			if (this.getAllPlayers().size() < 2) {
				this.stop();
			}
		}
	}

	public void leavePlayerRaw(final String playername, boolean fullLeave) {
		if (!this.containsPlayer(playername)) {
			return;
		}
		this.players.remove(playername);
		pli.global_players.remove(playername);
		if (fullLeave) {
			plugin.getConfig().set("temp.left_players." + playername + ".name", playername);
			plugin.getConfig().set("temp.left_players." + playername + ".plugin", plugin.getName());
			for (ItemStack i : pinv.get(playername)) {
				if (i != null) {
					plugin.getConfig().set("temp.left_players." + playername + ".items." + Integer.toString((int) Math.round(Math.random() * 10000)) + i.getType().toString(), i);
				}
			}
			plugin.saveConfig();
			return;
		}
		final Player p = Bukkit.getPlayer(playername);
		Util.clearInv(p);
		p.setWalkSpeed(0.2F);
		p.setFoodLevel(20);
		p.setHealth(20D);
		p.removePotionEffect(PotionEffectType.JUMP);

		for (Entity e : p.getNearbyEntities(50D, 50D, 50D)) {
			if (e.getType() == EntityType.DROPPED_ITEM) {
				e.remove();
			}
		}

		if (started) {
			pli.getRewardsInstance().giveWinReward(playername);
		}

		pli.global_players.remove(playername);
		if (pli.global_lost.containsKey(playername)) {
			pli.global_lost.remove(playername);
		}

		Util.updateSign(plugin, this);

		final String arenaname = this.getName();
		final Arena a = this;
		Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
			public void run() {
				Util.teleportPlayerFixed(p, a.mainlobby);
				p.setFlying(false);
				if (!p.isOp()) {
					p.setAllowFlight(false);
				}
				if (pgamemode.containsKey(p.getName())) {
					p.setGameMode(pgamemode.get(p.getName()));
				}
				p.getInventory().setContents(pinv.get(playername));
				p.getInventory().setArmorContents(pinv_armor.get(playername));
				p.updateInventory();
				try {
					pli.scoreboardManager.removeScoreboard(arenaname, p);
				} catch (Exception e) {
					//
				}
			}
		}, 5L);
	}

	/**
	 * Spectates the game
	 * 
	 * @param playername
	 *            the playername
	 */
	public void spectate(String playername) {
		if (Validator.isPlayerValid(plugin, playername, this)) {
			Player p = Bukkit.getPlayer(playername);
			if (!plugin.getConfig().getBoolean("config.spectator_after_fall_or_death")) {
				this.leavePlayer(playername, false, true);
			}
			Util.clearInv(p);
			pli.global_lost.put(playername, this);
			p.setAllowFlight(true);
			p.setFlying(true);
			pli.scoreboardManager.updateScoreboard(plugin, this);
			if (this.getPlayerAlive() < 2) {
				final Arena a = this;
				Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
					public void run() {
						a.stop();
					}
				}, 20L);
			} else {
				Location temp = this.spawns.get(0);
				Util.teleportPlayerFixed(p, temp.clone().add(0D, 30D, 0D));
			}
		}
	}

	int currentlobbycount = 10;
	int currentingamecount = 10;
	int currenttaskid = 0;

	public void setTaskId(int id) {
		this.currenttaskid = id;
	}

	public int getTaskId() {
		return this.currenttaskid;
	}

	/**
	 * Starts the lobby countdown and the arena afterwards
	 * 
	 * You can insta-start an arena by using Arena.start();
	 */
	public void startLobby() {
		startLobby(true);
	}

	public void startLobby(boolean countdown) {
		if (currentstate != ArenaState.JOIN) {
			return;
		}
		this.setArenaState(ArenaState.STARTING);
		currentlobbycount = pli.lobby_countdown;
		final Arena a = this;

		// skip countdown
		if (!countdown) {
			Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
				public void run() {
					currentarena.getArena().start(true);
				}
			}, 10L);
		}

		currenttaskid = Bukkit.getScheduler().runTaskTimer(MinigamesAPI.getAPI(), new Runnable() {
			public void run() {
				currentlobbycount--;
				if (currentlobbycount == 60 || currentlobbycount == 30 || currentlobbycount == 15 || currentlobbycount == 10 || currentlobbycount < 6) {
					for (String p_ : a.getAllPlayers()) {
						if (Validator.isPlayerOnline(p_)) {
							Player p = Bukkit.getPlayer(p_);
							p.sendMessage(pli.getMessagesConfig().teleporting_to_arena_in.replaceAll("<count>", Integer.toString(currentlobbycount)));
						}
					}
				}
				if (currentlobbycount < 1) {
					Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
						public void run() {
							currentarena.getArena().start(true);
						}
					}, 10L);
					try {
						Bukkit.getScheduler().cancelTask(currenttaskid);
					} catch (Exception e) {
					}
				}
			}
		}, 5L, 20).getTaskId();
	}

	/**
	 * Instantly starts the arena, teleports players and udpates the arena
	 */
	public void start(boolean tp) {
		try {
			Bukkit.getScheduler().cancelTask(currenttaskid);
		} catch (Exception e) {
		}
		currentingamecount = pli.ingame_countdown;
		for (String p_ : currentarena.getArena().getAllPlayers()) {
			Player p = Bukkit.getPlayer(p_);
			p.setWalkSpeed(0.0F);
			p.setFoodLevel(5);
			p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 9999999, -7)); // -5
		}
		if (tp) {
			Util.teleportAllPlayers(currentarena.getArena().getAllPlayers(), currentarena.getArena().spawns);
		}
		final Arena a = this;
		pli.scoreboardManager.updateScoreboard(plugin, a);
		currenttaskid = Bukkit.getScheduler().runTaskTimer(MinigamesAPI.getAPI(), new Runnable() {
			public void run() {
				currentingamecount--;
				if (currentingamecount == 60 || currentingamecount == 30 || currentingamecount == 15 || currentingamecount == 10 || currentingamecount < 6) {
					for (String p_ : a.getAllPlayers()) {
						if (Validator.isPlayerOnline(p_)) {
							Player p = Bukkit.getPlayer(p_);
							p.sendMessage(pli.getMessagesConfig().starting_in.replaceAll("<count>", Integer.toString(currentingamecount)));
						}
					}
				}
				if (currentingamecount < 1) {
					currentarena.getArena().setArenaState(ArenaState.INGAME);
					Util.updateSign(plugin, a);
					for (String p_ : a.getAllPlayers()) {
						if (!pli.getClassesHandler().hasClass(p_)) {
							pli.getClassesHandler().setClass("default", p_);
						}
						pli.getClassesHandler().getClass(p_);
						Player p = Bukkit.getPlayer(p_);
						p.setWalkSpeed(0.2F);
						p.setFoodLevel(20);
						p.removePotionEffect(PotionEffectType.JUMP);
					}
					started = true;
					started();
					try {
						Bukkit.getScheduler().cancelTask(currenttaskid);
					} catch (Exception e) {
					}
				}
			}
		}, 5L, 20).getTaskId();
	}

	/**
	 * Gets executed after an arena started (after lobby countdown)
	 */
	public void started() {
		System.out.println(this.getName() + " started.");
	}

	/**
	 * Stops the arena and teleports all players to the mainlobby
	 */
	public void stop() {
		final Arena a = this;
		try {
			Bukkit.getScheduler().cancelTask(currenttaskid);
		} catch (Exception e) {

		}

		this.setArenaState(ArenaState.RESTARTING);

		ArrayList<String> temp = new ArrayList<String>(this.getAllPlayers());
		for (String p_ : temp) {
			leavePlayer(p_, false, true);
		}

		if (a.getArenaType() == ArenaType.REGENERATION) {
			reset();
		} else {
			a.setArenaState(ArenaState.JOIN);
			Util.updateSign(plugin, a);
		}

		Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
			public void run() {
				players.clear();
				pinv.clear();
				pinv_armor.clear();
			}
		}, 10L);

		started = false;

		/*
		 * try { pli.getStatsInstance().updateSkulls(); } catch (Exception e) {
		 * 
		 * }
		 */

		if (plugin.getConfig().getBoolean("config.execute_cmds_on_stop")) {
			String[] cmds = plugin.getConfig().getString("config.cmds").split(";");
			for (String cmd : cmds) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
			}
		}

		if (plugin.getConfig().getBoolean("config.bungee.teleport_all_to_server_on_stop.tp")) {
			final String server = plugin.getConfig().getString("config.bungee.teleport_all_to_server_on_stop.server");
			Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
				public void run() {
					for (Player p : Bukkit.getOnlinePlayers()) {
						BungeeUtil.connectToServer(MinigamesAPI.getAPI(), p.getName(), server);
					}
				}
			}, 30L);
			return;
		}

		Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
			public void run() {
				if (ai != null) {
					ai.nextMinigame();
					ai = null;
				}
			}
		}, 10L);
	}

	/**
	 * Rebuilds an arena from file (only for arenas of REGENERATION type)
	 */
	public void reset() {
		/*
		 * Runnable r = new Runnable() { public void run() { Util.loadArenaFromFileSYNC(plugin, currentarena); } }; new Thread(r).start();
		 */
		sr.reset();
		/*
		 * Bukkit.getScheduler().runTask(plugin, new Runnable() { public void run() { // Util.loadArenaFromFileSYNC(plugin, currentarena); sr.reset();
		 * } });
		 */
	}

	public String getPlayerCount() {
		int alive = 0;
		for (String p_ : getAllPlayers()) {
			if (pli.global_lost.containsKey(p_)) {
				continue;
			} else {
				alive++;
			}
		}
		return Integer.toString(alive) + "/" + Integer.toString(getAllPlayers().size());
	}

	public int getPlayerAlive() {
		int alive = 0;
		for (String p_ : getAllPlayers()) {
			if (pli.global_lost.containsKey(p_)) {
				continue;
			} else {
				alive++;
			}
		}
		return alive;
	}

	public Location getWaitingLobbyTemp() {
		return this.waitinglobby;
	}

	public Location getMainLobbyTemp() {
		return this.mainlobby;
	}

	public ArcadeInstance getArcadeInstance() {
		return ai;
	}

}
