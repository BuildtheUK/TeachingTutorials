package teachingtutorials.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialplaythrough.TutorialPlaythrough;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An extension of a concurrent hashmap for lists of virtual block
 * @param <K> Key
 * @param <V> Value
 */
public class VirtualBlockGroup<K, V> extends ConcurrentHashMap<K,V>
{
    /** A reference to the tutorial playthrough which this VirtualBlockGroup belongs to */
    private final TutorialPlaythrough tutorialPlaythrough;

    /**
     * A list of real world blocks. This list runs parallel to the virtual blocks group list
     * <p> </p>
     * WARNING: This may not hold the true value of the block. It is important to reset all virtual blocks groups in the
     * reverse order to which they were created, this will ensure proper resetting
     */
    private ConcurrentHashMap<Location, BlockData> realWorldBlocks = new ConcurrentHashMap<>();

    /**
     * If true, marks that the group is not to be displayed and is ready to be removed from the list of active virtual block
     * groups once the safety mechanism is ready. The group is only remaining in the master list so that the safety reset
     * mechanism of virtual blocks maintains its proper order.
     */
    private boolean bStale = false;

    public VirtualBlockGroup(TutorialPlaythrough tutorialPlaythrough)
    {
        this.tutorialPlaythrough = tutorialPlaythrough;
    }

    public String getOwner()
    {
        return tutorialPlaythrough.getCreatorOrStudent().player.getName();
    }

    /**
     * Sets the group to be not stale
     */
    public void setActive()
    {
        bStale = false;
    }

    /**
     *
     * @return Whether the group is stale
     */
    public boolean isStale()
    {
        return bStale;
    }

    /**
     * Returns whether this VirtualBlockGroup belongs to the given tutorial playthrough. Will check the references with
     * an == statement.
     * @param tutorialPlaythrough The given tutorial playthrough to query this group's membership status of
     * @return True if the reference to the given tutorial playthrough and the tutorialPlaythrough reference of this
     * VirtualBlockGroup are the same.
     * False if not.
     */
    public boolean isOfPlaythrough(TutorialPlaythrough tutorialPlaythrough)
    {
        //Note: We check the references here
        return tutorialPlaythrough == this.tutorialPlaythrough;
    }

    /**
     * Adds a virtual block to the list, and adds the real block at the location of that virtual block to the list of
     * real blocks.
     * @param key key with which the specified value is to be associated - this should be a bukkit Location object
     * @param value value to be associated with the specified key - This should be a bukkit BlockData object
     * @return
     */
    @Override
    public V put(K key, V value)
    {
        //Find the real value for this block and record this
        if (key instanceof Location)
        {
            //Stores a local reference to the world
            World worldToRead = tutorialPlaythrough.getLocation().getWorld();

            BlockData realBlock = worldToRead.getBlockData((Location) key).clone(); //This actually gets run asynchronously and takes over 2 seconds sometimes

            Bukkit.getLogger().log(Level.FINE, "Adding block to list and block's original material: "+realBlock.getMaterial());
            realWorldBlocks.put((Location) key, realBlock);
        }
        return super.put(key, value);
    }

    /**
     * Displays the virtual blocks to the player and all spies - sends block changes to each viewer
     */
    public void displayBlocks()
    {
        if (bStale)
            return;

        //Sends the changes to the player
        tutorialPlaythrough.getCreatorOrStudent().player.sendMultiBlockChange((Map<Location, BlockData>) this);

        //Sends the changes to the spies
        ArrayList<Player> spies = tutorialPlaythrough.getSpies();
        int iNumSpies = spies.size();
        int i;
        for (i = 0 ; i < iNumSpies ; i++)
        {
            spies.get(i).sendMultiBlockChange((Map<Location, BlockData>) this);
        }
    }

    /**
     * Removes the virtual blocks from the player and all spies, sets their views back to the real world.
     * <p>Marks the group as stale</p>
     */
    public void removeBlocks()
    {
        //Creates a map holding the world's real data at the locations of the virtual blocks
        Map<Location, BlockData> realWorldBlocks = getRealBlocks();

        //Sends the changes to the player
        tutorialPlaythrough.getCreatorOrStudent().player.sendMultiBlockChange(realWorldBlocks);

        //Sends the changes to the spies
        ArrayList<Player> spies = tutorialPlaythrough.getSpies();
        int iNumSpies = spies.size();
        int i;
        for (i = 0 ; i < iNumSpies ; i++)
        {
            spies.get(i).sendMultiBlockChange(realWorldBlocks);
        }

        this.bStale = true;
    }

    /**
     * Removes the virtual blocks from the given spy, sets their view back to the real world.
     * @param spy The player to remove the virtual blocks for
     */
    public void removeVirtualBlocksForSpy(Player spy)
    {
        //Creates a map holding the world's real data at the locations of the virtual blocks
        Map<Location, BlockData> realWorldBlocks = getRealBlocks();

        //Sends the changes to the spies
        spy.sendMultiBlockChange(realWorldBlocks);
    }

    /**
     * Adds all of the virtual blocks in this list to the actual world.
     * <p> </p>
     * USE WITH EXTREME CAUTION!
     * <p> </p>
     */
    public void addBlocksToWorld(Logger logger)
    {
        //Store a reference to the world locally
        World worldToSet = tutorialPlaythrough.getLocation().getWorld();

        //Gets the list of locations and block data for this list
        final Location[] locations = this.keySet().stream().toArray(Location[]::new);
        final BlockData[] virtualBlockData = this.values().toArray(BlockData[]::new);
        int iLocations = locations.length;

        //Get the blocks in the world
        for (int i = 0 ; i < iLocations ; i++)
        {
            logger.log(Level.FINE, ChatColor.AQUA +"Setting a " +virtualBlockData[i].getMaterial() +" block to the actual world");
            worldToSet.setBlockData(locations[i], virtualBlockData[i]);
        }
    }

    /**
     * Resets the world back to its original state.
     * <P> </P>
     * When using this method with lots of virtual block groups it is important to call the resets from later virtual block groups first. As in FILO.
     */
    public void resetWorld(TeachingTutorials plugin)
    {
        //Store a reference to the world locally
        World worldToSet = tutorialPlaythrough.getLocation().getWorld();

        //Gets the list of locations and block data for this list
        final Location[] locations = realWorldBlocks.keySet().stream().toArray(Location[]::new);
        final BlockData[] virtualBlockData = realWorldBlocks.values().toArray(BlockData[]::new);
        int iLocations = locations.length;

        Bukkit.getLogger().log(Level.FINE, ChatColor.AQUA +"Resetting the blocks to the world for "+getOwner());

        Bukkit.getScheduler().runTask(plugin, () -> {
            //Set the blocks in the world
            for (int i = 0 ; i < iLocations ; i++)
            {
                worldToSet.setBlockData(locations[i], virtualBlockData[i]);
            }
        });
    }

    /**
     * Returns the map of location to real blocks at the locations of the virtual blocks of this list
     * @return A list of the real world blocks at locations of the virtual blocks of this list
     */
    public Map<Location, BlockData> getRealBlocks()
    {
        return realWorldBlocks;
    }


}
