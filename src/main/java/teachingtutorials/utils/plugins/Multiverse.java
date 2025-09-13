package teachingtutorials.utils.plugins;

import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import org.bukkit.*;

import com.onarandombox.MultiverseCore.MultiverseCore;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles all Multiverse operations
 */
public class Multiverse
{
    /**
     * Creates a void world using multiverse and void gen
     * @param name The name of the new world
     * @return Whether the new world was created successfully
     */
    public static boolean createVoidWorld(String name, Logger logger)
    {
        MultiverseCore core = (MultiverseCore) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");

        if (core == null)
        {
            logger.log(Level.SEVERE, "Multiverse is a dependency of TeachingTutorials!");
            return false;
        }

        MVWorldManager worldManager = core.getMVWorldManager();

        worldManager.addWorld(
                name,
                World.Environment.NORMAL,
                null,
                WorldType.FLAT,
                false,
                "VoidGen:{biome:PLAINS}"
        );

        MultiverseWorld MVWorld = worldManager.getMVWorld(name);
        MVWorld.setGameMode(GameMode.CREATIVE);
        MVWorld.setAllowAnimalSpawn(false);
        MVWorld.setAllowMonsterSpawn(false);
        MVWorld.setDifficulty(Difficulty.PEACEFUL);
        MVWorld.setEnableWeather(false);
        MVWorld.setHunger(false);
        MVWorld.setAllowFlight(true);
        MVWorld.setKeepSpawnInMemory(false);
        MVWorld.setEnableWeather(false);

        //Get world from bukkit.
        World world = Bukkit.getWorld(name);

        if (world == null) {
            logger.warning("World is null!");
            return false;
        }

        //Disable daylightcycle.
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setTime(6000);

        //Disable fire tick.
        world.setGameRule(GameRule.DO_FIRE_TICK, false);

        //Disable random tick.
        world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);

        Bukkit.getLogger().info("Created new world with name " + name);

        return true;
    }

    /**
     * Returns whether there is a world which exists with the given name
     * @param name
     * @return Whether such a world exists
     */
    public static boolean hasWorld(String name, Logger logger)
    {
        MultiverseCore core = (MultiverseCore) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");

        if (core == null) {
            logger.log(Level.SEVERE, "Multiverse is a dependency of TeachingTutorials!");
            return false;
        }

        //If the world exists return true.

        MVWorldManager worldManager = core.getMVWorldManager();

        MultiverseWorld world = worldManager.getMVWorld(name);

        return world != null;
    }

    /**
     * Deletes the given world
     * @param name The name of the world to delete
     * @return Whether the world was deleted
     */
    public static boolean deleteWorld(String name, Logger logger)
    {
        MultiverseCore core = (MultiverseCore) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");

        if (core == null) {
            logger.log(Level.SEVERE, "Multiverse is a dependency of TeachingTutorials!");
            return false;
        }

        //If world exists delete it.
        MVWorldManager worldManager = core.getMVWorldManager();

        MultiverseWorld world = worldManager.getMVWorld(name);

        if (world == null) {
            return false;
        } else {
            worldManager.removePlayersFromWorld(name);
            worldManager.deleteWorld(name);
            return true;
        }

    }
}