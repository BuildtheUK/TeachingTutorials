package teachingtutorials.tutorialplaythrough;

import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.guis.TutorialNavigationMenu;
import teachingtutorials.listeners.Falling;
import teachingtutorials.listeners.PlaythroughCommandListeners;
import teachingtutorials.tutorialobjects.Location;
import teachingtutorials.tutorialobjects.Tutorial;
import teachingtutorials.utils.Mode;
import teachingtutorials.utils.User;
import teachingtutorials.utils.VirtualBlockGroup;

import java.util.ArrayList;
import java.util.Stack;

/**
 * Represents a playthrough of a tutorial. This can be either a Lesson or a Location Creation scenario.
 */
public abstract class TutorialPlaythrough
{
    /** A reference to the plugin instance */
    protected final TeachingTutorials plugin;

    /** The user doing this playthrough */
    protected final User creatorOrStudent;

    /** The tutorial of the playthrough */
    protected final Tutorial tutorial;

    /** The location of the tutorial for this playthrough */
    protected Location location;

    /** A list of stage playthroughs which ust be completed as part of this tutorial playthrough */
    private final ArrayList<StagePlaythrough> stagePlaythroughs;

    /** A reference to the current stage*/
    protected StagePlaythrough currentStagePlaythrough;

    /** The index (0 indexed) of the stage to start next. Therefore also equals the stage currently on if 1 indexed
     * <p></p>
     * A playthrough starts on iStageIndex = 0; It is incremented when nextStage() is called.
     */
    protected int iStageIndex;

    /**
     * The highest step that has been fully completed as part of this lesson. (1 index).
     * <p> </p>
     * In combination with the highest stage completed variable, this is used to determine the step which
     * a player can wind forwards to.
     */
    protected int iHighestStepCompleted;

    /**
     * The highest stage that has been fully completed as part of this lesson. (1 index).
     * <p> </p>
     * In combination with the highest step completed variable, this is used to determine the stage which
     * a player can wind forwards to.
     */
    protected int iHighestStageCompleted;

    /** The current mode which the playthrough is in */
    private PlaythroughMode currentPlaythroughMode;

    /** The listener listening out for if a player falls into the void */
    protected Falling fallListener;

    /** Enables tpll, ll and controls gmask */
    protected PlaythroughCommandListeners playthroughCommandListeners;

    /** The Tutorial Navigation Menu for this playthrough */
    protected final TutorialNavigationMenu navigationMenu;

    /** A list of spies also viewing the virtual blocks for this playthrough */
    private ArrayList<Player> spies = new ArrayList<>();

    /**
     * Constructs the TutorialsPlaythrough object and loads a list of StagePlaythroughs for the relevant tutorial into
     * its list. The list is ordered, with the first stage being at index 0, etc. .
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @param creatorOrStudent A reference to the user who is to do this playthrough
     * @param tutorial A reference to the tutorial of which this is a playthrough
     */
    public TutorialPlaythrough(TeachingTutorials plugin, User creatorOrStudent, Tutorial tutorial, PlaythroughMode playthroughMode)
    {
        this.plugin = plugin;
        this.creatorOrStudent = creatorOrStudent;
        this.tutorial = tutorial;
        this.currentPlaythroughMode = playthroughMode;

        //Fetches a list of stage playthroughs for this lesson and puts that list into the main list
        this.stagePlaythroughs = StagePlaythrough.fetchStagesByTutorialIDForPlaythrough(this.getCreatorOrStudent().player, plugin, this);

        //Sets up the nav menu
        navigationMenu = new TutorialNavigationMenu(this, plugin);
    }

    /**
     *
     * @return A reference to the tutorial being completed
     */
    public Tutorial getTutorial()
    {
        return tutorial;
    }

    /**
     *
     * @return A reference to the location of this tutorial playthrough
     */
    public Location getLocation()
    {
        return location;
    }

    /**
     *
     * @return A reference to the user do this playthrough
     */
    public User getCreatorOrStudent()
    {
        return creatorOrStudent;
    }

    public PlaythroughMode getCurrentPlaythroughMode()
    {
        return currentPlaythroughMode;
    }

    public boolean setCurrentPlaythroughMode(PlaythroughMode playthroughMode)
    {
        //Checks to see if current player is the creator of the tutorial they are playing
        if (!creatorOrStudent.player.getUniqueId().equals(tutorial.getUUIDOfAuthor()))
        {
            return false;
        }

        //Block changes during location creation
        if (currentPlaythroughMode.equals(PlaythroughMode.CreatingLocation))
            return false;

        //Check to see if the mode is actually to be changed
        if (!currentPlaythroughMode.equals(playthroughMode))
        {
            //Update the mode
            this.currentPlaythroughMode = playthroughMode;

            //Perform any necessary actions to adjust gameplay
            try
            {
                currentStagePlaythrough.currentStepPlaythrough.switchPlaythroughMode();

                //Take the location edit menu away from the user and refresh the navigation menu
                if (playthroughMode.equals(PlaythroughMode.PlayingLesson))
                {
                    this.navigationMenu.refresh();
                    this.creatorOrStudent.mainGui = this.navigationMenu;
                }

                //That's all kinda global stuff though. No actual step editor menu changes

                return true;
            }
            catch (NullPointerException e)
            {
                return false;
            }
        }
        else
        {
            return true;
        }
    }

    public void openNavigationMenu()
    {
        this.navigationMenu.refresh();
        this.creatorOrStudent.mainGui = this.navigationMenu;
        this.creatorOrStudent.mainGui.open(creatorOrStudent.player);
    }

    public void openStepEditorMenu()
    {
        //Checks to see if current player is the creator of the tutorial they are playing
        if (!creatorOrStudent.player.getUniqueId().equals(tutorial.getUUIDOfAuthor()))
        {
            return;
        }
        this.creatorOrStudent.mainGui = this.currentStagePlaythrough.currentStepPlaythrough.getEditorMenu();
        this.creatorOrStudent.mainGui.open(creatorOrStudent.player);
    }

    /**
     * Returns the list of spies on this playthrough
     */
    public ArrayList<Player> getSpies()
    {
        return spies;
    }

    /**
     * Add a spy to the list of spies viewing virtual blocks for this playthrough. Updates the user's spy target.
     * <P> </P>
     * If a player is already spying on a separate tutorial, it will remove them from that first and then add them to this one.
     * @param player The player to add
     */
    public void addSpy(Player player)
    {
        //Check perms to spy
        if (player.hasPermission("teachingtutorials.canspy"))
        {
            //Identify the user instance
            User spyUser = User.identifyUser(plugin, player);
            if (spyUser != null)
            {
                //Ensures they are idle
                if (spyUser.getCurrentMode().equals(Mode.Idle))
                {
                    //Check if they are already spying and if so, remove them from that
                    if (spyUser.isSpying())
                        spyUser.disableSpying();

                    //Add them as a spy to this tutorial playthrough
                    spies.add(player);

                    //Mark the spy user's spy target to this playthrough
                    spyUser.setSpyTarget(this);

                    //Refreshes the hologram visible list
                    this.currentStagePlaythrough.currentStepPlaythrough.refreshHologramViewers();

                    //Refresh happens frequently so no need to call for an adhoc refresh
                }
            }
        }
    }

    /**
     * Removes a player from the list of spies viewing virtual blocks for this playthrough and resets their view.
     * <P> </P>
     * Also stops the spy from viewing the holograms of this playthrough
     * <p> </p>
     * This method will also set the spy target of the User to null.
     * @param player The player to remove
     */
    public void removeSpy(Player player)
    {
        //Removes the player from the list of spies
        if (spies.remove(player))
        {
            //Resets all of their blocks
            //Get the list of virtual block groups
            VirtualBlockGroup[] virtualBlockGroups = this.plugin.getVirtualBlockGroups().toArray(VirtualBlockGroup[]::new);

            //Declares the temporary list object
            VirtualBlockGroup<org.bukkit.Location, BlockData> virtualBlockGroup;

            //Goes through all virtual block groups - will do this going from end of tutorial to start
            int iTasksActive = virtualBlockGroups.length;
            for (int j = iTasksActive-1 ; j >=0 ; j--)
            {
                //Extracts the jth virtual block group
                virtualBlockGroup = virtualBlockGroups[j];

                //Resets the virtual blocks to the real blocks of this spy if the group was of this playthrough
                if (virtualBlockGroup.isOfPlaythrough(this))
                {
                    //Call for the world blocks to be reset
                    virtualBlockGroup.removeVirtualBlocksForSpy(player);
                }
            }

            //Makes the visible hologram invisible to the spy
            this.currentStagePlaythrough.currentStepPlaythrough.removePlayerFromHologram(player);

            //Identify the user instance and set the user's spy target to null
            User spyUser = User.identifyUser(plugin, player);
            if (spyUser != null)
            {
                spyUser.setSpyTarget(null);
            }
        }
    }

    /**
     * Removes all spies and resets their views. Also sets the spytarget of their User instances to null.
     */
    private void removeAllSpies()
    {
        int iNumSpies = getSpies().size();
        while (iNumSpies > 0)
        {
            removeSpy(getSpies().get(0).getPlayer());
            iNumSpies--;
        }
    }

    /**
     * Returns whether this playthrough is being spied on by the given player
     * @param player The player to query
     * @return True if this playthrough's list of spies contains the given player, False if not.
     */
    public boolean hasSpy(Player player)
    {
        return this.spies.contains(player);
    }

    /**
     * Sets the 'safe' location of the fall listener, i.e where they will be teleported to should then fall into the void
     * @param location A bukkit location object representing the position of the safe location
     */
    public void setFallListenerSafeLocation(org.bukkit.Location location)
    {
        //Raises the safe location by 1 block to ensure players do not tp inside blocks as occurred at times
        fallListener.setSafeLocation(location.add(0, 1, 0));
    }

    /**
     * Displays all of the virtual blocks for this tutorial and location up to and including the provided step and stage
     * @param iStage The stage up to and including which to display the blocks for (1 indexed)
     * @param iStep The step within the aforementioned stage up to and including which to display the blocks up to (1 indexed)
     */
    public void displayVirtualBlocks(int iStage, int iStep)
    {
        //Goes through all stages up to and including the one to display
        for (int i = 1 ; i <= iStage ; i++)
        {
            //Checks whether we are at the last stage to display or not
            if (i == iStage)
                //Displays virtual blocks up to the step they are on
                stagePlaythroughs.get(i-1).displayAllVirtualBlocks(iStep);
            else
                //If this is not the last stage, display the virtual blocks of all steps
                stagePlaythroughs.get(i-1).displayAllVirtualBlocks(-1);

        }
    }

    /**
     * Moves the tutorial on to the next stage. Accessed after the end of each stage (Called from StagePlaythrough.endStage() asynchronously)
     * or at the start of the playthrough.
     * @param iStepToStartStageOn The step to start the stage on (1 indexed). For example, to start the stage from the start (step 1), this value should be 1.
     * @param bDelayTitle Whether to delay the displaying of the title - used for example when there may be a delay in the player rendering the location at the start
     */
    public void nextStage(int iStepToStartStageOn, boolean bDelayTitle)
    {
        //Gets the number of stages
        int iNumStages = stagePlaythroughs.size();

        //Increases the stage index as the next stage is being started
        iStageIndex++;

        //Checks to see whether there is a next stage or if they have now completed the tutorial
        if (iStageIndex <= iNumStages)
        {
            currentStagePlaythrough = stagePlaythroughs.get(iStageIndex-1);
            currentStagePlaythrough.startStage(iStepToStartStageOn, bDelayTitle);
        }
        else
        {
            endPlaythrough();
        }
    }

    // -----------------------------------------------------------------------------------------------
    // ------------------------------------- Tutorial Navigation -------------------------------------
    // -----------------------------------------------------------------------------------------------

    /**
     * If parts of the current stage have been completed, will reset the player back to the start of the current stage.
     * <p></p>
     * If no progress has been made on the current stage, will take the player back to the start of the previous stage, provided that a previous stage exists.
     * <p></p>
     */
    public void previousStage()
    {
        //Checks if the stage has progress
        if (currentStagePlaythrough.inProgress()) //Has progress
        {
            //If in progress, attempt to reset to the start of the stage
            currentStagePlaythrough.terminateEarly();
            currentStagePlaythrough.startStage(1, false);

            //Save the positions if moved
            if (this instanceof Lesson lesson)
                lesson.savePositions();
        }

        else //Has no progress - attempt move to previous stage
        {
            //Only move to start of previous stage if there is one
            if (iStageIndex > 1)
            {
                //Terminate and start previous stage from start
                currentStagePlaythrough.terminateEarly();
                iStageIndex--;
                currentStagePlaythrough = stagePlaythroughs.get(iStageIndex - 1);
                //Reset the stage
                currentStagePlaythrough.terminateEarly();
                currentStagePlaythrough.startStage(1, false);

                //Save the positions if moved
                if (this instanceof Lesson lesson)
                    lesson.savePositions();
            }
        }
    }

    /**
     * Moves the player to the final step of the previous stage. Saves the positions if moved.
     */
    public void previousStageStepBack()
    {
        //Checks that there is a previous stage
        if (iStageIndex > 1)
        {
            //Close the current stage
            currentStagePlaythrough.terminateEarly(); //This should already be called but we call again

            //Move to the previous stage
            iStageIndex--;
            currentStagePlaythrough = stagePlaythroughs.get(iStageIndex-1);

            //Reset the new stage
            currentStagePlaythrough.terminateEarly();

            //Displays all virtual blocks but the last step
            currentStagePlaythrough.displayAllVirtualBlocks(currentStagePlaythrough.getStage().steps.size() - 1);

            //Starts the stage on the largest step
            currentStagePlaythrough.startStage(Integer.MAX_VALUE, false);

            //Save the positions if moved
            if (this instanceof Lesson lesson)
                lesson.savePositions();
        }
    }

    /**
     * Moves a player to the start of the next step, if they have already completed the current stage.
     */
    public void skipStage()
    {
        //If they have already completed this stage or one above it, attempt to move them on
        if (iStageIndex <= iHighestStageCompleted)
        {
            //Only move them on if there is a higher stage
            if (iStageIndex < stagePlaythroughs.size())
            {
                //Terminate and start next stage from start
                currentStagePlaythrough.terminateEarly();
                currentStagePlaythrough.displayAllVirtualBlocks(-1);
                currentStagePlaythrough = stagePlaythroughs.get(iStageIndex);
                iStageIndex++;
                currentStagePlaythrough.startStage(1, false);

                //Save the positions if moved
                if (this instanceof Lesson lesson)
                    lesson.savePositions();
            }
        }
    }

    /**
     * If parts of the current step have been completed, will reset the player back to the start of the current step.
     * <p></p>
     * If no progress has been made on the current step, will take the player back to the start of the previous step, provided that a previous step exists.
     * <p></p>
     */
    public void previousStep()
    {
        if (currentStagePlaythrough != null)
            currentStagePlaythrough.previousStep();
    }

    /**
     * Moves a player to the start of the next step, if they have already completed the current step.
     */
    public void skipStep()
    {
        if (currentStagePlaythrough != null)
            currentStagePlaythrough.skipStep();
    }

    /**
     *
     * @return Whether the player can navigate to the previous stage
     */
    public boolean canMoveBackStage()
    {
        if (currentStagePlaythrough != null)
            return currentStagePlaythrough.inProgress() || iStageIndex > 1;
        return false;
    }

    /**
     *
     * @return Whether the player can navigate to the next stage
     */
    public boolean canMoveForwardsStage()
    {
        return (iStageIndex <= iHighestStageCompleted && iStageIndex < stagePlaythroughs.size());
    }

    /**
     *
     * @return Whether the player can navigate to the previous step
     */
    public boolean canMoveBackStep()
    {
        if (currentStagePlaythrough != null)
            return currentStagePlaythrough.canMoveBackStep();
        return false;
    }

    /**
     *
     * @return Whether the player can navigate to the next step
     */
    public boolean canMoveForwardsStep()
    {
        if (currentStagePlaythrough != null)
            return currentStagePlaythrough.canMoveForwardsStep();
        return false;
    }

    /**
     * Teleports the player to the start of the current step
     */
    public void tpToStepStart()
    {
        if (fallListener != null)
            this.fallListener.teleportToSafeLocation();
    }

    /**
     * Sends a link to the video walkthrough in chat
     */
    public void callVideoLink()
    {
        if (currentStagePlaythrough != null)
            if (currentStagePlaythrough.currentStepPlaythrough != null)
                this.currentStagePlaythrough.currentStepPlaythrough.locationStep.displayVideoLink(this.creatorOrStudent.player);
    }

    /**
     * @return Whether the current step has a video link
     */
    public boolean currentStepHasVideoLink()
    {
        if (currentStagePlaythrough != null)
            if (currentStagePlaythrough.currentStepPlaythrough != null)
                return this.currentStagePlaythrough.currentStepPlaythrough.locationStep.isLinkAvailable();
        return false;
    }

    /**
     * Ran upon the successful completion of a playthrough. Then calls commandEndPlaythrough.
     */
    protected abstract void endPlaythrough();

    /**
     * Ran if a playthrough must be terminated early. Will safely terminate the playthrough and then call
     * commandEndPlaythrough.
     */
    public abstract void terminateEarly();

    /**
     * Performs actions common to the end or pausing of all Playthroughs: Updates the User's mode, unregisters fall listeners,
     * unregisters the /tpll, /ll and gmask listeners, removes all virtual blocks, removes all spies, teleports the player to the lobby,
     * removes the playthrough from the plugin's list of active playthroughs
     */
    protected void commonEndPlaythrough()
    {
        //Update current playthrough, set to null, this will also update the user's mode
        creatorOrStudent.setCurrentPlaythrough(null);

        //Unregisters the gameplay listeners
        fallListener.unregister();
        playthroughCommandListeners.unregister();

        //Removes virtual blocks
        Stack<VirtualBlockGroup<org.bukkit.Location, BlockData>> virtualBlockGroups = plugin.getVirtualBlockGroups();

        //Goes through the list of the plugin's active virtual block groups
        int iTasksActive = virtualBlockGroups.size();
        for (int j = iTasksActive-1 ; j >=0 ; j--)
        {
            VirtualBlockGroup<org.bukkit.Location, BlockData> virtualBlockGroup = virtualBlockGroups.get(j);
            //Checks whether the virtual block group is of this tutorial playthrough
            if (virtualBlockGroup.isOfPlaythrough(this))
            {
                //Removes the blocks from the player view and marks them as stale
                virtualBlockGroup.removeBlocks();
            }
        }

        //Removes all spies
        removeAllSpies();

        //Remove the gui
        this.navigationMenu.delete();
        this.creatorOrStudent.mainGui = null;

        //Teleport the player back to the lobby area
        teleportToLobby();

        //Removes the playthrough from the plugins list of active playthroughs
        plugin.activePlaythroughs.remove(this);
    }

    /**
     * Teleports the player of this tutorial playthrough to the lobby as defined in config, after the 'wait time' as specified in config
     */
    protected void teleportToLobby()
    {
        //Gets the config
        FileConfiguration config = this.plugin.getConfig();

        String szLobbyTPType = "";
        szLobbyTPType = config.getString("Lobby_TP_Type");

        //If a server switch is to occur
        if (szLobbyTPType.equals("Server"))
        {
            String szServerName = config.getString("Server_Name");

            //Switches the player's server after a delay
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> creatorOrStudent.player.performCommand("server " +szServerName), config.getLong("Completion_TP_Wait"));
        }

        //If a simple player teleport is to occur
        else if (szLobbyTPType.equals("LobbyLocation"))
        {
            //Teleports the player to the lobby after a delay
            User.teleportPlayerToLobby(creatorOrStudent.player, plugin, config.getLong("Completion_TP_Wait"));
        }
    }
}
