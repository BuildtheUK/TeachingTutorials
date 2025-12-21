package teachingtutorials.guis;

import net.bteuk.minecraft.gui.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.guis.adminscreators.CreatorMenu;
import teachingtutorials.tutorialobjects.LessonObject;
import teachingtutorials.tutorialobjects.Location;
import teachingtutorials.tutorialobjects.TutorialRecommendation;
import teachingtutorials.tutorialplaythrough.Lesson;
import teachingtutorials.tutorialobjects.Tutorial;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

/**
 * The main menu of the TeachingTutorials system from which all functions can be accessed
 */
public class MainMenu extends Gui
{
    /** Stores the name of the inventory */
    private static final Component inventoryName = TutorialGUIUtils.inventoryTitle("Tutorials Menu");

    /** Notes the size of the inventory */
    private static final int iNumRows = 27;

    /** Gets the config */
    private final FileConfiguration config;

    /** Gets the compulsory tutorial setting */
    private final boolean bCompulsoryTutorialEnabled;

    /** The Tutorial of the compulsory tutorial. Null if no compulsory tutorial is set */
    private Tutorial compulsoryTutorial;

    /** A reference to the TeachingTutorials plugin instance */
    private final TeachingTutorials plugin;

    /** A reference to the user for which this menu is for */
    private final User user;

    /** A list of the current lessons that a player has ongoing **/
    private LessonObject[] currentLessons;

    /** The next tutorial which a player would play if clicking continue learning */
    private Tutorial nextTutorial;

    /**
     *
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @param user A reference to the user for which this menu is for
     */
    public MainMenu(TeachingTutorials plugin, User user)
    {
        super(plugin.getTutGuiManager(), iNumRows, inventoryName);
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.bCompulsoryTutorialEnabled = config.getBoolean("Compulsory_Tutorial.Enabled");
        this.user = user;

        fetchInformation();

        //Adds the icons and actions to the menu
        addMenuOptions();
    }

    private void fetchInformation()
    {
        //Get compulsory tutorial
        Tutorial[] compulsoryTutorials = Tutorial.fetchAll(true, true, null, plugin.getDBConnection(), plugin.getLogger());
        if (compulsoryTutorials.length == 0)
            compulsoryTutorial = null;
        else
            compulsoryTutorial = compulsoryTutorials[0];

        //Get the current unfinished lessons of the player
        currentLessons = LessonObject.getUnfinishedLessonsOfPlayer(user.player.getUniqueId(), plugin.getDBConnection(), plugin.getLogger());

        //Get the next tutorial for this player
        nextTutorial = Lesson.decideTutorial(user, plugin.getDBConnection(), plugin.getLogger());
    }

    /**
     * Adds the menu options to the menu
     */
    private void addMenuOptions()
    {
        //-----------------------------------------------------------------------
        //--------------------- Begin Adding the Menu Items ---------------------
        //-----------------------------------------------------------------------

        //Checks the system has the compulsory tutorial feature enabled and the user hasn't completed the compulsory tutorial
        if (bCompulsoryTutorialEnabled && compulsoryTutorial != null && !user.bHasCompletedCompulsory)
        {
            //Check if they have started the compulsory
            LessonObject compulsoryLesson = null;
            for (LessonObject lesson : currentLessons)
            {
                if (lesson.getTutorialID() == compulsoryTutorial.getTutorialID())
                {
                    compulsoryLesson = lesson;
                    break;
                }
            }

            if (compulsoryLesson == null)
                compulsoryNeverStarted();
            else
                compulsoryNotFinished(compulsoryLesson);
        }
        else {
            //User has not completed compulsory tutorial or doesn't need to
            compulsoryFinished(); }


        //Admin and creator menu
        if (user.player.hasPermission("TeachingTutorials.Admin") || user.player.hasPermission("TeachingTutorials.Creator"))
        {
            //Admin and creator menu
            super.setItem(19 - 1, Utils.createItem(Material.LECTERN, 1,
                    TutorialGUIUtils.optionTitle("Creator Menu")), new GuiAction() {
                @Override
                public void click(InventoryClickEvent event) {
                    user.mainGui = new CreatorMenu(plugin, user);
                    user.mainGui.open(user.player);
                    delete();
                }
            });
        }
    }

    /**
     * Adds the menu items for if the compulsory tutorial has never been started
     */
    private void compulsoryNeverStarted()
    {
        ItemStack beginCompulsory = Utils.createItem(Material.BOOK, 1,
                TutorialGUIUtils.optionTitle("Begin the Starter Tutorial"),
                TutorialGUIUtils.optionLore("Gain the " +config.getString("Compulsory_Tutorial.RankNew") +" rank"));

        super.setItem(14 - 1, beginCompulsory, new GuiAction() {
            @Override
            public void click(InventoryClickEvent event) {
                startTutorial(compulsoryTutorial, null);
            }
        });

    }

    /**
     * Adds the menu items for if the compulsory tutorial has been started but never finished
     * @param compulsoryLesson The lesson object for the compulsory tutorial lesson they currently have ongoing
     */
    private void compulsoryNotFinished(LessonObject compulsoryLesson)
    {
        //Restart compulsory
        ItemStack restartCompulsory = Utils.createItem(Material.BOOK, 1,
                TutorialGUIUtils.optionTitle("Restart the Starter Tutorial"),
                TutorialGUIUtils.optionLore("Gain the " +config.getString("Compulsory_Tutorial.RankNew") +" rank"));

        super.setItem(12 - 1, restartCompulsory, new GuiAction() {
            @Override
            public void click(InventoryClickEvent event) {
                resumeLesson(compulsoryLesson, true);
            }
        });


        //Resume compulsory
        ItemStack resumeCompulsory = Utils.createItem(Material.WRITABLE_BOOK, 1,
                TutorialGUIUtils.optionTitle("Resume the Starter Tutorial"),
                TutorialGUIUtils.optionLore("Gain the " +config.getString("Compulsory_Tutorial.RankNew") +" rank"));

        super.setItem(16 - 1, resumeCompulsory, new GuiAction() {
            @Override
            public void click(InventoryClickEvent event) {
                resumeLesson(compulsoryLesson, false);
            }
        });
    }

    /**
     * Adds the menu items for if the compulsory tutorial has been completed, and the main tutorials system is unlocked
     */
    private void compulsoryFinished()
    {
        //Compulsory tutorial
        ItemStack compulsory = Utils.createItem(Material.JUNGLE_DOOR, 1,
                TutorialGUIUtils.optionTitle("Redo the Starter Tutorial"),
                TutorialGUIUtils.optionLore("Refresh your essential knowledge"));

        super.setItem(9, compulsory, new GuiAction() {
            @Override
            public void click(InventoryClickEvent event) {
                startTutorial(compulsoryTutorial, null);
            }
        });


        //---------- Library Option ----------
        ItemStack tutorialLibrary = Utils.createItem(Material.BOOKSHELF, 1,
                TutorialGUIUtils.optionTitle("Tutorial Library"),
                TutorialGUIUtils.optionLore("Browse all of our available tutorials"));

        super.setItem(11, tutorialLibrary, new GuiAction() {
            @Override
            public void click(InventoryClickEvent event) {
                user.mainGui = new LibraryMenu(plugin, user, Tutorial.getInUseTutorialsWithLocations(plugin.getDBConnection(), plugin.getLogger()),
                        LessonObject.getUnfinishedLessonsOfPlayer(user.player.getUniqueId(), plugin.getDBConnection(), plugin.getLogger()));
                user.mainGui.open(user.player);
                delete();
            }
        });


        //Current lessons
        ItemStack currentLessons = Utils.createItem(Material.WRITABLE_BOOK, 1,
                TutorialGUIUtils.optionTitle("Current Lessons"),
                TutorialGUIUtils.optionLore("View your unfinished lessons"));
        super.setItem(13, currentLessons, new GuiAction() {
            @Override
            public void click(InventoryClickEvent event) {
                user.mainGui = new LessonsMenu(plugin, user, MainMenu.this, MainMenu.this.currentLessons);
                user.mainGui.open(user.player);
            }
        });


        //Tutorial recommendations
        super.setItem(15, Utils.createItem(Material.CHEST, 1, TutorialGUIUtils.optionTitle("Recommended Tutorials")),
                new GuiAction() {
                    @Override
                    public void click(InventoryClickEvent event) {
                        user.mainGui = new RecommendedTutorialsMenu(plugin, MainMenu.this, user, TutorialRecommendation.fetchTutorialRecommendationsForPlayer(plugin.getDBConnection(), plugin.getLogger(), user.player.getUniqueId()));
                        user.mainGui.open(user.player);
                    }
                });


        //Continue learning/next tutorial

        if (nextTutorial != null) {
            ItemStack continueLearning = Utils.createItem(Material.END_CRYSTAL, 1,
                    TutorialGUIUtils.optionTitle("Start a new Tutorial:"),
                    TutorialGUIUtils.optionLore(nextTutorial.getTutorialName()));

                super.setItem(17 , continueLearning, new GuiAction() {
                @Override
                public void click(InventoryClickEvent event) {
                    startTutorial(nextTutorial, null);
                }
            }); }
    }


    /**
     * Handles the logic when a player wishes to resume a lesson
     * @param lessonToResume A reference to the Lesson that the player wishes to resume
     * @param bResetProgress Whether to reset the reset the progress to stage 1 step 1 when starting the lesson
     * @return
     */
    private boolean resumeLesson(LessonObject lessonToResume, boolean bResetProgress)
    {
        Lesson lessonPlaythrough = new Lesson(user, plugin, lessonToResume);
        return lessonPlaythrough.startLesson(bResetProgress);
    }

    /**
     * Handles the logic when a player wishes to start a tutorial
     * @param tutorialToStart A reference to the Tutorial that the player wishes to start
     * @return
     */
    public boolean startTutorial(Tutorial tutorialToStart, Location locationToStart)
    {
        //Check whether the player already has a current lesson for this tutorial
        boolean bLessonFound = false;
        for (LessonObject lesson : currentLessons)
        {
            if (tutorialToStart.getTutorialID() == lesson.getTutorialID())
            {
                //Open confirmation menu
                //If location matters then check that
                if (locationToStart != null)
                {
                    if (locationToStart.getLocationID() == lesson.getLocation().getLocationID())
                    {
                        bLessonFound = true;

                        user.mainGui = new LessonContinueConfirmer(plugin, user, this, lesson, "You have a lesson at this location already");
                        user.mainGui.open(user.player);

                        //Break, let the other menu take over
                        break;
                    }
                }
                else
                {
                    bLessonFound = true;
                    //If not then open confirmation menu
                    user.mainGui = new LessonContinueConfirmer(plugin, user, this, lesson, "You have a lesson for this tutorial already");
                    user.mainGui.open(user.player);

                    //Break, let the other menu take over
                    break;
                }
            }
        }

        //If player doesn't have current lesson for this tutorial then create a new one
        if (!bLessonFound)
        {
            Lesson lesson;
            if (locationToStart == null)
            {
                lesson = new Lesson(user, plugin, tutorialToStart);
            }
            else
            {
                lesson = new Lesson(user, plugin, locationToStart);
            }
            return lesson.startLesson(true);
        }
        else
        {
            return true;
        }
    }

    /**
     * Performs externally initiated events
     * @param event The type of event to perform
     * @param user A reference to the person performing this event
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @param iData Data related to the event
     * @return Whether the event performed successfully
     */
    public static boolean performEvent(EventType event, User user, TeachingTutorials plugin, int iData)
    {
        LessonObject lessonToContinue;

        switch (event) {
            case RESTART_LESSON:
                lessonToContinue = LessonObject.getLessonByLessonID(iData, plugin.getDBConnection(), plugin.getLogger());
                if (lessonToContinue == null)
                {
                    user.player.sendMessage(Display.errorText("Could not find the lesson you intend to continue"));
                    return false;
                }
                else if (lessonToContinue.isFinished())
                {
                    user.player.sendMessage(Display.errorText("The lesson you intend to continue is already finished"));
                    return false;
                }
                else
                {
                    Lesson lesson = new Lesson(user, plugin, lessonToContinue);
                    return lesson.startLesson(true);
                }

            case CONTINUE_LESSON:
                lessonToContinue = LessonObject.getLessonByLessonID(iData, plugin.getDBConnection(), plugin.getLogger());
                if (lessonToContinue == null)
                {
                    user.player.sendMessage(Display.errorText("Could not find the lesson you intend to continue"));
                    return false;
                }
                else if (lessonToContinue.isFinished())
                {
                    user.player.sendMessage(Display.errorText("The lesson you intend to continue is already finished"));
                    return false;
                }
                else
                {
                    Lesson lesson = new Lesson(user, plugin, lessonToContinue);
                    return lesson.startLesson(false);
                }

            case ADMIN_MENU:
                if (user.player.hasPermission("TeachingTutorials.Admin") || user.player.hasPermission("TeachingTutorials.Creator"))
                {
                    user.mainGui = new CreatorMenu(plugin, user);
                    user.mainGui.open(user.player);
                    return true;
                }
                else
                    return false;

            case START_TUTORIAL:
                Tutorial tutorialToStart = Tutorial.fetchByTutorialID(iData, plugin.getDBConnection(), plugin.getLogger());
                if (tutorialToStart == null)
                {
                    user.player.sendMessage(Display.errorText("Could not find the tutorial you intend to start"));
                    return false;
                }
                else if (!tutorialToStart.isInUse())
                {
                    user.player.sendMessage(Display.errorText("The tutorial you intend to start is not in use"));
                    return false;
                }
                else
                {
                    Lesson lesson = new Lesson(user, plugin, tutorialToStart);
                    return lesson.startLesson(true);
                }

            case START_LOCATION:
                Location locationToStart = Location.getLocationByLocationID(plugin.getDBConnection(), plugin.getLogger(), iData);
                if (locationToStart == null)
                {
                    user.player.sendMessage(Display.errorText("Could not find the location you intend to start"));
                    return false;
                }
                else if (!locationToStart.isInUse())
                {
                    user.player.sendMessage(Display.errorText("The location you intend to start is not in use"));
                    return false;
                }

                Tutorial tutorialOfLocationToStart = Tutorial.fetchByTutorialID(locationToStart.getTutorialID(), plugin.getDBConnection(), plugin.getLogger());
                if (tutorialOfLocationToStart == null)
                {
                    user.player.sendMessage(Display.errorText("Could not find the tutorial you intend to start"));
                    return false;
                }
                else if (!tutorialOfLocationToStart.isInUse())
                {
                    user.player.sendMessage(Display.errorText("The tutorial you intend to start is not in use"));
                    return false;
                }
                else
                {
                    Lesson lesson = new Lesson(user, plugin, locationToStart);
                    return lesson.startLesson(true);
                }

            case null, default:
                return false;

        }
    }

    /**
     * Clears items from the GUI, recreates the items and then opens the menu
     */
    public void refresh()
    {
        this.clear();
        fetchInformation();
        this.addMenuOptions();

        this.open(user.player);
    }
}
