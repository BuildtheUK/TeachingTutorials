package teachingtutorials.guis.adminscreators;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import teachingtutorials.TeachingTutorials;
import net.bteuk.minecraft.gui.*;
import teachingtutorials.guis.TutorialGUIUtils;
import net.bteuk.minecraft.texteditorbooks.*;
import teachingtutorials.newlocation.NewLocation;
import teachingtutorials.tutorialobjects.Location;
import teachingtutorials.tutorialobjects.Tutorial;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.Mode;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

/**
 * Used to access options for managing a Tutorial.
 * Accessed by clicking a Tutorial on the manage tutorial page
 */
public class TutorialManageMenu extends Gui
{
    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /** A reference to the user which this menu is for */
    private final User user;

    /** A reference to the parent Creator Tutorials Menu */
    private final CreatorTutorialsMenu creatorTutorialsMenu;

    /** A reference to the Tutorial which this menu is for */
    private final Tutorial tutorial;

    private final TextEditorBookListener nameEditor;

    /**
     *
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @param user A reference to the user which this menu is for
     * @param tutorial A reference to the Tutorial which this menu is for
     * @param creatorTutorialsMenu A reference to the parent Creator Tutorials Menu
     */
    public TutorialManageMenu(TeachingTutorials plugin, User user, Tutorial tutorial, CreatorTutorialsMenu creatorTutorialsMenu)
    {
        super(plugin.getTutGuiManager(), 27, TutorialGUIUtils.inventoryTitle(tutorial.getTutorialName()));

        this.plugin = plugin;
        this.user = user;
        this.creatorTutorialsMenu = creatorTutorialsMenu;
        this.tutorial = tutorial;

        this.nameEditor = new TextEditorBookListener(plugin, user.player, this, "Edit Tutorial Name", new BookCloseAction() {
            @Override
            public boolean runBookClose(BookMeta oldBookMeta, BookMeta newBookMeta, TextEditorBookListener textEditorBookListener, String szNewContent) {
                //Unregister the listener
                textEditorBookListener.unregister();

                //Remove the book
                user.player.getInventory().getItemInMainHand().setAmount(0);

                if (szNewContent.length() > 45)
                    return false;
                else
                {
                    //Updates the name of the tutorial
                    tutorial.updateName(plugin.getDBConnection(), plugin.getLogger(), szNewContent);
                    return true;
                }
            }

            @Override
            public boolean runBookSign(BookMeta bookMeta, BookMeta bookMeta1, TextEditorBookListener textEditorBookListener, String s) {
                return runBookClose(bookMeta, bookMeta1, textEditorBookListener, s);
            }

            @Override
            public void runPostClose()
            {
                //Updates the title of the menu and opens the inventory with the new title
                TutorialManageMenu.super.editName(TutorialGUIUtils.inventoryTitle(tutorial.getTutorialName()), user.player);
                //Refreshes the parent menu with the new name
                creatorTutorialsMenu.refreshTutorialIcons();
            }
        }, tutorial.getTutorialName());

        //Add icons and actions to the menu
        addItems();
    }

    /**
     * Adds the icons and actions to the Menu
     */
    private void addItems()
    {
        //Toggle in use
        ItemStack inUseIcon;
        if (tutorial.isInUse())
            inUseIcon = Utils.createItem(Material.KNOWLEDGE_BOOK, 1, TutorialGUIUtils.optionTitle("In Use"),
                    TutorialGUIUtils.optionLore("Click click to remove from use"));
        else
            inUseIcon = Utils.createItem(Material.BOOK, 1, TutorialGUIUtils.optionTitle("Not In Use"),
                    TutorialGUIUtils.optionLore("Click to set in use"));

        super.setItem(10, inUseIcon, new GuiAction() {
            @Override
            public void click(InventoryClickEvent event) {
                //If it is currently not in use and there are no locations, block the toggle in use
                if (tutorial.isInUse() || Location.getAreThereInUseLocationsForTutorial(tutorial.getTutorialID(), plugin.getDBConnection(), plugin.getLogger()))
                    tutorial.toggleInUse(plugin);
                refresh();
            }
        });

        //Manage Locations
        ItemStack locationsIcon = Utils.createItem(Material.BOOKSHELF, 1, TutorialGUIUtils.optionTitle("Manage Locations"),
                TutorialGUIUtils.optionLore("Click to manage the locations of this tutorial"));
        super.setItem(12, locationsIcon, new GuiAction() {
            @Override
            public void click(InventoryClickEvent event) {
                //Create the menu
                user.mainGui = new TutorialLocationsMenu(plugin, user, tutorial, TutorialManageMenu.this,
                        //Fetch the locations
                        Location.getAllLocationForTutorial(tutorial.getTutorialID(), plugin.getDBConnection(), plugin.getLogger()));
                user.mainGui.open(user.player);
            }
        });


        //New Location
        ItemStack newLocation = Utils.createItem(Material.WRITABLE_BOOK, 1, TutorialGUIUtils.optionTitle("Create Location"),
                TutorialGUIUtils.optionLore("Create a new location for this tutorial"));
        super.setItem(14, newLocation, new GuiAction() {
            @Override
            public void click(InventoryClickEvent event) {
                //Only starts the new location process if the creator is idle
                if (user.getCurrentMode().equals(Mode.Idle))
                {
                    delete();
                    user.mainGui = null;

                    user.player.closeInventory();
                    user.player.sendMessage(Display.aquaText("Preparing Session..."));

                    //Creates a NewLocation object
                    NewLocation newLocation = new NewLocation(user, tutorial, plugin);

                    //Launches them into the new location adding process
                    newLocation.launchNewLocationAdding();
                }
                else
                    user.player.sendMessage(Display.errorText("Complete or pause your current tutorial or location creation first"));
            }
        });

        //Change name
        ItemStack changeName = Utils.createItem(Material.NAME_TAG, 1, TutorialGUIUtils.optionTitle("Change Tutorial Name"),
                TutorialGUIUtils.optionLore("Update the name of this tutorial"));
        super.setItem(16, changeName, new GuiAction() {
            @Override
            public void click(InventoryClickEvent event) {
                user.player.sendMessage(Display.aquaText("Use the name editor book to enter a new name. It must be no more than 45 characters"));
                nameEditor.startEdit("Tutorial Name Editor");
            }
        });

        //Back
        ItemStack backButton = Utils.createItem(Material.SPRUCE_DOOR, 1,
                TutorialGUIUtils.backButton("Back to creator tutorials menu"));

        super.setItem(26, backButton, new GuiAction() {
            @Override
            public void click(InventoryClickEvent event) {
                user.mainGui = creatorTutorialsMenu;
                user.mainGui.open(user.player);
            }
        });
    }

    /**
     * Clears items from the GUI, recreates the items and then opens the menu
     */
    public void refresh() {
        //Clears items from the gui
        this.clear();

        //Adds the items back
        this.addItems();

        //Opens the gui
        this.open(user.player);
    }
}
