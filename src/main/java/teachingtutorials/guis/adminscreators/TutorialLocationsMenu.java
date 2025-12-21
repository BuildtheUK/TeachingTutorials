package teachingtutorials.guis.adminscreators;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.TeachingTutorials;
import net.bteuk.minecraft.gui.*;
import teachingtutorials.guis.TutorialGUIUtils;
import teachingtutorials.tutorialobjects.Location;
import teachingtutorials.tutorialobjects.Tutorial;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

/**
 * A menu to display a list of locations for a given tutorial
 */
public class TutorialLocationsMenu extends Gui
{
    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /** A reference to the user which this menu is for */
    private final User user;

    /** A reference to the tutorial which these locations are a part of */
    private Tutorial tutorial;

    /** A reference to the parent Creator Tutorials Menu */
    private TutorialManageMenu parentTutorialManageMenu;

    /** A list of all locations for the relevant tutorial */
    private Location[] locationsOfTutorial;

    /** A list of Location Manage Menu */
    private LocationManageMenu[] locationManageMenus;

    public TutorialLocationsMenu(TeachingTutorials plugin, User user, Tutorial tutorial, TutorialManageMenu parentTutorialManageMenu, Location[] locations)
    {
        super(plugin.getTutGuiManager(), CreatorTutorialsMenu.calculateNumRows(locations.length)*9, TutorialGUIUtils.inventoryTitle("Locations for Tutorial"));

        this.plugin = plugin;
        this.user = user;
        this.tutorial = tutorial;
        this.parentTutorialManageMenu = parentTutorialManageMenu;
        this.locationsOfTutorial = locations;

        //Create Location Manage Menus
        locationManageMenus = new LocationManageMenu[locationsOfTutorial.length];

        //Create the location manage menus
        int iNumLocs = locationsOfTutorial.length;
        if (iNumLocs > 45)
            iNumLocs = 45;
        for (int i = 0 ; i < iNumLocs ; i++)
        {
            locationManageMenus[i] = new LocationManageMenu(plugin, user, this, locations[i]);
        }

        addItems();
    }

    /**
     * Adds the icons and actions to the Menu
     */
    private void addItems()
    {
        //Indicates that this tutorial has no locations
        if (locationsOfTutorial.length == 0)
            setItem(5-1, Utils.createItem(Material.BARRIER, 1, TutorialGUIUtils.optionTitle("This tutorial has no locations")));

        //Adds the location icons
        for (int i = 0 ; i < locationsOfTutorial.length ; i++)
        {
            int finalI = i;
            setItem(i, Utils.createItem(Material.FILLED_MAP, 1,
                            TutorialGUIUtils.optionTitle(locationsOfTutorial[i].getLocationName()),
                            TutorialGUIUtils.optionLore("Click to manage location")),
                    new GuiAction() {
                        @Override
                        public void click(InventoryClickEvent event) {
                            user.mainGui = new LocationManageMenu(plugin, user, TutorialLocationsMenu.this, locationsOfTutorial[finalI]);
                            user.mainGui.open(user.player);
                        }
                    });
        }

        //Adds back button
        ItemStack back = Utils.createItem(Material.SPRUCE_DOOR, 1, TutorialGUIUtils.backButton("Back to tutorial menu"));
        setItem((CreatorTutorialsMenu.calculateNumRows(locationsOfTutorial.length) * 9) - 1, back, new GuiAction() {
            @Override
            public void click(InventoryClickEvent event) {
                delete();
                user.mainGui = parentTutorialManageMenu;
                user.mainGui.open(user.player);
            }
        });
    }

    /**
     * Refreshes the name on the location icons
     */
    public void refreshLocationIcons()
    {
        //Adds the location icons
        int iNumTutorials = locationsOfTutorial.length;
        if (iNumTutorials > 45)
            iNumTutorials = 45;
        for (int i = 0 ; i < iNumTutorials ; i++)
        {
            setItem(i, Utils.createItem(Material.FILLED_MAP, 1,
                    TutorialGUIUtils.optionTitle(locationsOfTutorial[i].getLocationName()),
                    TutorialGUIUtils.optionLore("Click to manage location")));
        }
    }
}
