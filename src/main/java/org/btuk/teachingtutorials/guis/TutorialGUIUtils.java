package org.btuk.teachingtutorials.guis;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Utils for Tutorial Menus
 */
public abstract class TutorialGUIUtils
{
    public static TextComponent backButton(String szText)
    {
        return Component.text(szText, NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true);
    }

    public static TextComponent optionTitle(String szText)
    {
        return Component.text(szText, NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false);
    }

    public static TextComponent optionLore(String szText)
    {
        return Component.text(szText, NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false);
    }

    public static TextComponent inventoryTitle(String szText)
    {
        return Component.text(szText, Style.style(TextDecoration.BOLD, NamedTextColor.DARK_AQUA));
    }
}
