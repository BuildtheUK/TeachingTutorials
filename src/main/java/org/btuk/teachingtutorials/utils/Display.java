package org.btuk.teachingtutorials.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * A class of util methods for displaying text to users
 */
public class Display
{
    /**
     * Sends an action bar message to a player
     * @param player The player to send the message to
     * @param textComponent The contents of the message
     */
    public static void ActionBar(Player player, net.kyori.adventure.text.TextComponent textComponent)
    {
        //Extracts the colour to the legacy format needed by the 1.18.2 api
        String szColourPrefix;
        TextColor color = textComponent.color();
        if (color.equals(NamedTextColor.DARK_RED)) {
            szColourPrefix = ChatColor.DARK_RED.toString();
        } else if (color.equals(NamedTextColor.RED)) {
            szColourPrefix = ChatColor.RED.toString();
        } else if (color.equals(NamedTextColor.GOLD)) {
            szColourPrefix = ChatColor.GOLD.toString();
        } else if (color.equals(NamedTextColor.YELLOW)) {
            szColourPrefix = ChatColor.YELLOW.toString();
        } else if (color.equals(NamedTextColor.GREEN)) {
            szColourPrefix = ChatColor.GREEN.toString();
        } else if (color.equals(NamedTextColor.DARK_GREEN)) {
            szColourPrefix = ChatColor.DARK_GREEN.toString();
        } else if (color.equals(NamedTextColor.AQUA)) {
            szColourPrefix = ChatColor.AQUA.toString();
        } else if (color.equals(NamedTextColor.DARK_AQUA)) {
            szColourPrefix = ChatColor.DARK_AQUA.toString();
        } else if (color.equals(NamedTextColor.DARK_BLUE)) {
            szColourPrefix = ChatColor.DARK_BLUE.toString();
        } else if (color.equals(NamedTextColor.BLUE)) {
            szColourPrefix = ChatColor.BLUE.toString();
        } else if (color.equals(NamedTextColor.LIGHT_PURPLE)) {
            szColourPrefix = ChatColor.LIGHT_PURPLE.toString();
        } else if (color.equals(NamedTextColor.DARK_PURPLE)) {
            szColourPrefix = ChatColor.DARK_PURPLE.toString();
        } else if (color.equals(NamedTextColor.BLACK)) {
            szColourPrefix = ChatColor.BLACK.toString();
        } else if (color.equals(NamedTextColor.WHITE)) {
            szColourPrefix = ChatColor.WHITE.toString();
        } else if (color.equals(NamedTextColor.DARK_GRAY)) {
            szColourPrefix = ChatColor.DARK_GRAY.toString();
        } else if (color.equals(NamedTextColor.GRAY)) {
            szColourPrefix = ChatColor.GRAY.toString();
        }
        else
            szColourPrefix = "";

        //Sends the nessage with the correct colour
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(szColourPrefix + textComponent.content()));
    }

    /**
     * Sends a Title to a player
     * @param player The player to send the title to
     * @param szTitle The contents of the title
     * @param szSubtitle The contents of the subtitle
     * @param iFadeInTicks The number of ticks for which the title should be fading in
     * @param iStayTicks The number of ticks for which the title should stay after fading in and before fading out
     * @param iFadeOutTicks The number of ticks for which the title should be fading out
     */
    public static void Title(Player player, String szTitle, String szSubtitle, int iFadeInTicks, int iStayTicks, int iFadeOutTicks)
    {
        //The title is the szText of the display
        player.sendTitle(szTitle, szSubtitle, iFadeInTicks, iStayTicks, iFadeOutTicks);
    }

    /**
     * A set of allowed display types within the tutorials system
     */
    public enum DisplayType
    {
        hologram, chat, action_bar
    }

    /**
     * Produces a non italicised red Component for the given text
     * @param szText The text
     */
    public static Component errorText(String szText)
    {
        return colouredText(szText, NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Produces a non italicised aqua Component for the given text
     * @param szText The text
     */
    public static Component aquaText(String szText)
    {
        return colouredText(szText, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Produces a coloured TextComponent for the given text
     * @param szText The text
     * @param namedTextColor A named text colour
     */
    public static net.kyori.adventure.text.TextComponent colouredText(String szText, NamedTextColor namedTextColor)
    {
        return Component.text(szText, namedTextColor);
    }
}
