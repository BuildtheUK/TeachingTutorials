package teachingtutorials.tutorialplaythrough;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.guis.locationcreatemenus.LocationTaskEditorMenu;
import teachingtutorials.tutorialobjects.Task;
import teachingtutorials.tutorialobjects.LocationTask;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.User;

import java.util.logging.Level;

/**
 * Represents a type of Task where the user must place a block. Contains the relevant listeners used when the task is active.
 */
public class Place extends PlaythroughTask implements Listener
{
    /** The minecraft coordinates of the intended location to place the block in x, y, z form */
    private final int iTargetCoords[] = new int[3];

    /** The material which to place */
    private Material mTargetMaterial;

    /**
     * Used when initialising a task for a lesson, i.e when the answers are already known
     * @param plugin A reference to the TeachingTutorials plugin instance
     * @param player A reference to the Bukkit player for which the task is for
     * @param locationTask A reference to the location task object of this place
     * @param groupPlaythrough A reference to the parent group play-through object which this task is a member of
     */
    Place(TeachingTutorials plugin, Player player, LocationTask locationTask, GroupPlaythrough groupPlaythrough)
    {
        super(plugin, player, locationTask, groupPlaythrough);

        //Extracts the coordinates
        String[] szCoordinates3AndMaterial = locationTask.getAnswer().split(",");
        iTargetCoords[0] = Integer.parseInt(szCoordinates3AndMaterial[0]);
        iTargetCoords[1] = Integer.parseInt(szCoordinates3AndMaterial[1]);
        iTargetCoords[2] = Integer.parseInt(szCoordinates3AndMaterial[2]);

        //Extracts the material
        mTargetMaterial = Material.getMaterial(szCoordinates3AndMaterial[3]);

        //Adds the virtual block to the list
        calculateVirtualBlocks();

        this.taskEditorMenu = new LocationTaskEditorMenu(plugin,
                groupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough().getCreatorOrStudent(),
                groupPlaythrough.getParentStep().getEditorMenu(),
                Display.colouredText("Place Task Difficulty Panel", NamedTextColor.AQUA),
                this.getLocationTask(), this) {
        };
    }

    /**
     * Used when initialising a task when creating a new location
     * @param plugin A reference to the TeachingTutorials plugin instance
     * @param player A reference to the Bukkit player for which the task is for
     * @param task A reference to the task
     * @param groupPlaythrough A reference to the parent group play-through object which this task is a member of
     */
    Place(TeachingTutorials plugin, Player player, Task task, GroupPlaythrough groupPlaythrough)
    {
        super(plugin, player, task, groupPlaythrough);

        this.taskEditorMenu = new LocationTaskEditorMenu(plugin,
                groupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough().getCreatorOrStudent(),
                groupPlaythrough.getParentStep().getEditorMenu(),
                Display.colouredText("Place Task Difficulty Panel", NamedTextColor.AQUA),
                this.getLocationTask(), this) {
        };
    }

    /**
     * Registers the task listener, activating the task
     */
    @Override
    public void register()
    {
        PlaythroughMode currentMode = this.parentGroupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough().getCurrentPlaythroughMode();

        //Output the required block and location to assist debugging
        //Calculate virtual blocks for Lesson mode
        switch (currentMode)
        {
            case PlayingLesson:
                if (this.parentGroupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough() instanceof Lesson lesson)
                    plugin.getLogger().log(Level.INFO, "Lesson: " +lesson.getLessonID()
                            +". Place Task: " +this.getLocationTask().iTaskID
                            +". Target block = "+this.mTargetMaterial +" at ("+iTargetCoords[0]+","+iTargetCoords[1]+","+iTargetCoords[2]+")");
                break;
            case EditingLocation:
                if (this.parentGroupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough() instanceof Lesson lesson)
                    plugin.getLogger().log(Level.INFO, "Lesson: " +lesson.getLessonID()
                            +". Editing Place Task: " +this.getLocationTask().iTaskID
                            +". Original Target block = "+this.mTargetMaterial +" at ("+iTargetCoords[0]+","+iTargetCoords[1]+","+iTargetCoords[2]+")");
                break;
            case CreatingLocation:
                plugin.getLogger().log(Level.INFO, "New Location being made by :"+player.getName()
                        +". Place Task: " +this.getLocationTask().iTaskID);
                break;
        }

        super.register();

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Detects player interact events, determines if it is from the player of this task, if it is a place, and if so performs necessary logic
     * @param event A reference to a player interact event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void interactEvent(PlayerInteractEvent event)
    {
        //Checks that it is the relevant player and is a block place
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId()))
            return;
        else if (!event.hasBlock())
            return;
        else if (!event.isBlockInHand())
            return;
        else if (!event.getAction().isRightClick())
            return;

        //It should now be a right click with a block against another block

        //Gets the location and material of the block being placed
        Location newBlockLocation = event.getClickedBlock().getLocation().add(event.getBlockFace().getDirection());
        Material newBlockMaterial = event.getItem().getType();

        //Cancels the event before WorldGuard displays a message
        event.setCancelled(true);

        //Handles logic
        blockPlaced(newBlockLocation, newBlockMaterial);
    }

    /**
     * Performs logic after a block was placed by a player in a lesson
     * @param newBlockLocation The location of the newly placed block
     * @param newBlockMaterial The material of the newly placed block
     */
    private void blockPlaced(Location newBlockLocation, Material newBlockMaterial)
    {
        fPerformance = 0F;

        //Get the coordinates of the placed block
        int iBlockX = newBlockLocation.getBlockX();
        int iBlockY = newBlockLocation.getBlockY();
        int iBlockZ = newBlockLocation.getBlockZ();

        //Checks whether it is a new location or editing
        if (!parentGroupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough().getCurrentPlaythroughMode().equals(PlaythroughMode.PlayingLesson))
        { //Edits or sets the answers
            //Store the material
            mTargetMaterial = newBlockMaterial;

            //Store the location
            iTargetCoords[0] = iBlockX;
            iTargetCoords[1] = iBlockY;
            iTargetCoords[2] = iBlockZ;

            //Set the answers in the LocationTask
            LocationTask locationTask = getLocationTask();
            locationTask.setAnswers(iBlockX +"," +iBlockY +"," +iBlockZ +"," +mTargetMaterial);

            //Data is added to database once difficulty is provided

            //Prompt difficulty
            taskEditorMenu.taskFullySet();
            taskEditorMenu.open(player);

            //SpotHit is then called from inside the difficulty panel once the difficulty has been established
            //This is what moves it onto the next task

            //Adds the virtual block to the list
            calculateVirtualBlocks();

            //Displays the virtual blocks
            displayVirtualBlocks();
        }

        //Blocks was placed during a lesson
        else
        {   //--Accuracy checker--

            boolean bCorrectPosition;
            boolean bCorrectMaterial;

            //Check material
            bCorrectMaterial = newBlockMaterial.equals(mTargetMaterial);

            //Check position
            bCorrectPosition = (iBlockX == iTargetCoords[0] && iBlockY == iTargetCoords[1] && iBlockZ == iTargetCoords[2]);

            //Checks whether both position ad material are correct
            if (bCorrectMaterial && bCorrectPosition)
            {
                //Correct everything
                Display.ActionBar(player, Display.colouredText("Correct", NamedTextColor.GREEN));
                fPerformance = 1; // Will be more comprehensive in future updates

                //Displays the virtual block
                displayVirtualBlocks();

                //Calls for the completion of the task
                spotHit();
            }

            //Gives feedback
            else if (bCorrectMaterial)
            {
                //Material correct, position wrong
                Display.ActionBar(player, Display.colouredText("Correct material, wrong position", NamedTextColor.GOLD));
            }
            else if (bCorrectPosition)
            {
                //Position correct, material wrong
                Display.ActionBar(player, Display.colouredText("Correct position, wrong material", NamedTextColor.GOLD));
            }
            else
            {
                //Nothing correct
                Display.ActionBar(player, Display.colouredText("Incorrect position and material", NamedTextColor.GOLD));
            }
        }
    }

    /**
     * Handles completion of the task - unregisters listeners and moves onto the next task
     */
    private void spotHit()
    {
        //Unregisters this task
        HandlerList.unregisterAll(this);

        //Marks the task as complete
        taskComplete();
    }

    /**
     * Unregisters the listener, marks the task as inactive and removes the virtual blocks of this task
     */
    public void unregister()
    {
        //Marks the task as inactive and removes the virtual blocks of this task
        super.deactivate();

        //Unregisters this task
        HandlerList.unregisterAll(this);
    }

    //A public version is required for when spotHit is called from the difficulty panel
    //This is required as it means that the tutorial can be halted until the difficulty panel completes the creation of the new LocationTask
    /**
     * To be called from a difficulty panel when the difficulty has been specified.
     * <p> </p>
     * Will unregister the place task and move forwards to the next task
     */
    public void newLocationSpotHit()
    {
        spotHit();
    }

    /**
     * Uses the target coordinates and target material to calculate the virtual block, and adds this to the list
     */
    public void calculateVirtualBlocks()
    {
        this.virtualBlocks.clear();
        Location location = new Location(this.parentGroupPlaythrough.getParentStep().getParentStage().getLocation().getWorld(), iTargetCoords[0], iTargetCoords[1], iTargetCoords[2]);
        this.virtualBlocks.put(location, mTargetMaterial.createBlockData());
    }
}
