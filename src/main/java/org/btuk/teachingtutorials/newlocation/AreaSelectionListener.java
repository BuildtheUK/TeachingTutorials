package org.btuk.teachingtutorials.newlocation;

import net.buildtheearth.terraminusminus.util.geo.CoordinateParseUtils;
import net.buildtheearth.terraminusminus.util.geo.LatLng;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.btuk.teachingtutorials.TeachingTutorials;
import org.btuk.teachingtutorials.utils.Display;
import org.btuk.teachingtutorials.utils.User;

import java.util.ArrayList;
import java.util.logging.Level;

/**
 * Used to define the generation area of a new location. Will detect tpll events and store these coordinates as the outer bounds
 */
public class AreaSelectionListener implements Listener
{
    /** A reference to the player who is creating the new location */
    private final User creator;

    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /** A reference to the NewLocation object which is managing this area selection listener */
    private final NewLocation callingClass;

    /** A list of latitude and longitude coordinates, storing the outer bounds of the area to generate */
    private final ArrayList<LatLng> bounds;

    /**
     * Constructs the object, inserting the necessary references to other objects, and creating an empty ArrayList for
     * the area bounds
     * @param Creator A reference to the player who is creating the new location
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @param callingClass A reference to the NewLocation object which is managing this area selection listener
     */
    public AreaSelectionListener(User Creator, TeachingTutorials plugin, NewLocation callingClass)
    {
        this.creator = Creator;
        this.plugin = plugin;
        this.callingClass = callingClass;
        this.bounds = new ArrayList<>();
    }

    /**
     * @return A reference to the list which contains the area bounds
     */
    public ArrayList<LatLng> getBounds()
    {
        return bounds;
    }

    //Want the tutorials tpll and /tutorials command process to occur first

    /**
     * Detects command preprocess events and intercepts /tpll commands, and also "/tutorial endarea" commands
     * @param event A command preprocess event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void tpllCommand(PlayerCommandPreprocessEvent event)
    {
        //Checks whether the player is the correct player - notice we directly check the object references - this is
        // less expensive than an .equals()
        if (event.getPlayer() == this.creator.player)
        {
            //Extracts the command
            String command = event.getMessage();

            //Checks whether it is a tpll
            if (command.startsWith("/tpll "))
            {
                //Cancels the event
                event.setCancelled(true);

                //Extracts the coordinates
                command = command.replace("/tpll ", "");
                LatLng latLong = CoordinateParseUtils.parseVerbatimCoordinates(command.replace(", ", " "));

                //Adds the coordinates
                if (latLong !=null)
                {
                    bounds.add(latLong);

                    //Notifies console and player
                    plugin.getLogger().log(Level.FINE, ChatColor.AQUA +"Point added to list");
                    event.getPlayer().sendMessage(Display.aquaText("Point added to list"));
                }
                else
                {
                    event.getPlayer().sendMessage(Display.errorText("Incorrect tpll command format, try again"));
                }
            }

            //Checks whether it is an endarea request
            else if (command.startsWith("/tutorials endarea"))
            {
                event.setCancelled(true);
                //Checks whether at least 3 points have been specified for the area
                if (this.bounds.size() >= 3)
                {
                    //Unregisters the event and calls the next stage of the location creation to commence
                    deregister();
                    callingClass.AreaSelectionMade();
                }
                else
                {
                    //Notifies user of error
                    event.getPlayer().sendMessage(Display.errorText("You need at least 3 points to create an area"));
                }
            }
        }
    }

    /**
     * Registers the listeners with the server's event listeners
     */
    public void register()
    {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Unregisters the listeners with the server's event listeners
     */
    public void deregister()
    {
        HandlerList.unregisterAll(this);
    }
}
