package org.btuk.teachingtutorials.listeners;

import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;
import net.buildtheearth.terraminusminus.util.geo.CoordinateParseUtils;
import net.buildtheearth.terraminusminus.util.geo.LatLng;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.btuk.teachingtutorials.TeachingTutorials;
import org.btuk.teachingtutorials.utils.GeometricUtils;
import org.btuk.teachingtutorials.utils.Display;

import java.text.DecimalFormat;

/**
 * Used to enable /ll and /tpll and disable /gmask when in a lesson or new location creation
 */
public class PlaythroughCommandListeners implements Listener
{
    /** BTE world generator settings - used for converting coordinates */
    private final EarthGeneratorSettings bteGeneratorSettings = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS);
    private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("##.#####");

    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /**
     * Constructs the object and registers the event listeners
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     */
    public PlaythroughCommandListeners(TeachingTutorials plugin)
    {
        this.plugin = plugin;

        //Registers the listener
        register();
    }

    /**
     * Checks various commands and intercepts them, including tpll, ll and gmask
     * @param event A player command preprocess event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void commandEvent(PlayerCommandPreprocessEvent event)
    {
        if (event.isCancelled())
            return;

        //Extracts the player to a local variable
        Player player = event.getPlayer();

        //Extracts the command to a local variable
        String command = event.getMessage();

        if (command.startsWith("/tpll"))
        {
            //Cancels the event
            event.setCancelled(true);

            //Extracts the coordinates, deals with inaccuracies
            if (!command.startsWith("/tpll "))
            {
                player.sendMessage(Display.errorText("Incorrect tpll format. Must be /tpll [lattitude], [longitude]"));
                return;
            }
            command = command.replace("/tpll ", "");
            LatLng latLong = CoordinateParseUtils.parseVerbatimCoordinates(command.replace(", ", " "));

            //Checks that the coordinates were established
            if (latLong == null)
            {
                event.setCancelled(true);
                player.sendMessage(Display.errorText("Incorrect tpll format. Must be /tpll [lattitude], [longitude]"));
                return;
            }

            //Teleports the player to where they tplled to
            //Gets the world
            World world = player.getWorld();

            //Performs the tpll
            if (!GeometricUtils.tpllPlayer(world, latLong.getLat(), latLong.getLng(), player))
                return; //Returns if the tpll was not in the bounds of the earth
        }
        else if (command.startsWith("/ll"))
        {
            //Cancels the event
            event.setCancelled(true);

            //Attempt to convert the coordinates, then formats a display and a link to the player in chat
            try
            {
                double[] coords = bteGeneratorSettings.projection().toGeo(player.getLocation().getX(), player.getLocation().getZ());

                TextComponent tMessage = Component.text("Your coordinates are ", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(DECIMAL_FORMATTER.format(coords[1]), NamedTextColor.DARK_AQUA))
                        .append(Component.text(",", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
                                .append(Component.text(DECIMAL_FORMATTER.format(coords[0]), NamedTextColor.DARK_AQUA)));

                player.sendMessage(tMessage);
                Component message = Component.text("Click here to view the coordinates in Google Maps.", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false);
                message = message.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://www.google.com/maps/@?api=1&map_action=map&basemap=satellite&zoom=21&center=" + coords[1] + "," + coords[0]));
                player.sendMessage(message);
            }
            catch (OutOfProjectionBoundsException e)
            {
                player.sendMessage(Display.errorText("You are not on the projection world"));
            }
        }

        //Block gmask changes if it wasn't expected and dealt with by a tutorials task
        else if (command.startsWith("/gmask") || command.startsWith("//gmask"))
        {
            //Cancels the event
            event.setCancelled(true);

            //Notifies them of gmask block
            Component message = Component.text("You can only change your gmask when prompted if you are in a tutorial", NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false);
            player.sendMessage(message);
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
    public void unregister()
    {
        HandlerList.unregisterAll(this);
    }
}