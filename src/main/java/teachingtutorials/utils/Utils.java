package teachingtutorials.utils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;
import com.destroystokyo.paper.profile.PlayerProfile;

/**
 * A set of Minecraft utils
 */
public class Utils {

    /**
     * Translates a text with & colour coding to use ChatColor codes
     * @param s The string to convert
     * @return A copy of the converted string
     */
    public static String chat (String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    /**
     * Creates an item stack from the parameters
     * @param material The material
     * @param amount The amount of the item
     * @param displayName The name of the item
     * @param loreString The lore of the item
     * @return A reference to the item stack created for the given parameters
     */
    public static ItemStack createItem(Material material, int amount, Component displayName, Component... loreString)
    {
        ItemStack item;

        item = new ItemStack(material);
        item.setAmount(amount);

        ItemMeta meta = item.getItemMeta();
        meta.displayName(displayName);
        List<Component> lore = new ArrayList<>(Arrays.asList(loreString));
        meta.lore(lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Gets the player's skull of the inputted skull as an item stack and adds it to the inventory provided
     * @param inv The inventory to add the skull to
     * @param p The player to get the skull of
     * @param amount The amount of the item to add
     * @param invSlot The slot in the inventory to add the item to
     * @param displayName The name of the skull
     * @param loreString The lore of the skull
     * @return A reference to the item stack created for the given parameters
     */
    public static ItemStack createPlayerSkull(Inventory inv, Player p, int amount, int invSlot, String displayName, String... loreString) {

        ItemStack item;

        List<String> lore = new ArrayList<String>();

        item = new ItemStack(Material.PLAYER_HEAD);

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setDisplayName(Utils.chat(displayName));
        for (String s : loreString) {
            lore.add(Utils.chat(s));
        }
        meta.setLore(lore);
        meta.setOwningPlayer(p);
        item.setItemMeta(meta);

        inv.setItem(invSlot - 1,  item);

        return item;

    }

    public static ItemStack createCustomSkullWithFallback(String texture, Material fallback, int amount, Component displayName, Component... loreString) {

        ItemStack item;

        try {

            if (texture == null) {
                throw new NullPointerException();
            }

            URL url = new URL("http://textures.minecraft.net/texture/" + texture);
            item = new ItemStack(Material.PLAYER_HEAD);

            SkullMeta meta = (SkullMeta) item.getItemMeta();

            //Create playerprofile.
            PlayerProfile profile = Bukkit.getServer().createProfile(UUID.randomUUID());

            PlayerTextures textures = profile.getTextures();
            textures.setSkin(url);

            profile.setTextures(textures);

            meta.setPlayerProfile(profile);

            item.setItemMeta(meta);

        } catch (Exception e) {
            item = new ItemStack(fallback);
        }

        item.setAmount(amount);

        ItemMeta meta = item.getItemMeta();
        meta.displayName(displayName);
        List<Component> lore = new ArrayList<>(Arrays.asList(loreString));
        meta.lore(lore);
        item.setItemMeta(meta);

        return item;

    }


    /**
     * Returns whether a given player is in a specific group
     * @param player The player to check the group membership of
     * @param group The group to check whether the player is in
     * @return Whether the given player is in the given group
     */
    public static boolean isPlayerInGroup(Player player, String group) {
        return player.hasPermission("group." + group);
    }

    /**
     * Spawns a 'British' firework at a player's location
     * @param p The player to spawn the firework at
     */
    public static void spawnFireWork(Player p) {

        Firework f = p.getWorld().spawn(p.getLocation(), Firework.class);
        FireworkMeta fm = f.getFireworkMeta();
        fm.addEffect(FireworkEffect.builder().flicker(true).trail(true).with(Type.BALL_LARGE).withColor(Color.RED).withColor(Color.BLUE).withColor(Color.WHITE).build());
        fm.setPower(1);
        f.setFireworkMeta(fm);
    }
}
