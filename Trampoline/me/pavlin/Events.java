package me.pavlin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import me.pavlin.Arena.State;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.material.Wool;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import static me.pavlin.Constants.*;

public class Events implements Listener {
	
	private Plugin plugin;
	
	private enum JoinableState { WAITING };
	
	public Events(Plugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerJoinEvent(PlayerJoinEvent event) {
		event.setJoinMessage("");
		Player player = event.getPlayer();
		
		if (!Arena.isDuplicate(player)) {
			Arena.addPlayer(player);
		}
		
		player.sendMessage(PLUGIN_PREFIX + ChatColor.YELLOW + " Welcome to " + Arena.getServerColumn("bungeecord_name"));
		
		Bukkit.broadcastMessage(PLUGIN_PREFIX + " " + ChatColor.GRAY + player.getName() + ChatColor.YELLOW + " has joined " + "(" + ChatColor.GREEN + Arena.getPlayerAmount() + ChatColor.YELLOW + "/" + ChatColor.GREEN + Arena.getMaxPlayers() + ChatColor.YELLOW + ")!");
	}
	
	@EventHandler
	public void onPlayerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		
		Arena.removePlayer(player);
		
		Bukkit.broadcastMessage(PLUGIN_PREFIX + " " + ChatColor.GRAY + player.getName() + ChatColor.YELLOW + " has left");
		event.setQuitMessage("");
	}
	
	@EventHandler
	public void onPlayerDrop(PlayerDropItemEvent event) {
		event.setCancelled(true);
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Action action = event.getAction();
		Player player = event.getPlayer();
		
		if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) && event.getPlayer().getItemInHand().getType().equals(Material.IRON_SPADE)) {
			plugin.getLogger().info("THROWING EGGS");
			Location loc = player.getEyeLocation().toVector().add(player.getLocation().getDirection().multiply(2)).toLocation(player.getWorld(), player.getLocation().getYaw(), player.getLocation().getPitch());
			Egg egg = player.getWorld().spawn(loc, Egg.class);
			egg.setShooter(player);
			egg.setVelocity(player.getEyeLocation().getDirection().multiply(2));
		}
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		
		event.getEntity().setHealth(20);
		
		// Match is still on, player is a spectator
		Arena.removePlayer(event.getEntity());
		Arena.addSpectator(event.getEntity());
		
		
		// Game is over, teleport players back to lobby
		if (Arena.getPlayerAmount() == 1) {
			Arena.currentState = State.RESETTING;
			Arena.endGameTasks();
		}
		
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		if (Arena.currentState == Arena.State.IN_GAME && !Arena.isSpectator(event.getPlayer()) 
				&& event.getPlayer().getVelocity().getX() == 0
				&& event.getPlayer().getVelocity().getY() == 0
				&& event.getPlayer().getVelocity().getZ() == 0) {
			event.getPlayer().setVelocity(new Vector(0, 1.5, 0));
		}
	}
	
	@EventHandler
	public void onFoodChange(FoodLevelChangeEvent event) {
		event.setCancelled(true);
	}
	
	@EventHandler
	public void onPlayerDamage(EntityDamageEvent event) {
		if(event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			
			if(event.getCause() == DamageCause.FALL && Arena.currentState == Arena.State.IN_GAME){
				Location loc = player.getLocation();
				Location impactLoc = new Location(player.getWorld(), loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());
				List<Block> impactBlocks = getNearbyBlocks(impactLoc, 1);
				
				// Create an explosion like effect on impact
				for (Block block : impactBlocks) {
					byte woolByte = block.getData();
					Arena.addBrokenBlock(block, DyeColor.getByData(woolByte));
					block.breakNaturally();
				}
				
				player.setVelocity(new Vector(0, 1.5, 0));
				
				event.setCancelled(true);
			}
		}
	}
	
	/*
	 * @author The_Doctor_123
	 */
    public List<Block> getNearbyBlocks(Location location, int Radius)
    {
    	List<Block> Blocks = new ArrayList<Block>();

    	for (int X = location.getBlockX() - Radius ; X <= location.getBlockX() + Radius ; X++)
    	{
    		for (int Y = location.getBlockY() - Radius ; Y <= location.getBlockY() + Radius ; Y++)
    		{
    			for (int Z = location.getBlockZ() - Radius ; Z <= location.getBlockZ() + Radius ; Z++)
    			{
    				Block block = location.getWorld().getBlockAt(X, Y, Z);
    				if (!block.isEmpty())
    				{
    					Blocks.add(block);
    				}
    			}
    		}
    	}

        return Blocks;
    }
	

	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		
		if (Arena.isSpectator(event.getPlayer())) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(PLUGIN_PREFIX + ChatColor.YELLOW + " You may not break blocks right now!");
		}
		
		if (!event.getPlayer().isOp()) {
			if (Arena.currentState != Arena.State.IN_GAME) {
				event.setCancelled(true);
				event.getPlayer().sendMessage(PLUGIN_PREFIX + ChatColor.YELLOW + " You may not break blocks right now!");
			}
		}
		
		Block block = event.getBlock();
		byte woolByte = block.getData();
		
		Arena.addBrokenBlock(block, DyeColor.getByData(woolByte));
	}

	
	@EventHandler
	public void onEntitySpawn(EntitySpawnEvent event) {
		// Dont want the chickens spawning :D
		event.setCancelled(true);
	}

	@EventHandler
	public void onProjectileHit(ProjectileHitEvent event) {
		if (event.getEntity() instanceof Egg) {
			Egg egg = (Egg) event.getEntity();
			if (egg.getShooter() instanceof Player) {
				BlockIterator iterator = new BlockIterator(event.getEntity().getWorld(), event.getEntity().getLocation().toVector(), event.getEntity().getVelocity().normalize(), 0.0D, 4);
				Block hitBlock = null;

				while (iterator.hasNext()) {
					hitBlock = iterator.next();

					// The egg has hit a block
					if (hitBlock.getTypeId() != 0) {
						break;
					}
				}

				// Assuming the arena is made entirely of wool
				if (hitBlock.getType() == Material.WOOL) {
					hitBlock.getWorld().playEffect(hitBlock.getLocation(), Effect.STEP_SOUND, hitBlock.getTypeId());
					Arena.addBrokenBlock(hitBlock, DyeColor.getByData(hitBlock.getData()));
					hitBlock.setType(Material.AIR);
				}
			}
		}
	}


	
	
	

	
	

}
