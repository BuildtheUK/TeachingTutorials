package teachingtutorials.tutorialobjects;

import teachingtutorials.TeachingTutorials;
import teachingtutorials.utils.DBConnection;
import teachingtutorials.utils.Display;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a Tutorials Step as stored in the DB
 */
public class Step
{
    /** The name of the step */
    private String szName;

    /** The ID of the step in the DB */
    private final int iStepID;

    /** The position of the step within the stage */
    private int iStepInStage;

    /** How the instructions should be displayed for this step */
    private final Display.DisplayType instructionDisplayType;

    /** Indicates whether the fields are editable or not */
    private final boolean editable;

    /** The groups forming this step */
    public ArrayList<Group> groups = new ArrayList<>();


    /**
     * Constructs a step from data in the database
     * @param iStepID The stepID as in the database
     * @param iStepInStage The position of this step within the parent stage, 1 indexed
     * @param szStepName The name of the step
     * @param szInstructionDisplayType How the instructions should be displayed for this step - must be a value of
     *                                 Display.DisplayType
     */
    public Step(int iStepID, int iStepInStage, String szStepName, String szInstructionDisplayType, Logger logger)
    {
        this.iStepID = iStepID;
        this.iStepInStage = iStepInStage;
        this.szName = szStepName;

        //Extract the display type and store
        Display.DisplayType displayType;
        try
        {
            displayType = Display.DisplayType.valueOf(szInstructionDisplayType);
        }
        catch (IllegalArgumentException e)
        {
            logger.log(Level.SEVERE, "The step instruction display type was not properly specified ("+szInstructionDisplayType +"), reverting to chat", e);
            displayType = Display.DisplayType.chat;
        }
        this.instructionDisplayType = displayType;

        this.editable = false;

    }

    /**
     * Constructs a Step object for use whilst creating a new tutorial
     * @param szName The name of the step
     * @param iOrder The position of the step within the parent stage, 1 indexed
     * @param instructionDisplayType The method for displaying the instructions
     */
    public Step(String szName, int iOrder, Display.DisplayType instructionDisplayType)
    {
        this.iStepID = -1;
        this.iStepInStage = iOrder;
        this.szName = szName;
        this.instructionDisplayType = instructionDisplayType;

        this.editable = true;
    }


    /**
     * @return A copy of the name of this step
     */
    public String getName()
    {
        return szName;
    }

    /**
     * @return A copy of the step ID of this step
     */
    public int getStepID()
    {
        return iStepID;
    }

    /**
     * @return A copy of the instruction display type for this step
     */
    public Display.DisplayType getInstructionDisplayType()
    {
        return this.instructionDisplayType;
    }

    public int getStepInStage()
    {
        return iStepInStage;
    }

    public void setName(String szName)
    {
        if (editable)
            this.szName = szName;
    }

    public void setStepInStage(int iStepInStage)
    {
        if (editable)
            this.iStepInStage = iStepInStage;
    }

    //---------------------------------------------------
    //---------------------- Utils ----------------------
    //---------------------------------------------------
    /**
     * @return whether this step and all of its child groups are fully filled and ready for adding
     */
    public boolean isComplete()
    {
        //Check name
        if (szName.equals(""))
            return false;

        //Check order
        if (iStepInStage < 1)
            return false;

        //Check whether there are any groups
        if (groups.size() == 0)
            return false;

        //Check whether instructions type is set
        if (instructionDisplayType == null)
            return false;

        //Check the groups themselves
        boolean bAllGroupsComplete = true;
        for (Group group : groups)
        {
            if (!group.isComplete())
            {
                bAllGroupsComplete = false;
                break;
            }
        }

        return bAllGroupsComplete;
    }

    /**
     * Inserts this step and all of its child groups and tasks into the database
     * @param dbConnection A DB connection to the tutorials database
     * @param logger A logger to output to
     * @param iStageID The StageID of the stage which these steps will belong to
     * @return Whether the step and all of its children were successfully added to the database
     */
    public boolean addStepToDB(DBConnection dbConnection, Logger logger, int iStageID)
    {
        //Catch if step info not completely set
        if (!isComplete())
        {
            logger.warning("Step information not fully set, aborting insert");
            return false;
        }

        //Add step
        //Notes the ID of the current step object we are within
        int iStepID;

        String sql;
        Statement SQL = null;
        ResultSet resultSet;

        logger.log(Level.INFO, "Adding step to database");

        //Insert the new step into the Steps table
        try
        {
            SQL = dbConnection.getConnection().createStatement();
            sql = "INSERT INTO `Steps` (`StepName`, `StageID`, `StepInStage`, `InstructionDisplay`) VALUES ('"+ szName.replace("'", "\\'")+"', "+iStageID+", "+iStepInStage+",'" +instructionDisplayType.name() +"')";
            SQL.executeUpdate(sql);

            //Gets the StepID of the step just inserted
            sql = "Select LAST_INSERT_ID()";
            resultSet = SQL.executeQuery(sql);
            resultSet.next();
            iStepID = resultSet.getInt(1);
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "Could not insert new step into DB. Name: "+szName, e);
            return false;
        }


        //Add the child groups
        logger.log(Level.INFO, groups.size()+" groups in this step");

        boolean bAllGroupsAdded = true;
        for (Group newGroup : groups)
        {
            if (!newGroup.addGroupToDB(dbConnection, logger, iStepID))
            {
                bAllGroupsAdded = false;
                break;
            }
        }

        if (!bAllGroupsAdded)
        {
            //Reverse step addition
            try {
                SQL = dbConnection.getConnection().createStatement();
                sql = "DELETE FROM Steps WHERE StepID = "+iStepID;
                SQL.executeUpdate(sql);
            }
            catch (SQLException se)
            {
            }
            return false;
        }
        else
            return true;
    }
}
