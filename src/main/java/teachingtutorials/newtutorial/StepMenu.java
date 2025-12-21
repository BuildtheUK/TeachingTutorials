package teachingtutorials.newtutorial;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import net.bteuk.minecraft.gui.*;
import teachingtutorials.guis.TutorialGUIUtils;
import net.bteuk.minecraft.texteditorbooks.*;
import teachingtutorials.tutorialobjects.Group;
import teachingtutorials.tutorialobjects.Step;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

import java.util.ArrayList;

/**
 * A menu to edit a step of a tutorial
 */
public class StepMenu extends Gui
{
    /** The parent stage menu */
    final StageMenu stageMenu;

    /** A reference to the creator */
    final User creator;

    /** A reference to the step which this Step Menu is editing */
    private final Step step;

    /** A reference to the list of steps which this step is a part of */
    private final ArrayList<Step> steps;

    private final TextEditorBookListener nameEditor;

    /** The number of pages in this menu */
    private int iPages;

    /** The current page that the player is on */
    private int iPage;

    /** Information book */
    private final ItemStack information;

    public StepMenu(StageMenu stageMenu, User creator, Step step, ArrayList<Step> steps)
    {
        super(stageMenu.getManager(), 54, TutorialGUIUtils.inventoryTitle(step.getName()));
        this.stageMenu = stageMenu;
        this.creator = creator;
        this.step = step;
        this.steps = steps;

        TutorialCreationSession tutorialCreationSession = stageMenu.tutorialMenu.tutorialCreationSession;

        nameEditor = new TextEditorBookListener(tutorialCreationSession.plugin, creator.player, this, "Step Name",
                new BookCloseAction() {
                    @Override
                    public boolean runBookClose(BookMeta oldBookMeta, BookMeta newBookMeta, TextEditorBookListener textEditorBookListener, String szNewContent) {
                        //Unregister the book
                        textEditorBookListener.unregister();

                        //Always remove the book
                        tutorialCreationSession.creator.player.getInventory().getItemInMainHand().setAmount(0);

                        //Check that it is the correct length etc
                        if (szNewContent.length() > 45)
                        {
                            tutorialCreationSession.creator.player.sendMessage(Display.errorText("The name must not be more than 45 characters"));
                            return false;
                        }
                        else
                        {
                            step.setName(szNewContent);
                            return true;
                        }
                    }

                    @Override
                    public boolean runBookSign(BookMeta bookMeta, BookMeta bookMeta1, TextEditorBookListener textEditorBookListener, String s) {
                        return runBookClose(bookMeta, bookMeta1, textEditorBookListener, s);
                    }

                    @Override
                    public void runPostClose() {
                        //Edit the menu title with the new name and reopen
                        StepMenu.this.editName(TutorialGUIUtils.inventoryTitle(step.getName()), tutorialCreationSession.creator.player);
                    }
                }, step.getName());

        this.iPages = (step.groups.size()/36)+1;
        this.iPage = 1;

        information = Utils.createItem(Material.WRITTEN_BOOK, 1, TutorialGUIUtils.optionTitle("Information"));
        BookMeta bookMeta = (BookMeta) information.getItemMeta();
        bookMeta.addPages(Component.text("" +
                        "A group is a series of linked tasks which much be performed in order." +
                        "\n" +
                        "An example use would be where you need a player to perform a series of actions in order to complete something, for example making a selection and running a command, or tplling a corner and placing a block."),
                Component.text("You can of course just have 1 task per group." +
                        "\n\n" +
                        "Groups themselves can be started in any order. This is useful for example when you want a player to tpll the corners of a house but it doesn't matter what order they do it in."),
                Component.text("If you don't want this behaviour you can just have 1 group per step, and thereby control the order of the tutorial."));
        information.setItemMeta(bookMeta);

        addItems();
    }

    private void addItems()
    {
        //Delete stage
        setItem(2, Utils.createItem(Material.BARRIER, 1, TutorialGUIUtils.optionTitle("Delete Step"),
                        TutorialGUIUtils.optionLore("Delete step and all child objects")),
                new GuiAction() {
                    @Override
                    public void click(InventoryClickEvent event) {
                        //Create new cancel confirmation menu
                        DeleteConfirmation deleteConfirmation = new DeleteConfirmation(StepMenu.this, () -> {
                            //Removes the step
                            steps.remove(step);

                            //Adjust the orders
                            for (int i = 0 ; i < steps.size() ; i++)
                            {
                                steps.get(i).setStepInStage(i+1);
                            }

                            //Refreshes the stage menu
                            stageMenu.refresh();

                            //Opens the stage menu
                            creator.mainGui = stageMenu;
                            stageMenu.open(creator.player);
                        }, TutorialGUIUtils.optionLore("Delete step"), TutorialGUIUtils.optionLore("Back to step menu"));

                        //Open it
                        deleteConfirmation.open(creator.player);
                    }
                });


        //Set name
        setItem(13, Utils.createItem(Material.NAME_TAG, 1, TutorialGUIUtils.optionTitle("Set Step Name")),
                new GuiAction() {
                    @Override
                    public void click(InventoryClickEvent event) {
                        creator.player.sendMessage(Display.aquaText("Use the name editor book to enter a new name. It must be no more than 45 characters"));
                        nameEditor.startEdit("Step Name Editor");
                    }
                });

        //Back to steps
        setItem(6, Utils.createItem(Material.SPRUCE_DOOR, 1, TutorialGUIUtils.optionTitle("Back to Steps"), TutorialGUIUtils.optionLore("Back to the list of steps")),
                new GuiAction() {
                    @Override
                    public void click(InventoryClickEvent event) {
                        //Delete this menu and reopen the parent menu
                        delete();
                        stageMenu.refresh();
                        creator.mainGui = stageMenu;
                        creator.mainGui.open(creator.player);
                    }
                });

        //Group information
        setItem(4, Utils.createItem(Material.KNOWLEDGE_BOOK, 1, TutorialGUIUtils.optionTitle("Information"),
                        TutorialGUIUtils.optionLore("Information about groups")),
                new GuiAction() {
                    @Override
                    public void click(InventoryClickEvent event) {
                        Bukkit.getScheduler().runTask(stageMenu.tutorialMenu.tutorialCreationSession.plugin, () -> creator.player.openBook(information));
                    }
                });

        //Page back
        if (iPage > 1)
        {
            ItemStack pageBack = Utils.createCustomSkullWithFallback("4eff72715e6032e90f50a38f4892529493c9f555b9af0d5e77a6fa5cddff3cd2",
                    Material.ACACIA_BOAT, 1,
                    TutorialGUIUtils.optionTitle("Page back"));
            super.setItem(0, pageBack, new GuiAction() {
                @Override
                public void click(InventoryClickEvent event) {

                    StepMenu.this.iPage--;
                    refresh();
                    creator.mainGui = StepMenu.this;
                    creator.mainGui.open(creator.player);
                }
            });
        }

        //Page forwards
        if (iPage < iPages)
        {
            ItemStack pageBack = Utils.createCustomSkullWithFallback("a7ba2aa14ae5b0b65573dc4971d3524e92a61dd779e4412e4642adabc2e56c44",
                    Material.ACACIA_BOAT, 1,
                    TutorialGUIUtils.optionTitle("Page forwards"));
            super.setItem(8, pageBack, new GuiAction() {
                @Override
                public void click(InventoryClickEvent event) {
                    StepMenu.this.iPage++;
                    refresh();
                    creator.mainGui = StepMenu.this;
                    creator.mainGui.open(creator.player);
                }
            });
        }

        //Adds the groups
        int iStart = (iPage-1)*36;
        int iMax = Math.min(iStart+36, step.groups.size());
        int i;

        for (i = iStart ; i < iMax ; i++)
        {
            int finalI = i;

            Material material;
            if (step.groups.get(i).isComplete())
                material = Material.WRITTEN_BOOK;
            else
                material = Material.BOOK;

            ItemStack group = Utils.createItem(material, 1,
                    TutorialGUIUtils.optionTitle("Group"), TutorialGUIUtils.optionLore(step.groups.get(finalI).tasks.size() +" tasks"));

            super.setItem(i-iStart+18, group, new GuiAction() {
                @Override
                public void click(InventoryClickEvent event) {
                    creator.mainGui = new GroupMenu(StepMenu.this, creator, step.groups.get(finalI), step.groups);
                    creator.mainGui.open(creator.player);
                }
            });
        }

        int iOrder = i;

        //Add the + button - will add the option then refresh. It won't open it.
        if (iPage == iPages) //If on last page
        {
            int iLocationOfAdd = iOrder-iStart+18;

            super.setItem(iLocationOfAdd,
                    Utils.createItem(Material.WRITABLE_BOOK, 1,
                            TutorialGUIUtils.optionTitle("Add Group"),
                            TutorialGUIUtils.optionLore("Click to add another group")),
                    new GuiAction() {
                        @Override
                        public void click(InventoryClickEvent event) {
                            //Adds a new option
                            step.groups.add(new Group());
                            refresh();
                        }
                    });
        }
    }

    /**
     * Refresh the gui.
     * This usually involves clearing the content and recreating it.
     */
    public void refresh() {
        this.clear();

        //Recalculate number of pages required
        this.iPages = (step.groups.size()/36)+1;

        //Knock the player back a page if they're now too far forwards
        if (iPage > iPages)
            iPage = iPages;

        //Add the items
        this.addItems();
    }
}
