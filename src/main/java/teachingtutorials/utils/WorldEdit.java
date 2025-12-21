package teachingtutorials.utils;

import com.google.common.base.Joiner;
import com.sk89q.worldedit.regions.RegionSelector;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialplaythrough.TutorialPlaythrough;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages WorldEdit calculations within the Tutorials system
 */
public class WorldEdit
{
    /** A queue of pending calculations to be performed */
    public static ConcurrentLinkedQueue<WorldEditCalculation> pendingCalculations = new ConcurrentLinkedQueue<>();

    /** Marks whether there is a calculation currently ongoing
     * <p> </p>
     * Current calculation refers to whether a calculation is already in the process from listening to the
     * EditSessionEvent to waiting for the first sign that the calculation has been complete and still having blocks on
     * the world which need to be removed after using them for calculation.
     * */
    private static AtomicBoolean bCurrentCalculationOngoing = new AtomicBoolean(false);

    /** Marks whether there are virtual blocks currently on the real world */
    private static AtomicBoolean bVirtualBlocksOnRealWorld = new AtomicBoolean(false);

    /**
     *
     * @return Whether there is a calculation currently ongoing.
     * <p> </p>
     * Current calculation refers to whether a calculation is already in the process from listening to the
     * EditSessionEvent to waiting for the first sign that the calculation has been complete and still having blocks on
     * the world which need to be removed after using them for calculation.
     */
    public static boolean isCurrentCalculationOngoing()
    {
        return bCurrentCalculationOngoing.get();
    }

    /**
     * Sets that there is currently a calculation in progress
     */
    public static void setCalculationInProgress(Logger logger)
    {
        bCurrentCalculationOngoing.set(true);
        logger.log(Level.INFO, ChatColor.RED +"The calculations queue has been blocked - calculation in progress");
    }

    /**
     * Sets that there is no longer a calculation in progress
     */
    public static void setCalculationFinished(Logger logger)
    {
        bCurrentCalculationOngoing.set(false);
        logger.log(Level.INFO, ChatColor.GREEN +"The calculations queue has been unblocked!");
    }

    /**
     * @return Whether there are virtual blocks currently on the world
     */
    public static boolean areVirtualBlocksOnRealWorld()
    {
        return bVirtualBlocksOnRealWorld.get();
    }

    /**
     * Sets that there are currently virtual blocks on the real world
     */
    public static void setVirtualBlocksOnRealWorld(Logger logger)
    {
        bVirtualBlocksOnRealWorld.set(true);
        logger.log(Level.INFO, ChatColor.GREEN +"Virtual blocks been marked as no longer on the world");
    }

    /**
     * Sets that there are no longer virtual blocks on the real world
     */
    public static void setVirtualBlocksOffRealWorld(Logger logger)
    {
        bVirtualBlocksOnRealWorld.set(false);
        logger.log(Level.INFO, ChatColor.GREEN +"Virtual blocks been marked as on the world");
    }

    /**
     * Creates a WorldEditCalculation which catches the block changes arising from WorldEdit commands owned by the
     * given player, and adds this to the queue of calculations to be performed
     * @param iTaskID The ID of the task for which the calculation is for
     * @param virtualBlocks The list of virtual blocks for the calling task, to which any virtual blocks calculated will
     *                      be added to
     * @param correctSelectionRegion The selection region associated with the command
     * @param szCommandLabel The command label (first word) of the command
     * @param szCommandArgs The command args (the reset of the command)
     * @param tutorialPlaythrough The tutorial playthrough which this task belongs to
     */
    public static void BlocksCalculator(TeachingTutorials plugin, int iTaskID, final VirtualBlockGroup<Location, BlockData> virtualBlocks, RegionSelector correctSelectionRegion, String szCommandLabel, String[] szCommandArgs, TutorialPlaythrough tutorialPlaythrough)
    {
        //1. Modifies the command
        //This code is taken from WorldEdit - See https://enginehub.org/
        int plSep = szCommandLabel.indexOf(':');
        if (plSep >= 0 && plSep < szCommandLabel.length() +1)
        {
            szCommandLabel = szCommandLabel.substring(plSep + 1);
        }
        if (szCommandArgs.length > 0)
        {
            szCommandLabel = szCommandLabel+" ";
        }
        StringBuilder sb = new StringBuilder(szCommandLabel);
        String szWorldEditCommand = Joiner.on(" ").appendTo(sb, szCommandArgs).toString();

        //The command is now fully formatted correctly
        plugin.getLogger().log(Level.INFO, "Upcoming command being run via the console: "+szWorldEditCommand);

        //Create a new calculation manager for this calculation and add to the list
        WorldEditCalculation newCalculation = new WorldEditCalculation(plugin, szWorldEditCommand, correctSelectionRegion, tutorialPlaythrough, iTaskID, virtualBlocks);
        WorldEdit.pendingCalculations.add(newCalculation);

        plugin.getLogger().log(Level.INFO, "This calculation has been added to the queue: "+szWorldEditCommand);
    }
}
