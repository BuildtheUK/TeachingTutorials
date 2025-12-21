package teachingtutorials.tutorialplaythrough;

import net.buildtheearth.terraminusminus.util.geo.CoordinateParseUtils;
import net.buildtheearth.terraminusminus.util.geo.LatLng;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.guis.locationcreatemenus.LocationTaskEditorMenu;
import teachingtutorials.tutorialobjects.Task;
import teachingtutorials.tutorialobjects.LocationTask;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.GeometricUtils;
import teachingtutorials.utils.User;

import java.util.logging.Level;

public class Tpll extends PlaythroughTask implements Listener
{
    /** Stores the target coords - the location a player should tpll to */
    final double dTargetCoords[] = new double[2];

    /** Stores the required accuracies. Perfect distance first then limit distance */
    private float fAccuracies[];

    /** Stores the distance from the tpll command coordinates to the target coordinates once a tpll command is run during the active time of the listener */
    private float fGeometricDistance;

    /** The task id of the tpll marker particle spawner schedule */
    private int iMarkerTaskID = 0;

    /**
     * Used when initialising a task for a lesson, i.e when the answers are already known
     * @param plugin A reference to the TeachingTutorials plugin instance
     * @param player A reference to the Bukkit player for which the task is for
     * @param locationTask A reference to the location task object of this tpll
     * @param groupPlaythrough A reference to the parent group play-through object which this task is a member of
     */
    Tpll(TeachingTutorials plugin, Player player, LocationTask locationTask, GroupPlaythrough groupPlaythrough)
    {
        super(plugin, player, locationTask, groupPlaythrough);

        //Extracts the answers
        String[] cords = locationTask.getAnswer().split(",");
        this.dTargetCoords[0] = Double.parseDouble(cords[0]);
        this.dTargetCoords[1] = Double.parseDouble(cords[1]);

        //Extracts the details - required accuracies
        if (locationTask.getDetails().equals("")) // Deals with pre 1.1.0 tutorials
        {
            this.fAccuracies = new float[]{0.25f, 1};
        }
        else
        {
            String[] szAccuracies = locationTask.getDetails().split(";");
            this.fAccuracies = new float[2];
            this.fAccuracies[0] = Float.parseFloat(szAccuracies[0]);
            this.fAccuracies[1] = Float.parseFloat(szAccuracies[1]);
        }

        this.taskEditorMenu = new LocationTaskEditorMenu(plugin,
                groupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough().getCreatorOrStudent(),
                groupPlaythrough.getParentStep().getEditorMenu(),
                Display.colouredText("Tpll Difficulty Panel", NamedTextColor.AQUA),
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
    Tpll(TeachingTutorials plugin, Player player, Task task, GroupPlaythrough groupPlaythrough)
    {
        super(plugin, player, task, groupPlaythrough);

        this.taskEditorMenu = new LocationTaskEditorMenu(plugin,
                groupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough().getCreatorOrStudent(),
                groupPlaythrough.getParentStep().getEditorMenu(),
                Display.colouredText("Tpll Difficulty Panel", NamedTextColor.AQUA),
                this.getLocationTask(), this) {
        };
    }

    public float getGeometricDistance()
    {
        return fGeometricDistance;
    }

    /**
     * Registers the task listener, activating the task
     */
    @Override
    public void register()
    {
        PlaythroughMode currentMode = this.parentGroupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough().getCurrentPlaythroughMode();

        //Output the required tpll coordinates to assist debugging and display the maker on the world if approriate
        switch (currentMode)
        {
            case PlayingLesson:
                if (this.parentGroupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough() instanceof Lesson lesson)
                    plugin.getLogger().log(Level.INFO, "Lesson: " +lesson.getLessonID()
                            +". Tpll Task: " +this.getLocationTask().iTaskID
                            +". Target tpll = ("+dTargetCoords[0]+","+dTargetCoords[1]+")"
                    );

                //Displays the marker on the world
                World world = this.parentGroupPlaythrough.getParentStep().getParentStage().getLocation().getWorld();

                iMarkerTaskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
                    @Override
                    public void run()
                    {
                        if (bActive)
                            player.spawnParticle(Particle.REDSTONE, GeometricUtils.convertToBukkitLocation(world, dTargetCoords[0], dTargetCoords[1]).add(0, 1, 0), 10, new Particle.DustOptions(Color.GREEN, 3));
                        else
                        {
                            //We cancel the schedule so it shouldn't reach this too much
                        }
                    }
                }, 0L, 15L);
                break;
            case EditingLocation:
                if (this.parentGroupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough() instanceof Lesson lesson)
                    plugin.getLogger().log(Level.INFO, "Lesson: " +lesson.getLessonID()
                            +". Tpll Task: " +this.getLocationTask().iTaskID
                            +". Original Target tpll = ("+dTargetCoords[0]+","+dTargetCoords[1]+")"
                    );
                break;
            case CreatingLocation:
                plugin.getLogger().log(Level.INFO, "New Location being made by :"+player.getName()
                        +". Tpll Task: " +this.getLocationTask().iTaskID);
                break;
        }

        super.register();

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Detects player command events, determines if it is from the player of this task,
     * determines whether it is a tpll command and if so performs necessary logic
     * @param event A reference to a player command event
     */
    //Want the tutorials tpll process to occur first
    @EventHandler(priority = EventPriority.LOWEST)
    public void commandEvent(PlayerCommandPreprocessEvent event)
    {
        fPerformance = 0F;

        //Checks that it is the correct player
        if (event.getPlayer().getUniqueId().equals(player.getUniqueId()))
        {
            //Checks that it is the correct command - a tpll command
            String command = event.getMessage();
            if (command.startsWith("/tpll"))
            {
                //Cancels the event
                event.setCancelled(true);

                //Extract the coordinates
                command = command.replace("/tpll ", "");
                LatLng latLong = CoordinateParseUtils.parseVerbatimCoordinates(command.replace(", ", " "));

                //Checks that the coordinates were established
                if (latLong == null)
                {
                    player.sendMessage(Display.errorText("Incorrect tpll format - do /tpll TheLatitude, TheLongitude"));
                    return;
                }

                //Teleport the player to where they tplled to

                //Gets the world
                World world = this.parentGroupPlaythrough.getParentStep().getParentStage().getLocation().getWorld();

                //Performs the tpll
                if (!GeometricUtils.tpllPlayer(world, latLong.getLat(), latLong.getLng(), player))
                    return; //Returns if the tpll was not in the bounds of the earth

                //Checks whether it is a new location or editing
                if (!parentGroupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough().getCurrentPlaythroughMode().equals(PlaythroughMode.PlayingLesson))
                {
                    //Set or edits the answers
                    LocationTask locationTask = this.getLocationTask();
                    locationTask.setAnswers(latLong.getLat()+","+latLong.getLng());

                    //Data is added to database once difficulty is provided

                    //Prompt difficulty
                    taskEditorMenu.taskFullySet();

                    User user = parentGroupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough().getCreatorOrStudent();
                    taskEditorMenu.open(user.player);

                    //SpotHit is then called from inside the difficulty panel once the difficulty has been established
                    //This is what moves it onto the next task
                }
                else //Is an actual lesson
                {
                    //Tpll accuracy checker
                    float fDistance = GeometricUtils.geometricDistance(latLong, dTargetCoords);
                    fGeometricDistance = fDistance;

                    float fPerfect = fAccuracies[0];
                    float fLimit = fAccuracies[1];

                    if (fDistance <= fPerfect)
                    {
                        //Very accurate
                        Display.ActionBar(player, Display.colouredText("Perfect! Well done", NamedTextColor.DARK_GREEN));
                        fPerformance = 1;
                        spotHit();
                        parentGroupPlaythrough.getParentStep().bPointWasHit = true;
                    }
                    else if (fDistance <= fLimit)
                    {
                        //Pretty decent
                        Display.ActionBar(player, Display.colouredText("Point hit", NamedTextColor.GREEN));
                        fPerformance = (1F / fLimit-fPerfect) * (fLimit - fDistance);
                        spotHit();
                        parentGroupPlaythrough.getParentStep().bPointWasHit = true;
                    }

                    //Provide the step with a value for how far away it was and a reference to this
                    this.parentGroupPlaythrough.getParentStep().handledTpllListeners.add(this);

                    //Once all tpll tasks have processed through it will then look through them all and if any were completed
                    //Then it will not bother outputting a distance message
                    //If none are complete then it will output a distance message with how far they were from the nearest point

                    //Only initiates the calculateNearestTpllPoint method if there is not already one queued
                    if (!this.parentGroupPlaythrough.getParentStep().bTpllDistanceMessageQueued)
                    {
                        plugin.getLogger().log(Level.INFO, "Initiating a tpll distance checker");
                        this.parentGroupPlaythrough.getParentStep().calculateNearestTpllPointAfterWait();
                    }
                }
            }
        }
    }

    /**
     * Handles completion of the task - unregisters listeners, removes marker and moves onto the next task
     */
    private void spotHit()
    {
        //Unregisters this task
        HandlerList.unregisterAll(this);

        //Cancels the marker schedule
        if (iMarkerTaskID != 0)
            Bukkit.getScheduler().cancelTask(iMarkerTaskID);

        //Marks the task as complete
        taskComplete();
    }

    /**
     * Unregisters the listener, resulting in the removal of the marker and removes virtual blocks
     */
    @Override
    public void unregister()
    {
        super.deactivate();

        //Cancels the marker schedule
        if (iMarkerTaskID != 0)
            Bukkit.getScheduler().cancelTask(iMarkerTaskID);

        //Unregisters this task
        HandlerList.unregisterAll(this);
    }

    //A public version is required for when spotHit is called from the difficulty listener
    //This is required as it means that the tutorial can be halted until the difficulty listener completes the creation of the new LocationTask
    /**
     * To be called from a difficulty listener when the difficulty has been specified.
     * <p> </p>
     * Will unregister the tpll task and move forwards to the next task
     */
    public void newLocationSpotHit()
    {
        spotHit();
    }
}