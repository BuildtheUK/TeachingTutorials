package teachingtutorials;

import lombok.Getter;
import net.bteuk.minecraft.gui.*;
import net.bteuk.minecraft.misc.PlayerUtils;
import net.bteuk.teachingtutorials.services.PromotionService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import teachingtutorials.commands.Blockspy;
import teachingtutorials.commands.PlayersPlayingTutorialsCompleter;
import teachingtutorials.guis.Event;
import teachingtutorials.guis.MainMenu;
import teachingtutorials.services.TutorialsPromotionService;
import teachingtutorials.tutorialobjects.*;
import teachingtutorials.tutorialplaythrough.FundamentalTaskType;
import teachingtutorials.listeners.PlayerInteract;
import teachingtutorials.listeners.JoinLeaveEvent;
import teachingtutorials.listeners.GlobalPlayerCommandProcess;
import teachingtutorials.tutorialplaythrough.*;
import teachingtutorials.utils.*;
import teachingtutorials.utils.plugins.WorldEditImplementation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

/**
 * The main class of the TeachingTutorials plugin
 */
public class TeachingTutorials extends JavaPlugin
{
    /** A reference to the config of the main instance plugin */
    private static FileConfiguration config;

    /** A connection to the database */
    private DBConnection dbConnection;

    /** An item stack for the menu opener icon */
    public static ItemStack menu;

    /** The gui manager */
    @Getter
    private GuiManager tutGuiManager;

    /** The slot in which the menu opener icon should appear */
    private int iLearningMenuSlot;

    /** A list of all connected players */
    public ArrayList<User> players;

    /** A list of all current tutorial playthroughs */
    public ArrayList<TutorialPlaythrough> activePlaythroughs;

    /**
     * A list of all active virtual block groups.
     * Each task's virtual blocks are stored in a group and placed here when they are active and need to be displayed
     */
    private Stack<VirtualBlockGroup<org.bukkit.Location, BlockData>> virtualBlockGroups;

    /** Identifies which world edit is being used */
    public WorldEditImplementation worldEditImplementation;

    /** Stores how many calculations have been attempted since a success */
    public int iFailedCalculations;

    /** Service to promote players after completing the compulsory tutorial */
    private PromotionService promotionService;

    /**
     * Performs all of the startup logic for this plugin
     */
    @Override
    public void onEnable()
    {
        //Checks the existence of the soft dependencies
        if (!Bukkit.getPluginManager().isPluginEnabled("DecentHolograms"))
        {
            getLogger().severe("*** DecentHolograms is not installed or not enabled. ***");
            getLogger().severe("*** This plugin will be disabled. ***");
            this.setEnabled(false);
            return;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("Multiverse-Core"))
        {
            getLogger().severe("*** Multiverse-Core is not installed or not enabled. ***");
            getLogger().severe("*** This plugin will be disabled. ***");
            this.setEnabled(false);
            return;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldGuard"))
        {
            getLogger().severe("*** WorldGuard is not installed or not enabled. ***");
            getLogger().severe("*** This plugin will be disabled. ***");
            this.setEnabled(false);
            return;
        }

        if (Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit"))
        {
            worldEditImplementation = WorldEditImplementation.FAWE;
            getLogger().info("WorldEdit implementation detected as FAWE");
        }
        else if (Bukkit.getPluginManager().isPluginEnabled("WorldEdit"))
        {
            worldEditImplementation = WorldEditImplementation.WorldEdit;
            getLogger().severe("WorldEdit implementation detected as WorldEdit (not FAWE). Please change to FAWE to continue.");
            getLogger().severe("Contact the authors of the plugin for support");
            getLogger().severe("*** This plugin will be disabled. ***");
            this.setEnabled(false);
            return;
        }
        else
        {
            worldEditImplementation = WorldEditImplementation.NONE;
            getLogger().severe("*** No type of WorldEdit is loaded on the server. ***");
            getLogger().severe("*** This plugin will be disabled. ***");
            this.setEnabled(false);
            return;
        }

        //Set the static instance of the config to the config of this instance
        TeachingTutorials.config = this.getConfig();
        saveDefaultConfig();

        //Initialise the arrays
        players = new ArrayList<>();
        activePlaythroughs = new ArrayList<>();
        virtualBlockGroups = new Stack<>();

        //-------------------------------------------------------------------------
        //----------------------------------MySQL----------------------------------
        //-------------------------------------------------------------------------
        //Records whether there were any failures connecting to the database
        boolean bSuccess;

        //Initiate the DB connection object
        dbConnection = new DBConnection();

        //Attempt set up from config and connect
        dbConnection.mysqlSetup(this.getConfig());
        bSuccess = dbConnection.connect();

        //Test whether database connected
        if (bSuccess)
        {
            getLogger().log(Level.INFO, ChatColor.GREEN + "MySQL database connected");

            //Creates the tables of the database
            createTables();
        }

        //----------------------------------------
        //-----------Load New Tutorials-----------
        //----------------------------------------

        //Makes the new tutorials folder if it doesn't already exist
        String szFolder = getDataFolder().getAbsolutePath()+"/NewTutorials";
        File folder = new File(szFolder);
        if (!folder.exists())
        {
            folder.mkdir();
        }

        //Makes the tutorial archive folder if it doesn't already exist
        String szArchiveFolder = getDataFolder().getAbsolutePath()+"/TutorialArchives";
        File archiveFolder = new File(szArchiveFolder);
        if (!archiveFolder.exists())
        {
            archiveFolder.mkdir();
        }

        //Goes through the folder and if files are found, load them into the database
        //Folder is sent as it is needed
//        int iNumFiles = folder.list().length;

        // This will break when file moving is fixed. Solution could be to create a local array of all the files then iterate through in a for loop
        for (int i = 0 ; i < folder.list().length ; i++)
        {
            File file = folder.listFiles()[i];
            if (!file.isDirectory())
            {
                getLogger().log(Level.INFO, ChatColor.AQUA +"Loading new tutorial file: "+file.getName());
                interpretNewTutorial(file);
            }
        }

        //------------------------------------------------
        //-------------- Create menu opener --------------
        //------------------------------------------------

        //Initiates the menu item as an emerald with "Learning Menu" name
        menu = new ItemStack(Material.EMERALD);
        ItemMeta meta = menu.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Learning Menu");
        menu.setItemMeta(meta);

        PlayerUtils.protectedItems.add(menu);

        //Initiates the slot index where the menu should be placed
        iLearningMenuSlot = config.getInt("Learning_Menu_Slot");

        //Repeating schedule - updates the learning menu slot to make sure all player always have the learning menu icon
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){
            public void run()
            {
                //Stores the item in the slot where the learning menu ought to be
                ItemStack currentItemInSlot;

                //Goes through all players
                for (Player p : Bukkit.getOnlinePlayers())
                {
                    //Gets the item in the slot where the learning menu ought to be
                    currentItemInSlot = p.getInventory().getItem(iLearningMenuSlot - 1);

                    //Updates the slot if necessary
                    if (currentItemInSlot == null)
                    {
                        p.getInventory().setItem(iLearningMenuSlot - 1, menu);
                    }
                    else if (!currentItemInSlot.equals(menu))
                    {
                        //Attempts to move the current item to a free slot if there is one available
                        int iEmptySlot = PlayerUtils.getAvailableHotbarSlot(p);
                        if (iEmptySlot != -1)
                            p.getInventory().setItem(iEmptySlot, currentItemInSlot);

                        p.getInventory().setItem(iLearningMenuSlot - 1, menu);
                    }
                }
            }
        }, 0L, config.getLong("Menu_Icon_Refresh_Period"));


        //-----------------------------------------------
        //--------- Initialises the Gui Manager ---------
        //-----------------------------------------------
        tutGuiManager = new GuiManager();

        //---------------------------------------
        //------------ Adds Commands ------------
        //---------------------------------------
        getCommand("blockspy").setTabCompleter(new PlayersPlayingTutorialsCompleter(this));
        getCommand("blockspy").setExecutor(new Blockspy(this));

        //---------------------------------------
        //------------ Enable Services ----------
        //---------------------------------------
        ServicesManager servicesManager = this.getServer().getServicesManager();
        // Register backup promotion service.
        servicesManager.register(PromotionService.class, new TutorialsPromotionService(this), this, ServicePriority.Lowest);
        RegisteredServiceProvider<PromotionService> registeredPromotionService = servicesManager.getRegistration(PromotionService.class);
        if (registeredPromotionService != null) {
            promotionService = registeredPromotionService.getProvider();
            getLogger().info(String.format("Loaded promotion service: %s", promotionService.getDescription()));
        } else {
            getLogger().severe("*** Promotion service was not loaded. ***");
            getLogger().severe("*** This plugin will be disabled. ***");
            this.setEnabled(false);
            return;
        }

        //---------------------------------------
        //----------Sets up event check----------
        //---------------------------------------
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public void run()
            {
                //Deal with external events in the DB
                ArrayList<Event> events = Event.getLatestEvents(dbConnection, getLogger());
                int iNumEvents = events.size();
                Event event;
                User user;

                //Goes through all of the fetched events
                for (int i = 0 ; i < iNumEvents ; i++)
                {
                    //Stores the event in its own local variable
                    event = events.get(i);

                    //Gets the user from the list of the plugin's users based on the player
                    user = User.identifyUser(TeachingTutorials.this, event.getPlayer());
                    if (user != null)
                    {
                        MainMenu.performEvent(event.getEventType(), user, TeachingTutorials.this, event.getData());
                        /*
                        We only want the event to be removed if the player was on the server and the event took place
                        There may be a delay/lag period where the event is in the DB but the user isn't yet on the server
                        So we want to keep the event around if that happens so on the next run through the user who might
                        Now be on the server will be taken to a tutorial or whatever
                         */
                        event.remove(dbConnection, getLogger());
                    }
                    //else
                        //Do nothing, they may still be in server transport
                }
            }
        }, 0L, config.getLong("Events_Check_Period"));


        //----------------------------------------
        //--------Refreshes virtual blocks--------
        //----------------------------------------
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, () ->
        {
            //Declares the temporary list object
            VirtualBlockGroup<Location, BlockData> virtualBlockGroup;

            //Goes through all virtual block groups
            int iTasksActive = virtualBlockGroups.size();
            for (int j = 0 ; j < iTasksActive ; j++)
            {
                //Extracts the jth virtual block group
                virtualBlockGroup = virtualBlockGroups.get(j);

                //Calls for the blocks to be displayed. This will only action if the group is not stale.
                virtualBlockGroup.displayBlocks();
            }
        }, 0, config.getLong("Virtual_Block_Refresh"));


        //-----------------------------------------
        //------ Performs calculation events ------
        //-----------------------------------------
        iFailedCalculations = 0;
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, () ->
        {
            if (!WorldEdit.isCurrentCalculationOngoing())
            {
                iFailedCalculations = 0;

                WorldEditCalculation worldEditCalculation = WorldEdit.pendingCalculations.peek();
                if (worldEditCalculation != null)
                {
                    getLogger().log(Level.INFO, ChatColor.YELLOW +"Calculation not already in progress, a new one has been detected");
                    worldEditCalculation.runCalculation();
                }
            }
            else
            {
                iFailedCalculations++;
                getLogger().log(Level.INFO, ChatColor.YELLOW +"Calculation ongoing, not initiating a new one");
                long lSecondsPerLoop = 6L/20L; // = second per tick * ticks per loop
                long iSecondsSinceFailedCalculation = iFailedCalculations * lSecondsPerLoop;
                if (iSecondsSinceFailedCalculation >= 15)
                {
                    WorldEdit.pendingCalculations.peek().terminateCalculation();
                }
            }
        }, 0, 6L);

        //Resets the world on a timer - when in a calculation, goes through all active virtual blocks and resets the
        // world back to the state in which it was before the virtual blocks were added, then redisplays the virtual
        // blocks to the viewers
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, () ->
        {
            //Run the resetting
            Bukkit.getScheduler().runTask(TeachingTutorials.this, new Runnable() {
                @Override
                public void run()
                {
                    if (!WorldEdit.isCurrentCalculationOngoing())
                    {
                        int iTasksActive = virtualBlockGroups.size();
                        for (int j = iTasksActive-1 ; j >=0 ; j--)
                        {
                            VirtualBlockGroup<Location, BlockData> virtualBlockGroup = virtualBlockGroups.get(j);
                            //Attempt to remove stale groups
                            if (virtualBlockGroup.isStale())
                            {
                                virtualBlockGroup.removeBlocks();
                                TeachingTutorials.this.getLogger().log(Level.FINE, "Found stale at head, removing a virtual blocks group for " +virtualBlockGroup.getOwner());
                                virtualBlockGroups.remove(j);
                            }
                            //If this group is not stale then break the clean up
                            else
                            {
                                break;
                            }
                        }

                        //Get the list of virtual blocks
                        VirtualBlockGroup[] virtualBlockGroups = TeachingTutorials.this.getVirtualBlockGroups().toArray(VirtualBlockGroup[]::new);

                        //Declares the temporary list object
                        VirtualBlockGroup<Location, BlockData> virtualBlockGroup;

                        //Goes through all virtual block groups - will do this going from end of tutorial to start
                        iTasksActive = virtualBlockGroups.length;
                        for (int j = iTasksActive-1 ; j >=0 ; j--)
                        {
                            //Extracts the jth virtual block group
                            virtualBlockGroup = virtualBlockGroups[j];

                            //Call for the world blocks to be reset
                            virtualBlockGroup.resetWorld(TeachingTutorials.this);
                            //Todo: These don't get done in sync. All world setting needs to be waited on with events.
                            //In order to solve this temporarily, make sure the virtual block refresh period occurs more frequently
                            // than the world reset
                            virtualBlockGroup.displayBlocks();
                        }
                    }
                }
            });
        }, 1, config.getLong("World_Reset_Period"));

        //---------------------------------------
        //---------------Listeners---------------
        //---------------------------------------

        //Handles welcome message and gamemode
        new JoinLeaveEvent(this);

        //Handles menus
        new PlayerInteract(this);
        new GuiListener(tutGuiManager).register(this);

        //Handles tpll, ll and /tutorials
        new GlobalPlayerCommandProcess(this);

        this.setEnabled(true);
    }

    /**
     * Add a group of virtual blocks to the active list
     * @param virtualBlocks A reference to the list you want to add
     */
    public void addVirtualBlocks(VirtualBlockGroup<org.bukkit.Location, BlockData> virtualBlocks)
    {
        this.getLogger().log(Level.INFO, "A group of virtual blocks have just been added to the list of active groups");
        virtualBlocks.setActive();
        virtualBlockGroups.add(virtualBlocks);
    }

    /**
     * Removes a group of virtual blocks from the active list and resets the view of the player and spies for this group
     * @param virtualBlocks A reference to the list you want to remove
     */
    public void removeVirtualBlocks(VirtualBlockGroup<org.bukkit.Location, BlockData> virtualBlocks)
    {
        //Resets the views and marks the group as stale
        virtualBlocks.removeBlocks();
    }

    /**
     *
     * @return A reference to the list of active virtual block groups
     */
    public Stack<VirtualBlockGroup<org.bukkit.Location, BlockData>> getVirtualBlockGroups()
    {
        return virtualBlockGroups;
    }

    /**
     * Interprets the data of a new tutorial
     * @param file The file from which to interpret the new tutorial
     */
    private void interpretNewTutorial(File file)
    {
        //Stores each line as a separate string
        String[] szLines;

        //Reads each line of the file into the string array
        int iLine;
        int iNumLines = 0;
        Scanner szFile;
        try
        {
            //Create scanner
            szFile = new Scanner(file);
            while (szFile.hasNextLine())
            {
                szFile.nextLine();
                iNumLines++;
            }

            //Reset scanner
            szFile.close();
            szFile = new Scanner(file);
            szLines = new String[iNumLines];

            for (iLine = 0 ; iLine < iNumLines ; iLine++)
            {
                szLines[iLine] = szFile.nextLine();
            }
            szFile.close();
        }
        catch (IOException e)
        {
            getLogger().log(Level.SEVERE, "IO - IO Error whilst reading file, skipping file", e);
            return;
        }

        iLine = 0;

        //-------------- Gets the tutorial name, author name and category relevance --------------
        String szName;
        UUID uuidAuthor;
        int[] categoryUsage = new int[Category.values().length];

        String[] szFields = szLines[iLine].split(",");
        if (szFields.length != 7)
        {
            getLogger().log(Level.WARNING, ChatColor.RED +"The tutorial name, author and relevance line does not have 7 fields");
            return;
        }

        szName = szFields[0];
        getLogger().log(Level.INFO, ChatColor.AQUA +"Tutorial name: "+szName);

        try
        {
            uuidAuthor = UUID.fromString(szFields[1]);
            getLogger().log(Level.INFO, ChatColor.AQUA +"Tutorial author: "+uuidAuthor.toString());
        }
        catch (IllegalArgumentException e)
        {
            getLogger().log(Level.WARNING, "Author UUID is not a valid UUID: "+szFields[1]);
            return;
        }

        //Get the category information
        for (int j = 2; j < 7 ; j++)
        {
            if (!szFields[j].matches("([0-9]|[1-9][0-9]|100)"))
            {
                getLogger().log(Level.WARNING, "Tutorial config is not configured correctly." +
                        "Relevances must be between 0 and 100. Line: "+(iLine+1));
                return;
            }
            categoryUsage[j-2] = Integer.parseInt(szFields[j]);
        }

        //Holds all of the tutorial information for the new tutorial
        Tutorial tutorial = new Tutorial(szName, uuidAuthor.toString());

        //Holds type of the last line interpreted
        TutorialObject typeOfLastObjectDealtWith = TutorialObject.Stage;

        //References the stage, step, group that we are currently compiling
        Stage lastStage = null;
        Step lastStep = null;
        Group lastGroup = null;

        //Goes through each line and interprets the instructions
        for (iLine = 1 ; iLine < iNumLines ; iLine++)
        {
            //Stage
            if (szLines[iLine].startsWith("["))
            {
                typeOfLastObjectDealtWith = TutorialObject.Stage;
                Stage stage = new Stage(szLines[iLine].replace("[",""), tutorial.stages.size()+1);
                tutorial.stages.add(stage);
                lastStage = stage;
            }
            //Step
            else if(szLines[iLine].startsWith("("))
            {
                if (!(typeOfLastObjectDealtWith.equals(TutorialObject.Stage)||typeOfLastObjectDealtWith.equals(TutorialObject.Task)))
                {
                    getLogger().log(Level.WARNING, "Tutorial config is not configured correctly, line: "+(iLine+1));
                    return;
                }
                szFields = szLines[iLine].split(",");

                //Field 1 is step name, field 2 is display type
                if (szFields.length < 2)
                {
                    getLogger().log(Level.WARNING, "Tutorial config is not configured correctly, line: "+(iLine+1));
                    return;
                }
                typeOfLastObjectDealtWith = TutorialObject.Step;

                Step step = new Step(szFields[0].replace("(",""), lastStage.steps.size()+1, Display.DisplayType.valueOf(szFields[1]));
                lastStage.steps.add(step);
                lastStep = step;
            }
            //Group
            else if(szLines[iLine].startsWith("{"))
            {
                if (!(typeOfLastObjectDealtWith.equals(TutorialObject.Step)||typeOfLastObjectDealtWith.equals(TutorialObject.Task)))
                {
                    getLogger().log(Level.WARNING, "Tutorial config is not configured correctly, line: "+(iLine+1));
                    return;
                }
                szFields = szLines[iLine].split(",");
                if (szFields.length != 1)
                {
                    getLogger().log(Level.WARNING, "Tutorial config is not configured correctly, line: "+(iLine+1));
                    return;
                }
                typeOfLastObjectDealtWith = TutorialObject.Group;
                Group group = new Group();
                lastStep.groups.add(group);
                lastGroup = group;
            }
            //Task
            else if(szLines[iLine].startsWith("~"))
            {
                //Checks whether we are expecting another task
                if (!(typeOfLastObjectDealtWith.equals(TutorialObject.Group)||typeOfLastObjectDealtWith.equals(TutorialObject.Task)))
                {
                    getLogger().log(Level.WARNING, "Tutorial config is not configured correctly, line: "+(iLine+1));
                    return;
                }
                typeOfLastObjectDealtWith = TutorialObject.Task;

                //Constructs the object
                Task task = new Task(lastGroup.tasks.size()+1);

                //Gets the fields. 1 is ~TaskType and 2 is the extra details
                szFields = szLines[iLine].split(",");

                //Extracts the task type
                String szTaskType = szFields[0].replace("~", "");
                FundamentalTaskType taskType;
                try
                {
                    taskType = FundamentalTaskType.valueOf(szTaskType);
                }
                catch (IllegalArgumentException e)
                {
                    getLogger().log(Level.WARNING, "Invalid task type (\'" +szTaskType +"\') line: "+(iLine+1));
                    return;
                }
                task.setType(taskType);

                //Deals with the extra details
                switch (taskType)
                {
                    case tpll:
                        //Checks the format of the details
                        if (szFields.length != 2)
                        {
                            getLogger().log(Level.WARNING, "Invalid tpll accuracy, you must specify the tpll accuracy, line: "+(iLine+1));
                            return;
                        }

                        String[] szPrecisions = szFields[1].split(";");
                        if (szPrecisions.length != 2)
                        {
                            getLogger().log(Level.WARNING, "Invalid tpll accuracy, you must have 2 floats separated by a ; with no spaces, line: "+(iLine+1));
                            return;
                        }
                        try
                        {
                            float iPerfectDistance = Float.parseFloat(szPrecisions[0]);
                            float iAcceptableDistance =  Float.parseFloat(szPrecisions[1]);

                            if (iAcceptableDistance < iPerfectDistance)
                            {
                                getLogger().log(Level.WARNING, "Invalid tpll accuracy, the limit must be greater than or equal to the perfect distance, line: "+(iLine+1));
                                return;
                            }
                            else
                            {
                                task.setPerfectDistance(iPerfectDistance);
                                task.setAcceptableDistance(iAcceptableDistance);
                            }

                        }
                        catch (NumberFormatException e)
                        {
                            getLogger().log(Level.WARNING, "Invalid tpll accuracy, the accuracies must be integers or floats, line: "+(iLine+1));
                            return;
                        }
                        break;
                    case command:
                        switch (szFields[1])
                        {
                            case "none":
                                task.setCommandActionType(CommandActionType.none);
                                break;
                            case "virtualBlocks":
                                task.setCommandActionType(CommandActionType.virtualBlocks);
                                break;
                            case "full":
                                task.setCommandActionType(CommandActionType.full);
                                break;
                            default:
                                getLogger().log(Level.WARNING, "Invalid command type, line: "+(iLine+1));
                                return;
                        }
                        break;
                } //End type switch

                //Adds the task to the list of tasks
                lastGroup.tasks.add(task);
            } //End task handler
        } //End iteration through lines

        //If it has got to this stage, then the details are all extracted and stored in the tutorial object
        if (addNewTutorialToDB(tutorial))
        {
            //Moves file to the archive folder
            file.renameTo(new File(getDataFolder().getAbsolutePath()+"/TutorialArchives"));
        }
    }

    /**
     * Adds a given tutorial to the DB
     * @param tutorial A tutorial, with all of the relevant information loaded
     * @return Whether it was successully added to the tutorials table of the database
     */
    public boolean addNewTutorialToDB(Tutorial tutorial)
    {
        getLogger().log(Level.INFO, "Inserting new tutorial into DB. Tutorial: "+tutorial.getTutorialName() +" by "+tutorial.getUUIDOfAuthor().toString());

        //Integers used in for loops
        int i, j, k, l;

        int iNumStages;
        int iNumSteps;
        int iNumGroups;
        int iNumTasks;

        //Notes the ID of the current tutorial objects we are within
        int iTutorialID, iStageID, iStepID, iGroupID;

        String sql;
        Statement SQL = null;
        ResultSet resultSet;

        //Insert the new tutorial into the tutorials table
        try
        {
            SQL = this.getDBConnection().getConnection().createStatement();
            sql = "INSERT INTO `Tutorials` (`TutorialName`, `Author`) VALUES ('"+tutorial.getTutorialName()+"', '"+tutorial.getUUIDOfAuthor() +"')";
            SQL.executeUpdate(sql);

            //Gets the Tutorial of the tutorial just inserted
            sql = "Select LAST_INSERT_ID()";
            resultSet = SQL.executeQuery(sql);
            resultSet.next();
            iTutorialID = resultSet.getInt(1);
        }
        catch (Exception e)
        {
            getLogger().log(Level.SEVERE, "Could not insert new tutorial into DB. Name: "+tutorial.getTutorialName(), e);
            return false;
        }

        //Add the relevances into the DB
        for (i = 0 ; i < 5 ; i++)
        {
            try
            {
                sql = "INSERT INTO `CategoryPoints` (`TutorialID`, `Category`, `Relevance`) VALUES (" + iTutorialID + ", '" + tutorial.szCategoryEnumsInOrder[i] + "', " +((float) tutorial.getCategoryUsage(i))/100f+ ")";
                SQL.executeUpdate(sql);
            }
            catch (Exception e)
            {
                getLogger().log(Level.SEVERE, "Could not insert relevances into DB. Name: "+tutorial.getTutorialName(), e);
                break;
            }
        }

        ArrayList<Stage> stages = tutorial.stages;
        iNumStages = stages.size();
        getLogger().log(Level.INFO, iNumStages+" stages in this tutorials");
        //Go through stages
        for (i = 0 ; i < iNumStages ; i++)
        {
            //Insert the new stage into the stages table
            Stage stage = stages.get(i);
            try
            {
                sql = "INSERT INTO `Stages` (`StageName`, `TutorialID`, `Order`) VALUES ('"+ stage.getName()+"', "+iTutorialID+", "+(i+1)+")";
                SQL.executeUpdate(sql);

                sql = "Select LAST_INSERT_ID()";
                resultSet = SQL.executeQuery(sql);
                resultSet.next();
                iStageID = resultSet.getInt(1);
            }
            catch (Exception e)
            {
                getLogger().log(Level.SEVERE, "Could not insert new stage into DB. Name: "+tutorial.getTutorialName(), e);
                continue;
            }

            ArrayList<Step> steps = stage.steps;
            iNumSteps = steps.size();
            getLogger().log(Level.INFO, iNumSteps+" steps in this stage");
            //Go through steps
            for (j = 0 ; j < iNumSteps ; j++)
            {
                //Insert the new step into the steps table
                Step step = steps.get(j);
                try
                {
                    Display.DisplayType instructionDisplayType = step.getInstructionDisplayType();

                    sql = "INSERT INTO `Steps` (`StepName`, `StageID`, `StepInStage`, `InstructionDisplay`) VALUES ('"+ step.getName()+"', "+iStageID+", "+(j+1)+",'" +instructionDisplayType +"')";
                    SQL.executeUpdate(sql);

                    sql = "Select LAST_INSERT_ID()";
                    resultSet = SQL.executeQuery(sql);
                    resultSet.next();
                    iStepID = resultSet.getInt(1);
                }
                catch (Exception e)
                {
                    getLogger().log(Level.SEVERE, "Could not insert new step into DB. Name: "+tutorial.getTutorialName(), e);
                    continue;
                }

                ArrayList<Group> groups = step.groups;
                iNumGroups = groups.size();
                getLogger().log(Level.INFO, iNumGroups+" groups in this step");
                //Go through groups
                for (k = 0 ; k < iNumGroups ; k++)
                {
                    //Insert the new group into the groups table
                    Group group = groups.get(k);

                    getLogger().log(Level.INFO, "Adding group "+k);
                    try
                    {
                        sql = "INSERT INTO `Groups` (`StepID`)" +
                                " VALUES (" +iStepID+")";
                        SQL.executeUpdate(sql);

                        sql = "Select LAST_INSERT_ID()";
                        resultSet = SQL.executeQuery(sql);
                        resultSet.next();
                        iGroupID = resultSet.getInt(1);
                    }
                    catch (Exception e)
                    {
                        getLogger().log(Level.SEVERE, "Could not insert new group into DB. Name: "+tutorial.getTutorialName(), e);
                        continue; // Doesn't attempt to then store the tasks
                    }

                    ArrayList<Task> tasks = group.tasks;
                    iNumTasks = tasks.size();
                    getLogger().log(Level.INFO, iNumTasks+" tasks in this group");
                    //Go through tasks
                    for (l = 0 ; l < iNumTasks ; l++)
                    {
                        //Insert the new group into the groups table
                        Task task = tasks.get(l);

                        //Compile the details string for the DB details field
                        String szDetails = "";

                        if (task.getType().equals(FundamentalTaskType.command))
                        {
                            szDetails = task.getDetails();
                        }

                        try
                        {
                            sql = "INSERT INTO `Tasks` (`GroupID`, `TaskType`, `Order`, `Details`)" +
                                    " VALUES (" +iGroupID+", '"+task.getType().name()+"', "+(l+1) +", '" +szDetails +"')";
                            SQL.executeUpdate(sql);
                        }
                        catch (Exception e)
                        {
                            getLogger().log(Level.SEVERE, "Could not insert new task into DB. Name: "+tutorial.getTutorialName(), e);
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Plugin disable logic
     */
    @Override
    public void onDisable()
    {
        // Plugin shutdown logic
        dbConnection.disconnect();
    }

    /**
     * @return A reference to the main DB connection object of the TeachingTutorials plugin
     */
    public DBConnection getDBConnection()
    {
        return dbConnection;
    }
    
    /**
     * @return The promotion service that is enabled.
     */
    public PromotionService getPromotionService() {
        return promotionService;
    }

    /**
     * Creates the necessary tables in the database from the file in config, if the tables don't already exist.
     * @return Whether the tables were created successfully
     */
    private boolean createTables()
    {
        String sql;
        Statement SQL = null;

        FileReader fileReader = null;
        BufferedReader bufferedReader = null;

        try
        {
            //Adds the Tutorials DB creation DDL if not already in the folder
            this.saveResource("TutorialsDDL.sql", false);

            //Creates a file object and sets it to the path of the Tutorials DB creation DDL
            String path = getDataFolder().getAbsolutePath() + "/TutorialsDDL.sql";
            File file = new File(path);

            fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);

            //Reads the file in
            sql = readAll(bufferedReader);

            //Replaces the database name with that of the one specified in the config
            sql = sql.replace("TeachingTutorials", dbConnection.Database);

            //Removes all line breaks
           // sql = sql.replaceAll("\n", "");

            //Splits each statement into separate strings
            String[] statements = sql.split(";");

            //Goes through each statement and executes it
            for (int i = 0 ; i < statements.length - 1 ; i++)
            {
                SQL = dbConnection.getConnection().createStatement();
                getLogger().log(Level.FINE, ChatColor.AQUA +"SQL - DB Creation, Statement "+i +" \n" + statements[i]);

                //Executes the update and returns how many rows were changed
                SQL.executeUpdate(statements[i]);
                getLogger().log(Level.INFO,ChatColor.AQUA +"Executed command\n");
            }
        }
        catch (IOException e)
        {
            getLogger().log(Level.SEVERE, "IO - IO Error Creating Tables", e);
        }
        catch (SQLException se)
        {
            getLogger().log(Level.SEVERE, "SQL - SQL Error Creating Tables", se);
        }
        catch (Exception e)
        {
            getLogger().log(Level.SEVERE, "SQL - Non-SQL Error Creating Tables", e);
        }
        finally
        {
            try
            {
                bufferedReader.close();
            }
            catch (Exception e)
            {
            }
            try
            {
                fileReader.close();
            }
            catch (Exception e)
            {
            }
            dbConnection.disconnect();
        }
        return true;
    }

    /**
     * Reads in all of the text from a buffered reader into one string
     * @param br The buffered reader to read the data in from
     * @return A string representation of the data loaded from the buffered reader
     */
    private String readAll(BufferedReader br)
    {
        StringBuilder sb = new StringBuilder("");
        //   int cp;
        try
        {
            String line;
            line = br.readLine();
            while (line != null)
            {
                if (!line.startsWith("--"))
                {
                    sb.append(line);
                    sb.append("\n");
                }
                line = br.readLine();
            }
        }
        catch (IOException e)
        {
            getLogger().log(Level.WARNING, "IO error reading tutorial file", e);
            sb = new StringBuilder();
        }
        return sb.toString();
    }
}

/**
 * A type of tutorial object
 */
enum TutorialObject
{
    Stage, Step, Group, Task
}