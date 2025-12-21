package teachingtutorials.tutorialobjects;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialplaythrough.StepPlaythrough;
import teachingtutorials.utils.DBConnection;
import teachingtutorials.utils.GeometricUtils;
import teachingtutorials.utils.Display;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds step data specific to a location and contains some procedures related to utilising this data
 */
public class LocationStep
{
    //Data stored in the LocationSteps table in the DB:

    /** A reference to the step of this LocationStep */
    private final Step step;

    /** A reference to the location of this LocationStep */
    private final Location location;

    /** The geographical start coordinates of the LocationStep */
    private double dStartLatitude, dStartLongitude;
    /** The camera angles for the start location */
    private float fStartYaw, fStartPitch;

    /** The instructions for this LocationStep */
    private String szInstructions;

    /** The coordinates for the instructions hologram of this LocationStep */
    private double dHologramLocationX, dHologramLocationY, dHologramLocationZ;

    /** Stores a link to the video walkthrough tutorial for this LocationStep */
    private String szVideoWalkthroughLink;

    //Used when creating a new location:

    /** Whether or not the start location has been set yet - when creating a new location */
    private boolean bLocationSet;

    /** Whether or not the instructions have been set yet - when creating a new location */
    private boolean bInstructionsSet;

    /** Whether or not the hologram location has been set yet - when creating a new location */
    private boolean bHologramLocationSet;

    /** Whether or not the Location step is already saved
     * <p></p>
     * This is used to determine whether to edit or update the location step
     */
    private boolean bIsSaved;

    /**
     * Creates the location step from the Location and Step
     * @param location A reference to the location of this LocationStep
     * @param step A reference to the step of this LocationStep
     */
    public LocationStep(Location location, Step step, boolean bIsSaved, Logger logger)
    {
        this.bIsSaved = bIsSaved;
        this.location = location;
        this.step = step;
        this.szInstructions = "";
        this.szVideoWalkthroughLink = "";

        bLocationSet = false;
        bInstructionsSet = false;
        boolean bHologramNeeded = step.getInstructionDisplayType().equals(Display.DisplayType.hologram);

        //If a hologram is not needed then we set this to true, indicating that this is effectively 'done' already
        bHologramLocationSet = !bHologramNeeded;
        //If hologram needed then we set this to false as the location is not set yet

        if (bHologramLocationSet)
            logger.log(Level.FINE, "Hologram location is set because no hologram is needed");
    }

    /**
     *
     * @return A reference to the step of this LocationStep
     */
    public Step getStep()
    {
        return this.step;
    }

    /**
     *
     * @return A copy of the instructions of the hologram
     */
    public String getInstructions()
    {
        return this.szInstructions;
    }

    /**
     *
     * @return A copy of the video link
     */
    public String getVideoWalkthroughLink()
    {
        return this.szVideoWalkthroughLink;
    }


    /**
     * Checks whether all of the extra information is set - the start location, the instructions and the
     * hologram instructions
     * @return Whether all of the extra information is set for a LocationStep in a NewLocation context
     */
    public boolean isOtherInformationSet(Logger logger)
    {
        boolean bAllExtraInformationIsSet = (bLocationSet && bInstructionsSet) && bHologramLocationSet;
        if (bAllExtraInformationIsSet)
            logger.log(Level.INFO, "All extra information is set");
        return bAllExtraInformationIsSet;
    }

    public boolean isLocationSet()
    {
        return bLocationSet;
    }

    public boolean isHologramLocationSet()
    {
        return bHologramLocationSet;
    }


    //------------------------------------------------
    //--------------------Database--------------------
    //------------------------------------------------

    /**
     * Accesses the DB and fetches the information about the step location
     * @param step The tutorials step of the LocationStep
     * @param location The tutorials location of the LocationStep
     * @param dbConnection A database connection to a tutorials database
     * @param logger A logger to output to
     * @return A LocationStep object with the details of the LocationStep for the inputted step and location
     */
    public static LocationStep getFromStepAndLocation(Location location, Step step, DBConnection dbConnection, Logger logger)
    {
        LocationStep locationStep = new LocationStep(location, step, true, logger);

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch the location step
            sql = "SELECT * FROM `LocationSteps` WHERE `Step` = "+step.getStepID() +" AND `Location` = " +location.getLocationID();
            SQL = dbConnection.getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            if (resultSet.next())
            {
                //Extracts and stores the data
                locationStep.dStartLatitude = resultSet.getDouble("Latitude");
                locationStep.dStartLongitude = resultSet.getDouble("Longitude");
                locationStep.fStartYaw = resultSet.getFloat("StartYaw");
                locationStep.fStartPitch = resultSet.getFloat("StartPitch");
                locationStep.szInstructions = resultSet.getString("Instructions");
                locationStep.dHologramLocationX = resultSet.getDouble("InstructionsX");
                locationStep.dHologramLocationY = resultSet.getDouble("InstructionsY");
                locationStep.dHologramLocationZ = resultSet.getDouble("InstructionsZ");
                locationStep.szVideoWalkthroughLink = resultSet.getString("VideoWalkthroughLink");
            }
        }
        catch (SQLException se)
        {
            logger.log(Level.SEVERE, "SQL - SQL Error fetching Steps by StageID", se);
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "SQL - Non-SQL Error fetching Steps by StageID", e);
        }
        return locationStep;
    }

    /**
     * Adds the location step to the DB
     * @param dbConnection A database connection to a tutorials database
     * @param logger A logger to output to
     * @return Whether the details were added successfully
     */
    public boolean storeDetailsInDB(DBConnection dbConnection, Logger logger)
    {
        //Diverts to the update
        if (bIsSaved)
            return updateDetailsInDB(dbConnection, logger);

        //Sanitise the instructions
        String szNewInstructions = szInstructions.replace("\\'", "'");
        szNewInstructions = szNewInstructions.replace("'", "\\'");

        String sql;
        Statement SQL = null;

        int iCount;

        try
        {
            SQL = dbConnection.getConnection().createStatement();
            sql = "INSERT INTO `LocationSteps` (`Location`, `Step`, `Latitude`, `Longitude`, `StartYaw`, `StartPitch`, `Instructions`, `InstructionsX`, `InstructionsY`, `InstructionsZ`, `VideoWalkthroughLink`) VALUES ("
                    + location.getLocationID() +", "
                    + step.getStepID() +", "
                    + dStartLatitude +", "
                    + dStartLongitude +", "
                    + fStartYaw +", "
                    + fStartPitch +", '"
                    + szNewInstructions +"', "
                    + dHologramLocationX +", "
                    + dHologramLocationY +", "
                    + dHologramLocationZ +", '"
                    + szVideoWalkthroughLink +"'"
                    +")";
            iCount = SQL.executeUpdate(sql);

            this.bIsSaved = true;
            return iCount == 1;
        }
        catch (SQLException se)
        {
            logger.log(Level.SEVERE, "SQL - SQL Error adding new location step", se);
            return false;
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "SQL - Non-Sql error adding new location step", e);
            return false;
        }
    }

    /**
     * Updates the details in the database with the information in this object
     * @param dbConnection A database connection to a tutorials database
     * @param logger A logger to output to
     * @return Whether the database update was successful
     */
    private boolean updateDetailsInDB(DBConnection dbConnection, Logger logger)
    {
        //Diverts to the add
        if (!bIsSaved)
            return storeDetailsInDB(dbConnection, logger);

        //Sanitise the instructions
        String szNewInstructions = szInstructions.replace("\\'", "'");
        szNewInstructions = szNewInstructions.replace("'", "\\'");

        String sql;
        Statement SQL = null;

        int iCount;

        try
        {
            SQL = dbConnection.getConnection().createStatement();

            sql = "UPDATE `LocationSteps` SET `Latitude` = "+dStartLatitude
                    + ", `Longitude` = " +dStartLongitude
                    + ", `StartYaw` = " +fStartYaw
                    + ", `StartPitch` = " +fStartPitch
                    + ", `Instructions` = '" +szNewInstructions+"'"
                    + ", `InstructionsX` = " +dHologramLocationX
                    + ", `InstructionsY` = " +dHologramLocationY
                    + ", `InstructionsZ` = " +dHologramLocationZ
                    + ", `VideoWalkthroughLink` = '" +szVideoWalkthroughLink+"'"
            +" WHERE Location = "+this.location.getLocationID() +" AND Step = "+this.step.getStepID()+";";

            iCount = SQL.executeUpdate(sql);

            return iCount == 1;
        }
        catch (SQLException se)
        {
            logger.log(Level.SEVERE, "SQL - SQL Error updating new location step", se);
            return false;
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "SQL - Non-Sql error updating new location step", e);
            return false;
        }
    }

    //-----------------------------------------------
    //---------------------Utils---------------------
    //-----------------------------------------------

    /**
     * Teleports the player to the start of the step
     * @param player The player to teleport
     * @param world The world for the relevant location
     * @return The start location of the step
     */
    public org.bukkit.Location teleportPlayerToStartOfStep(Player player, World world, TeachingTutorials plugin)
    {
        org.bukkit.Location location = getStartLocation(world);
        if (location != null)
        {
            //Teleports the player
            org.bukkit.Location finalLocation = location;
            Bukkit.getScheduler().runTask(plugin, () -> player.teleport(finalLocation));
        }
        else
        {
            location = player.getLocation();
            player.sendMessage(ChatColor.RED +"No start location for this step has been set yet");
        }

        return location;
    }

    /**
     * Sets the step start location
     * @param location The location of the intended step start location
     * @param dbConnection A database connection to a tutorials database
     * @param logger A logger to output to
     */
    public void setStartLocation(org.bukkit.Location location, DBConnection dbConnection, Logger logger)
    {
        double[] longLat = GeometricUtils.convertToGeometricCoordinates(location.getX(), location.getZ());

        if (longLat != null)
        {
            this.dStartLongitude = longLat[0];
            this.dStartLatitude = longLat[1];

            this.fStartPitch = location.getPitch();
            this.fStartYaw = location.getYaw();

            bLocationSet = true;
        }

        if (bIsSaved)
            updateDetailsInDB(dbConnection, logger);
    }

    /**
     *
     * @return The start location for the step as a bukkit object
     */
    private org.bukkit.Location getStartLocation(World world)
    {
        org.bukkit.Location location = GeometricUtils.convertToBukkitLocation(world, dStartLatitude, dStartLongitude);

        if (location != null)
        {
            location.setY(location.getY() + 1);
            location.setYaw(fStartYaw);
            location.setPitch(fStartPitch);
        }
        return location;
    }

    /**
     * Sets the location of the instructions hologram and displays the instructions to the creator
     * @param stepPlaythrough A reference to the StepPlaythrough managing this change
     * @param player The player to which the location must be set to
     * @param szStepName The name of the step, so that the instructions can then be displayed
     * @param dbConnection A database connection to a tutorials database
     * @param logger A logger to output to
     */
    public void setHologramLocationToThatOfPlayer(StepPlaythrough stepPlaythrough, Player player, String szStepName, DBConnection dbConnection, Logger logger)
    {
        //Sets the location
        org.bukkit.Location playerLocation = player.getLocation();
        this.dHologramLocationX = playerLocation.getX();
        this.dHologramLocationY = playerLocation.getY();
        this.dHologramLocationZ = playerLocation.getZ();

        //Displays the instructions
        stepPlaythrough.removeInstructionsHologram();
        stepPlaythrough.displayInstructions(Display.DisplayType.hologram, player, szStepName, player.getWorld());

        this.bHologramLocationSet = true;

        if (bIsSaved)
            updateDetailsInDB(dbConnection, logger);
    }

    /**
     *
     * @return The instructions' hologram location for the step as a bukkit object
     */
    public org.bukkit.Location getHologramLocation(World world)
    {
        final org.bukkit.Location hologramLocation = new org.bukkit.Location(world, dHologramLocationX, dHologramLocationY, dHologramLocationZ);
        return hologramLocation;
    }

    /**
     * Sets the instructions of the step and displays the instructions
     * @param stepPlaythrough A reference to the StepPlaythrough managing this change
     * @param szInstructions The desired instructions
     * @param displayType The display type, so the instructions can be displayed
     * @param player The player, so the instructions can be displayed
     * @param szStepName The player, so the instructions can be displayed
     * @param dbConnection A database connection to a tutorials database
     * @param logger A logger to output to
     */
    public void setInstruction(StepPlaythrough stepPlaythrough, String szInstructions, Display.DisplayType displayType,
                               Player player, String szStepName, DBConnection dbConnection, Logger logger)
    {
        //Sets the instructions
        this.szInstructions = szInstructions;

        //Displays the instructions
        stepPlaythrough.removeInstructionsHologram();
        stepPlaythrough.displayInstructions(displayType, player, szStepName, player.getWorld());

        //Instructions may be blank at this point, but this is fine and is displayed blank on the hologram

        this.bInstructionsSet = true;

        if (bIsSaved)
            updateDetailsInDB(dbConnection, logger);
    }

    /**
     * Sets the video walkthrough link for this LocationStep
     * @param szLink The desired link
     */
    public void setVideoLink(String szLink)
    {
        //Sanitise the instructions
        szLink = szLink.replace("'", "\\'");

        this.szVideoWalkthroughLink = szLink;
    }

    /**
     * Displays a message with a link embedded of the video tutorial for this LocationStep to the player, or message
     * notifying the user that there is no video available.
     * @param player The player to send the link to
     */
    public void displayVideoLink(Player player)
    {
        TextComponent linkMessage;

        if (szVideoWalkthroughLink.equals(""))
            linkMessage = Component.text("There is no video available for this step, sorry", NamedTextColor.GREEN);
        else
        {
            linkMessage = Component.text("Click here to access a video walk-through for this step !", NamedTextColor.GREEN);

            //Create the click event
            ClickEvent openLinkEvent = ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, this.szVideoWalkthroughLink);

            //Add the click event to the message
            linkMessage = linkMessage.clickEvent(openLinkEvent);
        }
        //Sends the message
        player.sendMessage(linkMessage);
    }

    /**
     *
     * @return Whether a link is available for this step
     */
    public boolean isLinkAvailable()
    {
        return !szVideoWalkthroughLink.isEmpty();
    }
}
