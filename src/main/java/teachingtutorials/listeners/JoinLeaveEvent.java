package teachingtutorials.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.utils.Mode;
import teachingtutorials.utils.User;

import java.util.ArrayList;

/**
 * Handles what happens when a user joins or leaves the tutorials server
 */
public class JoinLeaveEvent implements Listener
{
    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /**
     * Constructs the object and registers the event listeners
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     */
    public JoinLeaveEvent(TeachingTutorials plugin)
    {
        this.plugin = plugin;
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Detects player join events, fetches their information from the DB, constructs a new User object, adds them to the
     * plugin's list of current users, updates their mode and teleports them to the lobby
     * @param event A player join event
     */
    @EventHandler
    public void playerJoin(PlayerJoinEvent event)
    {
        //Creates a new user object
        User user = new User(event.getPlayer());
        user.fetchDetailsByUUID(plugin.getDBConnection(), plugin.getLogger());
        user.reassessHasIncompleteLesson(plugin.getDBConnection(), plugin.getLogger());
        user.calculateRatings(plugin.getDBConnection());

        //Adds player to the main list of players
        plugin.players.add(user);

        //Teleports the player to the lobby
        if (plugin.getConfig().getBoolean("TeleportToSpawnOnJoin", true))
            User.teleportPlayerToLobby(event.getPlayer(), plugin, plugin.getConfig().getLong("PlayerJoinTPDelay"));
    }

    /**
     * Detects player leave events, calls for player leave logic to be performed, removes them from the plugin's list of
     * users and teleports the player to the lobby
     * @param event A player leave event
     */
    @EventHandler
    public void playerLeave(PlayerQuitEvent event)
    {
        //Identifies the user
        Player player = event.getPlayer();

        //Gets a reference to the list of players in the plugin
        ArrayList<User> users = plugin.players;

        //Goes through the list of users
        int i;
        for (i = 0 ; i < users.size() ; i++)
        {
            //Declares a local reference to each user in the list
            User user = users.get(i);

            //Queries whether this user is the relevant user
            if (user.player.getUniqueId().equals(player.getUniqueId()))
            {
                //Initiates any player leave logic
                user.playerLeave(plugin);

                //Removes the user from the plugin's list
                users.remove(i);
                i--;
                //Does not break because they may be there multiple times through an error,
                // and this would finally clear them and resolve that error
            }
        }

        //Teleports the player to the lobby
        if (plugin.getConfig().getBoolean("TeleportToSpawnOnJoin", true))
            User.teleportPlayerToLobby(event.getPlayer(), plugin, 0);
    }
}