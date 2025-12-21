package teachingtutorials.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.utils.Mode;
import teachingtutorials.utils.User;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to handle tab completers needing a list of online players who are currently playing through a tutorial
 */
public class PlayersPlayingTutorialsCompleter implements TabCompleter
{
    /** A reference to the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    public PlayersPlayingTutorialsCompleter(TeachingTutorials plugin)
    {
        this.plugin = plugin;
    }

    /**
     * @param commandSender
     * @param command
     * @param s
     * @param strings
     * @return
     */
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings)
    {
        //The list of player names of players currently playing through a tutorial
        List<String> activePlayers = new ArrayList<>();

        //Gets the list of online users
        ArrayList<User> onlineUsers = plugin.players;
        User user;
        int iNumUsers = onlineUsers.size();

        //Identifies which users are currently playing through a tutorial and adds those to the list to be returned
        for (int i = 0 ; i < iNumUsers ; i++)
        {
            user = onlineUsers.get(i);
            //Checks whether they are running a tutorial
            if (user.getCurrentMode().equals(Mode.Creating_New_Location) || user.getCurrentMode().equals(Mode.Doing_Tutorial))
            {
                //Adds them to the list
                activePlayers.add(user.player.getName());
            }
        }

        //Returns the list
        return activePlayers;
    }
}
