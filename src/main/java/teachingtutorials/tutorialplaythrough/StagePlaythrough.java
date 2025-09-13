package teachingtutorials.tutorialplaythrough;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialobjects.Location;
import teachingtutorials.tutorialobjects.Stage;
import teachingtutorials.utils.Display;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;

public class StagePlaythrough
{
    /** A reference to the stage which this stage playthrough is a play-through of */
    private final Stage stage;

    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /** A reference to the instance of the player who is doing this stage play-through */
    private final Player player;

    /** A reference to the tutorials playthrough which this stage is a part of */
    final TutorialPlaythrough tutorialPlaythrough;

    /** Tracks the current status of the stage playthrough*/
    private StepPlaythroughStatus status;

    /** A list of all of the steps which much be completed as part of this stage */
    private ArrayList<StepPlaythrough> stepPlaythroughs = new ArrayList<>();

    /** The index (0 indexed) of the step to start next. Therefore also equals the step currently on if 1 indexed */
    private int iCurrentStep;

    /** A reference to the current step playthrough which the player is on */
    StepPlaythrough currentStepPlaythrough;


    //--------------------------------------------------
    //-------------------Constructors-------------------
    //--------------------------------------------------

    /**
     * Used for initiating a stage playthrough, either as part of a Lesson or as part of a creation of a new Location
     * @param player A reference to the instance of the player who is doing this stage play-through
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @param stage A reference to the stage which this stage playthrough is a play-through of
     * @param tutorialPlaythrough A reference to the playthrough which this stage is a part of
     */
    StagePlaythrough(Player player, TeachingTutorials plugin, Stage stage, TutorialPlaythrough tutorialPlaythrough)
    {
        //Inherited properties from the lesson
        this.plugin = plugin;
        this.player = player;
        this.tutorialPlaythrough = tutorialPlaythrough;

        this.stage = stage;

        //Other
        this.status = StepPlaythroughStatus.SubsNotFetched;
    }


    //---------------------------------------------------
    //----------------------Getters----------------------
    //---------------------------------------------------
    public Stage getStage()
    {
        return stage;
    }

    public int getLocationID()
    {
        return this.tutorialPlaythrough.getLocation().getLocationID();
    }

    public Location getLocation()
    {
        return tutorialPlaythrough.getLocation();
    }

    public TutorialPlaythrough getTutorialPlaythrough()
    {
        return tutorialPlaythrough;
    }

    public Player getPlayer()
    {
        return player;
    }

    public int getCurrentStep()
    {
        return iCurrentStep;
    }

    public boolean isFirstStage()
    {
        return (stage.getOrder() == 1);
    }

    /**
     *
     * @return Whether all of the steps within this stage have been completed
     */
    public boolean isFinished()
    {
        return (status.equals(StepPlaythroughStatus.Finished));
    }

    /**
     * Checks the current status for equivalence to StepPlaythroughStatus.ActiveStarted
     * @return Whether any tasks of this stage have been completed
     */
    public boolean inProgress()
    {
        return status.equals(StepPlaythroughStatus.ActiveStarted);
    }

    /**
     * Displays the virtual blocks of all tasks in all steps of this stage up to and including the iStepTH step
     * @param iStep The step up to and including which to display virtual blocks for. (This is 1 indexed. The first step has iStep = 1).
     *                     <p> </p>
     *                     If iStep = -1 then all blocks will be displayed
     *                     <P> If iStep = 0 then no blocks will be displayed</P>
     *                     <P> If iStep = 1 then the blocks from step 1 will be displayed</P>
     */
    public void displayAllVirtualBlocks(int iStep)
    {
        //Gets the steps of this stage from the DB
        if (status.equals(StepPlaythroughStatus.SubsNotFetched))
            fetchAndInitialiseSteps();

        //The number of steps to display the virtual blocks for
        int iNumStepsToDisplay;

        if (iStep == -1)
            iNumStepsToDisplay = stepPlaythroughs.size();
        else
            iNumStepsToDisplay = iStep;

        //Calls each step to display all relevant virtual blocks
        for (int i = 0 ; i < iNumStepsToDisplay ; i++)
        {
            stepPlaythroughs.get(i).displayAllVirtualBlocks();
        }
    }

    /**
     * Fetches the list of steps of this stage from the database and stores this in {@link #stepPlaythroughs}. The list is ordered.
     */
    private void fetchAndInitialiseSteps()
    {
        //Gets a list of all of the steps of the specified stage and loads each with the relevant data.
        //List is in order of step 1 1st
        stepPlaythroughs = StepPlaythrough.fetchStepsByStageID(player, plugin, this);
        status = StepPlaythroughStatus.SubsFetched;
    }

    /**
     * Begins the playthrough of this stage: Displays the stage title, fetches and initialises each step, and begins the
     * first step
     * @param iStepToStartStageOn The step to start the stage on (1 indexed). For example, to start the stage from the
     *                            start (step 1), this value should be 1.
     * @param bDelayTitle Whether to delay the display of the title of the stage and title of the first step
     */
    public void startStage(int iStepToStartStageOn, boolean bDelayTitle)
    {
        //Starting new stage message
        if (this.getTutorialPlaythrough() instanceof Lesson lesson)
        {
            plugin.getLogger().log(Level.INFO, "Lesson: "+lesson.getLessonID() +". Stage " +this.stage.getOrder()
                    +" (" +this.stage.getName() +") of tutorial with ID "+this.getTutorialPlaythrough().getTutorial().getTutorialID() +" starting.");
        }
        else
        {
            plugin.getLogger().log(Level.INFO, "New location of " +this.getTutorialPlaythrough().creatorOrStudent.player.getName() +". Stage " +this.stage.getOrder()
                    +" (" +this.stage.getName() +") of tutorial with ID "+this.getTutorialPlaythrough().getTutorial().getTutorialID() +" starting.");
        }

        //Display the Stage title
        if (bDelayTitle)
        {
            Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
                @Override
                public void run() {
                    Display.Title(player, " ", ChatColor.AQUA +"Stage " +stage.getOrder() +" - " +stage.getName(), 10, 60, 12);
                }
            }, this.plugin.getConfig().getLong("Stage_Title_Delay_On_Start"));
        }
        else
            Display.Title(player, " ", ChatColor.AQUA +"Stage " +stage.getOrder() +" - " +stage.getName(), 10, 60, 12);

        //Get the steps
        if (status.equals(StepPlaythroughStatus.SubsNotFetched))
        {
            fetchAndInitialiseSteps();
        }

        if (iStepToStartStageOn > stepPlaythroughs.size())
            iStepToStartStageOn = stepPlaythroughs.size();

        //Takes the step back, for it to be increased in the next step - as in, we are currently on step 0 to make the
        // next step be step 1
        iCurrentStep = iStepToStartStageOn - 1;
        nextStep(bDelayTitle);

        //Update the stage status
        if (iCurrentStep == 1)
            status = StepPlaythroughStatus.SubsRegistered;
        else
            status = StepPlaythroughStatus.ActiveStarted;
    }

    /**
     * Gets the next step and launches it, or if all steps have now been completed, will call for the next stage to be
     * started.
     * @param bDelayTitle Whether to delay the display of the title of the step
     */
    protected void nextStep(boolean bDelayTitle)
    {
        //1 indexed - increment the step
        iCurrentStep++;

        if (iCurrentStep <= stepPlaythroughs.size())
        {
            //It's the pair that we need to check, they work together.

            //If the current stage is higher than the one they have fully completed (they are doing a stage they have never completed)
            if (tutorialPlaythrough.iStageIndex > tutorialPlaythrough.iHighestStageCompleted)
            {
                plugin.getLogger().log(Level.INFO, "You are further on than the highest stage completed. Checking step update.");
                //iCurrentStep - 1 is the step just completed.
                if (iCurrentStep - 1 > tutorialPlaythrough.iHighestStepCompleted)
                {
                    plugin.getLogger().log(Level.INFO, "That step is higher than the highest completed step so far, updating");
                    tutorialPlaythrough.iHighestStepCompleted = iCurrentStep - 1;
                }
            }

            //Save the positions of stage and step
            if (tutorialPlaythrough instanceof Lesson lesson)
                lesson.savePositions();

            //Uses -1 because iCurrentStep is 1 indexed, so need it in computer terms
            currentStepPlaythrough = stepPlaythroughs.get(iCurrentStep-1);
            currentStepPlaythrough.startStep(bDelayTitle);
        }
        else //Stage finished
        {
            //At this point we put the highest step completed to 0 as we are updating the stage
            //It's the pair that we need to check, they work together.
            if (tutorialPlaythrough.iStageIndex > tutorialPlaythrough.iHighestStageCompleted)
            {
                tutorialPlaythrough.iHighestStepCompleted = 0;
                tutorialPlaythrough.iHighestStageCompleted = tutorialPlaythrough.iStageIndex;
            }

            //Save the positions of stage and step
            if (tutorialPlaythrough instanceof Lesson lesson)
                lesson.savePositions();

            status = StepPlaythroughStatus.Finished;
            tutorialPlaythrough.nextStage(1, false);
        }
    }

    /**
     * Notifies the stage playthrough that one of its tasks has been finished.
     * <p></p>
     * <b>Do not make the mistake of calling this immediately after calling that a group is finished</b>. If you do
     * you may inadvertently remark the step as still active even if all groups have finished.
     */
    void notifyStageInProgress()
    {
        //Blocks the issue a group calling that a task was complete after the step was marked as finished
        if (!this.status.equals(StepPlaythroughStatus.Finished))
            this.status = StepPlaythroughStatus.ActiveStarted;
    }

    /**
     * If parts of the current step have been completed, will reset the player back to the start of the current step.
     * <p></p>
     * If no progress has been made on the current step, will take the player back to the start of the previous step, provided that a previous step exists.
     * <p></p>
     */
    void previousStep()
    {
        //If the current step has progress, reset to the start
        if (currentStepPlaythrough.inProgress())
        {
            //Reset progress
            currentStepPlaythrough.terminateEarly();
            currentStepPlaythrough.startStep(false);

            //Update the stage status if we've reset to the first step of a stage
            if (iCurrentStep == 1)
                status = StepPlaythroughStatus.SubsRegistered;

            //Save the positions if moved
            if (tutorialPlaythrough instanceof Lesson lesson)
                lesson.savePositions();
        }

        //If the step has no progress and there is a previous step, reset to the start of the previous step
        else if (iCurrentStep > 1)
        {
            //Terminate and start previous step
            currentStepPlaythrough.terminateEarly();
            iCurrentStep--;
            currentStepPlaythrough = stepPlaythroughs.get(iCurrentStep-1);
            //Reset the new step
            currentStepPlaythrough.terminateEarly();
            currentStepPlaythrough.startStep(false);

            //Update the stage status if we've reset to the first step of a stage
            if (iCurrentStep == 1)
                status = StepPlaythroughStatus.SubsRegistered;

            //Save the positions if moved
            if (tutorialPlaythrough instanceof Lesson lesson)
                lesson.savePositions();
        }

        //If the start of the stage see if there is a previous stage to move to the end of
        //Attempt to move to the previous stage
        else if (tutorialPlaythrough.iStageIndex > 1)
        {
            //Move to previous stage
            currentStepPlaythrough.terminateEarly();

            iCurrentStep = 1;

            //Will call for the previous step to start. Starts the previous stage at the final step.
            tutorialPlaythrough.previousStageStepBack();
        }
    }

    /**
     * Moves a player to the start of the next step, if they have already completed the current step.
     */
    void skipStep()
    {
        //If the current stage is lower than or equal to the highest stage they have completed then we know they can automatically be moved on
        //If they are on the stage after the highest stage they have completed, check that they are on a step
        // lower than or equal to one they have already completed
        if (tutorialPlaythrough.iStageIndex <= tutorialPlaythrough.iHighestStageCompleted || (tutorialPlaythrough.iStageIndex == tutorialPlaythrough.iHighestStageCompleted + 1 && iCurrentStep <= tutorialPlaythrough.iHighestStepCompleted))
        {
            //Check that there are any steps left in this stage
            if (iCurrentStep == stepPlaythroughs.size())
                //If not, just call a move to the next stage
                tutorialPlaythrough.skipStage();
            else
            {
                //Terminate current step
                currentStepPlaythrough.terminateEarly();
                currentStepPlaythrough.displayAllVirtualBlocks();

                //Really we need a way to display all the blocks without terminating in the first place
                //Boolean on the terminate thing?

                //Update the stage status
                status = StepPlaythroughStatus.ActiveStarted;

                currentStepPlaythrough = stepPlaythroughs.get(iCurrentStep);
                iCurrentStep++;
                currentStepPlaythrough.startStep(false);
            }

            //Save the positions if moved
            if (tutorialPlaythrough instanceof Lesson lesson)
                lesson.savePositions();
        }

    }

    /**
     *
     * @return Whether the player can navigate to the previous step
     */
    public boolean canMoveBackStep()
    {
        return currentStepPlaythrough.inProgress() || iCurrentStep > 1 || tutorialPlaythrough.iStageIndex > 1;
    }

    /**
     *
     * @return Whether the player can navigate to the next step
     */
    public boolean canMoveForwardsStep()
    {
        return tutorialPlaythrough.iStageIndex <= tutorialPlaythrough.iHighestStageCompleted || (tutorialPlaythrough.iStageIndex == tutorialPlaythrough.iHighestStageCompleted + 1 && iCurrentStep <= tutorialPlaythrough.iHighestStepCompleted);
    }

    /**
     * Safely terminates all of the steps in this stage. This deregisters and removes virtual blocks for all steps.
     */
    public void terminateEarly()
    {
        int iNumSteps = stepPlaythroughs.size();
        for (int i = iNumSteps - 1 ; i >= 0 ; i--)
        {
            stepPlaythroughs.get(i).terminateEarly();
        }
    }

    /**
     * Fetches a list of stages from the DB, creates a set of StagePlaythroughs from these, knowing the location, and
     * returns this list. The list is ordered, such that the first stage to play through is at index 0.
     * @param player A reference to the player for whom the lesson will be for
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @param tutorialPlaythrough A reference to the tutorialPlaythrough for which these stage playtrhoughs will be a part of
     * @return An array list of StagePlaythroughs for the relevant lesson, tutorial and location
     */
    public static ArrayList<StagePlaythrough> fetchStagesByTutorialIDForPlaythrough(Player player, TeachingTutorials plugin, TutorialPlaythrough tutorialPlaythrough)
    {
        ArrayList<StagePlaythrough> stagePlaythroughs = new ArrayList<>();

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch stages
            sql = "SELECT * FROM `Stages` WHERE `TutorialID` = "+tutorialPlaythrough.getTutorial().getTutorialID() +" ORDER BY 'Order' ASC";
            SQL = plugin.getDBConnection().getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                Stage stage = new teachingtutorials.tutorialobjects.Stage(resultSet.getInt("StageID"), resultSet.getInt("Order"), resultSet.getString("StageName"));
                StagePlaythrough stagePlaythrough = new StagePlaythrough(player, plugin, stage, tutorialPlaythrough);
                stagePlaythroughs.add(stagePlaythrough);
            }
        }
        catch (SQLException se)
        {
            plugin.getLogger().log(Level.SEVERE, "SQL - SQL Error fetching Stages by TutorialID", se);
        }
        catch (Exception e)
        {
            plugin.getLogger().log(Level.SEVERE, "SQL - Non-SQL Error fetching Stages by TutorialID", e);
        }
        return stagePlaythroughs;
    }
}
