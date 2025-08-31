package teachingtutorials.guis.adminscreators;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.TeachingTutorials;
import net.bteuk.minecraft.gui.*;
import teachingtutorials.guis.TutorialGUIUtils;
import teachingtutorials.tutorialobjects.Tutorial;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

/**
 * A menu which allows an admin to select the compulsory tutorial. The menu is of a reverse whack-a-mole design
 * - only one tutorial can be compulsory at a time and if another is selected the original will be replaced
 */
public class CompulsoryTutorialMenu extends Gui
{
    /** Stores the name of the inventory */
    private static final Component inventoryName = TutorialGUIUtils.inventoryTitle("Select Compulsory Tutorial");

    /** A reference to the TeachingTutorials plugin instance */
    private final TeachingTutorials plugin;

    /** A reference to the user for which this menu is for */
    private final User user;

    /** Stores a list of all in use tutorials */
    private Tutorial[] tutorials;

    /** The material used as the icon of the compulsory tutorial */
    private static final Material compulsoryBlock = Material.LECTERN;

    /** The material used as the icon of the tutorial currently not selected as the compulsory tutorial */
    private static final Material nonCompulsoryBlock = Material.BOOKSHELF;

    public CompulsoryTutorialMenu(TeachingTutorials plugin, User user, Tutorial[] tutorials)
    {
        //Sets up the menu with the icons already in place
        super(plugin.getTutGuiManager(), getGUI(tutorials));
        this.plugin = plugin;
        this.user = user;
        this.tutorials = tutorials;

        //Adds the actions to the slots of the menu
        setActions();
    }

    /**
     * Creates an inventory design for the current status of tutorials
     * @param tutorials A list of tutorials to include in the menu
     * @return An inventory filled with icons, informing the user of the current state of tutorials, including which
     * one is currently selected as compulsory
     */
    private static Inventory getGUI (Tutorial[] tutorials)
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

        //Adds an empty row and then a row for the back button
        iRows = iDiv+2;

        //Creates the inventories - one to add items to and one to return to the user
        inventory = Bukkit.createInventory(null, iRows * 9);
        inventory.clear();

        Inventory toReturn = Bukkit.createInventory(null, iRows * 9, inventoryName);

        //Adds an indicator icon if there are no tutorials to select as compulsory
        if (iTutorials == 0)
        {
            ItemStack noTutorials = Utils.createItem(Material.BARRIER, 1,
                    TutorialGUIUtils.optionTitle("There are no in-use tutorials on the system"));
            inventory.setItem(5-1, noTutorials); //Sets the item in the middle
        }

        //Adds a back button
        ItemStack back = Utils.createItem(Material.SPRUCE_DOOR, 1,
                TutorialGUIUtils.backButton("Back to creator menu"));
        inventory.setItem((iRows * 9)-1, back);

        //Inv slot 0 = the top left place
        //Add the tutorials to the gui
        for (i = 0 ; i < tutorials.length ; i++)
        {
            ItemStack tutorial;
            if (tutorials[i].isCompulsory())
            {
                tutorial = Utils.createItem(compulsoryBlock, 1,
                        TutorialGUIUtils.optionTitle(tutorials[i].getTutorialName()).decoration(TextDecoration.BOLD, true),
                        TutorialGUIUtils.optionLore(Bukkit.getOfflinePlayer(tutorials[i].getUUIDOfAuthor()).getName()));
            }
            else
            {
                tutorial = Utils.createItem(nonCompulsoryBlock, 1,
                        TutorialGUIUtils.optionTitle(tutorials[i].getTutorialName()),
                        TutorialGUIUtils.optionLore(Bukkit.getOfflinePlayer(tutorials[i].getUUIDOfAuthor()).getName()));
            }
            inventory.setItem(i, tutorial);
        }

        //Copies the inventory to the inventory to return
        toReturn.setContents(inventory.getContents());

        return toReturn;
    }

    /**
     * Sets the actions to the selection menu
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

        //Adds an empty row and then a row for the back button
        iRows = iDiv+2;

        //Adds back button
        setAction((iRows * 9) - 1, new GuiAction() {
            @Override
            public void click(InventoryClickEvent inventoryClickEvent) {
                delete();
                user.mainGui = new CreatorMenu(plugin, user);
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
                public void click(InventoryClickEvent inventoryClickEvent)
                {
                    //Toggles whether the tutorial is compulsory
                    tutorials[iSlot].toggleCompulsory(plugin);

                    //Refresh tutorial data
                    tutorials = Tutorial.fetchAll(true, false, null, plugin.getDBConnection(), plugin.getLogger());

                    //Refreshes the display
                    refresh();
                }
            });
        }
    }

    /**
     * Clears items from the GUI, re-adds the items, then reopens the menu
     */
    public void refresh() {
        //Clears the gui
        super.clear();

        //Sets the icons
        super.setItemsFromInventory(getGUI(tutorials));

        //Sets the actions
        this.setActions();

        //Open the menu
        this.open(user.player);
    }
}
