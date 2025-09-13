package teachingtutorials.tutorialplaythrough;

import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialobjects.Group;
import teachingtutorials.tutorialobjects.Task;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;

/**
 * Represents a play-through of a Group
 */
public class GroupPlaythrough
{
    /** The group with this group play-through is a play-through of */
    private final Group group;

    /** Stores which task of the group that the player is on. It is 1 indexed. If on first task, this will be 1 */
    private int taskNo;

    /** A reference to the task that the player is current playing through */
    private PlaythroughTask currentTask;

    /** Tracks the current status of the group playthrough*/
    private StepPlaythroughStatus status;

    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /** A reference to the step which this group belongs to */
    protected final StepPlaythrough parentStepPlaythrough;

    /** A list of play-through tasks to be performed for this group */
    private ArrayList<PlaythroughTask> tasks = new ArrayList<>();

    /**
     * Used to construct a group class as part of a play-through
     * @param group The group which this group play-through is a play-through of
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @param parentStepPlaythrough A reference to the step which this group belongs to
     */
    GroupPlaythrough(Group group, TeachingTutorials plugin, StepPlaythrough parentStepPlaythrough)
    {
        this.plugin = plugin;
        this.parentStepPlaythrough = parentStepPlaythrough;
        this.group = group;
        this.status = StepPlaythroughStatus.SubsNotFetched;
    }

    /**
     * @return A list of play-through tasks for this group
     */
    public ArrayList<PlaythroughTask> getTasks()
    {
        return tasks;
    }

    /**
     *
     * @return A reference to the group
     */
    public Group getGroup()
    {
        return group;
    }

    /**
     *
     * @return A reference to the parent step play-through
     */
    public StepPlaythrough getParentStep()
    {
        return parentStepPlaythrough;
    }

    /**
     *
     * @return A reference to the current task which the player is on
     */
    public PlaythroughTask getCurrentTask()
    {
       return this.currentTask;
    }

    /**
     *
     * @return A reference to the number of the task in the group that the player is on, 1 indexed.
     */
    public int getTaskNo()
    {
        return this.taskNo;
    }

    /**
     *
     * @return Whether or not all of the tasks in the group have been completed
     */
    public boolean getFinished()
    {
        return this.status.equals(StepPlaythroughStatus.Finished);
    }

    /**
     * Fetches a list of playthrough tasks from the database and initialises them, then adds them to the group's list
     */
    public void fetchAndInitialiseTasks()
    {
        plugin.getLogger().log(Level.FINE, "Fetching and initialising tasks");

        if (this.parentStepPlaythrough.getParentStage().getTutorialPlaythrough().getCurrentPlaythroughMode().equals(PlaythroughMode.CreatingLocation))
        {
            plugin.getLogger().log(Level.FINE,"GroupPlaythrough.fetchAndInitialiseTasks(): Fetching tasks without answers");
            tasks = PlaythroughTask.fetchTasksWithoutAnswers(plugin, plugin.getDBConnection(), this, parentStepPlaythrough.parentStagePlaythrough.getPlayer());
            plugin.getLogger().log(Level.INFO,"Group with ID + " +this.group.getGroupID() +": "+tasks.size() +" tasks found");
        }
        else
        {
            tasks = PlaythroughTask.fetchTasksForLocation(plugin, plugin.getDBConnection(), parentStepPlaythrough.parentStagePlaythrough.getLocationID(), this, parentStepPlaythrough.parentStagePlaythrough.tutorialPlaythrough.getCreatorOrStudent().player);
        }

        this.status = StepPlaythroughStatus.SubsFetched;
    }

    /**
     * Calls all tasks within this group to display their virtual blocks
     */
    public void displayAllVirtualBlocks()
    {
        //Gets the tasks from the DB
        if (this.status.equals(StepPlaythroughStatus.SubsNotFetched))
        {
            fetchAndInitialiseTasks();
        }

        //Goes through each task and calls it to display its virtual blocks
        int iNumTasks = tasks.size();
        for (int i = 0 ; i < iNumTasks ; i++)
        {
            tasks.get(i).displayVirtualBlocks();
        }
    }

    /**
     * Fetches all of the groups for this task, then registers the first task of the group
     */
    public void startGroupPlaythrough()
    {
        //Log to console
        plugin.getLogger().log(Level.INFO, "Setting up group playthrough of group with Group ID "+this.group.getGroupID());

        //Ensures that the tasks are fetched and initialised
        if (this.status.equals(StepPlaythroughStatus.SubsNotFetched))
        {
            fetchAndInitialiseTasks();
        }
        else
            plugin.getLogger().log(Level.FINE, "Tasks already fetched and initialised for this group");

        //Checks whether there are actually any tasks in the group
        if (tasks.size() > 0)
        {
            //Gets the first task, sets the current task to this
            currentTask = tasks.get(0);

            //Registers the first tasks. Tasks unregister themselves once complete
            currentTask.register();
            plugin.getLogger().log(Level.FINE, "First task registered: "+currentTask.getLocationTask().getType());

            status = StepPlaythroughStatus.ActiveStarted;
        }
        else //If there are no tasks in the group, move on
        {
            //Signal that group is complete before it even started
            status = StepPlaythroughStatus.Finished;

            //Log to console
            if (this.getParentStep().parentStagePlaythrough.getTutorialPlaythrough() instanceof Lesson lesson)
            {
                plugin.getLogger().log(Level.INFO, "Lesson: "+lesson.getLessonID() +". Group with GroupID "
                        +this.getGroup().getGroupID() +" (of step with StepID "+this.getParentStep().getStep().getStepID()
                        +") has been completed");
            }
            else
            {
                plugin.getLogger().log(Level.INFO, "New location of " +this.getParentStep().getParentStage().getPlayer().getName() +". Group with GroupID "
                        +this.getGroup().getGroupID() +" (of step with StepID "+this.getParentStep().getStep().getStepID()
                        +") has been completed");
            }
            parentStepPlaythrough.groupFinished();
        }
        //Sets the current task number to the first task
        //1 indexed
        taskNo = 1;
    }

    /**
     * Acknowledges completion of a task. If group is now complete, informs the parent step, else, registers the next
     * task of the group. Informs the parent step that the step now has progress.
     */
    public void taskFinished()
    {
        parentStepPlaythrough.notifyStepInProgress();

        //taskNo is that of the previous, so it is the correct index of the next
        //taskNo is 1 indexed
        if (taskNo >= tasks.size()) //If the task was the last one in the group
        {
            //Signal that group is complete
            status = StepPlaythroughStatus.Finished;

            //Log to console
            if (this.getParentStep().parentStagePlaythrough.getTutorialPlaythrough() instanceof Lesson lesson)
            {
                plugin.getLogger().log(Level.INFO, "Lesson: "+lesson.getLessonID() +". Group with GroupID "
                        +this.getGroup().getGroupID() +" (of step with StepID "+this.getParentStep().getStep().getStepID()
                        +") has been completed");
            }
            else
            {
                plugin.getLogger().log(Level.INFO, "New location of " +this.getParentStep().getParentStage().getPlayer().getName() +". Group with GroupID "
                        +this.getGroup().getGroupID() +" (of step with StepID "+this.getParentStep().getStep().getStepID()
                        +") has been completed");
            }
            parentStepPlaythrough.groupFinished();
        }
        else //Registers the next task
        {
            //Gets the next task, sets the current task to that
            currentTask = tasks.get(taskNo);

            //Registers the task. Tasks unregister themselves once complete
            currentTask.register();
            plugin.getLogger().log(Level.INFO,"Next task registered: "+currentTask.getLocationTask().getType());

            //Moves the task counter on
            taskNo++;
        }
    }

    /**
     * Calls the current task to unregister and deactivate and to remove the virtual blocks of this task
     */
    public void terminateEarly()
    {
        int iNumTask = tasks.size();
        for (int i = iNumTask - 1 ; i >= 0 ; i--)
        {
            tasks.get(i).unregister();
        }
        status = StepPlaythroughStatus.Finished;
    }

    /**
     * Retrieves from the database the list of groups for the specified step and creates GroupPlaythrough objects from
     * these
     * @param plugin The instance of the plugin
     * @param stepPlaythrough The stage for which all groups must be retrieved
     * @return An arraylist of GroupPlaythroughs for this step
     */
    public static ArrayList<GroupPlaythrough> fetchGroupsByStepID(TeachingTutorials plugin, StepPlaythrough stepPlaythrough)
    {
        //Initialises the array
        ArrayList<GroupPlaythrough> groupPlaythroughs = new ArrayList<>();

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch groups
            sql = "Select * FROM `Groups` WHERE `StepID` = "+ stepPlaythrough.getStep().getStepID();
            SQL = plugin.getDBConnection().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                Group group = new Group(resultSet.getInt("GroupID"));
                GroupPlaythrough groupPlaythrough = new GroupPlaythrough(group, plugin, stepPlaythrough);
                groupPlaythroughs.add(groupPlaythrough);
            }
        }
        catch (SQLException se)
        {
            plugin.getLogger().log(Level.SEVERE, "SQL - SQL Error fetching Groups by StepID", se);
        }
        catch (Exception e)
        {
            plugin.getLogger().log(Level.SEVERE, "SQL - Non-SQL Error fetching Groups by StepID", e);
        }
        return groupPlaythroughs;
    }
}
