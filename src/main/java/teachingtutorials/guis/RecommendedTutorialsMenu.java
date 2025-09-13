package teachingtutorials.guis;

import net.bteuk.minecraft.gui.*;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialobjects.LessonObject;
import teachingtutorials.tutorialobjects.Location;
import teachingtutorials.tutorialobjects.Tutorial;
import teachingtutorials.tutorialobjects.TutorialRecommendation;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

public class RecommendedTutorialsMenu extends Gui
{
    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /** The user whom this menu is for */
    private final User user;

    /** A reference to the parent Gui */
    private final Gui parentGui;

    /** The list of tutorial recommendations which this menu is displaying */
    private final TutorialRecommendation[] tutorialRecommendations;

    /** The number of pages in this menu */
    private final int iPages;

    /** The current page that the player is on */
    private int iPage;

    public RecommendedTutorialsMenu(TeachingTutorials plugin, MainMenu mainMenu, User user, TutorialRecommendation[] tutorialRecommendations)
    {
        super(plugin.getTutGuiManager(), 54, TutorialGUIUtils.inventoryTitle("Recommended Tutorials"));
        this.plugin = plugin;
        this.parentGui = mainMenu;
        this.user = user;
        this.tutorialRecommendations = tutorialRecommendations;

        this.iPages = ((tutorialRecommendations.length-1)/36)+1;

        addItems();
    }

    public void addItems()
    {
        //Reset page
        this.iPage = 1;

        //Back button
        setItem(53, Utils.createItem(Material.SPRUCE_DOOR, 1, TutorialGUIUtils.optionTitle("Back to Main Menu")), new GuiAction() {
            @Override
            public void click(InventoryClickEvent event) {
                user.mainGui = parentGui;
                user.mainGui.open(user.player);
                delete();
            }
        });

        //Indicates that there are no tutorial recommendations
        if (tutorialRecommendations.length == 0)
        {
            ItemStack noTutorials = Utils.createItem(Material.BARRIER, 1,
                    TutorialGUIUtils.optionTitle("You have no tutorial recommendations!"));
            setItem(5-1, noTutorials);
        }

        //Adds the tutorials
        int iStart = (iPage-1)*9;
        int iMax = Math.min((iPage+3)*9, tutorialRecommendations.length);
        for (int i = iStart ; i < iMax ; i++)
        {
            //Fetches the tutorial
            Tutorial tutorial = Tutorial.fetchByTutorialID(tutorialRecommendations[i].getTutorialID(), plugin.getDBConnection(), plugin.getLogger());
            if (tutorial == null)
                continue;
            if (!tutorial.isInUse())
                continue;

            final Location location;
            if (tutorialRecommendations[i].getLocationID() > 0)
                location = Location.getLocationByLocationID(plugin.getDBConnection(), plugin.getLogger(), tutorialRecommendations[i].getLocationID());
            else
                location = null;

            ItemStack lesson = Utils.createItem(Material.KNOWLEDGE_BOOK, 1,
                    TutorialGUIUtils.optionTitle(tutorial.getTutorialName()),
                    TutorialGUIUtils.optionLore("Recommended by " +tutorialRecommendations[i].getRecommenderName()));

            super.setItem(i-iStart, lesson, new GuiAction() {
                @Override
                public void click(InventoryClickEvent event) {
                    LibraryMenu.startTutorial(plugin, LessonObject.getUnfinishedLessonsOfPlayer(user.player.getUniqueId(), plugin.getDBConnection(), plugin.getLogger()),
                            user, RecommendedTutorialsMenu.this, tutorial, location);
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
                    iPage--;
                    refresh();
                    user.mainGui = RecommendedTutorialsMenu.this;
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
                    iPage++;
                    refresh();
                    user.mainGui = RecommendedTutorialsMenu.this;
                    user.mainGui.open(user.player);
                }
            });
        }

    }

    /**
     * Refresh the gui.
     * This usually involves clearing the content and recreating it.
     */
    public void refresh()
    {
        this.clear();

        this.addItems();
    }
}
