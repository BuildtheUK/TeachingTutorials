package teachingtutorials.tutorialplaythrough;

import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.projection.GeographicProjection;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;
import net.buildtheearth.terraminusminus.util.geo.LatLng;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.guis.locationcreatemenus.LocationTaskEditorMenu;
import teachingtutorials.tutorialobjects.Task;
import teachingtutorials.tutorialobjects.LocationTask;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.GeometricUtils;
import teachingtutorials.utils.User;

import java.util.logging.Level;

/**
 * Represents a type of Task where the user must make a WorldEdit selection (cuboid). Contains the relevant listeners used when the task is active.
 */
public class Selection extends PlaythroughTask implements Listener
{
    /** Stores the target coordinates of the first point - the geographical location a player should select as a point
     * <p> </p>
     * Latitude, then longitude, then height
     */
    final double dTargetCoords1[] = new double[3];

    /** Stores the target coordinates of the second point - the geographical location a player should select as a point
     * <p> </p>
     * Latitude, then longitude, then height
     */
    final double dTargetCoords2[] = new double[3];

    /** Stores the geographical coordinates of the first point which the player has selected
     * <p> </p>
     * Latitude, then longitude, then height
     */
    double[] dSelectionPoint1 = new double[3]; //Lat then long, then height

    /** Stores the geographical coordinates of the second point which the player has selected
     * <p> </p>
     * Latitude, then longitude, then height
     */
    double[] dSelectionPoint2 = new double[3]; //Lat then long, then height

    //NOTE: Height is height of the block (i.e the block's Y coordinate)

    /** Variables used by new location procedures */
    boolean bSelection1Made, bSelection2Made;

    /**
     * Used when initialising a task for a lesson, i.e when the answers are already known
     * @param plugin A reference to the TeachingTutorials plugin instance
     * @param player A reference to the Bukkit player for which the task is for
     * @param locationTask A reference to the location task object of this selection
     * @param groupPlaythrough A reference to the parent group play-through object which this task is a member of
     */
    Selection(TeachingTutorials plugin, Player player, LocationTask locationTask, GroupPlaythrough groupPlaythrough)
    {
        super(plugin, player, locationTask, groupPlaythrough);

        //Extracts the target coordinates
        String[] cords = locationTask.getAnswer().split(",");
        this.dTargetCoords1[0] = Double.parseDouble(cords[0]);
        this.dTargetCoords1[1] = Double.parseDouble(cords[1]);
        this.dTargetCoords1[2] = Double.parseDouble(cords[2]);
        this.dTargetCoords2[0] = Double.parseDouble(cords[3]);
        this.dTargetCoords2[1] = Double.parseDouble(cords[4]);
        this.dTargetCoords2[2] = Double.parseDouble(cords[5]);

        this.taskEditorMenu = new LocationTaskEditorMenu(plugin,
                groupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough().getCreatorOrStudent(),
                groupPlaythrough.getParentStep().getEditorMenu(),
                Display.colouredText("Selection Task Difficulty Panel", NamedTextColor.AQUA),
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
    Selection(TeachingTutorials plugin, Player player, Task task, GroupPlaythrough groupPlaythrough)
    {
        super(plugin, player, task, groupPlaythrough);

        this.taskEditorMenu = new LocationTaskEditorMenu(plugin,
                groupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough().getCreatorOrStudent(),
                groupPlaythrough.getParentStep().getEditorMenu(),
                Display.colouredText("Selection Task Difficulty Panel", NamedTextColor.AQUA),
                this.getLocationTask(), this) {
        };
    }

    /**
     * Registers the task listener, activating the task. Sets up the starting state of the selections.
     */
    @Override
    public void register()
    {
        PlaythroughMode currentMode = this.parentGroupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough().getCurrentPlaythroughMode();

        //Output the required selection locations to assist debugging
        switch (currentMode)
        {
            case PlayingLesson:
                if (this.parentGroupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough() instanceof Lesson lesson)
                    plugin.getLogger().log(Level.INFO, "Lesson: " +lesson.getLessonID()
                            +". Selection Task: " +this.getLocationTask().iTaskID
                            +". Target point 1 (lat, long, height) = ("+dTargetCoords1[0]+","+dTargetCoords1[1]+","+dTargetCoords1[2]+")"
                            +". Target point 2 (lat, long, height) = ("+dTargetCoords2[0]+","+dTargetCoords2[1]+","+dTargetCoords2[2]+")"
                    );
                break;
            case EditingLocation:
                if (this.parentGroupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough() instanceof Lesson lesson)
                    plugin.getLogger().log(Level.INFO, "Lesson: " +lesson.getLessonID()
                            +". Editing Selection Task: " +this.getLocationTask().iTaskID
                            +". Original Target point 1 (lat, long, height) = ("+dTargetCoords1[0]+","+dTargetCoords1[1]+","+dTargetCoords1[2]+")"
                            +". Original Target point 2 (lat, long, height) = ("+dTargetCoords2[0]+","+dTargetCoords2[1]+","+dTargetCoords2[2]+")"
                    );
                break;
            case CreatingLocation:
                plugin.getLogger().log(Level.INFO, "New Location being made by :"+player.getName()
                        +". Selection Task: " +this.getLocationTask().iTaskID);
                break;
        }

        //Prepares the selection task - sets the starting state
        bSelection1Made = bSelection2Made = false;

        super.register();

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Detects player interact events, determines if it is from the player of this task,
     * determines whether it is a wand selection and if so performs necessary logic
     * @param event A reference to a player chat event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void interactEvent(PlayerInteractEvent event)
    {
        fPerformance = 0F;

        if (!isCorrectPlayerAndCorrectInteraction(event))
            return;
        
        //Converts Minecraft coordinates to geographical coordinates
        double[] longLatOfSelectedBlock;
        final GeographicProjection projection = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS).projection();
        try
        {
            longLatOfSelectedBlock = projection.toGeo(event.getClickedBlock().getX()+0.5d, event.getClickedBlock().getZ()+0.5d);
        }
        catch (OutOfProjectionBoundsException e)
        {
            //Player has selected an area outside of the projection
            return;
        }

        //Checks whether it is a new location or editing
        if (!parentGroupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough().getCurrentPlaythroughMode().equals(PlaythroughMode.PlayingLesson))
        { // Edits or sets answers
            //Checks whether it is a left click or right click and stores the coordinates
            if (event.getAction().equals(Action.LEFT_CLICK_BLOCK))
            {
                bSelection1Made = true;
                dTargetCoords1[0] = longLatOfSelectedBlock[1];
                dTargetCoords1[1] = longLatOfSelectedBlock[0];
                dTargetCoords1[2] = event.getClickedBlock().getY();

                Display.ActionBar(player, Display.colouredText("Position 1 selected", NamedTextColor.GREEN));
            }
            else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
            {
                bSelection2Made = true;
                dTargetCoords2[0] = longLatOfSelectedBlock[1];
                dTargetCoords2[1] = longLatOfSelectedBlock[0];
                dTargetCoords2[2] = event.getClickedBlock().getY();

                Display.ActionBar(player, Display.colouredText("Position 2 selected", NamedTextColor.GREEN));
            }
            else
            {
                //Should never reach this really
                return;
            }

            //Checks whether both selections have been made - whether the player has done a left click and right click selection
            if ((bSelection1Made && bSelection2Made))
            {
                plugin.getLogger().log(Level.FINE, ChatColor.AQUA +"Player has now made both points of the selection");
                Display.ActionBar(player, Display.colouredText("Selection complete", NamedTextColor.DARK_GREEN));

                //Set the answers - answers are stored in a format matching that of how they are decoded in the constructor of this class
                LocationTask locationTask = this.getLocationTask();
                String szAnswers = dTargetCoords1[0] +"," +dTargetCoords1[1] +"," +dTargetCoords1[2] +"," +dTargetCoords2[0] +"," +dTargetCoords2[1] +"," +dTargetCoords2[2] ;
                locationTask.setAnswers(szAnswers);

                //Data is added to database once difficulty is provided

                //Prompt difficulty
                taskEditorMenu.taskFullySet();
                taskEditorMenu.open(player);

                //SpotHit is then called from inside the difficulty panel once the difficulty has been established
                //This is what moves it onto the next task
            }
        }

        //Task is of a Lesson
        else
        {
            //Checks whether it is a left click or right click and stores the coordinates,
            // then checks whether it was a correct point and whether both selection points have been hit
            boolean[] bCorrectPointSelectedAndBothSelectionsHit = wasCorrectPointAndBothSelectionsHit(event, longLatOfSelectedBlock);

            //Checks whether it was a correct point
            if (bCorrectPointSelectedAndBothSelectionsHit[0])
            {
                //Checks whether both points have been made
                if (bCorrectPointSelectedAndBothSelectionsHit[1])
                {
                    plugin.getLogger().log(Level.FINE, ChatColor.AQUA +"Player has now made both points of the selection");
                    Display.ActionBar(player, Display.colouredText("Selection complete", NamedTextColor.DARK_GREEN));

                    //Generally, a tutorial would have a tpll task first, so any reasonable value here is performance of 1
                    // especially since this is quantised into blocks, so it wouldn't be a precise measure of performance anyway
                    fPerformance = 1;

                    //This will block any "Correct position selected" messages until the "Selection complete" message has faded
                    this.parentGroupPlaythrough.getParentStep().holdSelectionComplete();

                    //Marks the task as complete and moves them forwards to the next task
                    bothSelectionsMade();
                }
                else
                {
                    //Checks to see if any of the other active selection listeners have both selections complete by this interaction
                    //(A corner can be of two sides and if one side is now complete we want that side's message to take precedence)
                    if (this.parentGroupPlaythrough.getParentStep().getSelectionCompleteHold())
                    {
                        //A different selection task was found and would be completed by this interaction
                    }
                    else
                    {
                        Display.ActionBar(player, Display.colouredText("Correct position selected", NamedTextColor.GREEN));
                    }
                }
            }
        }
    }


    /**
     * Determines whether the player selected one of the points on this selection, and also whether they have now selected
     * both points on the selection
     * @param event The interact event
     * @param longLatOfSelectedBlock The longitude and latitude of the point selected
     * @return A 2 value boolean array containing the required answers, in the form of {correct point selected?, both points now selected?}
     */
    public boolean[] wasCorrectPointAndBothSelectionsHit(PlayerInteractEvent event, double[] longLatOfSelectedBlock)
    {
        //Stores whether it was a left or right click. True if left, false if right.
        boolean bIsLeft = false;
        
        //Determines what point of the selection it was
        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK))
        {
            // plugin.getLogger().log(Level.FINE, ChatColor.AQUA +"Player made left click selection");
            bIsLeft = true;
            dSelectionPoint1[0] = longLatOfSelectedBlock[1];
            dSelectionPoint1[1] = longLatOfSelectedBlock[0];
            dSelectionPoint1[2] = event.getClickedBlock().getY();
            bSelection1Made = true;
        }
        else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
        {
            // plugin.getLogger().log(Level.FINE, ChatColor.AQUA +"Player made right click selection");
            bIsLeft = false;
            dSelectionPoint2[0] = longLatOfSelectedBlock[1];
            dSelectionPoint2[1] = longLatOfSelectedBlock[0];
            dSelectionPoint2[2] = event.getClickedBlock().getY();
            bSelection2Made = true;
        }

        boolean bPointFound;
        boolean bWasTarget1;
        boolean bBothSelectionsHit = false;

        //Returns whether a target point was found, and which one
        boolean[] bPointFoundWhichTarget = isCorrectPointWhichTarget(longLatOfSelectedBlock, event.getClickedBlock().getY());
        bPointFound = bPointFoundWhichTarget[0];
        bWasTarget1 = bPointFoundWhichTarget[1];

        //Now check that the other is also a valid point
        if (bPointFound)
        {
            LatLng selectionGeoCoords;

            float fOtherDistance;
            //If user left-clicked earlier and found a point, we want to check the right click point. Visa-versa
            if (bIsLeft)
                selectionGeoCoords = new LatLng(dSelectionPoint2[0], dSelectionPoint2[1]);
            else
                selectionGeoCoords = new LatLng(dSelectionPoint1[0], dSelectionPoint1[1]);

            //If both selections have now been made, proceed with checking whether both are on point
            if (bSelection1Made && bSelection2Made)
            {
                //If the point the user just found was target 1 then we want to check target 2
                if (bWasTarget1)
                    fOtherDistance = GeometricUtils.geometricDistance(selectionGeoCoords, dTargetCoords2);
                else
                    fOtherDistance = GeometricUtils.geometricDistance(selectionGeoCoords, dTargetCoords1);

                //Generally, tutorials should have a player tpll to the position first, so any reasonable value here is performance of 1
                if (fOtherDistance <= 1.5)
                {
                    bBothSelectionsHit = true;
                }
            }
        }
        boolean[] bCorrectPointSelectedAndBothSelectionsHit = {bPointFound, bBothSelectionsHit};
        return bCorrectPointSelectedAndBothSelectionsHit;
    }

    /**
     * Checks whether an interaction is a WorldEdit selection event from the relevant player
     * @param event A player interact evebt
     * @return True if the event of the player was the relevant player of this task AND the interaction involved left or
     * right clicking a block with the WorldEdit wand (a wooden axe). Returns False otherwise.
     */
    private boolean isCorrectPlayerAndCorrectInteraction(PlayerInteractEvent event)
    {
        //Checks that it is the correct player
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId()))
        {
            return false;
        }

        //Checks that it is the correct tool
        if (!event.hasItem())
        {
            return false;
        }
        if (!event.getItem().getType().equals(Material.WOODEN_AXE))
        {
            return false;
        }

        //Checks that it is a left or right click of a block
        if (!(event.getAction().equals(Action.LEFT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)))
        {
            return false;
        }

        //Will return true if it is the correct player and correct interaction
        return true;
    }

    /**
     * Whether the parsed coordinates correspond to one of the target points of this selection, and if so which one.
     * It does this by calculating the geometric distance from the query point to each of the target points.
     * @param longLatOfSelectedBlock Longitude and latitude coordinates of the point to query.
     * @param iSelectedHeight The height of the block on the world of the point to query.
     * @return A 2 value boolean array containing results of the two questions.
     * The first value is whether the parsed coordinates correspond to one of the target points.
     * The second value is {@code true} if it corresponds to point 1 and {@code false} if it corresponds to point 2, or {@code false} if the first value is false.
     * If the queried point is close to both, then it is assumed to correspond to point 1.
     */
    private boolean[] isCorrectPointWhichTarget(double[] longLatOfSelectedBlock, int iSelectedHeight)
    {
        boolean bPointFound = false;
        boolean bWasTarget1 = false;

        //Stores the distance from the single selection to each of the two target points of this selection listener
        float[] fDistance = new float[2];

        //Calculates the distance between the queried point and the two target points
        LatLng selectionGeoCoords;
        selectionGeoCoords = new LatLng(longLatOfSelectedBlock[1], longLatOfSelectedBlock[0]);
        fDistance[0] = GeometricUtils.geometricDistance(selectionGeoCoords, dTargetCoords1);
        fDistance[1] = GeometricUtils.geometricDistance(selectionGeoCoords, dTargetCoords2);

        int iCorrectHeight;

        //Checks whether it is anywhere near one of the block they are supposed to select
        for (int i = 0 ; i < 2 ; i++)
        {
            if (i == 0)
                iCorrectHeight = (int) dTargetCoords1[2];
            else
                iCorrectHeight = (int) dTargetCoords2[2];

            if (fDistance[i] <= 1.5 && iCorrectHeight == iSelectedHeight)
            {
                bPointFound = true;

                //Records which target was found
                bWasTarget1 = (i == 0);
                break; //If they found a point, then good on them, but they can't get both with just one click, so we can escape
            }
        }
        boolean[] bPointFoundWhichTarget = new boolean[]{bPointFound, bWasTarget1};
        return bPointFoundWhichTarget;
    }

    /**
     * Handles completion of the task - unregisters listeners and moves onto the next task
     */
    private void bothSelectionsMade()
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
     * Will unregister the selection task and move forwards to the next task
     */
    public void newLocationSpotHit()
    {
        bothSelectionsMade();
    }

}
