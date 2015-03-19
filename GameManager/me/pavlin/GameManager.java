package me.pavlin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;





import static me.pavlin.Constants.*;

public class GameManager extends JavaPlugin {
	
	Plugin plugin = this;
	
	private MySQL dbInfo = new MySQL(plugin, "localhost", "3306", "splegg", "root", "hyp123");
	
	public static Connection db = null;
	
	public boolean tryConnection() {
		try {
			db = dbInfo.openConnection();
			return true;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public void loadConfig() {
		
		// Load each signs default locations, change this to MySQL
		for (int i = 1; i < 4; i++) {
			String[] coords = {"x", "y", "z"};
			
			for (int j = 0; j < 3; j++) {
				String path = "signs." + i + "." + coords[j];
				getConfig().addDefault(path, "");
			}
		}
		getConfig().options().copyDefaults(true);
		saveConfig();
	}
	
	@Override
	public void onEnable() {
		getLogger().info(PLUGIN_NAME + " is enabled!");
		
		// Load MySQL
		if (!tryConnection()) {
			getLogger().severe("Could not connect to MySQL, quitting...");
			Bukkit.getPluginManager().disablePlugin(this);
		} else {
			getLogger().info("Successfully connected to MySQL!");
		}
		
		// Load Sign Config file
		loadConfig();
		
		// Update lobby signs
		SignManager.getInstance().setPlugin(plugin);
		SignManager.getInstance().updateSigns();
		
		// Listen for events
		getServer().getPluginManager().registerEvents(new Events(plugin), this);

	}
	

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (cmd.getName().equalsIgnoreCase("lobby")) {
			Player player = (Player) sender;
			Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");

			ByteArrayOutputStream b = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(b);

			try {
				out.writeUTF("Connect");
				out.writeUTF("lobby");
			} catch (IOException ex) {

			}

			player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
			return true;
		}
		return false; 
	}
	
	

}
