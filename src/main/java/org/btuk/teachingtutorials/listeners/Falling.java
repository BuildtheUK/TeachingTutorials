package org.btuk.teachingtutorials.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.btuk.teachingtutorials.TeachingTutorials;

/**
 * A Listener designed to catch people when they fall into the void and teleport them back to a safe location. It will
 * detect if they move below a certain elevation and if so, teleports them back to the safe location.
 */
public class Falling implements Listener
{
    /** The player to listen for falling for */
    private final Player player;

    /** A safe location to teleport the player back to */
    private Location safeLocation;

    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /**
     * Used when initialising the object in a New Location. In this scenario, the start location that the player defines
     * can be used as the safe location
     * @param player The player to listen for falling for
     * @param safeLocation A Bukkit location object containing the coordinates of the safe location
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     */
    public Falling(Player player, Location safeLocation, TeachingTutorials plugin)
    {
        this.player = player;
        this.safeLocation = safeLocation;
        this.plugin = plugin;
    }

    /**
     * Used when initialising the object in a lesson. When the first step starts the safe location will automatically be
     * set. It will not have an absent start location for any playable period of time.
     * @param player The player to listen for falling for
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     */
    public Falling (Player player, TeachingTutorials plugin)
    {
        this.player = player;
        this.plugin = plugin;
    }

    /**
     * Updates the safe location to the location specified
     * @param location A location to set as the safe location
     */
    public void setSafeLocation(Location location)
    {
        this.safeLocation = location;
    }

    /**
     * Detects when players fall below a certain Y level and teleports them to the safe location if the do so
     * @param event A player move event
     */
    @EventHandler
    public void onFall(PlayerMoveEvent event)
    {
        if (!event.getPlayer().equals(player))
        {
            //If it's not the correct player then do nothing. Must be the correct player because
            // the safeLocation depends on the player
        }
        else if (event.getTo().getY() < plugin.getConfig().getInt("Min_Y"))
        {
            teleportToSafeLocation();
        }
    }

    /**
     * Teleports the player to the safe location
     */
    public void teleportToSafeLocation()
    {
        player.teleport(safeLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    /**
     * Registers the event with the server listeners
     */
    public void register()
    {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Unregisters the event with the server listeners
     */
    public void unregister()
    {
        HandlerList.unregisterAll(this);
    }
}