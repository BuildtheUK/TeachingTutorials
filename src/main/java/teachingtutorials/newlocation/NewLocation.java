package teachingtutorials.newlocation;

import net.buildtheearth.terraminusminus.generator.CachedChunkData;
import net.buildtheearth.terraminusminus.generator.ChunkDataLoader;
import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.projection.GeographicProjection;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;
import net.buildtheearth.terraminusminus.substitutes.ChunkPos;
import net.buildtheearth.terraminusminus.util.geo.LatLng;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.player.PlayerTeleportEvent;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialplaythrough.PlaythroughMode;
import teachingtutorials.tutorialplaythrough.TutorialPlaythrough;
import teachingtutorials.utils.GeometricUtils;
import teachingtutorials.guis.MainMenu;
import teachingtutorials.listeners.Falling;
import teachingtutorials.listeners.PlaythroughCommandListeners;
import teachingtutorials.tutorialobjects.Location;
import teachingtutorials.tutorialobjects.Tutorial;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.User;
import teachingtutorials.utils.plugins.Multiverse;
import teachingtutorials.utils.plugins.WorldGuard;

import java.util.ArrayList;
import java.util.logging.Level;

/**
 * Defines the stages that the New Location process can be in.
 * This is used when terminating the process early.
 */
enum NewLocationProcess
{
    startUp, inputtingAreaBounds, calculatingBounds, inputtingStartPosition, creatingLocationForDB, creatingNewWorld, generatingTerrain, inputtingAnswers
}

/**
 * Represents a creation of a new location for a tutorial
 */
public class NewLocation extends TutorialPlaythrough
{
    /** World generation objects */
    private final EarthGeneratorSettings bteGeneratorSettings = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS);
    /** World generation objects */
    private final GeographicProjection projection = bteGeneratorSettings.projection();
    /** World generation objects */
    private final ChunkDataLoader loader;

    /** Records what 'stage' the new location creation process is at */
    private NewLocationProcess stage;

    /** Stores a list of the area bounds for the location */
    private ArrayList<LatLng> areaBounds;

    /** Stores the rectangular bounds of the area to be generated */
    private int ixMin, ixMax, izMin, izMax;

    /** Stores the height map of the new location */
    private int[][] iHeights;

    //Stores the listeners globally so that they can be accessed and deregistered from outside of the class
    private AreaSelectionListener areaSelectionListener;
    private StartLocationListener startLocationListener;

    public NewLocation(User creator, Tutorial tutorial, TeachingTutorials plugin)
    {
        super(plugin, creator, tutorial, PlaythroughMode.CreatingLocation);

        //Stage set first since the projection creation can take a while, hence the startUp phase
        this.stage = NewLocationProcess.startUp;

        //Initialises the generator objects
        loader = new ChunkDataLoader(bteGeneratorSettings);

        //Adds this tutorial playthrough instance to the list
        this.plugin.activePlaythroughs.add(this);

        //Update the user's current Playthrough
        creatorOrStudent.setCurrentPlaythrough(this);

        iHighestStageCompleted = 0;
        iHighestStepCompleted = 0;
    }

    public int getTutorialID()
    {
        return this.tutorial.getTutorialID();
    }


    /**
     * Launches the new location adding, started from the CreatorTutorialsMenu when they right click
     */
    public void launchNewLocationAdding()
    {
        //Log start
        plugin.getLogger().log(Level.INFO, "Starting new location adding on tutorial "+tutorial.getTutorialID() +" (\"" +tutorial.getTutorialName()+"\") by " +creatorOrStudent.player.getName());

        //Disables spying if they are currently spying
        if (creatorOrStudent.isSpying())
            creatorOrStudent.disableSpying();

        //Register the tpll and ll command and the gmask blocker
        playthroughCommandListeners = new PlaythroughCommandListeners(plugin);
        playthroughCommandListeners.register();

        //Register tpll listeners to allow the creator to input the bounds of the area to generate. The listener will also listen out for /tutorials endarea which marks the areas as completely defined
        areaSelectionListener = new AreaSelectionListener(this.creatorOrStudent, plugin, this);
        areaSelectionListener.register();
        //This will get unregistered when the command "/tutorials endarea" is run

        //Updates the stage of the new location process
        this.stage = NewLocationProcess.inputtingAreaBounds;

        //Log to console
        plugin.getLogger().log(Level.INFO, "Registered area selection listener ("+this.creatorOrStudent.player.getName()+")");
        plugin.getLogger().log(Level.INFO, "Registered endarea listener ("+this.creatorOrStudent.player.getName()+")");

        this.creatorOrStudent.player.sendMessage(ChatColor.AQUA +"Use /tpll to draw out an area in a circular order to generate the terrain for the location");
        this.creatorOrStudent.player.sendMessage(ChatColor.AQUA +"Run /tutorials endarea once you are done");

        //Stores a reference to the area bounds globally in this object
        this.areaBounds = areaSelectionListener.getBounds();
    }

    /**
     * Called once the area definition has been made. Will begin the next stages of the new location creation process, starting with a calculation of the bounds
     */
    public void AreaSelectionMade()
    {
        //Log to console
        plugin.getLogger().log(Level.INFO, ChatColor.AQUA +"Area selection made ("+this.creatorOrStudent.player.getName()+")");

        //Updates the stage
        stage = NewLocationProcess.calculatingBounds;

        //---------------------------------------------------
        //----------Calculate range in the xz plane----------
        //---------------------------------------------------
        // We need to calculate a squared area from Lat and Long bounds for the Location's area
        // Therefore we need to convert all of the coordinates to minecraft coordinates and find the min and max X and Z

        int i;

        //Amount of corners on the shape
        int iBounds = areaBounds.size();

        //Stores the corners of the box where blocks are to be generated, in decimal form
        double xMin, zMin, xMax, zMax;

        //Stores the mc coordinates of each corner as it gets converted from geographical to minecraft
        double[] xz;

        //Converts the first corner to minecraft coordinates
        LatLng areaBound = areaBounds.get(0);
        try
        {
            xz = projection.fromGeo(areaBound.getLng(), areaBound.getLat());
        }
        catch (OutOfProjectionBoundsException e)
        {
            //This shouldn't ever happen since they pass through checks before being added to the area bounds array, but it is necessary to catch the exception
            plugin.getLogger().log(Level.WARNING, ChatColor.RED +"Could not create new location, one of the vertices of the area is outside the projection");
            creatorOrStudent.player.sendMessage(Display.errorText("Could not create new location, one of the vertices of the area is outside the projection"));
            return;
        }

        //Stores this corner as the min and max
        xMin = xz[0];
        xMax = xz[0];
        zMin = xz[1];
        zMax = xz[1];

        //Gets the minecraft coordinates of the subsequent corners, and updates the minimum and maximum of the x and z coordinates
        for (i = 1 ; i < iBounds ; i++)
        {
            areaBound = areaBounds.get(i);
            try
            {
                xz = projection.fromGeo(areaBound.getLng(), areaBound.getLat());
            }
            catch (OutOfProjectionBoundsException e)
            {
                //This shouldn't ever happen since they pass through checks before being added to the area bounds array, but it is necessary to catch the exception
                plugin.getLogger().log(Level.WARNING, "Could not create new location, one of the corners of the area is outside the projection");
                creatorOrStudent.player.sendMessage(Display.errorText("Could not create new location, one of the vertices of the area is outside the projection"));
                return;
            }

            //Updates the min and max if necessary
            if (xz[0] < xMin)
                xMin = xz[0];
            if (xz[0] > xMax)
                xMax = xz[0];
            if (xz[1] < zMin)
                zMin = xz[1];
            if (xz[1] > zMax)
                zMax = xz[1];
        }

        //Converts the min and maxes into integer form
        ixMin = (int) Math.round(xMin);
        ixMax = (int) Math.round(xMax);
        izMin = (int) Math.round(zMin);
        izMax = (int) Math.round(zMax);

        plugin.getLogger().log(Level.INFO, ChatColor.AQUA +"Area of the minecraft world to be generated has been calculated");
        plugin.getLogger().log(Level.INFO, ChatColor.AQUA +"Range in x: " +ixMin +" to "+ixMax);
        plugin.getLogger().log(Level.INFO, ChatColor.AQUA +"Range in z: " +izMin +" to "+izMax);

        //Creates the listener for the start position coordinates
        startLocationListener = new StartLocationListener(this.creatorOrStudent, plugin, this, ixMin, ixMax, izMin, izMax, projection);
        startLocationListener.register();

        plugin.getLogger().log(Level.INFO, ChatColor.AQUA +"Registered start location listener");

        //Updates the stage
        this.stage = NewLocationProcess.inputtingStartPosition;

        //Prompts the creator to teleport (using tpll) to the start location of the tutorial
        this.creatorOrStudent.player.sendMessage(Display.aquaText(ChatColor.AQUA +"Use /tpll to teleport to the desired start location"));
    }

    /**
     * Called from the StartLocationListener once the start location has been dictated.
     * <p> </p>
     * Creates the new location in the DB, creates the world, generates the area, teleports the player to the start, begins the answer recording process
     * @param startTP The start location of the tutorial
     */
    public void lessonStart(LatLng startTP)
    {
        //Updates the stage
        this.stage = NewLocationProcess.creatingLocationForDB;

        //Creates a new location object
        this.location = new Location(this.getTutorialID());

        //Adds the location to the database
        if (location.insertNewLocation(plugin.getDBConnection(), plugin.getLogger()))
            plugin.getLogger().log(Level.INFO, "Inserted location into DB with LocationID "+location.getLocationID());
        else
        {
            plugin.getLogger().log(Level.SEVERE, ChatColor.RED +"Could not insert location into DB");
            return;
        }

        String szWorldName = location.getLocationID()+"";

        this.stage = NewLocationProcess.creatingNewWorld;

        //Creates the new world to store this location in
        try
        {
            if (Multiverse.createVoidWorld(szWorldName, plugin.getLogger()))
            {
                plugin.getLogger().log(Level.INFO, "Created new world");
                //Sets the world perms
                WorldGuard.setWorldPerms(Bukkit.getWorld(szWorldName), creatorOrStudent.player, plugin.getLogger());
                plugin.getLogger().log(Level.INFO, "Set the world perms");
            }
            else
            {
                plugin.getLogger().log(Level.SEVERE, "Could not create new world");
                creatorOrStudent.player.sendMessage(Display.errorText("Could not create the world"));
                return;
            }
        }
        catch (IllegalArgumentException e)
        {
            if (e.getMessage().equals("That world is already loaded!"))
            {
                plugin.getLogger().log(Level.SEVERE, "Could not create new world. World already loaded.", e);
                creatorOrStudent.player.sendMessage(Display.errorText("Could not create the world. World already loaded."));
                return;
            }
            else
            {
                plugin.getLogger().log(Level.SEVERE, "Could not create new world.", e);
                creatorOrStudent.player.sendMessage(Display.errorText("Could not create the world. Speak to your server admin, or the author of TeachingTutorials"));
            }
        }
        catch (Exception e)
        {
            plugin.getLogger().log(Level.SEVERE, "Could not create new world.", e);
            creatorOrStudent.player.sendMessage(Display.errorText("Could not create the world. Speak to your server admin, or the author of TeachingTutorials"));
        }

        //Fetch the object for this bukkit world
        plugin.getLogger().log(Level.INFO, "Attempting to get world from Bukkit with name: "+szWorldName);
        World world = Bukkit.getWorld(szWorldName);
        if (world != null)
            plugin.getLogger().log(Level.INFO, "World object created in plugin with name: "+world.getName());
        else
        {
            plugin.getLogger().log(Level.SEVERE, "World object for world with name: "+world.getName() +" could not be created in the plugin, so generation can't proceed");
            return;
        }

        //Sets the world in the location object
        this.location.setWorld(world);

        //Updates the stage at of the creation process
        this.stage = NewLocationProcess.generatingTerrain;

        //Generates the required area in the world
        creatorOrStudent.player.sendMessage(Display.aquaText("Generating the area"));

        //TODO: For the future, an idea could be to generate terrain as soon as 3 area selection points have been made

        //Runs generation scheduling asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try
                {
                    TerraMinusMinusGeneration(world, startTP);
                }
                catch (Exception e)
                {
                    plugin.getLogger().log(Level.SEVERE, "Could not generate the required area in the world: ", e);
                    return;
                }
            }
        });
    }

    /**
     * Teleports the player to the start location previously inputted. Then initiates the playthrough of the tutorial:
     * Registers the fall listener, fetches the stages and starts the first stage
     * @param world The world for this new location
     * @param startTP The start position of the location (where to tp the player to)
     */
    private void teleportCreatorAndStartLesson(World world, LatLng startTP)
    {
        //Finds the start location
        org.bukkit.Location tpLocation = GeometricUtils.convertToBukkitLocation(world, startTP.getLat(), startTP.getLng());
        tpLocation.add(0, 1, 0);

        //Registers the fall listener
        fallListener = new Falling(creatorOrStudent.player, tpLocation, plugin);
        fallListener.register();

        //Teleports player to the start location - yaw and pitch are irrelevant here
        creatorOrStudent.player.teleport(tpLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
        plugin.getLogger().log(Level.INFO, ChatColor.AQUA +"[TeachingTutorials] Creator teleported to the start location, starting lesson to get answers");

        //Sets stage number to 0
        this.iStageIndex = 0;

        //Starts the first stage
        this.stage = NewLocationProcess.inputtingAnswers;
        nextStage(1, true);
    }

    /**
     * Cancels the new location creation process. Will attempt to delete any worlds and new locations in the DB if required
     * and unregisters listeners and teleport the player back to te lobby.
     */
    public void terminateEarly()
    {
        playthroughCommandListeners.unregister();

        //Unregisters the correct listeners
        switch (stage)
        {
            case inputtingAreaBounds:
                areaSelectionListener.deregister();
                break;
            case inputtingStartPosition:
                startLocationListener.deregister();
                break;
            case creatingLocationForDB:
                if (Location.deleteLocationByID(location.getLocationID(), plugin.getDBConnection(), plugin.getLogger()))
                    plugin.getLogger().log(Level.INFO, "Location with LocationID = "+location.getLocationID() +" was deleted");
                else
                    plugin.getLogger().log(Level.WARNING, "Location with LocationID = "+location.getLocationID() +" could not be deleted");
                break;
            case creatingNewWorld:
            case generatingTerrain:
                //Delete the location in the DB
                if(Location.deleteLocationByID(location.getLocationID(), plugin.getDBConnection(), plugin.getLogger()))
                    plugin.getLogger().log(Level.INFO, "Location with LocationID = "+location.getLocationID() +" was deleted");
                else
                    plugin.getLogger().log(Level.WARNING, "Location with LocationID = "+location.getLocationID() +" could not be deleted");

                //Delete the world
                Multiverse.deleteWorld(location.getLocationID()+"", plugin.getLogger());
                break;
            case inputtingAnswers:
                //Performs common playthrough termination processes
                super.commonEndPlaythrough();
                creatorOrStudent.mainGui = new MainMenu(plugin, creatorOrStudent);

                //Delete the location in the DB
                if(Location.deleteLocationByID(location.getLocationID(), plugin.getDBConnection(), plugin.getLogger()))
                    plugin.getLogger().log(Level.INFO, "Location with LocationID = "+location.getLocationID() +" was deleted");
                else
                    plugin.getLogger().log(Level.WARNING, "Location with LocationID = "+location.getLocationID() +" could not be deleted");

                //Teleport the player off the world
                User.teleportPlayerToLobby(creatorOrStudent.player, plugin, 0);

                //Delete the world after some time, to ensure player has left
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        Multiverse.deleteWorld(location.getLocationID()+"", plugin.getLogger());
                    }
                }, 2L);
                break;
            default:
                break;
        }

        //Removes the new location from the new locations list
        this.plugin.activePlaythroughs.remove(this);
    }

    /**
     * Called at the end of answer inputting when the location has been fully set up.
     */
    protected void endPlaythrough()
    {
        //Informs the user of successful creation of a location
        TextComponent message = Component.text("Location Created!").style(Style.style(NamedTextColor.DARK_GREEN, TextDecoration.UNDERLINED));
        creatorOrStudent.player.sendMessage(message);
        Display.ActionBar(creatorOrStudent.player, message);

        //Informs the console of successful creation of a location
        plugin.getLogger().log(Level.INFO, ChatColor.DARK_GREEN +"Location Created with LocationID: "+this.location.getLocationID());

        //Removes the new location from the new locations list
        this.plugin.activePlaythroughs.remove(this);

        //Performs common playthrough completion processes
        super.commonEndPlaythrough();
    }

    /**
     * Generates the terrain of the world using TerraMinusMinus
     * <p> </p>
     * Once this is complete, it will teleport the player to the start location and begin the answer gathering phase.
     * @param world The world to generate the terrain in
     * @param startTP The start position of this Tutorial Location
     */
    private void TerraMinusMinusGeneration(World world, LatLng startTP)
    {
        //Get chunk bounds
        int ixMinChunk = ixMin >> 4;
        int ixMaxChunk = ixMax >> 4;
        int izMinChunk = izMin >> 4;
        int izMaxChunk = izMax >> 4;

        plugin.getLogger().log(Level.INFO, "Min X Chunk: "+ixMinChunk);
        plugin.getLogger().log(Level.INFO, "Max X Chunk: "+ixMaxChunk);
        plugin.getLogger().log(Level.INFO, "Min Z Chunk: "+izMinChunk);
        plugin.getLogger().log(Level.INFO, "Max Z Chunk: "+izMaxChunk);

        plugin.getLogger().log(Level.INFO, "Generating Terrain with terra-- generation");

        int iChunksScheduled = 0;

        //Go through each chunk and set up the generation task
        for (int ixChunk = ixMinChunk ; ixChunk <= ixMaxChunk ; ixChunk++)
        {
            for (int izChunk = izMinChunk ; izChunk <= izMaxChunk ; izChunk++)
            {
                int finalIxChunk = ixChunk;
                int finalIzChunk = izChunk;
                plugin.getLogger().log(Level.FINE, "Scheduling chunk: "+finalIxChunk+","+finalIzChunk);

                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                        @Override
                        public void run() {
                            plugin.getLogger().log(Level.FINE, "Generating Chunk: "+finalIxChunk+","+finalIzChunk);
                            try
                            {
                                CachedChunkData terraData = loader.load(new ChunkPos(finalIxChunk, finalIzChunk)).get();
                                int starterX = finalIxChunk << 4;
                                int starterZ = finalIzChunk << 4;

                                for (int x = 0 ; x < 16 ; x++)
                                {
                                    for (int z = 0 ; z < 16 ; z++)
                                    {
                                        world.getBlockAt(starterX + x, terraData.groundHeight(x, z), starterZ + z).setType(Material.GRASS_BLOCK);
                                    }
                                }

                                //All chunks scheduled
                                if (finalIxChunk == ixMaxChunk && finalIzChunk == izMaxChunk)
                                {
                                    //Log to console and player
                                    plugin.getLogger().log(Level.INFO, "Generated the area in the world");
                                    creatorOrStudent.player.sendMessage(Display.aquaText("Area generated"));

                                    //Begins the teleportation and answer gathering phase
                                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                                        @Override
                                        public void run() {
                                            teleportCreatorAndStartLesson(world, startTP);
                                        }
                                    });
                                }
                            }
                            catch (Exception e)
                            {
                                plugin.getLogger().log(Level.SEVERE, "Unable to generate block in the world: " +e.getMessage(), e);
                            }

                        }
                    }, iChunksScheduled/10 + iChunksScheduled/64);
                //After 64 chunks, the next tick is free for any other processes.
                //We use 64 to assist the CPU.

                plugin.getLogger().log(Level.FINE, "Scheduled chunk: "+finalIxChunk+","+finalIzChunk +" to run in " +(iChunksScheduled/10 + iChunksScheduled/50) +" ticks");

                iChunksScheduled++;
            }
        }
    }
}
