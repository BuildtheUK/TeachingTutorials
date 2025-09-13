package teachingtutorials.utils;

import com.fastasyncworldedit.core.extent.processor.ProcessorScope;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.util.ExtentTraverser;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialplaythrough.TutorialPlaythrough;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stores the details of a WorldEdit block change calculation which is to be made, and contains the methods for
 * performing this calculation
 */
public class WorldEditCalculation
{
    /** A reference to an instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /** The tutorial playthrough for which the WE task calculation is ultimately a part of */
    private final TutorialPlaythrough tutorialPlaythrough;

    /** The ID of the task for which this calculation is for - used for output */
    private final int iTaskID;

    /** The command to run */
    private final String szWorldEditCommand;

    /** The selection region associated with the command */
    private final RegionSelector regionSelector;

    /** A WorldEdit listener used for detecting edit events and inject a recorder 'extent' */
    private WorldEditEventListener worldEditEventListener;


    /**
     * Indicates whether the blocks in the world need resetting. The resetBlocks() method may be called multiple times,
     * so this is used to ensure that it is only enacted once.
     */
    private AtomicBoolean bBlocksRequireReset = new AtomicBoolean(false);


    public Player getPlayer()
    {
        return tutorialPlaythrough.getCreatorOrStudent().player;
    }

    public World getWorld()
    {
        return tutorialPlaythrough.getLocation().getWorld();
    }

    public TutorialPlaythrough getTutorialPlaythrough()
    {
        return tutorialPlaythrough;
    }

    /**
     *
     * @param iTaskID The ID of the task for which the calculation is for
     * @param virtualBlocks The list of virtual blocks for the calling task, to which any virtual blocks calculated will
     *                      be added to
     * @param regionSelector The selection region associated with the command
     * @param szWorldEditCommand The full command to run
     * @param tutorialPlaythrough The tutorial playthrough which this task belongs to
     */
    public WorldEditCalculation(TeachingTutorials plugin, String szWorldEditCommand, RegionSelector regionSelector, TutorialPlaythrough tutorialPlaythrough, int iTaskID, VirtualBlockGroup<Location, BlockData> virtualBlocks)
    {
        this.plugin = plugin;
        this.szWorldEditCommand = szWorldEditCommand;
        this.regionSelector = regionSelector;
        this.tutorialPlaythrough = tutorialPlaythrough;
        this.iTaskID = iTaskID;

        //Get the console actor
        Actor consoleActor = BukkitAdapter.adapt(Bukkit.getConsoleSender());

        //Creates a listener to listen out for WorldEdit events and insert the recorder extent into the correct one
        worldEditEventListener = new WorldEditEventListener(consoleActor, this, virtualBlocks, iTaskID, plugin.getLogger());
    }

    /**
     * Runs the block calculation: Makes the selection, then calls for the listener to be activated and the command to
     * be run.
     * @return True if the calculation was implemented successfully, False if there was an error.
     */
    public void runCalculation()
    {
        //Provide another check to see if a calculation is ongoing already, and abort if so
        if (teachingtutorials.utils.WorldEdit.isCurrentCalculationOngoing())
        {
            return;
        }
        teachingtutorials.utils.WorldEdit.setCalculationInProgress(plugin.getLogger());

        //Console output
        plugin.getLogger().log(Level.INFO, "Running calculation of the" +
                "WorldEdit blocks on task: "+iTaskID);

        //Makes the selection through the console, then registers the listener, then runs the WorldEdit command through
        // the console and
        Bukkit.getScheduler().runTask(plugin, () ->
        {
            //Sets the selection
            plugin.getLogger().log(Level.INFO, "Adjusting the selection on task: "+iTaskID);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "/world "+tutorialPlaythrough.getLocation().getLocationID());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "/pos1 " + ((CuboidRegion) regionSelector.getRegion()).getPos1().toParserString());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "/pos2 " + ((CuboidRegion) regionSelector.getRegion()).getPos2().toParserString());

            //Adds virtual blocks to the world from current lesson, then registers the listener and runs the command
            recordRealBlocksAndSetVirtualsAndRunCommand();

        });
    }

    /**
     * Ensures all virtual blocks are off the world (as these may be from a different lesson), then sets all active
     * virtual blocks from the relevant lesson on to the actual world, whilst recording what the world was like originally.
     * <p> </p>
     * Then registers the listener and runs the command.
     */
    private void recordRealBlocksAndSetVirtualsAndRunCommand()
    {
        //Check for whether blocks are on the world - uses atomic boolean to be thread safe
        if (teachingtutorials.utils.WorldEdit.areVirtualBlocksOnRealWorld())
        {
            //If blocks are currently on the world, will keep delaying it until they are off

            plugin.getLogger().log(Level.INFO, "Delaying the real world block recording and command run on task: "+iTaskID);

            //Wait 4 ticks and try again
            Bukkit.getScheduler().runTaskLater(plugin, () -> recordRealBlocksAndSetVirtualsAndRunCommand(), 4L);

            return;
        }
        //If the virtual blocks are off the world then we can set the next one now
        else
        {
            //Marks that virtual blocks have been placed on the real world
            teachingtutorials.utils.WorldEdit.setVirtualBlocksOnRealWorld(plugin.getLogger());

            // ---------------- Begin Placing Virtual Blocks ----------------
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    plugin.getLogger().log(Level.INFO, ChatColor.AQUA +"Setting the virtual blocks to the world on task: "+iTaskID);

                    //Get the list of virtual blocks
                    VirtualBlockGroup[] virtualBlockGroups = plugin.getVirtualBlockGroups().toArray(VirtualBlockGroup[]::new);

                    //Declares the temporary VirtualBlockGroup object
                    VirtualBlockGroup<Location, BlockData> virtualBlockGroup;

                    //Goes through all virtual block groups - will do this going from start of tutorial to end
                    int iTasksActive = virtualBlockGroups.length;
                    for (int j = iTasksActive-1 ; j >=0 ; j--)
                    {
                        //Extracts the jth virtual block group
                        virtualBlockGroup = virtualBlockGroups[j];

                        //Checks whether this group and this calculation are part of the same lesson
                        if (!virtualBlockGroup.isOfPlaythrough(tutorialPlaythrough))
                        {
                            continue;
                        }

                        //Call for these blocks to be placed on the world
                        virtualBlockGroup.addBlocksToWorld(plugin.getLogger());
                    }

                    plugin.getLogger().log(Level.INFO, ChatColor.AQUA +"Finished recording the world blocks and setting virtual blocks on task: "+iTaskID);

                    //Once the chunks have started being processed it will put all of the blocks back
                    bBlocksRequireReset.set(true);

                    // ---------------- Register listener and run commands ----------------

                    //Registers the world change event listener
                    try
                    {
                        WorldEdit.getInstance().getEventBus().register(worldEditEventListener);
                        plugin.getLogger().log(Level.INFO, ChatColor.AQUA +"Registered the EditSessionEvent listener on task: "+iTaskID);
                    }
                    catch (Exception e)
                    {
                        plugin.getLogger().log(Level.WARNING,  "Error registering WorldEdit event listener on task: "+iTaskID, e);
                    }

                    //Runs the command
                    plugin.getLogger().log(Level.INFO, ChatColor.AQUA +"Sending command for task: "+iTaskID);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), szWorldEditCommand);
                }
            });
        }
    }

    /**
     * Unblocks the calculation queue and removes this calculation from the queue
     */
    private void unblockCalculationQueue()
    {
        //Updates the queue system, unblocking the queue
        teachingtutorials.utils.WorldEdit.pendingCalculations.remove(this);
        teachingtutorials.utils.WorldEdit.setCalculationFinished(plugin.getLogger());
        plugin.getLogger().log(Level.INFO, "Unblocked calculation queue from task: " +iTaskID);
        plugin.getLogger().log(Level.INFO, ChatColor.GREEN +"There are " +teachingtutorials.utils.WorldEdit.pendingCalculations.size() +" calculations remaining in the queue");
    }

    /**
     * Attempts to reset the actual blocks of the world back to how they were before the calculation took place.
     * Will then free the calculation up for the next group.
     * Should only happen once per calculation.
     */
    void tryResettingWorld()
    {
        //Get a local reference to the logger
        Logger logger = plugin.getLogger();

        //Checks whether the blocks have already been reset and if not then mark it as having been reset so that something else doesn't access it
        if (bBlocksRequireReset.getAndSet(false))
        {
            logger.log(Level.INFO, ChatColor.GREEN +"About to run the resetting of the blocks on the world for task: " +iTaskID);

            //Run the resetting
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run()
                {
                    //Get the list of virtual blocks
                    VirtualBlockGroup[] virtualBlockGroups = plugin.getVirtualBlockGroups().toArray(VirtualBlockGroup[]::new);

                    //Declares the temporary list object
                    VirtualBlockGroup<Location, BlockData> virtualBlockGroup;

                    logger.log(Level.INFO, ChatColor.GREEN +"Running the resetting of the blocks on the world for task: " +iTaskID);

                    //Goes through all virtual block groups - will do this going from end of tutorial to start
                    int iTasksActive = virtualBlockGroups.length;
                    for (int j = iTasksActive-1 ; j >=0 ; j--)
                    {
                        //Extracts the jth virtual block group
                        virtualBlockGroup = virtualBlockGroups[j];

                        //Checks whether this group and this calculation are part of the same lesson
                        if (!virtualBlockGroup.isOfPlaythrough(tutorialPlaythrough))
                        {
                            continue;
                        }

                        //Call for the world blocks to be reset
                        virtualBlockGroup.resetWorld(plugin);
                    }

                    //Wait a few ticks before saying that the blocks have been set to give it a chance to set the blocks before the next calculation
                    logger.log(Level.INFO,ChatColor.DARK_PURPLE +"In " +plugin.getConfig().getLong("BlockResetDelay") +" ticks we will mark the blocks as having been reset for task: " +iTaskID);

                    //Mark blocks as reset and unblock calculations queue after a time
                    Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                        @Override
                        public void run() {
                            logger.log(Level.INFO, ChatColor.DARK_PURPLE +"Marking the blocks as having been reset for task: " +iTaskID);

                            //Marks that virtual blocks have been taken off the real world
                            teachingtutorials.utils.WorldEdit.setVirtualBlocksOffRealWorld(plugin.getLogger());

                            //Unblock the calculation queue
                            unblockCalculationQueue();

                            logger.log(Level.INFO,ChatColor.DARK_PURPLE +"Marked the blocks as having been reset for task: " +iTaskID);
                        }
                    }, plugin.getConfig().getLong("BlockResetDelay"));
                }
            });
        }
    }

    /**
     * Used to safely terminate a calculation.
     * <p> </p>
     * Unregisters the event listener and attempts to reset the world
     */
    public void terminateCalculation()
    {
        this.worldEditEventListener.unregisterWorldChangeListener();
        tryResettingWorld();
        teachingtutorials.utils.WorldEdit.setCalculationFinished(plugin.getLogger());
    }
}

/**
 * A new type of extent which records block changes and attempts* to block them from being placed into the world.
 * <p> </p>
 * * = Blocking doesn't appear to work
 */
class BlockChangeRecorderExtentFAWE extends AbstractDelegateExtent implements IBatchProcessor
{
    /** A reference to the calculation object managing the calculation for this WE process */
    private final WorldEditCalculation worldEditCalculation;

    /** A reference to the list of virtual blocks to add to */
    private final VirtualBlockGroup<Location, BlockData> virtualBlocks;

    /** A copy of the ID of the task for which this WE process is for */
    private final int iTaskID;

    /** Get a local reference to the logger */
    private final Logger logger;


    /**
     * Constructs the new FAWE extent, adding the new processors
     * @param originalExtent The original extent to replace this new extend with
     * @param worldEditCalculation A reference to the world edit calculation managing this WE command/process
     * @param virtualBlocks A reference to the list of virtual blocks to add to
     * @param iTaskID A copy of the ID of the task for which this WE process is for
     */
    public BlockChangeRecorderExtentFAWE(Extent originalExtent, WorldEditCalculation worldEditCalculation, VirtualBlockGroup<Location, BlockData> virtualBlocks, int iTaskID, Logger logger)
    {
        super(originalExtent);
        this.logger = logger;
        this.worldEditCalculation = worldEditCalculation;
        this.virtualBlocks = virtualBlocks;
        this.iTaskID = iTaskID;

        super.addProcessor(this);
        super.addPostProcessor(this);
    }

    /**
     * Called when processing the changes of a given chunk in the operation.
     * This is where blocks are recorded and blocked from being placed on the world.
     * @param chunk An object representing the chunk
     * @param get An object representing the state of the chunk before the operation
     * @param set An object representing the state of the chunk after the operation
     * @return An object representing the required state of the chunk after the operation
     */
    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set)
    {
        //Now we know the calculation has taken place, we can remove the blocks we placed for the preexisting virtual blocks.
        // This will get called for each chunk but after it is called the first time, any future calls won't have an effect
        // because there is a boolean marking whether it has already been called or not
        worldEditCalculation.tryResettingWorld();

        //For some reason this gets called way too much - It gets called for every chunk

        //Set:
        // An implementation of CharSetBlocks
        // Layers are height layers of a chunk
        // A layer is a 16x16x16 piece of a chunk which stack up vertically to make up the chunk, There are 15 above 0 and 8 below 0.
        // The layer index is layer = y >> 4

        //The index of a chunk is calculated by Index = (y & 15) << 8 | z << 4 | x;
        //This is just an index to a 16*16*16 array, with Index = Y*256 + Z*16 + X

        logger.log(Level.FINE, ChatColor.AQUA +"Recording the changes in chunk: " +chunk.getChunkBlockCoord().toString() +" for task: " +iTaskID);

        //Declare the variables for the block coordinate markers
        int Y, dX, dZ;

        //Declare the variables for the new and old block states
        BlockState newBlock;
        BlockState oldBlock;

        //Initialise the chunk vector - a 2d vector to the 0,0 point of the chunk
        BlockVector2 chunkVector;
        chunkVector = BlockVector2.at(chunk.getX()*16, chunk.getZ()*16);

        //Stores the ID of the 'reserved' block state locally
        final int iReservedState = BlockTypesCache.ReservedIDs.__RESERVED__;

        //Check whether the chunk is empty
        if (set.isEmpty())
        {
            //Skip this chunk
            logger.log(Level.FINE, "Chunk has no edits");
            return set;
        }

        //Goes through every 'layer' in the new chunk
        final int iMaxSectionPosition = set.getMaxSectionPosition();
        for (int iLayer = set.getMinSectionPosition() ; iLayer <= iMaxSectionPosition ; iLayer++)
        {
            //Get the layer
            char[] blocks = set.loadIfPresent(iLayer);
            if (blocks == null)
            {
                //Layer had no blocks sections
                logger.log(Level.FINE, "Layer "+iLayer +" of chunk " +chunk.getChunkBlockCoord().toString() +" had no block sections");
                //Do nothing
            }
            else
            {
                //Layer had block sections
                logger.log(Level.FINE, "Layer "+iLayer +" of chunk " +chunk.getChunkBlockCoord().toString() +" had block sections, identifying blocks in this layer now");

                //Goes through every block in the layer
                int iNumBlocksInLayer = blocks.length;
                for (int index = 0 ; index < iNumBlocksInLayer ; index++)
                {
                    //Extracts the ID of the block
                    char block = blocks[index];

                    //Check whether there is a block change
                    if (block == iReservedState)
                    {
                        //No block change recorded
                    }
                    else
                    {
                        //Record this block change

                        //Extract the block coordinates
                        //Index works as if you are indexing a 3D matrix but on a 1D scale

                        //In the index, Y is taken and timesed by 256, add Z by 16 and X is added on at the end

                        //To get X we need to take index % 16
                        //To get Z we need to take index/16  % 16
                        //To get Y we need to take index/256  % 16

                        //The following code is equivalent to using mod and div by uses bitwise operations to extract the appropriate bits instead
                        dX = index & 15;
                        dZ = (index >> 4) & 15;
                        Y = (index >> 8) & 15; //the Y within the layer

                        Y = Y + iLayer * 16; //This should shift the Y back

                        //Extract the block type
                        newBlock = BlockTypesCache.states[block];

                        //Creates a vector to the block
                        BlockVector3 block3Vector = BlockVector3.at(chunkVector.getX() + dX, Y, chunkVector.getBlockZ() +dZ);

                        //Gets the old block
                        oldBlock = get.getBlock(dX, Y, dZ);

                        //Records the block change
                        adaptAndRecordBlockChange(block3Vector, oldBlock, newBlock);

                        //Cancels the world change
                        blocks[index] = iReservedState;
                    }
                }
            }
        }

        return set;
    }

    @Override
    public void postProcess(final IChunk chunk, final IChunkGet get, final IChunkSet set)
    {
        //Print out the number of new virtual blocks
        int iSize = virtualBlocks.size();
        logger.log(Level.FINE, ChatColor.AQUA +"Post processing of chunk: " +chunk.getChunkBlockCoord().toString()+". There have been " +iSize + " block changes recorded so far for task:" +iTaskID);
    }

    @Override
    public Extent construct(final Extent child) {
        if (getExtent() != child) {
            new ExtentTraverser<Extent>(this).setNext(child);
        }
        return this;
    }

    @Override
    public ProcessorScope getScope() {
        return ProcessorScope.READING_SET_BLOCKS;
    }

    /**
     * Takes a vector, and WorldEdit BlockState objects for the old and new blocks, converts them to bukkit objects,
     * then records the change of block - thereby creating a virtual block
     * @param vector A vector pointing to the location of the block change
     * @param blockDataOld WorldEdit BlockState object for the old block
     * @param blockDataNew WorldEdit BlockState object for the new block
     */
    private void adaptAndRecordBlockChange(BlockVector3 vector, BlockState blockDataOld, BlockState blockDataNew)
    {
        addVirtualBlock(BukkitAdapter.adapt(worldEditCalculation.getWorld(), vector), BukkitAdapter.adapt(blockDataOld), BukkitAdapter.adapt(blockDataNew));
    }

    /**
     * Creates a new virtual block and adds it to the list for this task
     * @param location The location of the virtual block
     * @param blockDataOld The old block's block data
     * @param blockDataNew The new block's block data
     */
    private void addVirtualBlock(Location location, BlockData blockDataOld, BlockData blockDataNew)
    {
        //Adds it to the new list of virtual blocks
        virtualBlocks.put(location, blockDataNew);
    }
}

/**
 * A listener which listens for World Edit command events and insert a recorder extent into them so as to record block
 * changes
 */
class WorldEditEventListener extends Object
{
    /** A reference to the console actor */
    Actor consoleActor;

    /** A reference to the calculation which is managing this listener */
    WorldEditCalculation parentCalculation;

    /** A reference to the virtual blocks group which to add any calculated virtual blocks to */
    VirtualBlockGroup<Location, BlockData> virtualBlocksGroups;

    /** A copy of the ID of the task for which we are calculating the virtual blocks */
    int iTaskID;

    /** Get a local reference to the logger */
    private final Logger logger;

    /**
     * Constructs the listener
     * @param consoleActor A reference to the console actor
     * @param worldEditCalculation A reference to the calculation which is managing this listener
     * @param virtualBlocksGroups A reference to the virtual blocks group which to add any calculated virtual blocks to
     * @param iTaskID A copy of the ID of the task for which we are calculating the virtual blocks
     */
    public WorldEditEventListener(Actor consoleActor, WorldEditCalculation worldEditCalculation, VirtualBlockGroup<Location, BlockData> virtualBlocksGroups, int iTaskID, Logger logger)
    {
        this.logger = logger;
        this.consoleActor = consoleActor;
        this.parentCalculation = worldEditCalculation;
        this.virtualBlocksGroups = virtualBlocksGroups;
        this.iTaskID = iTaskID;
    }

    /**
     * Detects WorldEdit command processes, verifies they originated from the console and injects an 'extent' into them
     * which records the changes being made. Then unregisters this listener.
     * @param event An edit session event
     */
    @Subscribe
    public void onEditSessionEvent(EditSessionEvent event)
    {
        //Verifies that the command originated from the console
        final Actor actor = event.getActor();
        if (actor == null)
        {
            logger.log(Level.INFO, "Edit session event detected belonging a" +
                    "null actor (assuming console) - at stage: "+event.getStage().toString() +". Assuming it is for task: " +iTaskID);
        }
        else if (actor.getName().equals(consoleActor.getName()))
        {
            logger.log(Level.INFO, "Edit session event detected belonging a" +
                    " console actor - at stage: "+event.getStage().toString() +". Assuming it is for task: " +iTaskID);
        }
        else
        {
            logger.log(Level.INFO, "Edit session event detected but doesn't belong to the correct actor, so ignoring");
            return;
        }

        //Creates the new recorder extent
        AbstractDelegateExtent blockChangeRecorderExtent = new BlockChangeRecorderExtentFAWE(event.getExtent(), parentCalculation, virtualBlocksGroups, iTaskID, logger);

        //Updates the extent of the edit session to be that of a block recording extent
        //The block recording extent means that block changes are recorded in the set block mechanism
        //Sets the new extent into the event
        logger.log(Level.INFO, "Setting the extent for task: " +iTaskID);
        event.setExtent(blockChangeRecorderExtent);

        //Once the extent has been set we don't need the listener anymore since any world edit changes under that extent will be recorded in the correct list
        unregisterWorldChangeListener();
    }

    /**
     * Unregisters this listener
     */
    void unregisterWorldChangeListener()
    {
        com.sk89q.worldedit.WorldEdit.getInstance().getEventBus().unregister(this);
        logger.log(Level.INFO, "Unregistered world edit listener for task: " +iTaskID);
    }
}
