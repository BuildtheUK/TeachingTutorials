package org.btuk.teachingtutorials.tutorialplaythrough;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.btuk.teachingtutorials.TeachingTutorials;
import org.btuk.teachingtutorials.guis.locationcreatemenus.LocationTaskEditorMenu;
import org.btuk.teachingtutorials.tutorialobjects.LocationTask;
import org.btuk.teachingtutorials.tutorialobjects.Task;
import org.btuk.teachingtutorials.utils.Category;
import org.btuk.teachingtutorials.utils.DBConnection;
import org.btuk.teachingtutorials.utils.VirtualBlockGroup;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;

//Possible change: Make a separate NewLocation task and LessonTask.
// Certainly would allow us to separate certain things which are only used in a particular implementation, which could
// be useful in the future if these classes expand further

/**
 * A class representing a playable task in game.
 */
public abstract class PlaythroughTask
{
    /** Represents the LocationTask that this is a play-through of. Holds the information of the task. */
    private final LocationTask locationTask;

    /**
     * A reference to the parent group play-through object
     */
    protected final GroupPlaythrough parentGroupPlaythrough;

    /**
     * A reference to the instance of the TeachingTutorials plugin.
     */
    protected final TeachingTutorials plugin;

    /**
     * A reference to the player that has to perform this task.
     */
    // This could be accessed via parentGroup but it is used so much within the children of the Task class
    // that having a separate reference to it significantly improves readability of those classes.
    protected final Player player;

    /**
     * Stores whether or not the task is currently active and completable
     */
    public boolean bActive;

    /**
     * A list of virtual blocks which are to be displayed as a result of the completion of this task
     */
    protected VirtualBlockGroup<Location, BlockData> virtualBlocks;

    //Things in place for the full scoring update
    public float fPerformance;
//    public float[] fFinalScores = new float[5];

    /** The menu used to edit the information and properties of this location task */
    protected LocationTaskEditorMenu taskEditorMenu;

    /**
     * Used when initialising a task for a tutorial play-through for an existing location.
     * @param plugin A reference to the TeachingTutorials plugin instance
     * @param player A reference to the Bukkit player for whom the task is for
     * @param locationTask A reference to the Tutorials LocationTask which this task is a play-through of
     * @param groupPlaythrough A reference to the GroupPlaythrough that this PlaythroughTask is a part of
     */
    public PlaythroughTask(TeachingTutorials plugin, Player player, LocationTask locationTask, GroupPlaythrough groupPlaythrough)
    {
        this.plugin = plugin;
        this.player = player;
        this.parentGroupPlaythrough = groupPlaythrough;
        this.locationTask = locationTask;

        //Initiates a new virtual blocks group list
        this.virtualBlocks = new VirtualBlockGroup<>(this.parentGroupPlaythrough.parentStepPlaythrough.parentStagePlaythrough.tutorialPlaythrough);
    }

    /**
     * Used when initialising a task for a tutorial play-through for a NewLocation. It will create a new LocationTask.
     * @param plugin A reference to the TeachingTutorials plugin instance
     * @param player A reference to the Bukkit player for whom the task is for
     * @param task A reference to the Tutorials Task which this task is a play-through of
     * @param groupPlaythrough A reference to the GroupPlaythrough that this PlaythroughTask is a part of
     */
    public PlaythroughTask(TeachingTutorials plugin, Player player, Task task, GroupPlaythrough groupPlaythrough)
    {
        this.plugin = plugin;
        this.player = player;
        this.parentGroupPlaythrough = groupPlaythrough;
        this.locationTask = new LocationTask(task, groupPlaythrough.parentStepPlaythrough.parentStagePlaythrough.getLocationID());

        //Initiates a new virtual blocks group list
        this.virtualBlocks = new VirtualBlockGroup<>(this.parentGroupPlaythrough.parentStepPlaythrough.parentStagePlaythrough.tutorialPlaythrough);
    }

    /**
     *
     * @return A reference to the LocationTask that this play-through task is of
     */
    public LocationTask getLocationTask()
    {
        return locationTask;
    }

    /**
     * Performs any common registration functionality when the task is ready to be completed. Sets the bActive marker to {@code true}.
     */
    public void register()
    {
        bActive = true;
    }

    /**
     * Calculates some scores and then tells the parent group that its active task has just been finished.
     * Deactivates the task.
     */
    protected void taskComplete()
    {
        //Marks the task as not being completable anymore, since it has been completed
        bActive = false;

        //Calculates the score
        if (parentGroupPlaythrough.parentStepPlaythrough.parentStagePlaythrough.getTutorialPlaythrough().getCurrentPlaythroughMode().equals(PlaythroughMode.PlayingLesson))
        {
            //A reference to the parent lesson
            Lesson lesson = (Lesson) parentGroupPlaythrough.parentStepPlaythrough.parentStagePlaythrough.tutorialPlaythrough;
            //Temporary variable used to store the final score calculations in each category
            float fFinalScore, fDifficulty;
            for (int i = 0 ; i < 5 ; i++)
            {
                fDifficulty = locationTask.getDifficulty(Category.values()[i]);
                fFinalScore = fDifficulty*fPerformance;

                //Add scores to the totals
                lesson.fTotalScores[i] = lesson.fTotalScores[i] +fFinalScore;
                lesson.fDifficultyTotals[i] = lesson.fDifficultyTotals[i] + fDifficulty;

            }

            //Add scores to the totals
        }
        //Notifies the parent group that one of its tasks has been complete
        parentGroupPlaythrough.taskFinished();
    }

    /**
     * Removes virtual blocks and sets task to inactive
     */
    public void deactivate()
    {
        removeVirtualBlocks();
        bActive = false;
    }

    /**
     * Should unregister the plugin listener and deactivate the play-through task
     */
    public abstract void unregister();

    /**
     * Adds the list of virtual blocks of this task to the plugin's virtual block group list
     */
    public void displayVirtualBlocks()
    {
        plugin.addVirtualBlocks(virtualBlocks);
    }

    /**
     * Removes the list of virtual blocks of this task from the plugin's list of virtual block groups and
     * resets the view of the players and spies.
     */
    protected void removeVirtualBlocks()
    {
        plugin.removeVirtualBlocks(virtualBlocks);
    }

    /**
     * To be called from a difficulty listener when the difficulty has been specified. Should unregister the
     * play-through task and move the new location creation on to the next task
     */
    public abstract void newLocationSpotHit();

    /**
     * Fetches a list of PlaythroughTasks for a particular group and location and returns a PlaythroughTask ArrayList.
     * @param plugin An instance of the TeachingTutorials plugin
     * @param dbConnection A database connection pointing to the database to fetch the data from
     * @param iLocationID The location ID of the location for which to get the tasks for
     * @param parentGroupPlaythrough The parent group of the tasks
     * @param player A reference to the player for whom these tasks are for
     * @return An ArrayList of PlaythroughTasks for tasks belonging to the group defined by parentGroup
     */
    public static ArrayList<PlaythroughTask> fetchTasksForLocation(TeachingTutorials plugin, DBConnection dbConnection, int iLocationID, GroupPlaythrough parentGroupPlaythrough, Player player)
    {
        //Initialises the list of tasks
        ArrayList<PlaythroughTask> tasks = new ArrayList<>();

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch the tasks and answers
            sql = "Select * FROM LocationTasks,Tasks WHERE LocationTasks.LocationID = "+iLocationID +" AND Tasks.GroupID = "+ parentGroupPlaythrough.getGroup().getGroupID() +" AND Tasks.TaskID = LocationTasks.TaskID ORDER BY 'Order' ASC";
            SQL = dbConnection.getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                int iTaskID = resultSet.getInt("Tasks.TaskID");
                String szType = resultSet.getString("Tasks.TaskType");
                int iOrder = resultSet.getInt("Tasks.Order");
                String szDetails = resultSet.getString("Tasks.Details");
                String szAnswers = resultSet.getString("LocationTasks.Answers");

                //The scoring, difficulty and rating system is not utilised in this release, so it can be mostly ignored
                float fTpllDifficulty = Float.parseFloat(resultSet.getString("LocationTasks.TpllDifficulty"));
                float fWEDifficulty = Float.parseFloat(resultSet.getString("LocationTasks.WEDifficulty"));
                float fColouringDifficulty = Float.parseFloat(resultSet.getString("LocationTasks.ColouringDifficulty"));
                float fDetailingDifficulty = Float.parseFloat(resultSet.getString("LocationTasks.DetailingDifficulty"));
                float fTerraDifficulty = Float.parseFloat(resultSet.getString("LocationTasks.TerraDifficulty"));

                float[] fDifficulties = new float[]{fTpllDifficulty, fWEDifficulty, fColouringDifficulty, fDetailingDifficulty, fTerraDifficulty};

                //Extract the task type
                FundamentalTaskType taskType = null;
                try
                {
                    taskType = FundamentalTaskType.valueOf(szType);
                }
                catch (IllegalArgumentException e)
                {
                    plugin.getLogger().log(Level.SEVERE, "A task was fetched which had an invalid task type value");
                }

                LocationTask locationTask;

                //Creates the correct child class depending on the task type, and adds details in
                switch (taskType)
                {
                    case tpll:
                        locationTask = new LocationTask(FundamentalTaskType.selection, iTaskID, iOrder, szDetails, iLocationID, szAnswers, fDifficulties);
                        Tpll tpll = new Tpll(plugin, player, locationTask, parentGroupPlaythrough);
                        tasks.add(tpll);
                        break;
                    case selection:
                        locationTask = new LocationTask(FundamentalTaskType.selection, iTaskID, iOrder, szDetails, iLocationID, szAnswers, fDifficulties);
                        Selection selection = new Selection(plugin, player, locationTask, parentGroupPlaythrough);
                        tasks.add(selection);
                        break;
                    case command:
                        locationTask = new LocationTask(FundamentalTaskType.command, iTaskID, iOrder, szDetails, iLocationID, szAnswers, fDifficulties);
                        Command command = new Command(plugin, player, locationTask, parentGroupPlaythrough, tasks);
                        tasks.add(command);
                        break;
                    case chat:
                        locationTask = new LocationTask(FundamentalTaskType.chat, iTaskID, iOrder, szDetails, iLocationID, szAnswers, fDifficulties);
                        Chat chat = new Chat(plugin, player, locationTask, parentGroupPlaythrough);
                        tasks.add(chat);
                        break;
                    case place:
                        locationTask = new LocationTask(FundamentalTaskType.place, iTaskID, iOrder, szDetails, iLocationID, szAnswers, fDifficulties);
                        Place place = new Place(plugin, player, locationTask, parentGroupPlaythrough);
                        tasks.add(place);
                        break;
                }
            }
        }
        catch (SQLException se)
        {
            plugin.getLogger().log(Level.SEVERE, ChatColor.RED + "SQL - SQL Error fetching Tasks with answers by LocationID and GroupID", se);
        }
        catch (Exception e)
        {
            plugin.getLogger().log(Level.SEVERE, ChatColor.RED + "SQL - Non SQL Error fetching Tasks with answers by LocationID and GroupID", e);
        }
        return tasks;
    }

    /**
     * Fetches all the tasks without their answers for a particular group and returns a Task ArrayList.
     * This can be used when creating a new location.
     * @param plugin An instance of the TeachingTutorials plugin
     * @param dbConnection A database connection pointing to the database to fetch the data from
     * @param parentGroupPlaythrough The parent group of the tasks
     * @param player A reference to the player for whom these tasks are for
     * @return An ArrayList of Tasks
     */
    public static ArrayList<PlaythroughTask> fetchTasksWithoutAnswers(TeachingTutorials plugin, DBConnection dbConnection, GroupPlaythrough parentGroupPlaythrough, Player player)
    {
        //Initialises the list of tasks
        ArrayList<PlaythroughTask> tasks = new ArrayList<>();

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            plugin.getLogger().log(Level.INFO, ChatColor.AQUA +"Searching for tasks in group with group ID: "+ parentGroupPlaythrough.getGroup().getGroupID());

            //Compiles the command to fetch groups
            sql = "Select * FROM Tasks WHERE GroupID = "+ parentGroupPlaythrough.getGroup().getGroupID() +" ORDER BY 'Order' ASC";
            plugin.getLogger().log(Level.FINE, sql);

            //Creates the statement
            SQL = dbConnection.getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);

            //Goes through the result set and initialises the relevant Task objects
            while (resultSet.next())
            {
                int iTaskID = resultSet.getInt("TaskID");
                String szType = resultSet.getString("TaskType");
                int iOrder = resultSet.getInt("Order");
                String szDetails = resultSet.getString("Details");

                //Extract the task type
                FundamentalTaskType taskType = null;
                try
                {
                    taskType = FundamentalTaskType.valueOf(szType);
                }
                catch (IllegalArgumentException e)
                {
                    plugin.getLogger().log(Level.SEVERE, "A task was fetched which had an invalid task type value");
                }

                Task task = new Task(taskType, iTaskID, iOrder, szDetails);

                //Creates the correct child class depending on the task type, and adds details in
                switch (taskType)
                {
                    case tpll:
                        Tpll tpll = new Tpll(plugin, player, task, parentGroupPlaythrough);
                        tasks.add(tpll);
                        break;
                    case selection:
                        Selection selection = new Selection(plugin, player, task, parentGroupPlaythrough);
                        tasks.add(selection);
                        break;
                    case command:
                        Command command = new Command(plugin, player, task, parentGroupPlaythrough, tasks);
                        tasks.add(command);
                        break;
                    case chat:
                        Chat chat = new Chat(plugin, player, task, parentGroupPlaythrough);
                        tasks.add(chat);
                        break;
                    case place:
                        Place place = new Place(plugin, player, task, parentGroupPlaythrough);
                        tasks.add(place);
                        break;
                }
            }
        }
        catch (SQLException se)
        {
            plugin.getLogger().log(Level.SEVERE, ChatColor.RED + "SQL - SQL Error fetching Tasks by LocationID and GroupID", se);
        }
        catch (Exception e)
        {
            plugin.getLogger().log(Level.SEVERE, ChatColor.RED + "SQL - Non SQL Error fetching Tasks by LocationID and GroupID", e);
        }
        return tasks;
    }

}
