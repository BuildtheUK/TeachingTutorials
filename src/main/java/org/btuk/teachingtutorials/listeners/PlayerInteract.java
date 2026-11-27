package org.btuk.teachingtutorials.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.btuk.teachingtutorials.guis.MainMenu;
import org.btuk.teachingtutorials.TeachingTutorials;
import org.btuk.teachingtutorials.utils.Display;
import org.btuk.teachingtutorials.utils.User;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * An interaction listener used to detect clicks of the menu item in the hot-bar
 */
public class PlayerInteract implements Listener
{
    /** A reference to the instance of the TeachingTutorials plugin */
    private final TeachingTutorials plugin;

    /**
     * Constructs the object and registers the event listeners
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     */
    public PlayerInteract(TeachingTutorials plugin)
    {
        this.plugin = plugin;
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Detects all interaction events, checks whether they interacted with the menu opener item, and if so, cancels
     * the event and opens the menu
     * @param event A player interact event
     */
    @EventHandler
    public void interactEvent(PlayerInteractEvent event)
    {
        //Extract the player into a local variable
        Player player = event.getPlayer();

        //Identifies the user for this player
        User user = User.identifyUser(plugin, player);

        if (user == null)
        {
            plugin.getLogger().severe("User " + player.getName() + " can not be found (is null)!");
            player.sendMessage(Display.errorText("Tutorials user can not be found, please relog!"));
            return;
        }

        //Checks the item in their hand
        if (event.getItem() != null)
        {
            //Checks whether the item is equal to the menu item
            if (event.getItem().equals(TeachingTutorials.menu))
            {
                //Cancel the event
                event.setCancelled(true);

                //Check if the mainGui is not null.
                if (user.mainGui != null)
                {
                    //If not then open it after refreshing its contents.
                    user.mainGui.open(user.player);
                }

                //If no gui exists open the learning menu
                else
                {
                    //Creates a new main menu and open
                    user.mainGui = new MainMenu(plugin, user);
                    user.mainGui.open(user.player);
                }
            }
        }
    }

    /**
     * Detects inventory click events, checks whether they clicked the menu opener item, and if so, cancels
     * the event and opens the menu
     * @param event An inventory click event
     */
    @EventHandler
    public void onClick(InventoryClickEvent event) {

        if (event.getCurrentItem() == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        User user = User.identifyUser(plugin, player);
        if (user == null)
        {
            plugin.getLogger().severe("User " + player.getName() + " can not be found (is null)!");
            player.sendMessage(Display.errorText("Tutorials user can not be found, please relog!"));
            return;
        }

        //Checks whether the item is equal to the menu item
        if (event.getCurrentItem().equals(TeachingTutorials.menu))
        {
            //Cancel the event
            event.setCancelled(true);

            //Check if the mainGui is not null.
            if (user.mainGui != null)
            {
                //If not then open it after refreshing its contents.
                user.mainGui.open(user.player);
            }

            //If no gui exists open the learning menu
            else
            {
                //Creates a new main menu and open
                user.mainGui = new MainMenu(plugin, user);
                user.mainGui.open(user.player);
            }
        }
    }

    /*
    The following events are to prevent the navigator being moved in the inventory,
    causing duplicate items which are difficult to remove.
     */
    @EventHandler
    public void swapHands(PlayerSwapHandItemsEvent e) {
        if (e.getOffHandItem() == null) {
            return;
        }

        if (e.getOffHandItem().equals(TeachingTutorials.menu)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void dropItem(PlayerDropItemEvent e) {
        if (e.getItemDrop().getItemStack().equals(TeachingTutorials.menu)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void dragItem(InventoryMoveItemEvent e) {
        if (e.getItem().equals(TeachingTutorials.menu)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void dragItem(InventoryDragEvent e) {
        if (e.getOldCursor().equals(TeachingTutorials.menu)) {
            e.setCancelled(true);
        }

        if (e.getCursor() == null) {
            return;
        }

        if (e.getCursor().equals(TeachingTutorials.menu)) {
            e.setCancelled(true);
        }
    }

}
