package teachingtutorials.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialplaythrough.TutorialPlaythrough;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.Mode;
import teachingtutorials.utils.User;


/**
 * A class to handle /blockspy commands - used for spying on virtual blocks of other player's lessons
 */
public class Blockspy implements CommandExecutor
{
    /** A reference to the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    public Blockspy(TeachingTutorials plugin)
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
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings)
    {
        //Check if the sender is a player.
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage((ChatColor.RED +"This command can only be used by a player"));
            return true;
        }

        //Ensure the player has permissions to spy
        if (!player.hasPermission("teachingtutorials.canspy"))
        {
            player.sendMessage(Display.errorText(plugin.getConfig().getString("SpyPermissionsErrorMessage")));
        }
        else
        {
            //Deal with if the prospective spy isn't idle themself
            User spyUser = User.identifyUser(plugin, player);
            if (spyUser == null)
                return true;
            if (!spyUser.getCurrentMode().equals(Mode.Idle))
            {
                player.sendMessage(Display.errorText("You can only spy on other players if you are idle, you are currently in a lesson"));
                return true;
            }

            //Deal with if there is no target specified
            if (strings.length == 0)
            {
                if (spyUser.isSpying())
                {
                    //Take this to mean they are wanting to disable spying
                    player.sendMessage(Display.aquaText("Unspying from " +spyUser.getNameOfSpyTarget()));
                    spyUser.disableSpying();
                }
                else
                    player.sendMessage(Display.errorText("You must specify a target user to spy on"));

                return true;
            }

            //Extracts the target player
            Player targetPlayer = Bukkit.getPlayer(strings[0]);

            //Deals with if the target is not valid/not on the server
            if (targetPlayer == null)
            {
                player.sendMessage(Display.errorText("This player doesn't exist"));
                return true;
            }

            //Gets the User for the target player
            User targetUser = User.identifyUser(plugin, targetPlayer);
            if (targetUser == null)
            {
                player.sendMessage(Display.errorText("Such player is not on the tutorials server"));
                return true;
            }

            //Deals with if the target is not doing a lesson
            if (!(targetUser.getCurrentMode().equals(Mode.Creating_New_Location) || targetUser.getCurrentMode().equals(Mode.Doing_Tutorial)))
            {
                player.sendMessage(Display.errorText(targetPlayer.getName() +" is not in a lesson currently"));
                return true;
            }
            else
            {
                //Deal with if they are already spying on someone - removes them as a spy
                if (spyUser.isSpying())
                {
                    //Deals with if they are already spying on this user - assume they wish to unspy
                    if (spyUser.getNameOfSpyTarget().equals(targetUser.player.getName()))
                    {
                        player.sendMessage(Display.aquaText("Unspying from " +spyUser.getNameOfSpyTarget()));
                        spyUser.disableSpying();
                        return true;
                    }

                    player.sendMessage(Display.aquaText("Unspying from " +spyUser.getNameOfSpyTarget()));
                    spyUser.disableSpying();
                }

                //Get the current playthrough of the target user to spy on
                TutorialPlaythrough playthrough = targetUser.getCurrentPlaythrough();
                if (playthrough != null)
                {
                    //Add the command sender as a spy to this playthough
                    player.sendMessage(Display.aquaText("Adding you as a spy to " +targetPlayer.getName()));
                    playthrough.addSpy(spyUser.player);
                }
                else
                {
                    player.sendMessage(Display.errorText("Could not find the current lesson of this player, please report this to staff"));
                }
            }
        }
        return true;
    }
}
