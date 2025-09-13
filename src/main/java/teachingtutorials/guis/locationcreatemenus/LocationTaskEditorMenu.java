package teachingtutorials.guis.locationcreatemenus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.meta.BookMeta;
import teachingtutorials.TeachingTutorials;
import net.bteuk.minecraft.gui.*;
import teachingtutorials.guis.TutorialGUIUtils;
import net.bteuk.minecraft.texteditorbooks.*;
import teachingtutorials.tutorialobjects.LocationTask;
import teachingtutorials.tutorialplaythrough.PlaythroughTask;
import teachingtutorials.utils.Category;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

import java.util.logging.Level;


/**
 * A base class for LocationTask editor menus. Defines the standard features of LocationTaskEditor menus, including
 * a link back to the Step Editor Menu, a place to define the difficulties of the task, and a confirm and continue
 * button to save the data and move on to the next task.
 */
public class LocationTaskEditorMenu extends Gui
{
    /**
     * A reference to the parent menu / menu of the associated step of this location task
     */
    private final StepEditorMenu parentStepMenu;

    /**
     * A reference to the instance of the TeachingTutorials plugin
     */
    protected final TeachingTutorials plugin;

    /**
     * A reference to the user who owns this menu
     */
    protected final User user;

    /**
     * A reference to the location task that this menu is designed to edit the information of
     */
    protected final LocationTask locationTask;

    /**
     * A reference to the task playthrough object that this menu is associated with
     */
    private final PlaythroughTask playthroughTask;

    /** A list of book editing listeners used for specifying the difficulties in each of the categories
     * <p>Order is that in which the categories are specified in the Category Enum</p>*/
    private final TextEditorBookListener[] difficultyListeners;

    /**
     * Defines whether the confirm and continue button should be enabled
     */
    private boolean bEnableConfirmAndContinueButton;

    public LocationTaskEditorMenu(TeachingTutorials plugin, User user, StepEditorMenu parentStepMenu, Component inventoryTitle, LocationTask locationTask, PlaythroughTask playthroughTask)
    {
        //Creates an empty gui
        super(plugin.getTutGuiManager(), 5*9, inventoryTitle);
        this.plugin = plugin;
        this.user = user;
        this.parentStepMenu = parentStepMenu;
        this.locationTask = locationTask;
        this.playthroughTask = playthroughTask;

        //Sets up the difficulty listeners, loading each book with the current value of the difficulty
        difficultyListeners = new TextEditorBookListener[5];
        for (int i = 0 ; i < 5 ; i ++)
        {
            int finalI = i;
            difficultyListeners[i] = new TextEditorBookListener(plugin, user.player, this, "Difficulty in " + Category.values()[i], new BookCloseAction()
            {
                /**
                 * Performs the action on book close. You will likely want to unregister the book close listener within this.
                 *
                 * @param oldBookMeta            The previous metadata of the book just closed.
                 * @param newBookMeta            The new metadata of the book just closed.
                 * @param textEditorBookListener A reference to the book listener itself which calls this. Enables unregistering to be called
                 * @param szNewContent           The combined content of all pages in the new book. This is always provided for convenience
                 * @return Whether to accept the input and save the text
                 */
                @Override
                public boolean runBookClose(BookMeta oldBookMeta, BookMeta newBookMeta, TextEditorBookListener textEditorBookListener, String szNewContent) {
                    boolean bVerificationPassed;

                    try
                    {
                        float fDifficulty = Float.parseFloat(szNewContent);

                        //Format will be of a float if it reaches this point

                        //Checks whether it is out of range
                        if (fDifficulty < 0 || fDifficulty > 1)
                        {
                            user.player.sendMessage(Display.errorText("You must enter something in the range 0-1"));
                            bVerificationPassed = false;
                        }
                        else
                        {
                            //Sets the difficulty in the ith category
                            locationTask.setDifficulty(Category.values()[finalI], fDifficulty);
                            bVerificationPassed = true;
                        }

                        //Remove the book
                        user.player.getInventory().getItemInMainHand().setAmount(0);
                        textEditorBookListener.unregister();

                        //Reopen the menu
                        refresh();
                        open(user.player);
                    }
                    catch (NumberFormatException e)
                    {
                        user.player.sendMessage(Display.errorText("You must enter a floating point number in the range 0-1"));
                        bVerificationPassed = false;
                    }
                    return bVerificationPassed;
                }

                @Override
                public boolean runBookSign(BookMeta bookMeta, BookMeta bookMeta1, TextEditorBookListener textEditorBookListener, String s) {
                    return runBookClose(bookMeta, bookMeta1, textEditorBookListener, s);
                }

                /**
                 * Performs the actions post saving and closing
                 */
                @Override
                public void runPostClose()
                {
                    refresh();
                    open(user.player);
                }

            }, locationTask.getDifficulty(Category.values()[i])+"");
        }

        //Adds the default options to the menu
        addBaseOptions();
    }

    /**
     * Adds the basic options to the inventory: The link back to the step editor menu and the difficulty editors
     */
    private void addBaseOptions()
    {
        //Adds the link back to the step editor menu
        super.setItem((4 * 9) - 1 + 1,
                Utils.createItem(Material.SPRUCE_DOOR, 1,
                TutorialGUIUtils.optionTitle("To step editor menu")),
                new GuiAction() {
                    @Override
                    public void click(InventoryClickEvent event) {
                        user.mainGui = parentStepMenu;
                        user.mainGui.open(user.player);
            }
        });

        //Stores which difficulty categories are open for setting
        boolean[] bEnabledDifficulties = new boolean[5];
        for (int i = 0 ; i < 5 ; i++)
        {
            bEnabledDifficulties[i] = false;
        }

        //Decides which categories to open up
        switch (locationTask.getType())
        {
            case tpll:
                //Tpll, nothing else
                bEnabledDifficulties[0] = true;
                break;
            case chat:
            case place:
            case command:
                //All enabled
                bEnabledDifficulties[0] = true;
                bEnabledDifficulties[1] = true;
                bEnabledDifficulties[2] = true;
                bEnabledDifficulties[3] = true;
                bEnabledDifficulties[4] = true;
                break;
            case selection:
                //Tpll, WorldEdit, terraforming
                bEnabledDifficulties[0] = true;
                bEnabledDifficulties[1] = true;
                bEnabledDifficulties[4] = true;
                break;
        }

        //Add in the difficulty options in a row.
        for (int i = 0 ; i < 5 ; i++)
        {
            int finalI = i;
            if (bEnabledDifficulties[i])
            {
                super.setItem((4 * 9) - 1 + 3 + i,
                        Utils.createItem(Material.WRITABLE_BOOK, 1,
                                TutorialGUIUtils.optionTitle("Define "+Category.values()[i] +" difficulty"), TutorialGUIUtils.optionLore(locationTask.getDifficulty(Category.values()[finalI]) +"")),
                        new GuiAction() {
                            @Override
                            public void click(InventoryClickEvent event) {
                                //Gives the player the book item and registers the book close listener
                                difficultyListeners[finalI].startEdit("Define "+Category.values()[finalI] +" difficulty");
                            }
                        });
            }
            else
            {
                super.setItem((4 * 9) - 1 + 3 + i,
                        Utils.createItem(Material.BARRIER, 1,
                                Display.colouredText("Define "+Category.values()[i] +" difficulty", NamedTextColor.GRAY)));
            }
        }

        //Adds the confirm and continue button if applicable - clicking on this will save the answers and move on to the next task
        if (bEnableConfirmAndContinueButton)
        {
            super.setItem((4 * 9) - 1 + 9,
                    Utils.createItem(Material.LIME_STAINED_GLASS_PANE, 1,
                            TutorialGUIUtils.optionTitle("Confirm Details"), TutorialGUIUtils.optionLore("Continue to next task")),
                    new GuiAction() {
                        @Override
                        public void click(InventoryClickEvent event) {
                            //Action on confirm

                            //Attempt to store the new data into the DB
                            if (locationTask.storeNewData(plugin))
                            {
                                plugin.getLogger().log(Level.INFO, "LocationTask stored in database");
                                switch (locationTask.getType())
                                {
                                    case selection -> user.player.sendMessage(Display.aquaText("Selection task answer successfully stored in DB"));
                                    case command -> user.player.sendMessage(Display.aquaText("Command task answer successfully stored in DB"));
                                    case place -> user.player.sendMessage(Display.aquaText("Place task answer successfully stored in DB"));
                                    case tpll -> user.player.sendMessage(Display.aquaText("Tpll task answer successfully stored in DB"));
                                    case chat -> user.player.sendMessage(Display.aquaText("Chat task answer successfully stored in DB"));
                                }
                            }
                            else
                            {
                                plugin.getLogger().log(Level.SEVERE, "LocationTask not stored in database");
                                user.player.sendMessage(Display.errorText("Task could not be stored in DB. Please report this to a server admin"));
                            }

                            //Refresh will block the task editing if all tasks are already added
                            LocationTaskEditorMenu.this.parentStepMenu.refresh();

                            //Set the main menu to the location task menu
                            LocationTaskEditorMenu.this.user.mainGui = LocationTaskEditorMenu.this.parentStepMenu;

                            //Closes the menu
                            LocationTaskEditorMenu.this.user.player.closeInventory();

                            //Calls for the play-through to move on to the next task
                            playthroughTask.newLocationSpotHit();
                        }
                    });
        }
    }

    protected void addAdditionalOptions() {
    }

    /**
     * Clears items from the GUI, recreates the items. If you call this you will need to open the menu yourself
     * with this.open(User) after calling this and adding any additional items.
     */
    private void refresh()
    {
        this.clear();
        this.addBaseOptions();
        addAdditionalOptions();
    }

    /**
     * Refreshes the menu and reopens it
     * @param player The player to open the menu for
     */
    @Override
    public void open(Player player)
    {
        refresh();
        super.open(player);
    }

    /**
     * Notifies the menu that all of the necessary information in the task is fully set and we are ready to move on to the next task
     */
    public void taskFullySet()
    {
        bEnableConfirmAndContinueButton = true;

        refresh();
    }

    /**
     * Notifies the menu that all of the necessary information in the task is no longer set and we are not ready to move on to the next task.
     * Will then call for a full refresh of the menu
     */
    public void taskNoLongerReadyToMoveOn()
    {
        bEnableConfirmAndContinueButton = false;

        //Refreshes the menu
        refresh();
    }
}
