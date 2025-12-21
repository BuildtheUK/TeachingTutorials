package teachingtutorials.newtutorial;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import net.bteuk.minecraft.gui.*;
import teachingtutorials.guis.TutorialGUIUtils;
import teachingtutorials.tutorialobjects.Group;
import teachingtutorials.tutorialobjects.Task;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

import java.util.ArrayList;

/**
 * A menu to edit a group of a tutorial
 */
public class GroupMenu extends Gui
{
    /** The parent step menu */
    final StepMenu stepMenu;

    /** A reference to the creator */
    final User creator;

    /** A reference to the group which this Group Menu is editing */
    private final Group group;

    /** A reference to the list of groups which this group is a part of */
    private final ArrayList<Group> groups;

    /** The number of pages in this menu */
    private int iPages;

    /** The current page that the player is on */
    private int iPage;

    /** Information book */
    private final ItemStack information;

    public GroupMenu(StepMenu stepMenu, User creator, Group group, ArrayList<Group> groups)
    {
        super(stepMenu.getManager(), 54, TutorialGUIUtils.inventoryTitle("Group Menu"));
        this.stepMenu = stepMenu;
        this.creator = creator;
        this.group = group;
        this.groups = groups;

        this.iPages = (group.tasks.size()/36)+1;
        this.iPage = 1;

        information = Utils.createItem(Material.WRITTEN_BOOK, 1, TutorialGUIUtils.optionTitle("Information"));
        BookMeta bookMeta = (BookMeta) information.getItemMeta();
        bookMeta.addPages(Component.text("" +
                "Tasks are the fundamental building blocks of all tutorials.\n" +
                "\n" +
                "They define the actions that a player must take in order to progress through the tutorial.\n" +
                "\n" +
                "A Task can be one of 5 types: Tpll, Command, WorldEdit Selection, Chat, Block Place."));
        information.setItemMeta(bookMeta);

        addItems();
    }

    private void addItems()
    {
        //Delete group
        setItem(2, Utils.createItem(Material.BARRIER, 1, TutorialGUIUtils.optionTitle("Delete Group"),
                        TutorialGUIUtils.optionLore("Delete group and all child tasks")),
                new GuiAction() {
                    @Override
                    public void click(InventoryClickEvent event) {
                        //Create new cancel confirmation menu
                        DeleteConfirmation deleteConfirmation = new DeleteConfirmation(GroupMenu.this, () -> {
                            //Removes the group
                            groups.remove(group);

                            //Refreshes the step menu
                            stepMenu.refresh();

                            //Opens the step menu
                            creator.mainGui = stepMenu;
                            stepMenu.open(creator.player);

                        }, TutorialGUIUtils.optionLore("Delete group"), TutorialGUIUtils.optionLore("Back to group menu"));

                        //Open it
                        deleteConfirmation.open(creator.player);
                    }
                });

        //Back to step
        setItem(6, Utils.createItem(Material.SPRUCE_DOOR, 1, TutorialGUIUtils.optionTitle("Back to Groups"), TutorialGUIUtils.optionLore("Back to the list of groups")),
                new GuiAction() {
                    @Override
                    public void click(InventoryClickEvent event) {
                        //Delete this menu and reopen the parent menu
                        delete();
                        stepMenu.refresh();
                        creator.mainGui = stepMenu;
                        creator.mainGui.open(creator.player);
                    }
                });

        //Task information
        setItem(4, Utils.createItem(Material.KNOWLEDGE_BOOK, 1, TutorialGUIUtils.optionTitle("Information"),
                        TutorialGUIUtils.optionLore("Information about tasks")),
                new GuiAction() {
                    @Override
                    public void click(InventoryClickEvent event) {
                        Bukkit.getScheduler().runTask(stepMenu.stageMenu.tutorialMenu.tutorialCreationSession.plugin, () -> creator.player.openBook(information));
                    }
                });


        //Page back
        if (iPage > 1)
        {
            ItemStack pageBack = Utils.createCustomSkullWithFallback("4eff72715e6032e90f50a38f4892529493c9f555b9af0d5e77a6fa5cddff3cd2",
                    Material.ACACIA_BOAT, 1,
                    TutorialGUIUtils.optionTitle("Page back"));
            super.setItem(0, pageBack,  new GuiAction() {
                @Override
                public void click(InventoryClickEvent event) {
                    GroupMenu.this.iPage--;
                    refresh();
                    creator.mainGui = GroupMenu.this;
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
                    GroupMenu.this.iPage++;
                    refresh();
                    creator.mainGui = GroupMenu.this;
                    creator.mainGui.open(creator.player);
                }
            });
        }


        //Adds the tasks
        int iStart = (iPage-1)*36;
        int iMax = Math.min(iStart+36, group.tasks.size());
        int i;

        for (i = iStart ; i < iMax ; i++)
        {
            int finalI = i;

            ItemStack taskIcon;

            Material material;
            if (group.tasks.get(i).isComplete())
                material = Material.WRITTEN_BOOK;
            else
                material = Material.BOOK;

            switch (group.tasks.get(i).getType())
            {
                case tpll -> taskIcon = Utils.createItem(material, 1,
                        TutorialGUIUtils.optionTitle("Tpll Task"));
                case command -> taskIcon = Utils.createItem(material, 1,
                        TutorialGUIUtils.optionTitle("Command Task"));
                case place -> taskIcon = Utils.createItem(material, 1,
                        TutorialGUIUtils.optionTitle("Place Task"));
                case selection -> taskIcon = Utils.createItem(material, 1,
                        TutorialGUIUtils.optionTitle("Selection Task"));
                case chat -> taskIcon = Utils.createItem(material, 1,
                        TutorialGUIUtils.optionTitle("Chat Task"));
                case null, default -> taskIcon = Utils.createItem(material, 1,
                        TutorialGUIUtils.optionTitle("Task"));
            }

            super.setItem(i-iStart+18, taskIcon, new GuiAction() {
                @Override
                public void click(InventoryClickEvent event) {

                    creator.mainGui = new TaskMenu(GroupMenu.this, group.tasks.get(finalI), group.tasks);
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
                            TutorialGUIUtils.optionTitle("Add Task"),
                            TutorialGUIUtils.optionLore("Click to add another task")),
                    new GuiAction() {
                        @Override
                        public void click(InventoryClickEvent event) {
                            //Adds a new task
                            group.tasks.add(new Task(iOrder+1));
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
        super.clear();

        //Recalculate number of pages required
        this.iPages = (group.tasks.size()/36)+1;

        //Knock the player back a page if they're now too far forwards
        if (iPage > iPages)
            iPage = iPages;

        addItems();
    }
}
