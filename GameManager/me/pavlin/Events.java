package me.pavlin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import static me.pavlin.Constants.*;

public class Events implements Listener {
	
	private Plugin plugin;
	
	private enum JoinableState { WAITING };
	
	public Events(Plugin plugin) {
		this.plugin = plugin;
	}
	
	// Check if the player clicked a game sign, and teleport accordingly
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.WALL_SIGN) {		
			plugin.getLogger().info("Checking the match...");
			Sign sign = (Sign) event.getClickedBlock().getState();
			String line = sign.getLine(0);
			line = ChatColor.stripColor(line);
			
			int id = SignManager.getId(line);
			
			plugin.getLogger().info(line);
			
			plugin.getLogger().info("Got id " + id + " state " + SignManager.getGameState(id) + " enabled " + SignManager.isEnabled(id));
			
			if (!SignManager.isEnabled(id)) {
				event.getPlayer().sendMessage(PLUGIN_PREFIX + " " + ChatColor.RED + ChatColor.BOLD + "ERROR: "+ ChatColor.RESET + ChatColor.YELLOW + SignManager.getServerColumn("bungeecord_name", id) + " is not enabled.");
			}
			
			
			String gameState = SignManager.getGameState(id);
			// Make sure we can send the player there
			for (JoinableState state: JoinableState.values()) {
				plugin.getLogger().info(state.toString());
				if (state.toString().equalsIgnoreCase(gameState)) {
					movePlayerToServer(event.getPlayer(), id);
				} else {
					event.getPlayer().sendMessage(PLUGIN_PREFIX + " " + ChatColor.RED + ChatColor.BOLD + "ERROR: " + ChatColor.RESET + ChatColor.YELLOW + SignManager.getServerColumn("bungeecord_name", id) + " is not currently accepting players.");
				}
			}
			
		}
	}
	
	@EventHandler
	public void onPlayerJoinEvent(PlayerJoinEvent event) {
		event.setJoinMessage("");
	}
	
	@EventHandler
	public void onPlayerExitEvent(PlayerQuitEvent event) {
		event.setQuitMessage("");
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		
		if (!event.getPlayer().isOp()) {
			event.setCancelled(true);
		}
	}
	
	public void movePlayerToServer(Player player, int id) {
		// Assuming the servers are named like the constant is
		plugin.getLogger().info("/server " + SignManager.getServerColumn("bungeecord_name", id));
		// player.chat("/server " + PLUGIN_NAME.toLowerCase() + id);
		
		Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");

		ByteArrayOutputStream b = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(b);

		try {
			out.writeUTF("Connect");
			out.writeUTF(SignManager.getServerColumn("bungeecord_name", id));
		} catch (IOException ex) {

		}
		player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
	}
	
	

	
	

}
