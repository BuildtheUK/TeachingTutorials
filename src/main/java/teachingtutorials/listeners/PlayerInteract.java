package teachingtutorials.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import teachingtutorials.guis.MainMenu;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.User;

import java.util.logging.Level;

/**
 * An interaction listener used to detect clicks of the menu item in the hot-bar
 */
public class PlayerInteract implements Listener
{
    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /**
     * Constructs the object and registers the event listeners
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     */
    public PlayerInteract(TeachingTutorials plugin)
    {
        this.plugin = plugin;
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Detects all interaction events, checks whether they had the menu opener item in their hand, and if so, cancels
     * the event and opens the menu
     * @param event A player interact event
     */
    @EventHandler
    public void interactEvent(PlayerInteractEvent event)
    {
        //Extract the player into a local variable
        Player player = event.getPlayer();

        //Identifies the user for this player
        User user = User.identifyUser(plugin, player);

        if (user == null)
        {
            plugin.getLogger().severe("User " + player.getName() + " can not be found (is null)!");
            player.sendMessage(Display.errorText("Tutorials user can not be found, please relog!"));
            return;
        }

        //Checks the item in their hand
        if (event.getItem() != null)
        {
            //Checks whether the item is equal to the menu item
            if (player.getInventory().getItemInMainHand().equals(TeachingTutorials.menu))
            {
                //Cancel the event
                event.setCancelled(true);

                plugin.getLogger().log(Level.INFO, "The main menu is null? "+(user.mainGui==null));

                //Check if the mainGui is not null.
                if (user.mainGui != null)
                {
                    //If not then open it after refreshing its contents.
                    user.mainGui.open(user.player);
                }

                //If no gui exists open the learning menu
                else
                {
                    //Creates a new main menu
                    user.mainGui = new MainMenu(plugin, user);

                    //Opens the gui
                    user.mainGui.open(user.player);
                }
            }
        }
    }
}
