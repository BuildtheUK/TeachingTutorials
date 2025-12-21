package teachingtutorials.guis;

import net.bteuk.minecraft.gui.*;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialplaythrough.PlaythroughMode;
import teachingtutorials.tutorialplaythrough.TutorialPlaythrough;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.Utils;

/**
 * Represents the Navigation Menu provided to players who are playing a Tutorial to assist with navigation.
 */
public class TutorialNavigationMenu extends Gui
{
    /**
     * A reference to the Tutorial playthrough for which this menu is for
     */
    private final TutorialPlaythrough tutorialPlaythrough;

    public TutorialNavigationMenu(TutorialPlaythrough tutorialPlaythrough, TeachingTutorials plugin)
    {
        super(plugin.getTutGuiManager(), 3*9, Display.colouredText("Tutorial Navigation Menu", NamedTextColor.AQUA));

        this.tutorialPlaythrough = tutorialPlaythrough;
    }

    private void addItems()
    {
        //Stage back
        if (tutorialPlaythrough.canMoveBackStage())
            super.setItem(0, Utils.createCustomSkullWithFallback("4eff72715e6032e90f50a38f4892529493c9f555b9af0d5e77a6fa5cddff3cd2", Material.ACACIA_BOAT,
                    1, TutorialGUIUtils.optionTitle("Stage Back"), TutorialGUIUtils.optionLore("Go back a stage")),
                    new GuiAction() {
                        @Override
                        public void click(InventoryClickEvent event) {
                            tutorialPlaythrough.previousStage();
                        }
                    });

        //Stage forwards
        if (tutorialPlaythrough.canMoveForwardsStage())
            super.setItem(8, Utils.createCustomSkullWithFallback("a7ba2aa14ae5b0b65573dc4971d3524e92a61dd779e4412e4642adabc2e56c44", Material.ACACIA_BOAT,
                            1, TutorialGUIUtils.optionTitle("Stage Forwards"), TutorialGUIUtils.optionLore("Skip to next stage")),
                    new GuiAction() {
                        @Override
                        public void click(InventoryClickEvent event) {
                            tutorialPlaythrough.skipStage();
                        }
                    });

        //Step back
        if (tutorialPlaythrough.canMoveBackStep())
            super.setItem(2, Utils.createCustomSkullWithFallback("4eff72715e6032e90f50a38f4892529493c9f555b9af0d5e77a6fa5cddff3cd2", Material.ACACIA_BOAT,
                            1, TutorialGUIUtils.optionTitle("Step Back"), TutorialGUIUtils.optionLore("Go back a step")),
                    new GuiAction() {
                        @Override
                        public void click(InventoryClickEvent event) {
                            tutorialPlaythrough.previousStep();
                        }
                    });

        //Stage forwards
        if (tutorialPlaythrough.canMoveForwardsStep())
            super.setItem(6, Utils.createCustomSkullWithFallback("a7ba2aa14ae5b0b65573dc4971d3524e92a61dd779e4412e4642adabc2e56c44", Material.ACACIA_BOAT,
                            1, TutorialGUIUtils.optionTitle("Step Forwards"), TutorialGUIUtils.optionLore("Skip to next step")),
                    new GuiAction() {
                        @Override
                        public void click(InventoryClickEvent event) {
                            tutorialPlaythrough.skipStep();
                        }
                    });

        //Spawn
        super.setItem(20, Utils.createItem(Material.HORSE_SPAWN_EGG, 1, TutorialGUIUtils.optionTitle("TP to start of step")),
                new GuiAction() {
                    @Override
                    public void click(InventoryClickEvent event) {
                        tutorialPlaythrough.tpToStepStart();
                    }
                });

        //Exit
        super.setItem(22, Utils.createItem(Material.SPRUCE_DOOR, 1, TutorialGUIUtils.optionTitle("Pause and Save"), TutorialGUIUtils.optionLore("Back to lobby")),
                new GuiAction() {
                    @Override
                    public void click(InventoryClickEvent event) {
                        tutorialPlaythrough.terminateEarly();
                        tutorialPlaythrough.getCreatorOrStudent().player.closeInventory();
                    }
                });

        //Video
        if (tutorialPlaythrough.currentStepHasVideoLink())
        {
            super.setItem(24, Utils.createItem(Material.PAINTING, 1, TutorialGUIUtils.optionTitle("Video Link")),
                    new GuiAction() {
                        @Override
                        public void click(InventoryClickEvent event) {
                            tutorialPlaythrough.callVideoLink();
                        }
                    });
        }

        //Editor mode switcher
        if (tutorialPlaythrough.getCreatorOrStudent().player.getUniqueId().equals(tutorialPlaythrough.getTutorial().getUUIDOfAuthor()) && tutorialPlaythrough.getCurrentPlaythroughMode().equals(PlaythroughMode.PlayingLesson))
        {
            super.setItem(4, Utils.createItem(Material.WRITABLE_BOOK, 1, TutorialGUIUtils.optionTitle("Switch to edit mode"), TutorialGUIUtils.optionLore("Restart step in edit mode")),
                    new GuiAction() {
                        @Override
                        public void click(InventoryClickEvent event) {
                            //Switch the playthrough mode
                            tutorialPlaythrough.setCurrentPlaythroughMode(PlaythroughMode.EditingLocation);
                        }
                    });
        }
        else if (tutorialPlaythrough.getCurrentPlaythroughMode().equals(PlaythroughMode.CreatingLocation))
        {
            super.setItem(4, Utils.createItem(Material.WRITABLE_BOOK, 1, TutorialGUIUtils.optionTitle("Open step editor menu")),
                    new GuiAction() {
                        @Override
                        public void click(InventoryClickEvent event) {
                            //Open the step editor menu
                            tutorialPlaythrough.openStepEditorMenu();
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

    /**
     * Refreshes and opens the menu
     * @param player The player to open the menu for
     */
    @Override
    public void open(Player player)
    {
        refresh();
        super.open(player);
    }
}
