package org.btuk.teachingtutorials.newlocation;

import net.buildtheearth.terraminusminus.projection.GeographicProjection;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;
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

import java.util.logging.Level;

/**
 * A listener solely used to allow a creator to specify the start location of the tutorial using a tpll command
 */
public class StartLocationListener implements Listener
{
    /** A reference to the user who is creating the new location */
    private final User creator;

    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /** A reference to the NewLocation object which is managing this start location listener */
    private final NewLocation callingClass;

    /** The max and min bounds - used to check whether the start location is within the bounds generated */
    private final int ixMin, ixMax, izMin, izMax;

    /** A reference to the projection - used to calculate whether the start location in is the bounds */
    private final GeographicProjection projection;

    /**
     * Constructs the object
     * @param Creator A reference to the user who is creating the new location
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @param callingClass A reference to the NewLocation object which is managing this start location listener
     * @param ixMin The min x of the area generated - used to check whether the start location is within the bounds generated
     * @param ixMax The max x of the area generated - used to check whether the start location is within the bounds generated
     * @param izMin The min z of the area generated - used to check whether the start location is within the bounds generated
     * @param izMax The max z of the area generated - used to check whether the start location is within the bounds generated
     * @param projection A reference to the projection - used to calculate whether the start location in is the bounds
     */
    public StartLocationListener(User Creator, TeachingTutorials plugin, NewLocation callingClass, int ixMin, int ixMax, int izMin, int izMax, GeographicProjection projection)
    {
        this.creator = Creator;
        this.plugin = plugin;
        this.callingClass = callingClass;

        //Copies the bounds of the generated area into the start location listener
        this.ixMin = ixMin;
        this.ixMax = ixMax;
        this.izMin = izMin;
        this.izMax = izMax;

        this.projection = projection;
    }

    //Want the tutorials tpll process to occur first

    /**
     * Detects tpll commands, verifies and processes the coordinates
     * @param event A player command preprocess event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void tpllCommand(PlayerCommandPreprocessEvent event)
    {
        //Checks whether the command is from the relevant player
        if (event.getPlayer().getUniqueId().equals(this.creator.player.getUniqueId()))
        {
            //Extracts the command
            String command = event.getMessage();

            //Verifies that the command is a tpll command
            if (command.startsWith("/tpll "))
            {
                event.setCancelled(true);

                //Extracts the coordinates
                command = command.replace("/tpll ", "");
                LatLng latLong = CoordinateParseUtils.parseVerbatimCoordinates(command.replace(", ", " "));
                if (latLong == null)
                {
                    plugin.getLogger().log(Level.FINE, ChatColor.DARK_AQUA +"Latitude, longitude is null");
                    creator.player.sendMessage(Display.errorText("An error occurred converting the coordinates"));
                    return;
                }

                double xz[];

                //Converts the tpll coordinates to minecraft coordinates
                try
                {
                    xz = projection.fromGeo(latLong.getLng(), latLong.getLat());
                }
                catch (OutOfProjectionBoundsException e)
                {
                    plugin.getLogger().log(Level.SEVERE, "Unable to convert lat,long coordinates of start location to minecraft coordinates", e);
                    creator.player.sendMessage(Display.errorText("Those coordinates are out of bounds"));
                    return;
                }

                //Make sure that it is within the generated area
                if ((int) xz[0] < ixMin || (int) xz[0] > ixMax || (int) xz[1] < izMin || (int) xz[1] > izMax)
                {
                    plugin.getLogger().log(Level.WARNING, "Start location coordinates were not within the generated area");
                    creator.player.sendMessage(Display.errorText("The start location must be inside of the generated area. Try again."));
                    return;
                }

                //Only unregisters and moves on if all the criteria for the start location are met. These means that they get another chance.
                HandlerList.unregisterAll(this);

                this.callingClass.lessonStart(latLong);
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
    public void deregister()
    {
        HandlerList.unregisterAll(this);
    }
}
