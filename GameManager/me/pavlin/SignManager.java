package me.pavlin;

import java.awt.List;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import static me.pavlin.Constants.*;

public class SignManager {
	
	private static SignManager instance = null;

	private Plugin plugin;
	private World world = Bukkit.getServer().getWorld("world");
	
	private SignManager() {}
	
	public void updateSigns() {
        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
            	plugin.getLogger().info("REPEATING");
                loadSigns();
            }
        }, 0L, 40L);
	}
	
	public void loadSigns() {
		
		// For database lookup
		int id = 1;
		
		for(String key : plugin.getConfig().getConfigurationSection("signs").getKeys(false)) {
			/* Debug
			/plugin.getLogger().info(plugin.getConfig().getString("signs." + key));
			plugin.getLogger().info(plugin.getConfig().getString("signs." + key + ".x"));
			plugin.getLogger().info(plugin.getConfig().getString("signs." + key + ".y"));
			plugin.getLogger().info(plugin.getConfig().getString("signs." + key + ".z"));
			*/
			
			int x = Integer.parseInt(plugin.getConfig().getString("signs." + key + ".x"));
			int y = Integer.parseInt(plugin.getConfig().getString("signs." + key + ".y"));
			int z = Integer.parseInt(plugin.getConfig().getString("signs." + key + ".z"));
			
			// Create a location
			Location blockLoc = new Location(world, x, y, z);
			
			// Add the block
			Block b = world.getBlockAt(blockLoc);		
			
			// We have the block, now draw the info about the server
			drawSign(b, id);
			
			
			id++;
		}
	}
	
	public void drawSign(Block b, int id) {
		if (b.getState() instanceof Sign) {
			Sign sign = (Sign) b.getState();
			sign.setLine(0, ChatColor.BLUE + getServerColumn("bungeecord_name", id));
			sign.setLine(1, "Players: " + getPlayers(id) + "/" + getMaxPlayers(id));
			sign.setLine(2, "State: " + getGameState(id));
			sign.update();
			getMaxPlayers(id);
			
		}
	}
	
	public static int getId(String bungeeName) {
		
		int id = -1;
		Statement statement = null;
		ResultSet res = null;
		try {
			statement = GameManager.db.createStatement();
			res = statement.executeQuery("SELECT * FROM `servers` WHERE bungeecord_name = '" + bungeeName + "';");
			res.next();
			//plugin.getLogger().info("" + res.getInt("max_players"));
			id = res.getInt("id");
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return id;
	}
	
	public int getMaxPlayers(int id) {
		
		int maxPlayers = -1;
		Statement statement = null;
		ResultSet res = null;
		try {
			statement = GameManager.db.createStatement();
			res = statement.executeQuery("SELECT * FROM `servers` WHERE id = '" + id + "';");
			res.next();
			//plugin.getLogger().info("" + res.getInt("max_players"));
			maxPlayers = res.getInt("max_players");
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return maxPlayers;
	}
	
	public int getPlayers(int id) {
		int players = -1;
		Statement statement = null;
		ResultSet res = null;
		try {
			statement = GameManager.db.createStatement();
			res = statement.executeQuery("SELECT * FROM `servers` WHERE id = '" + id + "';");
			res.next();
			//plugin.getLogger().info("" + res.getInt("players"));
			players = res.getInt("players");
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return players;
	}

	public static String getServerColumn(String column, int id) {

		String value = "";
		Statement statement = null;
		ResultSet res = null;
		try {
			statement = GameManager.db.createStatement();
			res = statement.executeQuery("SELECT * FROM `servers` WHERE id = '" + id + "';");
			res.next();
			//plugin.getLogger().info("" + res.getInt("players"));
			value = res.getString(column);

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return value;

	}
	
	public static String getGameState(int id) {
		String state = null;
		Statement statement = null;
		ResultSet res = null;
		try {
			statement = GameManager.db.createStatement();
			res = statement.executeQuery("SELECT * FROM `servers` WHERE id = '" + id + "';");
			res.next();
			//plugin.getLogger().info("" + res.getString("state"));
			state = res.getString("state");
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return state;
	}
	
	public static boolean isEnabled(int id) {
		
		boolean enabled = false;
		Statement statement = null;
		ResultSet res = null;
		try {
			statement = GameManager.db.createStatement();
			res = statement.executeQuery("SELECT * FROM `servers` WHERE id = '" + id + "';");
			res.next();
			//plugin.getLogger().info("" + res.getInt("max_players"));
			enabled = res.getBoolean("enabled");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return enabled;
	}
	
	
	
	public static SignManager getInstance() {
		if(instance == null) {
			instance = new SignManager();
		}
		return instance;
	}
	
	public void setPlugin(Plugin plugin) {
		this.plugin = plugin;
	}

}
