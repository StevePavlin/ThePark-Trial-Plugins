package me.pavlin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Wool;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import static me.pavlin.Constants.*;

public class Arena {
	
	private static Plugin plugin;
	
	private static int id;
	private static ArrayList<Player> players = new ArrayList<Player>();
	private static ArrayList<Player> spectators = new ArrayList<Player>();
	private static HashMap<Block, DyeColor> brokenBlocks = new HashMap<Block, DyeColor>();
	
	private static Location minSpawn;
	private static Location maxSpawn;
	
	// SQL
	private static int maxPlayers;
	
	private static HashMap<String, Integer> runningTasks = new HashMap<String, Integer>();
	
	public static enum State {WAITING, IN_GAME, RESETTING}
	public static State currentState;
	
	public Arena(Plugin plugin) {
		this.plugin = plugin;
		this.id = Integer.parseInt(plugin.getConfig().getString("serverid"));
		this.minSpawn = new Location(Bukkit.getWorld("world"), Integer.parseInt(plugin.getConfig().getString("arena.spawn.min.x")), Integer.parseInt(plugin.getConfig().getString("arena.spawn.min.y")), Integer.parseInt(plugin.getConfig().getString("arena.spawn.min.z")));
		this.maxSpawn = new Location(Bukkit.getWorld("world"), Integer.parseInt(plugin.getConfig().getString("arena.spawn.max.x")), Integer.parseInt(plugin.getConfig().getString("arena.spawn.max.y")), Integer.parseInt(plugin.getConfig().getString("arena.spawn.max.z")));
		plugin.getLogger().info("Got id = " + id);
	}
	
	public static void start() {
		// Load up this arenas info from mysql
		maxPlayers = Integer.parseInt(getServerColumn("max_players"));
		plugin.getLogger().info("Maxplayers = " + maxPlayers);
		waitForPlayers();
		updateDatabase();
	}
	
	
	public static void reset() {
		for (Block b : brokenBlocks.keySet()) {
			b.setType(Material.WOOL);
			BlockState bs = b.getState();
			Wool wool = (Wool) bs.getData();
			wool.setColor(brokenBlocks.get(b));
			bs.setData(wool);
			bs.update();

		}
		
	}
	
	public static void updateDatabase() {
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		runningTasks.put("databaseTasks", scheduler.scheduleSyncRepeatingTask(plugin, new Runnable() {
			int time = 10;
			@Override
			public void run() {
				updateServerColumn("players", String.valueOf(getPlayerAmount()));
				updateServerColumn("state", String.valueOf(currentState));
				
			
			}
			
		}, 0L, 60L));
	}
	
	public static void waitForPlayers() {
		currentState = State.WAITING;
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		runningTasks.put("waitForPlayers", scheduler.scheduleSyncRepeatingTask(plugin, new Runnable() {
			@Override
			public void run() {
				if (getPlayerAmount() >= PLAYERS_TO_START) {
					// Cancel task, start countdown
					Bukkit.getScheduler().cancelTask(runningTasks.get("waitForPlayers"));
					countdown();
				}
				Bukkit.broadcastMessage(PLUGIN_PREFIX + ChatColor.YELLOW + " Waiting for " + (PLAYERS_TO_START - getPlayerAmount()) + " more player(s).");
				
			
			}
			
		}, 0L, 60L));
	}

	
	public static void countdown() {
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		runningTasks.put("countdown", scheduler.scheduleSyncRepeatingTask(plugin, new Runnable() {
			int time = 11;
			@Override
			public void run() {
                if (time > 0) {
                    time--;
                    switch (time) {
                    case 10:
                        Bukkit.broadcastMessage(PLUGIN_PREFIX + ChatColor.YELLOW + " 10 seconds until the game begins!");
                        break;
                    case 5:
                        Bukkit.broadcastMessage(PLUGIN_PREFIX + ChatColor.YELLOW + " 5 seconds until the game begins!");
                        break;
                    case 4:
                    	Bukkit.broadcastMessage(PLUGIN_PREFIX + ChatColor.YELLOW + " 4!");
                    	break;
                    case 3:
                        Bukkit.broadcastMessage(PLUGIN_PREFIX + ChatColor.YELLOW + " 3!");
                        break;
                    case 2:
                        Bukkit.broadcastMessage(PLUGIN_PREFIX + ChatColor.YELLOW + " 2!");
                        break;
                    case 1:
                        Bukkit.broadcastMessage(PLUGIN_PREFIX + ChatColor.YELLOW + " 1!");
                        break;
                }
 
                } else {
                    Bukkit.broadcastMessage(PLUGIN_PREFIX + ChatColor.YELLOW + " Game starting!");
                    Bukkit.getScheduler().cancelTask(runningTasks.get("countdown"));
                    
                    // Spawn the players
                    spawnPlayers();
                }
			}
		}, 0L, 20L));
	}
	
	public static Location generateTeleportLocation() {
		Random rand = new Random();

	    int randomX = rand.nextInt(maxSpawn.getBlockX() - minSpawn.getBlockX()) + minSpawn.getBlockX();
	    int randomY = rand.nextInt(maxSpawn.getBlockY() - minSpawn.getBlockY()) + minSpawn.getBlockY();
	    int randomZ = rand.nextInt(maxSpawn.getBlockZ() - minSpawn.getBlockZ()) + minSpawn.getBlockZ();
	    
	    plugin.getLogger().info("Random loc x:" + randomX + " y:" + randomY + " z:" + randomZ);
	    
	    Location tpLoc = new Location(Bukkit.getWorld("world"), randomX, randomY, randomZ);
	    
	    return tpLoc;
	}
	
	public static void spawnPlayers() {
		currentState = State.IN_GAME;
		for (Player player : players) {
		    
		    player.teleport(generateTeleportLocation());
		    
		    // Give them a shovel to dig blocks and throw eggs
		    player.getInventory().addItem(new ItemStack(Material.IRON_SPADE));
		    
		    // Heal player
		    player.setHealth(20);
		    player.setExhaustion(10);
		    
		    
			
		}
	}
	
	public void cancelTask(int taskID) {
		Bukkit.getScheduler().cancelTask(taskID);
	}
	
	public static String getServerColumn(String column) {
		
		String value = null;
		Statement statement = null;
		ResultSet res = null;
		try {
			statement = Splegg.db.createStatement();
			res = statement.executeQuery("SELECT * FROM `servers` WHERE id = '" + id + "';");
			res.next();
			value = res.getString(column);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return value;
	}
	
	public static void updateServerColumn(String column, String val) {
		
		String value = null;
		Statement statement = null;
		ResultSet res = null;
		try {
			statement = Splegg.db.createStatement();
			statement.executeUpdate("UPDATE `servers` SET " + column + " = '" + val + "' WHERE id = '" + id + "'");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	public static void endGameTasks() {
		
		Bukkit.broadcastMessage(PLUGIN_PREFIX + ChatColor.GOLD + " " + players.get(0).getName() + " wins!");
		Bukkit.broadcastMessage(PLUGIN_PREFIX + ChatColor.GOLD + " You will be teleported back to the lobby in 10 seconds.");
		
		// Teleport to the spawn while the arena rebuilds and remove shovel
		for (Player player : Bukkit.getOnlinePlayers()) {
			player.teleport(Bukkit.getWorld("world").getSpawnLocation());
			player.getInventory().clear();
		}
		
		
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {

			@Override
			public void run() {
				updateServerColumn("players", String.valueOf(getPlayerAmount()));
				Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");

				ByteArrayOutputStream b = new ByteArrayOutputStream();
				DataOutputStream out = new DataOutputStream(b);

				try {
					out.writeUTF("Connect");
					out.writeUTF("lobby");
				} catch (IOException ex) {

				}
				
				// Now teleport all players to lobby
				
				for (Player player : Bukkit.getOnlinePlayers()) {
					player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
				}
				
			
			}
			
		}, 200L);
		
		scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {

			@Override
			public void run() {
				// Restart the arenas state
				start();
			
			}
			
		}, 300L);
		
		// Reset the arena
		reset();
		
		// Clear the lists
		players.clear();
		spectators.clear();
		brokenBlocks.clear();
		
		
		
	}
	
	public static int getPlayerAmount() {
		return players.size();
	}
	
	public static int getMaxPlayers() {
		return maxPlayers;
	}
	
	public static void addPlayer(Player player) {
		players.add(player);
		player.teleport(Bukkit.getWorld("world").getSpawnLocation());
	}
	
	public static void addBrokenBlock(Block block, DyeColor color) {
		brokenBlocks.put(block, color);
	}
	
	public static int brokenBlockSize() {
		return brokenBlocks.size();
	}
	
	public static void removePlayer(Player player) {
		players.remove(player);
	}
	
	public static void addSpectator(Player player) {
		spectators.add(player);
		player.sendMessage(PLUGIN_PREFIX + ChatColor.YELLOW + " You are now a spectator, you may type /server lobby to return back to the lobby at any time.");
		player.getInventory().clear();
		
		// Spawn somewhere random
		player.teleport(generateTeleportLocation());
		
		// Invisibility to players
		for (Player p : players) {
			p.hidePlayer(player);
		}
		
		// Flying
		player.setAllowFlight(true);
		player.setFlying(true);
		
	    
		
	}
	
	public static void removeSpectator(Player player) {
		spectators.remove(player);
	}
	
	public static boolean isDuplicate(Player player) {
		for (Player p : players) {
			if (p.getUniqueId().equals(player.getUniqueId())) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isSpectator(Player player) {
		for (Player p : spectators) {
			if (p.getUniqueId().equals(player.getUniqueId())) {
				return true;
			}
		}
		return false;
	}
	
	
	

}
