package teachingtutorials.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.guis.MainMenu;
import teachingtutorials.utils.User;

/**
 * A PlayerCommandPreprocessEvent listener used to deal with /tutorial and /learn commands
 */
public class GlobalPlayerCommandProcess implements Listener
{
    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /**
     * Constructs the listener object and registers the listener
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     */
    public GlobalPlayerCommandProcess(TeachingTutorials plugin)
    {
        this.plugin = plugin;

        //Registers the listener
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    //Want the /tutorials endarea and /tutorials [difficulty], then this
    /**
     * Listens for PlayerCommandPreprocessEvent events and checks whether they are /tutorial commands
     * @param event A PlayerCommandPreprocessEvent event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void commandEvent(PlayerCommandPreprocessEvent event)
    {
        //If it has already been dealt with then do nothing
        if (event.isCancelled())
            return;

        //Extracts the player to a local variable
        Player player = event.getPlayer();

        //Checks that it is the correct command
        String command = event.getMessage();
        if (command.startsWith("/tutorials") || command.startsWith("/learn"))
        {
            //Cancels the event
            event.setCancelled(true);

            //Open the menu
            User user = User.identifyUser(plugin, player);
            if (user != null)
            {
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
