package teachingtutorials.guis;

import net.bteuk.minecraft.gui.*;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialobjects.LessonObject;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

/**
 * A menu which shows a list of lessons that a player has unfinished
 */
public class LessonsMenu extends Gui
{
    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /** The user whom this menu is for */
    private final User user;

    /** A reference to the parent Gui */
    private final Gui parentGui;

    /** The list of lessons to display in this menu */
    private final LessonObject[] lessons;

    /** The number of pages in this menu */
    private final int iPages;

    /** The current page that the player is on */
    private int iPage;

    public LessonsMenu(TeachingTutorials plugin, User user, Gui parentGui, LessonObject[] lessons)
    {
        super(plugin.getTutGuiManager(), 54, TutorialGUIUtils.inventoryTitle("Your Lessons"));
        this.plugin = plugin;
        this.user = user;
        this.parentGui = parentGui;
        this.lessons = lessons;

        this.iPages = ((lessons.length-1)/36)+1;
        this.iPage = 1;

        addItems();
    }

    private void addItems()
    {
        // We need a page system
        // 4 lines of options
        // Blank line
        // Then arrows and back button

        //Reset page
        this.iPage = 1;

        //Indicates that there are no unfinished lessons
        if (lessons.length == 0)
        {
            ItemStack noTutorials = Utils.createItem(Material.BARRIER, 1,
                    TutorialGUIUtils.optionTitle("You have no unfinished lessons!"));
            setItem(5-1, noTutorials);
        }

        //Adds the lessons
        int iStart = (iPage-1)*9;
        int iMax = Math.min((iPage+3)*9, lessons.length);
        for (int i = iStart ; i < iMax ; i++)
        {
            int finalI = i;

            ItemStack lesson = Utils.createItem(Material.WRITABLE_BOOK, 1,
                    TutorialGUIUtils.optionTitle(lessons[i].getTutorial().getTutorialName()),
                    TutorialGUIUtils.optionLore(lessons[i].getLocation().getLocationName()));

            super.setItem(i-iStart, lesson, new GuiAction() {
                @Override
                public void click(InventoryClickEvent event) {
                    user.mainGui = new LessonContinueConfirmer(plugin, user, LessonsMenu.this, lessons[finalI], "Do you want to restart or resume?");
                    user.mainGui.open(user.player);
                }
            });
        }

        //Page back
        if (iPage > 1)
        {
            ItemStack pageBack = Utils.createCustomSkullWithFallback("4eff72715e6032e90f50a38f4892529493c9f555b9af0d5e77a6fa5cddff3cd2",
                    Material.ACACIA_BOAT, 1,
                    TutorialGUIUtils.optionTitle("Page back"));
            super.setItem(45, pageBack, new GuiAction() {
                @Override
                public void click(InventoryClickEvent event) {
                    LessonsMenu.this.iPage--;
                    refresh();
                    user.mainGui = LessonsMenu.this;
                    user.mainGui.open(user.player);
                }
            });
        }

        //Page forwards
        if (iPage < iPages)
        {
            ItemStack pageBack = Utils.createCustomSkullWithFallback("a7ba2aa14ae5b0b65573dc4971d3524e92a61dd779e4412e4642adabc2e56c44",
                    Material.ACACIA_BOAT, 1,
                    TutorialGUIUtils.optionTitle("Page forwards"));
            super.setItem(53, pageBack, new GuiAction() {
                @Override
                public void click(InventoryClickEvent event) {
                    LessonsMenu.this.iPage++;
                    refresh();
                    user.mainGui = LessonsMenu.this;
                    user.mainGui.open(user.player);
                }
            });
        }


        //Back button
        ItemStack back = Utils.createItem(Material.SPRUCE_DOOR, 1,
                TutorialGUIUtils.optionTitle("Back"));
        super.setItem(49, back, new GuiAction() {
            @Override
            public void click(InventoryClickEvent event) {
                user.mainGui = parentGui;
                user.mainGui.open(user.player);
                delete();
            }
        });
    }

    /**
     * Refresh the gui.
     * This usually involves clearing the content and recreating it.
     */
    public void refresh() {
        super.clear();

        this.addItems();
    }
}
