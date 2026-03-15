package org.btuk.teachingtutorials.newlocation;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.btuk.teachingtutorials.TeachingTutorials;
import org.btuk.teachingtutorials.tutorialplaythrough.PlaythroughTask;
import org.btuk.teachingtutorials.utils.Display;

import java.util.logging.Level;

/**
 * Used to allow a player to input a difficulty on each LocationTask
 */
public class DifficultyListener implements Listener
{
    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /** A reference to the instance of the player who is creating the new location */
    private final Player player;

    /** A reference to the playthrough task which is managing this DifficultyListener */
    private final PlaythroughTask task;

    /** Whether or not the listener is registered */
    private boolean bRegistered;

    /**
     * Constructs the difficulty listener
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @param player A reference to the instance of the player who is creating the new location
     * @param playthroughTask A reference to the playthrough task which is managing this DifficultyListener
     */
    public DifficultyListener(TeachingTutorials plugin, Player player, PlaythroughTask playthroughTask)
    {
        this.plugin = plugin;
        this.player = player;
        this.task = playthroughTask;
        bRegistered = false;
    }

    /**
     * Registers the event listeners with the server's listener system, if not already registered
     */
    public void register()
    {
        //Ensures it is only registered once
        if (!this.bRegistered)
        {
            this.bRegistered = true;
            Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
        }
    }

    /** Returns whether this listener is ready to take the difficulty */
    public boolean getIsReady()
    {
        return bRegistered;
    }

    //Want this /tutorials event to be handled first, so we set it to lowest priority so it can be cancelled straight away
    /**
     * Detects /tutorials commands, verifies the syntax, then stores the difficulties and triggers the tutorial to move
     * on to the next task
     * @param event A player command preprocess event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void commandEvent(PlayerCommandPreprocessEvent event)
    {
        //Checks if it came from the relevant player
        if (event.getPlayer().getUniqueId().equals(player.getUniqueId()))
        {
            //Extracts the command
            String command = event.getMessage();

            //Verifies the syntax of the command
            if (command.matches("(/tutorials 0)\\.[0-9]+") || command.equals("/tutorials 1"))
            {
                //Cancels the event
                event.setCancelled(true);

                if (!bRegistered)
                {
                    event.getPlayer().sendMessage(Display.errorText("Complete the " +task.getLocationTask().getType().name() +" task first"));
                    return;
                }

                //Extracts the difficulty of the task
                command = command.replace("/tutorials ", "");
                float fDifficulty = Float.parseFloat(command);

                //Sets the difficulties of the location tasks
                switch (task.getLocationTask().getType())
                {
                    case tpll:
                        task.getLocationTask().setDifficulties(fDifficulty, 0, 0, 0, 0);
                        break;
                    case selection:
                    case command:
                        task.getLocationTask().setDifficulties(0, fDifficulty, 0, 0, 0);
                        break;
                    case chat:
                        task.getLocationTask().setDifficulties(0, 0, 0, 0, 0);
                        break;
                    case place:
                        task.getLocationTask().setDifficulties(0, 0, fDifficulty, 0, 0);
                        break;
                }

                //Attempt to store the new data into the DB
                if (task.getLocationTask().storeNewData(plugin))
                {
                    plugin.getLogger().log(Level.INFO, "LocationTask stored in database");
                    player.sendMessage(Display.aquaText("Task answer successfully stored in DB"));
                }
                else
                {
                    plugin.getLogger().log(Level.SEVERE, "LocationTask not stored in database");
                    player.sendMessage(Display.errorText("Task could not be stored in DB. Please report this"));
                }

                //Unregisters this listener
                HandlerList.unregisterAll(this);

                bRegistered = false;

                //Calls for the play-through to move on to the next task
                task.newLocationSpotHit();
            }
        }
    }
}