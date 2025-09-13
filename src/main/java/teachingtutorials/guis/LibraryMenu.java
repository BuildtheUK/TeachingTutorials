package teachingtutorials.guis;

import net.bteuk.minecraft.gui.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialobjects.LessonObject;
import teachingtutorials.tutorialobjects.Location;
import teachingtutorials.tutorialplaythrough.Lesson;
import teachingtutorials.tutorialobjects.Tutorial;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

/**
 * A menu which displays all of the tutorials available for a player to play through and allows them to select one to do
 */
public class LibraryMenu extends Gui
{
    /** Stores the name of the inventory */
    private static final Component inventoryName = TutorialGUIUtils.inventoryTitle("Library");

    /** A reference to the TeachingTutorials plugin instance */
    private final TeachingTutorials plugin;

    /** A reference to the user for which this menu is for */
    private final User user;

    /** Stores a list of all in use tutorials */
    private Tutorial[] tutorials;

    /** The list of lessons that this player has ongoing */
    private final LessonObject[] lessons;

    /**
     *
     * @param plugin An instance of the plugin
     * @param user The user for which the menu is being created for
     * @param tutorials A list of all in use tutorials which have locations
     */
    public LibraryMenu(TeachingTutorials plugin, User user, Tutorial[] tutorials, LessonObject[] userCurrentLesson)
    {
        //Sets up the menu with the icons already in place
        super(plugin.getTutGuiManager(), getGUI(tutorials));
        this.plugin = plugin;
        this.user = user;
        this.tutorials = tutorials;
        this.lessons = userCurrentLesson;

        //Adds the click-actions to the menu
        setActions();
    }

    /**
     * Creates an inventory with icons representing a library of available tutorials
     * @param tutorials A list of all in-use tutorials
     * @return An inventory of icons
     */
    public static Inventory getGUI (Tutorial[] tutorials)
    {
        //Declare variables
        int i;
        int iTutorials;
        int iDiv;
        int iMod;
        int iRows;

        Inventory inventory;

        //Works out how many rows in the inventory are needed
        iTutorials = tutorials.length;
        iDiv = iTutorials/9;
        iMod = iTutorials%9;

        if (iMod != 0 || iDiv == 0)
        {
            iDiv = iDiv + 1;
        }

        //Enables an empty row and then a row for the back button
        iRows = iDiv+2;

        //Create inventory
        inventory = Bukkit.createInventory(null, iRows * 9);
        inventory.clear();

        Inventory toReturn = Bukkit.createInventory(null, iRows * 9, inventoryName);

        //Indicates that there are no tutorials in the system
        if (iTutorials == 0)
        {
            ItemStack noTutorials = Utils.createItem(Material.BARRIER, 1,
                    TutorialGUIUtils.optionTitle("There are no tutorials available to play currently"),
                    TutorialGUIUtils.optionLore("Ask a server admin to get some created"));
            inventory.setItem(5-1, noTutorials);
        }

        //Adds the tutorials to the menu options
        //Inv slot 0 = the first one
        ItemStack tutorial;
        for (i = 0 ; i < iTutorials ; i++)
        {
            tutorial = Utils.createItem(Material.KNOWLEDGE_BOOK, 1,
                    TutorialGUIUtils.optionTitle(tutorials[i].getTutorialName()).decoration(TextDecoration.BOLD, true),
                    TutorialGUIUtils.optionLore("Tutor - " +Bukkit.getOfflinePlayer(tutorials[i].getUUIDOfAuthor()).getName()));
            inventory.setItem(i, tutorial);
        }

        //Adds a back button
        ItemStack back = Utils.createItem(Material.SPRUCE_DOOR, 1, TutorialGUIUtils.backButton("Back to main menu"));
        inventory.setItem((iRows * 9) - 1, back);

        toReturn.setContents(inventory.getContents());
        return toReturn;
    }

    /**
     * Adds the click-actions to the menu slots of this library menu
     */
    private void setActions()
    {
        //Declare variables
        int i;
        int iTutorials;
        int iDiv;
        int iMod;
        int iRows;

        //Works out how many rows in the inventory are needed
        iTutorials = tutorials.length;
        iDiv = iTutorials/9;
        iMod = iTutorials%9;

        if (iMod != 0 || iDiv == 0)
        {
            iDiv = iDiv + 1;
        }

        //Enables an empty row and then a row for the back button
        iRows = iDiv+2;

        //Adds back button
        setAction((iRows * 9) - 1, new GuiAction() {
            @Override
            public void click(InventoryClickEvent e) {
                delete();
                user.mainGui = new MainMenu(plugin, user);
                user.mainGui.open(user.player);
            }
        });


        //Inv slot 0 = the first one
        //Adds the actions of each slot
        for (i = 0 ; i < tutorials.length ; i++)
        {
            int iSlot = i;
            setAction(iSlot, new GuiAction() {
                @Override
                public void click(InventoryClickEvent e) {
                    startTutorial(plugin, lessons, user, LibraryMenu.this, tutorials[iSlot], null);
                }
            });
        }
    }

    /**
     * Handles the logic when a player wishes to start a specific tutorial
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @param lessons A list of unfinished lessons for the given player
     * @param user A reference to the user who wishes to start a specific tutorial
     * @param parentGui A reference to the parent gui which to return back
     * @param tutorialToStart A reference to the Tutorial that the player wishes to start
     * @param locationToStart A reference to the Location that a player wishes to start, if specified
     * @return
     */
    public static boolean startTutorial(TeachingTutorials plugin, LessonObject[] lessons, User user, Gui parentGui, Tutorial tutorialToStart, Location locationToStart)
    {
        //Check whether the player already has a current lesson for this tutorial
        boolean bLessonFound = false;
        for (LessonObject lesson : lessons)
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

                        user.mainGui = new LessonContinueConfirmer(plugin, user, parentGui, lesson, "You have a lesson at this location already");
                        user.mainGui.open(user.player);

                        //Break, let the other menu take over
                        break;
                    }
                }
                else
                {
                    bLessonFound = true;
                    //If not then open confirmation menu
                    user.mainGui = new LessonContinueConfirmer(plugin, user, parentGui, lesson, "You have a lesson for this tutorial already");
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
     * Clears items from the GUI, recreates the items and then opens the menu
     */
    public void refresh() {
        //Clears items from the gui
        this.clear();

        //Adds icons to the gui
        this.setItemsFromInventory(getGUI(tutorials));

        //Adds click actions to the gui
        this.setActions();

        //Opens the gui
        this.open(user.player);
    }
}
