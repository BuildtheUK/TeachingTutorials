package teachingtutorials.newtutorial;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import net.bteuk.minecraft.gui.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import teachingtutorials.guis.TutorialGUIUtils;
import teachingtutorials.utils.Utils;

/**
 * A menu to verify whether a user really wants to save the tutorial
 */
class ConfirmConfirmation extends Gui
{
    private final Gui parentMenu;

    private final Runnable deleteAction;

    private final TextComponent confirmLore;
    private final TextComponent cancelLore;

    /**
     *
     * @param parentMenu A reference to the menu to return back to if the user doesn't verify their confirmation
     * @param deleteAction Confirm action. This must involve either closing the inventory or opening another menu.
     * @param confirmLore The lore on the confirmation icon
     * @param cancelLore The lore on the cancel icon
     */
    public ConfirmConfirmation(Gui parentMenu, Runnable deleteAction, TextComponent confirmLore, TextComponent cancelLore)
    {
        super(parentMenu.getManager(), 27, TutorialGUIUtils.inventoryTitle("Tutorial is ready to add. Are you?"));
        this.parentMenu = parentMenu;
        this.deleteAction = deleteAction;
        this.confirmLore = confirmLore;
        this.cancelLore = cancelLore;
        this.addItems();
    }

    private void addItems()
    {
        this.setItem(11, Utils.createItem(Material.EMERALD, 1, TutorialGUIUtils.optionTitle("Confirm save"), confirmLore),
                new GuiAction() {
                    @Override
                    public void click(InventoryClickEvent event) {
                        deleteAction.run();
                        ConfirmConfirmation.this.delete();
                    }
                });

        this.setItem(15, Utils.createItem(Material.LECTERN, 1, TutorialGUIUtils.optionTitle("Cancel"), cancelLore),
                new GuiAction() {
                    @Override
                    public void click(InventoryClickEvent event) {
                        parentMenu.open((Player) event.getWhoClicked());
                        ConfirmConfirmation.this.delete();
                    }
                });
    }
}
