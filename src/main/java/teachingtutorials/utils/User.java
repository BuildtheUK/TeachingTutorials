package teachingtutorials.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.newlocation.NewLocation;
import teachingtutorials.tutorialobjects.LessonObject;
import teachingtutorials.tutorialplaythrough.TutorialPlaythrough;
import net.bteuk.minecraft.gui.*;
import teachingtutorials.tutorialplaythrough.Lesson;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a User of the Tutorials system, and holds their data when on the server
 */
public class User
{
    /** The bukkit player for which this User is of */
    public final Player player;

    /** Stores whether a player has completed the compulsory tutorial or not */
    public boolean bHasCompletedCompulsory;

    /** Records whether a player has Lesson to finish in the Lesson's table of the database */
    private boolean bHasIncompleteLesson;

    /** Tracks what the player is currently doing on the tutorials server */
    private Mode currentMode;

    /** Holds the information on the ratings for a user */
    public int iScoreTpll, iScoreWE, iScoreTerraforming, iScoreColouring, iScoreDetailing;

    /** The lesson/new location which the user is currently playing through */
    private TutorialPlaythrough currentPlaythrough;

    /** The tutorial which this user is spying on, null if they are not spying on any tutorial */
    private TutorialPlaythrough spyTarget;

    /** Main gui, includes everything that is part of the navigator */
    public Gui mainGui;

    //--------------------------------------------------
    //-------------------Constructors-------------------
    //--------------------------------------------------
    public User(Player player)
    {
        this.player = player;
        this.currentMode = Mode.Idle;
    }

    //---------------------------------------------------
    //----------------------Getters----------------------
    //---------------------------------------------------

    /**
     * Refreshes whether the user has incomplete lessons then returns this value
     * @param dbConnection A database connection object for the tutorials database
     * @param logger A plugin logger
     * @return Whether the user has incomplete lessons
     */
    public boolean hasIncompleteLessons(DBConnection dbConnection, Logger logger)
    {
        reassessHasIncompleteLesson(dbConnection, logger);
        return bHasIncompleteLesson;
    }

    public TutorialPlaythrough getCurrentPlaythrough()
    {
        return this.currentPlaythrough;
    }

    public Mode getCurrentMode() {
        return currentMode;
    }

    //---------------------------------------------------
    //----------------------Setters----------------------
    //---------------------------------------------------
    public void setHasIncompleteLesson(boolean bHasIncompleteLesson)
    {
        this.bHasIncompleteLesson = bHasIncompleteLesson;
    }

    /**
     * Updates the reference to the current playthrough, and updates the current mode of the user
     * @param playthrough The TutorialPlaythrough to set the user's current playthrough to
     */
    public void setCurrentPlaythrough(TutorialPlaythrough playthrough)
    {
        this.currentPlaythrough = playthrough;
        if (playthrough == null)
        {
            currentMode = Mode.Idle;
        }
        else if (playthrough instanceof Lesson)
        {
            currentMode = Mode.Doing_Tutorial;
        }
        else if (playthrough instanceof NewLocation)
        {
            currentMode = Mode.Creating_New_Location;
        }
    }


    /**
     * Recalculates all ratings for this player
     * @param dbConnection A connection to the database
     */
    public void calculateRatings(DBConnection dbConnection)
    {
        iScoreTpll = calculateRating(Category.tpll, dbConnection);
        iScoreWE = calculateRating(Category.worldedit, dbConnection);
        iScoreTerraforming = calculateRating(Category.terraforming, dbConnection);
        iScoreColouring = calculateRating(Category.colouring, dbConnection);
        iScoreDetailing = calculateRating(Category.detail, dbConnection);
    }

    /**
     * Uses scores from previous lessons to calculate a rating for a player in a category
     * @param category The category to calculate the rating for
     * @param dbConnection A connection to the database
     * @return The rating out of 100 of the user in the given category
     */
    private int calculateRating(Category category, DBConnection dbConnection)
    {
        //Declare variables
        int iTotalScore = 0;

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        //Attempts to fetch the data from the database
        try
        {
            //Compiles the command to add the new user
            sql = "SELECT * FROM `Lessons`, `Scores` WHERE `Lessons`.`UUID` = '"+player.getUniqueId() +"' " +
                    "AND `Scores`.`Category` = '" + category.toString() + "' "+
                    "AND `Lessons`.`LessonID` = `Scores`.`LessonID` " +
                    "ORDER BY `Scores`.`LessonID` DESC";
            SQL = dbConnection.getConnection().createStatement();

            //Executes the update and returns the amount of records updated
            resultSet = SQL.executeQuery(sql);

            //Iterate over last 5 and add up score
            for (int iCount = 5 ; resultSet.next() && iCount >= 1 ; iCount--)
            {
                iTotalScore = iTotalScore + iCount*resultSet.getInt("Score");
            }
            iTotalScore = iTotalScore/15;
        }
        catch (SQLException se)
        {
            Bukkit.getLogger().log(Level.SEVERE, "SQL - SQL Error fetching lessons scores by UUID for category: " +category.toString(), se);
        }
        catch (Exception e)
        {
            Bukkit.getLogger().log(Level.SEVERE, "SQL - Non-SQL Error fetching lessons scores by UUID for category: " +category.toString(), e);
        }

        return iTotalScore;
    }

    /**
     * Performs all the necessary processes when a player leaves the server
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     */
    public void playerLeave(TeachingTutorials plugin)
    {
        //Checks their status
        switch (currentMode)
        {
            case Idle:
                plugin.getLogger().log(Level.INFO, ChatColor.LIGHT_PURPLE +"Player "+player.getName() +" is leaving but was idle");
                break; //Assume the system is keeping an accurate account of the player's status
            case Doing_Tutorial:
            case Creating_New_Location:
                plugin.getLogger().log(Level.INFO, ChatColor.LIGHT_PURPLE +"Player "+player.getName() +" is leaving and was doing a tutorial or creating location. Saving and removing listeners...");

                //Searches through the current tutorial playthroughs for any playthroughs belonging to the user leaving
                ArrayList<TutorialPlaythrough> playthroughs = plugin.activePlaythroughs;
                TutorialPlaythrough playthrough;
                int i;
                int iPlaythroughs = playthroughs.size();
                for (i = 0 ; i < iPlaythroughs ; i++)
                {
                    playthrough = playthroughs.get(i);
                    if (playthrough.getCreatorOrStudent().equals(this))
                    {
                        //Saves the scores, saves the position, removes the listeners, removes the virtual blocks (of the current task)
                        playthrough.terminateEarly();

                        //If a lesson is removed from the list we must start the for-loop again
                        i--;
                        iPlaythroughs--;
                    }
                }

                plugin.getLogger().log(Level.INFO, ChatColor.LIGHT_PURPLE +"Paused all tutorial playthroughs for player "+player.getName());

                //Removes the player from any spy lists
                this.disableSpying();

                break;
            case Creating_New_Tutorial:
            default:
                break;
        }
    }

    /**
     * Identifies a given player's User instance from the plugin's list of users
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @param player The player to identify the user of
     * @return A reference to the User object for the given player, or null if the given player is offline
     */
    public static User identifyUser(TeachingTutorials plugin, Player player)
    {
        //Finds the correct user for this player from the plugin's list of users
        boolean bUserFound = false;

        //Get a local reference to the list of users
        ArrayList<User> users = plugin.players;
        int iLength = users.size();
        int i;
        User user = null;

        //Prevents null exception error if player is null as would happen if the player was offline
        if (player == null)
            return null;

        //Goes through all of the online users
        for (i = 0 ; i < iLength ; i++)
        {
            if (users.get(i).player.getUniqueId().equals(player.getUniqueId()))
            {
                user = users.get(i);
                bUserFound = true;
                break;
            }
        }

        if (!bUserFound)
        {
            player.sendMessage(Display.errorText("An error with the tutorials system occurred. Please contact a support staff. Error: 1"));
            player.sendMessage(Display.errorText("Try relogging"));
            return null;
        }
        else
        {
            return user;
        }
    }

    /**
     * Teleports a player to the lobby after a wait
     * @param player The player to teleport
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @param waitTimeTicks The amount of ticks to wait for until teleportation commences
     */
    public static void teleportPlayerToLobby(Player player, TeachingTutorials plugin, long waitTimeTicks)
    {
        //Gets a reference to the config
        FileConfiguration config = plugin.getConfig();

        World tpWorld = Bukkit.getWorld(config.getString("Spawn_Information.Lobby_World"));
        if (tpWorld == null)
        {
            player.sendMessage(Display.errorText("Cannot tp you to lobby"));
            plugin.getLogger().log(Level.SEVERE, "Cannot tp player to lobby, world null");
        }
        else
        {
            //Creates an object representing the location of the lobby - note we don't make this a static member of
            // TeachingTutorials because we want to allow it to change mid-game
            org.bukkit.Location location = new org.bukkit.Location(tpWorld, config.getDouble("Spawn_Information.Lobby_X"), config.getDouble("Spawn_Information.Lobby_Y"), config.getDouble("Spawn_Information.Lobby_Z"), config.getInt("Spawn_Information.Lobby_Yaw"), config.getInt("Spawn_Information.Lobby_Pitch"));

            //Teleports the player
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
            {
                @Override
                public void run()
                {
                    player.teleport(location);
                }
            }, waitTimeTicks);
        }

    }

    /**
     * Returns whether this user is currently spying on another
     */
    public boolean isSpying()
    {
        return (this.spyTarget != null);
    }

    /**
     * If they are currently spying, removes this player as a spy from the current playthrough they are spying on, and sets the spy target to null
     */
    public void disableSpying()
    {
        if (isSpying())
        {
            //Removes this player as a spy from the current spy target, and sets the spy target to null
            this.spyTarget.removeSpy(this.player);
        }

        //Else, Do nothing
    }

    /**
     * Mark the player as spying on the given target. This method will perform a check to see whether the target does indeed have this user as a spy.
     * @param tutorialPlaythrough The tutorial playthrough that this player is currently a spy on
     */
    public void setSpyTarget(TutorialPlaythrough tutorialPlaythrough)
    {
        if (tutorialPlaythrough == null)
        {
            this.spyTarget = null;
        }
        else if (tutorialPlaythrough.hasSpy(this.player))
        {
            this.spyTarget = tutorialPlaythrough;
        }
    }

    /**
     * Get the name of the player of whom this user is current spying on
     * @return The name of the player of whom this user is current spying on, or an empty string if they are not spying
     * on anyone
     */
    public String getNameOfSpyTarget()
    {
        if (isSpying())
        {
            return this.spyTarget.getCreatorOrStudent().player.getName();
        }
        else
        {
            return "";
        }
    }

    //---------------------------------------------------
    //--------------------SQL Fetches--------------------
    //---------------------------------------------------
    /**
     * Fetches all of the information about a user in the DB and loads it into this object.
     * <p></p>
     * Or adds them to the DB if they are not already in it.
     * @param dbConnection A database connection
     */
    public void fetchDetailsByUUID(DBConnection dbConnection, Logger logger)
    {
        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to select all data about the user
            sql = "SELECT * FROM `Players` WHERE `UUID` = '"+player.getUniqueId()+"'";
            SQL = dbConnection.getConnection().createStatement();

            //Executes the update and returns the amount of records updated
            resultSet = SQL.executeQuery(sql);
            if (resultSet.next())
            {
                this.bHasCompletedCompulsory = resultSet.getBoolean("CompletedCompulsory");
            }

            //If no result is found for the user, insert a new user into the database
            else
            {
                sql = "INSERT INTO `Players` (`UUID`) VALUES ('"+ player.getUniqueId() +"')";
                SQL.executeUpdate(sql);
                this.bHasCompletedCompulsory = false;
                this.bHasIncompleteLesson = false;
            }
        }
        catch (SQLException se)
        {
            logger.log(Level.SEVERE, "SQL - SQL Error fetching user info by UUID", se);
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "SQL - Non-SQL Error fetching user info by UUID", e);
        }

        reassessHasIncompleteLesson(dbConnection, logger);
    }

    /**
     * Goes through the list of Lessons and works out whether a user has a lesson to complete
     * @param dbConnection A database connection object for the tutorials database
     * @param logger A plugin logger
     */
    public void reassessHasIncompleteLesson(DBConnection dbConnection, Logger logger)
    {
        this.bHasIncompleteLesson = (LessonObject.getUnfinishedLessonsOfPlayer(player.getUniqueId(), dbConnection, logger).length > 0);
    }

    //---------------------------------------------------
    //--------------------SQL Updates--------------------
    //---------------------------------------------------
    /**
     * Records in the database that this user has completed the compulsory tutorial
     * @param dbConnection A database connection to a tutorials database
     * @param logger A logger
     */
    public void triggerCompulsory(DBConnection dbConnection, Logger logger)
    {
        //Declare variables
        String szSql;
        Statement SQL;

        //Updates the variable
        this.bHasCompletedCompulsory = true;

        //Updates the database
        try
        {
            SQL = dbConnection.getConnection().createStatement();
            szSql = "UPDATE `Players` SET `CompletedCompulsory` = 1 WHERE `UUID` = '"+ this.player.getUniqueId()+"'";
            SQL.executeUpdate(szSql);
        }
        catch (SQLException e)
        {
            logger.log(Level.SEVERE, "SQL - SQL Error setting user " +player.getName() +" as having completed the compulsory tutorial", e);
        }
    }
}
