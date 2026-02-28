package org.btuk.teachingtutorials.tutorialplaythrough;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.btuk.teachingtutorials.TeachingTutorials;
import org.btuk.teachingtutorials.tutorialobjects.LocationStep;

/**
    Listens out for /link when a tutorial is being played
 */
public class VideoLinkCommandListener implements Listener
{
    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /** A reference to the player who is completing the relevant step */
    private final Player player;

    /** A reference to the location step */
    private final LocationStep locationStep;

    /**
     * Constructs the listener
     * @param plugin
     * @param player
     * @param locationStep
     */
    public VideoLinkCommandListener(TeachingTutorials plugin, Player player, LocationStep locationStep)
    {
        this.plugin = plugin;
        this.player = player;
        this.locationStep = locationStep;
    }

    /**
     * Detects /link and /video commands if they are from the relevant player, and then displays the video link in chat
     * @param event A command preprocess event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void commandEvent(PlayerCommandPreprocessEvent event)
    {
        if (event.isCancelled())
            return;

        if (this.player.getUniqueId().equals(event.getPlayer().getUniqueId()))
        {
            if (event.getMessage().startsWith("/link") || event.getMessage().startsWith("/video"))
            {
                locationStep.displayVideoLink(event.getPlayer());
                event.setCancelled(true);
            }
        }
    }

    /**
     * Registers the listener with the server's event listener system
     */
    public void register()
    {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Unregisters the listener with the server's event listener system
     */
    public void unregister()
    {
        HandlerList.unregisterAll(this);
    }
}
