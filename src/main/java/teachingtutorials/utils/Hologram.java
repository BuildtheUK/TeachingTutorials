package teachingtutorials.utils;

import eu.decentsoftware.holograms.api.DHAPI;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialplaythrough.Lesson;
import teachingtutorials.tutorialplaythrough.TutorialPlaythrough;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a hologram within the Tutorials system
 */
public class Hologram
{
    /** A reference to the DH hologram which this Tutorial hologram is an extension of */
    private final eu.decentsoftware.holograms.api.holograms.Hologram hologram;

    /** A reference to the tutorials playthrough which this hologram is ultimately owned by */
    private final TutorialPlaythrough playthrough;
    /**
     * Creates an instance of the hologram with the given details
     * @param location A reference to a bukkit location object at which this hologram should be displayed
     * @param playthrough A reference to the player to which this hologram should exclusively be displayed to
     * @param szTitle The title of the hologram - will appear on the first line
     * @param szText The main body of the hologram
     * @param iStepID The step ID of the step which this hologram is a part of - used to compile the name of
     *                the hologram
     */
    public Hologram(Location location, TutorialPlaythrough playthrough, String szTitle, String szText, int iStepID, final int iMax_Width)
    {
        this.playthrough = playthrough;

        //Create the lines first

        //A list of all hologram lines
        ArrayList<String> szLines = new ArrayList<>();

        //Inserts the title
        szLines.add(szTitle);

        //Get a list of all of the words
        String[] szWords = szText.split(" ");

        //At the start of the for loop, represents the current line being compiled before
        // the current word has been added
        String szLine = "";

        //Represents the current line after the current word has been added
        String szLineNew;

        //Represents the current line after the current word has been added
        // but also after the text has been stripped of colour codes
        String szDisplayedText;

        //The maximum width a hologram line can be
        boolean bCreateNewLineAfter;
        bCreateNewLineAfter = false;

        //Used when a new line is manually specified and the start of the next line needs to be stored to added to the szLine at the end of the loop after the new line is added
        String szStartOfNextLine = "";

        //Goes through each word in the text
        for (int iWord = 0; iWord < szWords.length ; iWord++)
        {
            //TODO: what if two line seperators together or in one word?
            //-Expand algorithm time
            //Checks to see if the word contains a new line separator
            if (szWords[iWord].contains(System.lineSeparator()))
            {
                int iIndexOfLine = szWords[iWord].indexOf(System.lineSeparator());
                if (iIndexOfLine == 0)
                {
                    szLines.add(szLine);
                    szLine = "";
                }
                else if (iIndexOfLine == (szWords[iWord].length() - 1))
                {
                    bCreateNewLineAfter = true;
                }
                else
                {
                    bCreateNewLineAfter = true;
                    String[] twoWords = szWords[iWord].split(System.lineSeparator());
                    szWords[iWord] = twoWords[0];
                    szStartOfNextLine = twoWords[1];
                }
            }

            //Adds the word to the new line being compiled
            szLineNew = szLine + szWords[iWord].replaceFirst(System.lineSeparator(), "");

            //Strips the text of colour codes so that the length can be accurately measured
            szDisplayedText = szLineNew.replace("&[A-Fa-f0-9]", "");

            //Checks to see whether the new line exceeds the maximum width
            if (szDisplayedText.length() > iMax_Width) //Line is Hologram_Max_Width without the space
            {
                //If the new line is too big, needs to deal with it

                //Unless the new line is purely one word (aka the old one was blank)
                //How does the old one get blank? - if the same thing happened previously, see below

                //Indicates that the previous line had one > Max_Width characters long word,
                // so the new word must display on a new line
                if (szLine == "")
                    //szLine remains as ""
                    szLines.add(szLineNew);

                    //Indicates that the line already had some words in it so display those
                    //Adding the new one to it takes it over the maximum
                else
                {
                    //Adds the previous line to the hologram, removing the trailing space
                    szLines.add(szLine.substring(0, szLine.length() - 1));

                    //Adds the new line if it is over max characters long
                    if (szWords[iWord].replace("&[A-Fa-f0-9]", "").length() > iMax_Width)
                    {
                        szLines.add(szWords[iWord]);
                        szLine = "";
                    }

                    //If this is the last word, can immediately append it on a new line
                    else if (iWord == szWords.length - 1)
                        szLines.add(szWords[iWord]);

                    else
                        //Sends the line with the new word just added into the next loop, adds the space
                        szLine = szWords[iWord] + " ";
                }
            }

            //If this is the last worst, but the length didn't max out, we can append the new line
            else if (bCreateNewLineAfter)
            {
                szLines.add(szLineNew);
                szLine = szStartOfNextLine + " ";
            }
            else if (iWord == szWords.length - 1)
            {
                szLines.add(szLineNew);
            }
            else
            {
                //Sends the line with the new word just added into the next loop, adds the space
                szLine = szLineNew + " ";
            }

            bCreateNewLineAfter = false;
            szStartOfNextLine = "";
        }

        //Create the hologram
        if (this.playthrough instanceof Lesson lesson)
            this.hologram = DHAPI.createHologram(""+lesson.getLessonID()+"_"+iStepID, location, false, szLines);
        else
            this.hologram = DHAPI.createHologram("New_Location_"+playthrough.getCreatorOrStudent().player.getName()+"_"+iStepID, location, false, szLines);
        //Set to be generally invisible
        hologram.setDefaultVisibleState(false);
    }

    /**
     * Shows the hologram to the player and all spies of the tutorial playthrough.
     * When called, this will 'refresh' the list; everyone will be removed and readded.
     */
    public void showHologram()
    {
        int i;

        //Removes current show players
        // Gets a local reference to the list of show players
        Set<UUID> showPlayers = this.hologram.getShowPlayers();
        // Removes everyone from the list of show players
        showPlayers.removeAll(showPlayers);

        //Adds the player of the playthrough
        this.hologram.setShowPlayer(this.playthrough.getCreatorOrStudent().player);

        //Adds the spies
        for (Player spy : this.playthrough.getSpies())
        {
            this.hologram.setShowPlayer(spy);
        }
    }

    /**
     * Removes the hologram from the visibility of the given player
     */
    public void removePlayerVisibility(Player player)
    {
        this.hologram.removeShowPlayer(player);
    }

    /**
     * Deletes the DH hologram
     */
    public void removeHologram()
    {
        this.hologram.delete();
    }
}
