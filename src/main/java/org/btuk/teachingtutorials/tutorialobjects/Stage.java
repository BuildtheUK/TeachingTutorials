package org.btuk.teachingtutorials.tutorialobjects;

import org.btuk.teachingtutorials.utils.DBConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a stage in the Tutorials system
 */
public class Stage
{
    /** The ID of the stage as in the DB */
    protected int iStageID;

    /** The order in which this stage is played within the Tutorial */
    private int iOrder;

    /** The name of the stage */
    private String szName;

    /** Indicates whether the fields are editable or not */
    private final boolean editable;

    //Used when creating a new tutorial
    /** The steps forming this stage */
    public ArrayList<Step> steps = new ArrayList<>();

    /**
     * Constructs a stage from an entry in the database
     * @param iStageID The ID of the stage as in the DB
     * @param iOrder The order in which this stage is completed within the tutorial
     * @param szName The name of the stage
     */
    public Stage(int iStageID, int iOrder, String szName)
    {
        this.iStageID = iStageID;
        this.iOrder = iOrder;
        this.szName = szName;
        this.editable = false;
    }

    /**
     * Constructs a Stage object for use whilst creating a new tutorial
     * @param szName The name of the stage
     * @param iOrder The order in which this stage is completed within the tutorial
     */
    public Stage(String szName, int iOrder)
    {
        this.iStageID = -1;
        this.iOrder = iOrder;
        this.szName = szName;
        this.editable = true;
    }

    /**
     *
     * @return A copy of the stage ID of the stage
     */
    public int getStageID()
    {
        return iStageID;
    }

    /**
     *
     * @return A copy of the order in which this stage is completed within it's tutorial
     */
    public int getOrder()
    {
        return iOrder;
    }

    /**
     *
     * @return A copy of the name of the stage
     */
    public String getName()
    {
        return szName;
    }


    public void setStageID(int iStageID)
    {
        if (editable)
            this.iStageID = iStageID;
    }

    public void setName(String szName)
    {
        if (editable)
            this.szName = szName;
    }

    public void setOrder(int iOrder)
    {
        if (editable)
            this.iOrder = iOrder;
    }

    //---------------------------------------------------
    //---------------------- Utils ----------------------
    //---------------------------------------------------

    /**
     * @return whether this stage and all of its child steps are fully filled and ready for adding
     */
    public boolean isComplete()
    {
        //Check name
        if (szName.equals(""))
            return false;

        //Check order
        if (iOrder < 1)
            return false;

        //Check whether there are any steps
        if (steps.size() == 0)
            return false;

        //Check the steps themselves
        boolean bAllStepsComplete = true;
        for (Step step : steps)
        {
            if (!step.isComplete())
            {
                bAllStepsComplete = false;
                break;
            }
        }

        return bAllStepsComplete;
    }

    /**
     * Inserts this stage and all of its child steps, groups and tasks into the database
     * @param dbConnection A DB connection to the tutorials database
     * @param logger A logger to output to
     * @param iTutorialID The TutorialID of the tutorial which these stages will belong to
     * @return Whether the stage and all of its children were successfully added to the database
     */
    public boolean addStageToDB(DBConnection dbConnection, Logger logger, int iTutorialID)
    {
        //Catch if stage info not completely set
        if (!isComplete())
        {
            logger.warning("Stage information not fully set, aborting insert");
            return false;
        }

        //Add stage
        //Notes the ID of the current stage object we are within
        int iStageID;

        String sql;
        Statement SQL = null;
        ResultSet resultSet;

        logger.log(Level.INFO, "Adding stage to database");

        //Insert the new stage into the Stages table
        try
        {
            SQL = dbConnection.getConnection().createStatement();
            sql = "INSERT INTO `Stages` (`StageName`, `TutorialID`, `Order`) VALUES ('"+ getName().replace("'", "\\'")+"', "+iTutorialID+", "+iOrder+")";
            SQL.executeUpdate(sql);

            //Gets the StageID of the stage just inserted
            sql = "Select LAST_INSERT_ID()";
            resultSet = SQL.executeQuery(sql);
            resultSet.next();
            iStageID = resultSet.getInt(1);
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "Could not insert new stage into DB. Name: "+szName, e);
            return false;
        }


        //Add the child steps
        logger.log(Level.INFO, steps.size()+" steps in this stage");

        boolean bAllStepsAdded = true;
        for (Step newStep : steps)
        {
            if (!newStep.addStepToDB(dbConnection, logger, iStageID))
            {
                bAllStepsAdded = false;
                break;
            }
        }

        if (!bAllStepsAdded)
        {
            //Reverse stage addition
            try {
                SQL = dbConnection.getConnection().createStatement();
                sql = "DELETE FROM Stages WHERE StageID = "+iStageID;
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
