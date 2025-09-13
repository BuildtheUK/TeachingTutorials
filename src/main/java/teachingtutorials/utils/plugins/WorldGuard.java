package teachingtutorials.utils.plugins;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles all WorldGuard operations
 */
public class WorldGuard
{
    /**
     * Sets the world permissions of a given world. Adds a global region then adds the provided player as an owner.
     * @param bukkitWorld A world to set the permissions for
     * @param creator The creator who owns the location of this world
     */
    public static void setWorldPerms(org.bukkit.World bukkitWorld, Player creator, Logger logger)
    {
        //Gets the worldguard instance
        WorldGuardPlatform platform = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform();
        //Gets the Worldguard representation of a bukkit world
        World world = BukkitAdapter.adapt(bukkitWorld);
        //Gets the region manager for this world
        RegionManager regionManager = platform.getRegionContainer().get(world);

        BlockVector3 blockVector3 = BlockVector3.ZERO;
        //Creates a global region
        ProtectedRegion protectedRegion = new ProtectedCuboidRegion(ProtectedRegion.GLOBAL_REGION, blockVector3, blockVector3);

        //Adds this region to the world
        regionManager.addRegion(protectedRegion);

        if (protectedRegion == null)
        {
            logger.log(Level.SEVERE, "Could not set worldguard perms for world - region was null");
        }
        else
        {
            //Make the whole world protected
            protectedRegion.setFlag(Flags.PASSTHROUGH, StateFlag.State.DENY);

            //Gets a reference to the list of members and owners
            DefaultDomain owners = protectedRegion.getOwners();
            DefaultDomain members = protectedRegion.getMembers();

            //Clears all members and owners
            members.clear();
            owners.clear();

            //Adds the player as an owner
            owners.addPlayer(creator.getUniqueId());
        }

        //Saves the changes
        try
        {
            regionManager.saveChanges();
        }
        catch (StorageException e)
        {
            logger.log(Level.SEVERE, "Could not set worldguard perms for world - " +
                    "failed to save changes", e);
        }
    }

    /**
     * Adds a player to the global region of the given world as a member
     * @param bukkitWorld The world to add the player to
     * @param player The player to add to the world as a member
     */
    public static void addToGlobalRegion(org.bukkit.World bukkitWorld, Player player, Logger logger)
    {
        //Gets the worldguard instance
        WorldGuardPlatform platform = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform();
        //Gets the Worldguard representation of a bukkit world
        World world = BukkitAdapter.adapt(bukkitWorld);
        //Gets the region manager for this world
        RegionManager regionManager = platform.getRegionContainer().get(world);

        //Gets the global region
        ProtectedRegion protectedRegion = regionManager.getRegion(ProtectedRegion.GLOBAL_REGION);

        if (protectedRegion == null)
        {
            logger.log(Level.SEVERE, "Could not add member to world - region was null");
        }
        else
        {
            //Gets a reference to the list of members
            DefaultDomain members = protectedRegion.getMembers();

            //Adds the player as a member
            members.addPlayer(player.getUniqueId());
        }

        //Saves the changes
        try
        {
            regionManager.saveChanges();
        }
        catch (StorageException e)
        {
            logger.log(Level.SEVERE, "Could not set worldguard perms for world - " +
                    "failed to save changes", e);
        }
    }

    /**
     * Removes a player as member from the global region of the given world
     * @param bukkitWorld The world to add the player to
     * @param player The player to add to the world as a member
     */
    public static void removeFromGlobalRegion(org.bukkit.World bukkitWorld, Player player, Logger logger)
    {
        //Gets the worldguard instance
        WorldGuardPlatform platform = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform();
        //Gets the Worldguard representation of a bukkit world
        World world = BukkitAdapter.adapt(bukkitWorld);
        //Gets the region manager for this world
        RegionManager regionManager = platform.getRegionContainer().get(world);

        //Gets the global region
        ProtectedRegion protectedRegion = regionManager.getRegion(ProtectedRegion.GLOBAL_REGION);

        if (protectedRegion == null)
        {
            logger.log(Level.SEVERE, "Could not remove member from world - region was null");
        }
        else
        {
            //Gets a reference to the list of members
            DefaultDomain members = protectedRegion.getMembers();

            //Removes the player as a member
            members.removePlayer(player.getUniqueId());
        }

        //Saves the changes
        try
        {
            regionManager.saveChanges();
        }
        catch (StorageException e)
        {
            logger.log(Level.SEVERE, "Could not set worldguard perms for world - " +
                    "failed to save changes", e);
        }
    }

    /**
     * Creates a new cuboid world guard region
     * @param szName The ID of the new region
     * @param world A WorldGuard representation of the world to add this region to
     * @param player Player to add as a member
     * @param minimumPoint Minimum x/z block of the region
     * @param maximumPoint Maximum x/z block of the region
     */
    public static void createNewRegion(String szName, World world, Player player, BlockVector3 minimumPoint, BlockVector3 maximumPoint, Logger logger)
    {
        //Gets the worldguard instance
        WorldGuardPlatform platform = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform();
        //Gets the Worldguard representation of a bukkit world
//        World world = BukkitAdapter.adapt(bukkitWorld);
        //Gets the region manager for this world
        RegionManager regionManager = platform.getRegionContainer().get(world);

        //Creates a new cuboid region
        ProtectedRegion protectedRegion = new ProtectedCuboidRegion(szName, minimumPoint, maximumPoint);

        //Adds this region to the world
        regionManager.addRegion(protectedRegion);

        if (protectedRegion == null)
        {
            logger.log(Level.SEVERE, "Could not add region to the world - region was null");
        }
        else
        {
            //Gets a reference to the list of members and owners for this region
            DefaultDomain owners = protectedRegion.getOwners();
            DefaultDomain members = protectedRegion.getMembers();

            //Clears all members and owners
            members.clear();
            owners.clear();

            //Adds the player as a member of this region
            members.addPlayer(player.getUniqueId());
        }

        //Saves the changes
        try
        {
            regionManager.saveChanges();
        }
        catch (StorageException e)
        {
            logger.log(Level.SEVERE, "Could not set worldguard perms for world - " +
                    "failed to save changes", e);
        }
    }

    /**
     * Removes a world guard region
     * @param szRegionName The ID of the region to delete
     * @param world The world which this region belongs in
     * @return Whether the region got removed or not
     */
    public static boolean removeRegion(String szRegionName, World world, Logger logger)
    {
        //Gets the worldguard instance
        WorldGuardPlatform platform = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform();
        //Gets the region manager for this world
        RegionManager regionManager = platform.getRegionContainer().get(world);

        try
        {
            //Removes the region
            Set<ProtectedRegion> removedRegions = regionManager.removeRegion(szRegionName);

            //Attempts to save the changes
            regionManager.saveChanges();
            if (removedRegions.size() > 0)
            {
                logger.log(Level.INFO, removedRegions.size()+ " regions were removed");
                return true;
            }
            else
            {
                logger.log(Level.INFO, "No regions were removed");
                return false;
            }

        }
        catch (StorageException e)
        {
            logger.log(Level.SEVERE, "WorldGuard storage error whilst removing regions: ", e);
            return false;
        }
        catch (NullPointerException npe)
        {
            logger.log(Level.SEVERE, "WorldGuard null pointer error whilst removing regions: ", npe);
            return false;
        }
    }
}
