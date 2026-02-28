package org.btuk.teachingtutorials.services;

import net.bteuk.teachingtutorials.services.PromotionService;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.btuk.teachingtutorials.TeachingTutorials;
import org.btuk.teachingtutorials.utils.plugins.Luckperms;

import java.util.List;
import java.util.logging.Level;

/**
 * A default promotion mechanism
 */
public class TutorialsPromotionService implements PromotionService {

    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    public TutorialsPromotionService(TeachingTutorials plugin) {
        this.plugin = plugin;
    }

    @Override
    public void promote(Player player) {

        //---------------------------------------------------
        //----------------Promotes the player----------------
        //---------------------------------------------------
        //Promotes the player - this is always performed in case there was an issue with the DB, or if the player was demoted and is starting again

        //Retrieves the relevant information from config
        FileConfiguration config = plugin.getConfig();
        String szCompulsoryTutorialPromotionType = config.getString("Compulsory_Tutorial.Promotion_Type");
        String szRankOld = config.getString("Compulsory_Tutorial.RankOld");
        String szRankNew = config.getString("Compulsory_Tutorial.RankNew");
        String szTrack = config.getString("Compulsory_Tutorial.Track");
        String[] szTracks = config.getString("Compulsory_Tutorial.TrackOutline").split(",");

        //Gets a local reference to the console sender
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();

        if (szCompulsoryTutorialPromotionType.equalsIgnoreCase("none")) {
            //Do nothing
            plugin.getLogger().log(Level.INFO, "Performing no promotion for " + player.getName());
        }
        //Deals with a promotion on a track
        else if (szCompulsoryTutorialPromotionType.equalsIgnoreCase("track")) {
            //Notifies console
            plugin.getLogger().log(Level.INFO, "Performing a 'track' promote for " + player.getName());

            //Gets the luckperms user
            plugin.getLogger().log(Level.FINE, "Retrieving the luckperms user for " + player.getName());
            net.luckperms.api.model.user.User lpUser = Luckperms.getUser(player.getUniqueId());

            //Go through all of a user's groups and see if they have a group with a weight above that of the target promotion
            plugin.getLogger().log(Level.FINE, "Extracting the parent groups (ranks) for " + player.getName());
            List<String> groups = lpUser.getNodes().stream()
                    .filter(NodeType.INHERITANCE::matches)
                    .map(NodeType.INHERITANCE::cast)
                    .map(InheritanceNode::getGroupName)
                    .toList();
            int iNumGroups = groups.size();
            plugin.getLogger().log(Level.FINE, iNumGroups + " parent groups (ranks) were found for " + player.getName());


            int iNumGroupsInTrack = szTracks.length;

            //We have a list of tracks in order, we have a target and an old
            //Find the index of the new and old rank in the track
            int j, k;
            int iIndexOldRank = -1;
            int iIndexNewRank = -1;
            int iHighestIndexOnTrack = -1;

            //Find the index of the old and new rank in the track
            for (j = 0; j < iNumGroupsInTrack; j++) {
                if (szRankOld.equals(szTracks[j])) {
                    iIndexOldRank = j;
                }
                if (szRankNew.equals(szTracks[j])) {
                    iIndexNewRank = j;
                }
            }

            //Do some verification on the track position of the ranks
            if (iIndexNewRank == -1)
                plugin.getLogger().log(Level.SEVERE, "The new rank is not within the track in config");
            else if (iIndexOldRank == -1)
                plugin.getLogger().log(Level.SEVERE, "The old rank is not within the track in config");
            else if (iIndexNewRank <= iIndexOldRank)
                plugin.getLogger().log(Level.SEVERE, "The position of the new rank in the track is less than or equal to the position of the old rank");

                //Verification complete
            else {
                //Let's find the index of the highest rank the player has in the track
                plugin.getLogger().log(Level.FINE, "Find the index of the highest rank that " + player.getName() + " has on the track");

                //Compares each of a user's groups to the groups in the relevant track
                for (j = 0; j < iNumGroups; j++) {
                    //Cycles through the ranks in the track
                    for (k = 0; k < iNumGroupsInTrack; k++) {
                        //The current group of the user is on the track
                        if (groups.get(j).equals(szTracks[k])) {
                            //Update iHighestIndexOnTrack if the rank is higher than the highest of the user's ranks already queried
                            if (k > iHighestIndexOnTrack)
                                iHighestIndexOnTrack = k;
                        }
                    }
                }

                //User is not currently on the track
                if (iHighestIndexOnTrack == -1) {
                    //Promote the player onto the track
                    plugin.getLogger().log(Level.INFO, player.getName() + " is not currently on the track, TeachingTutorials will run a promote command to put the player onto the '" + szTrack + "' track");
                    Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(console, "lp user " + player.getName() + " promote " + szTrack));
                    iHighestIndexOnTrack = 0;
                }
                if (iHighestIndexOnTrack >= iIndexNewRank) {
                    //No action should be taken, user is already at the new rank or higher
                    plugin.getLogger().log(Level.INFO, player.getName() + " has either the required rank, or a greater rank, so TeachingTutorials will not take any promote action");
                } else {
                    //Calculate the amount of promotes to perform
                    int iDifference = iIndexNewRank - iHighestIndexOnTrack;
                    plugin.getLogger().log(Level.INFO, "Index difference between new rank and current highest rank on track '" + szTrack + "' = " + iDifference);
                    plugin.getLogger().log(Level.INFO, "TeachingTutorial will now perform promotions");

                    //Runs the promotions
                    for (int l = 0; l < iDifference; l++) {
                        Bukkit.getScheduler().runTask(plugin, new Runnable() {
                            @Override
                            public void run() {
                                Bukkit.dispatchCommand(console, "lp user " + player.getName() + " promote " + szTrack);
                            }
                        });
                    }

                    //Broadcasts the promotion to the whole server
                    broadcastPromotion(player, szRankNew);
                }
            }
        }
        //Deals with a promotion on a rank - will just add a rank
        else if (szCompulsoryTutorialPromotionType.equalsIgnoreCase("rank")) {
            //Notifies console
            plugin.getLogger().log(Level.INFO, "Performing a 'rank' promote for " + player.getName());

            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    Bukkit.dispatchCommand(console, "lp user " + player.getName() + " parent add " + szRankNew);
                }
            });

            //Broadcasts the promotion to the whole server
            broadcastPromotion(player, szRankNew);
        }

        //Deals with a manual exchange of ranks
        else if (szCompulsoryTutorialPromotionType.equalsIgnoreCase("manualpromote")) {
            //Notifies console
            plugin.getLogger().log(Level.INFO, "Performing a 'manual promote' for " + player.getName());

            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(console, "lp user " + player.getName() + " parent remove " + szRankOld);
                Bukkit.dispatchCommand(console, "lp user " + player.getName() + " parent add " + szRankNew);
            });

            //Broadcasts the promotion to the whole server
            broadcastPromotion(player, szRankNew);
        }

    }

    @Override
    public String getDescription() {
        return "Tutorial's built-in promotion service.";
    }

    /**
     * Broadcasts a promotion message to the whole server
     * @param player The player that is being promoted
     * @param szRankNew The name of the rank that the player has been promoted to
     */
    private void broadcastPromotion(Player player, String szRankNew)
    {
        //Formats the promotion broadcast message
        String szMessage;
        if (szRankNew.charAt(0) == 'A' || szRankNew.charAt(0) == 'a' ||
                szRankNew.charAt(0) == 'E' || szRankNew.charAt(0) == 'e' ||
                szRankNew.charAt(0) == 'I' || szRankNew.charAt(0) == 'i' ||
                szRankNew.charAt(0) == 'O' || szRankNew.charAt(0) == 'o' ||
                szRankNew.charAt(0) == 'U' || szRankNew.charAt(0) == 'u')
        {
            szMessage = ChatColor.AQUA + player.getName() +" is now an " +szRankNew +" !";
        }
        else
        {
            szMessage = ChatColor.AQUA + player.getName() +" is now a " +szRankNew +" !";
        }

        //Broadcast promotion
        Bukkit.broadcast(szMessage, "");

    }
}
