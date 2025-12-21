package teachingtutorials.guis.adminscreators;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.TeachingTutorials;
import net.bteuk.minecraft.gui.*;
import teachingtutorials.guis.MainMenu;
import teachingtutorials.guis.TutorialGUIUtils;
import teachingtutorials.newtutorial.TutorialCreationSession;
import teachingtutorials.tutorialobjects.Tutorial;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

import java.util.UUID;

/**
 * A menu accessible to admins and tutorial creators in order to manage the tutorials on the system
 */
public class CreatorMenu extends Gui
{
    /** Stores the name of the inventory */
    private static final Component inventoryName = TutorialGUIUtils.inventoryTitle("Admin and Creator Menu");

    /** Notes the size of the inventory */
    private static final int iNumRows = 27;

    /** A reference to the TeachingTutorials plugin instance */
    private final TeachingTutorials plugin;

    /** A reference to the user for which this menu is for */
    private final User user;

    /**
     * @param plugin An instance of the plugin
     * @param user The user for which the menu is being created for
     */
    public CreatorMenu(TeachingTutorials plugin, User user)
    {
        super(plugin.getTutGuiManager(), iNumRows, inventoryName);
        this.plugin = plugin;
        this.user = user;

        //Adds the items to the gui
        addMenuOptions();
    }

    /**
     * Creates and adds the menu options to this gui
     */
    private void addMenuOptions()
    {
        //Creates the menu item icons that are common to both menu designs
        ItemStack setCompulsory = Utils.createItem(Material.IRON_DOOR, 1,
                TutorialGUIUtils.optionTitle("Set Compulsory Tutorial"),
                TutorialGUIUtils.optionLore("Admins only"));

        ItemStack myTutorials = Utils.createItem(Material.BOOKSHELF, 1,
                TutorialGUIUtils.optionTitle("My Tutorials"),
                TutorialGUIUtils.optionLore("- View the tutorials you have created"),
                TutorialGUIUtils.optionLore("- Add locations"),
                TutorialGUIUtils.optionLore("- Set a tutorial as in use or not in use"));

        ItemStack createTutorial = Utils.createItem(Material.WRITABLE_BOOK, 1,
                TutorialGUIUtils.optionTitle("Create a new Tutorial"),
                TutorialGUIUtils.optionLore("Create a new tutorial in game"));

        //A variable used to mark at which inventory position the 'My Tutorials' option should appear at
        int iSlotMyTutorials;

        //Adds a compulsory tutorial selection menu for admins
        if (user.player.hasPermission("TeachingTutorials.Admin"))
        {
            setItem(12 - 1, setCompulsory, new GuiAction() {
                @Override
                public void click(InventoryClickEvent e) {
                    delete();
                    user.mainGui = new CompulsoryTutorialMenu(plugin, user, Tutorial.fetchAll(true, false, null, plugin.getDBConnection(), plugin.getLogger()));
                    user.mainGui.open(user.player);
                }
            });

            //Sets the position of the MyTutorials option for admins
            iSlotMyTutorials = 14 - 1;
        }
        else
            //Sets the position of the MyTutorials option for creators
            iSlotMyTutorials = 12 - 1;

        //Adds the MyTutorials item
        setItem(iSlotMyTutorials, myTutorials, new GuiAction() {
            @Override
            public void click(InventoryClickEvent e) {
                UUID uuid;
                delete();
                if (user.player.hasPermission("TeachingTutorials.Admin")) {
                    uuid = null; // Allows for fetching of all Tutorials (should be used in a better way)
                } else {
                    uuid = user.player.getUniqueId();
                }
                user.mainGui = new CreatorTutorialsMenu(plugin, user, Tutorial.fetchAll(false, false, uuid, plugin.getDBConnection(), plugin.getLogger()));
                user.mainGui.open(user.player);
            }
        });

        //Adds the 'Create Tutorial' item
        setItem(16 - 1, createTutorial, new GuiAction() {
            @Override
            public void click(InventoryClickEvent e) {
                //Launches a new tutorial creation session
                TutorialCreationSession session = new TutorialCreationSession(plugin, user);
                session.startSession();

                delete();
            }
        });

        //Adds the back item
        setItem(27 - 1, Utils.createItem(Material.SPRUCE_DOOR, 1,
                        TutorialGUIUtils.backButton("Back to main menu")),
                new GuiAction() {
            @Override
            public void click(InventoryClickEvent e) {
                delete();
                user.mainGui = new MainMenu(plugin, user);
                user.mainGui.open(user.player);
            }
        });
    }
}