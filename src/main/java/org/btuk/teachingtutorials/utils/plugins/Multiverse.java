package org.btuk.teachingtutorials.utils.plugins;

import org.bukkit.*;

import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;
import org.mvplugins.multiverse.core.world.MultiverseWorld;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.core.world.options.CreateWorldOptions;
import org.mvplugins.multiverse.core.world.options.DeleteWorldOptions;

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
        MultiverseCoreApi core = MultiverseCoreApi.get();

        if (core == null)
        {
            logger.log(Level.SEVERE, "Multiverse is a dependency of TeachingTutorials!");
            return false;
        }

        WorldManager worldManager = core.getWorldManager();

        CreateWorldOptions createWorldOptions = CreateWorldOptions.worldName(name).biome("VoidGen:{biome:PLAINS}").environment(World.Environment.NORMAL)
                .worldType(WorldType.FLAT)
                .generateStructures(false);

        LoadedMultiverseWorld loadedMultiverseWorld = worldManager.createWorld(createWorldOptions).getOrNull();
        if (loadedMultiverseWorld == null)
        {
            logger.severe("MV Error: World did not get created!");
            return false;
        }

        loadedMultiverseWorld.setGameMode(GameMode.CREATIVE);
        loadedMultiverseWorld.setDifficulty(Difficulty.PEACEFUL);
        loadedMultiverseWorld.setAllowWeather(false);
        loadedMultiverseWorld.setHunger(false);
        loadedMultiverseWorld.setAllowFlight(true);
        loadedMultiverseWorld.setKeepSpawnInMemory(false);

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

        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);

        logger.info("Created new world with name " + name);

        return true;
    }

    /**
     * Deletes the given world
     * @param name The name of the world to delete
     * @return Whether the world was deleted
     */
    public static boolean deleteWorld(String name, Logger logger)
    {
        MultiverseCoreApi core = MultiverseCoreApi.get();

        if (core == null) {
            logger.log(Level.SEVERE, "Multiverse is a dependency of TeachingTutorials!");
            return false;
        }

        WorldManager worldManager = core.getWorldManager();

        //If world exists delete it.
        MultiverseWorld world = worldManager.getWorld(name).getOrNull();

        if (world == null) {
            return false;
        } else {
            worldManager.deleteWorld(DeleteWorldOptions.world(world)).getOrNull();
            return true;
        }

    }
}