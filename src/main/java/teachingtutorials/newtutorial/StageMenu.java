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
import teachingtutorials.tutorialobjects.Stage;
import teachingtutorials.tutorialobjects.Step;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

import java.util.ArrayList;

/**
 * A menu to edit a stage of a tutorial
 */
public class StageMenu extends Gui
{
    /** The parent new tutorial menu */
    final NewTutorialMenu tutorialMenu;

    /** A reference to the creator */
    final User creator;

    /** A reference to the stage which this Stage Menu is editing */
    private final Stage stage;

    /** A reference to the list of stages */
    final ArrayList<Stage> stages;

    private final TextEditorBookListener nameEditor;

    /** The number of pages in this menu */
    private int iPages;

    /** The current page that the player is on */
    private int iPage;

    /** Information book */
    private final ItemStack information;

    public StageMenu(NewTutorialMenu tutorialMenu, User creator, Stage stage, ArrayList<Stage> stages)
    {
        super(tutorialMenu.getManager(), 54, TutorialGUIUtils.inventoryTitle(stage.getName()));
        this.tutorialMenu = tutorialMenu;
        this.creator = creator;
        this.stage = stage;
        this.stages = stages;

        this.iPages = (stage.steps.size()/36)+1;
        this.iPage = 1;

        information = Utils.createItem(Material.WRITTEN_BOOK, 1, TutorialGUIUtils.optionTitle("Information"));
        BookMeta bookMeta = (BookMeta) information.getItemMeta();
        bookMeta.addPages(Component.text("" +
                        "Steps are lower level divisions of tutorials. All of the steps within a stage must be done in the correct order.\n" +
                        "\n" +
                        "A step can have many groups. The groups of a step can be started in any order. The groups may be corners of a house for example, where order doesn't matter."),
                Component.text("The tasks of a group must be completed in order."));
        information.setItemMeta(bookMeta);

        TutorialCreationSession tutorialCreationSession = tutorialMenu.tutorialCreationSession;

        nameEditor = new TextEditorBookListener(tutorialCreationSession.plugin, creator.player, this, "Stage Name",
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
                            stage.setName(szNewContent);
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
                        StageMenu.this.editName(TutorialGUIUtils.inventoryTitle(stage.getName()), tutorialCreationSession.creator.player);
                    }
                }, stage.getName());


        addItems();
    }

    private void addItems()
    {
        //Delete stage
        setItem(2, Utils.createItem(Material.BARRIER, 1, TutorialGUIUtils.optionTitle("Delete Stage"),
                        TutorialGUIUtils.optionLore("Delete stage and all child objects")),
                new GuiAction() {
                    @Override
                    public void click(InventoryClickEvent event) {
                        //Create new cancel confirmation menu
                        DeleteConfirmation deleteConfirmation = new DeleteConfirmation(StageMenu.this, () -> {
                            //Removes the stage
                            stages.remove(stage);

                            //Adjust the orders
                            for (int i = 0 ; i < stages.size() ; i++)
                            {
                                stages.get(i).setOrder(i+1);
                            }

                            //Refreshes the tutorial menu
                            tutorialMenu.refresh();

                            //Opens the tutorial menu
                            creator.mainGui = tutorialMenu;
                            tutorialMenu.open(creator.player);

                            //Deletes this menu
                            delete();
                        }, TutorialGUIUtils.optionLore("Delete stage"), TutorialGUIUtils.optionLore("Back to stage menu"));

                        //Open it
                        deleteConfirmation.open(creator.player);
                    }
                });


        //Set name
        setItem(13, Utils.createItem(Material.NAME_TAG, 1, TutorialGUIUtils.optionTitle("Set Stage Name")),
                new GuiAction() {
                    @Override
                    public void click(InventoryClickEvent event) {
                        creator.player.sendMessage(Display.aquaText("Use the name editor book to enter a new name. It must be no more than 45 characters"));
                        nameEditor.startEdit("Stage Name Editor");
                    }
                });

        //Back to stages
        setItem(6, Utils.createItem(Material.SPRUCE_DOOR, 1, TutorialGUIUtils.optionTitle("Back to Stages"), TutorialGUIUtils.optionLore("Back to the list of stages")),
                new GuiAction() {
                    @Override
                    public void click(InventoryClickEvent event) {
                        //Delete this menu and reopen the parent menu
                        delete();
                        tutorialMenu.refresh();
                        creator.mainGui = tutorialMenu;
                        creator.mainGui.open(creator.player);
                    }
                });

        //Step information
        setItem(4, Utils.createItem(Material.KNOWLEDGE_BOOK, 1, TutorialGUIUtils.optionTitle("Information"),
                        TutorialGUIUtils.optionLore("Information about steps")),
                new GuiAction() {
                    @Override
                    public void click(InventoryClickEvent event) {
                        Bukkit.getScheduler().runTask(tutorialMenu.tutorialCreationSession.plugin, () -> creator.player.openBook(information));
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

                    StageMenu.this.iPage--;
                    refresh();
                    creator.mainGui = StageMenu.this;
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
                    StageMenu.this.iPage++;
                    refresh();
                    creator.mainGui = StageMenu.this;
                    creator.mainGui.open(creator.player);
                }
            });
        }
        
        //Adds the Steps
        int iStart = (iPage-1)*36;
        int iMax = Math.min(iStart+36, stage.steps.size());
        int i;

        for (i = iStart ; i < iMax ; i++)
        {
            int finalI = i;

            Material material;
            if (stage.steps.get(i).isComplete())
                material = Material.WRITTEN_BOOK;
            else
                material = Material.BOOK;

            ItemStack step = Utils.createItem(material, 1,
                    TutorialGUIUtils.optionTitle(stage.steps.get(i).getName()),
                    TutorialGUIUtils.optionLore(stage.steps.get(i).getStepInStage()+""));

            super.setItem(i-iStart+18, step, new GuiAction() {
                @Override
                public void click(InventoryClickEvent event) {

                    creator.mainGui = new StepMenu(StageMenu.this, creator, stage.steps.get(finalI), stage.steps);
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
                            TutorialGUIUtils.optionTitle("Add Step"),
                            TutorialGUIUtils.optionLore("Click to add another step")),
                    new GuiAction() {
                        @Override
                        public void click(InventoryClickEvent event) {
                            //Adds a new option
                            stage.steps.add(new Step("", iOrder+1, Display.DisplayType.hologram));
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
        this.iPages = (stage.steps.size()/36)+1;

        //Knock the player back a page if they're now too far forwards
        if (iPage > iPages)
            iPage = iPages;

        //Add the items
        this.addItems();
    }
}
