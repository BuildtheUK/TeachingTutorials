package org.btuk.teachingtutorials.tutorialobjects;

import org.btuk.teachingtutorials.tutorialplaythrough.FundamentalTaskType;
import org.btuk.teachingtutorials.utils.DBConnection;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Task class represents a Task that a player has to complete. Each type of fundamental task has its own class that
 * extends this. This class holds the information of the task as contained in the Tasks table of the database, and
 * contains database utilities and methods for common processes. It also handles some virtual block mechanics.
 */
public class Task
{
    /**
     * The unique TaskID of this task
     */
    public final int iTaskID;

    /**
     * The type of the task, e.g, "command". Can only be any one of the values of FundamentalTaskType
     */
    private FundamentalTaskType type;

    /**
     * The number of the task in the group. 1 is the first task in the group.
     */
    private int iOrder;

    /**
     * Any extra information about the task as given in the database
     */
    private String szDetails;

    private float fAcceptableDistance;
    private float fPerfectDistance;

    private CommandActionType commandActionType;

    /** Indicates whether the fields are editable or not */
    private final boolean editable;

    /**
     * Constructs a task from the DB
     * @param type The type of the task
     * @param iTaskID The ID of the task as in the DB
     * @param iOrder The order within the group which this task should be completed - 1 indexed
     * @param szDetails The details of the task
     */
    public Task(FundamentalTaskType type, int iTaskID, int iOrder, String szDetails)
    {
        this.type = type;
        this.szDetails = szDetails;
        this.iTaskID = iTaskID;
        this.iOrder = iOrder;
        this.editable = false;
    }

    /**
     * Constructs a task whilst creating a new tutorial
     * @param iOrder The order within the group which this task should be completed - 1 indexed
     */
    public Task(int iOrder)
    {
        this.iTaskID = -1;
        this.iOrder = iOrder;
        this.szDetails = "";
        this.fPerfectDistance = 0.25f;
        this.fAcceptableDistance = 1f;
        this.editable = true;
    }

    public int getTaskID() {
        return iTaskID;
    }

    public FundamentalTaskType getType() {
        return type;
    }

    public int getOrder() {
        return iOrder;
    }

    public String getDetails() {
        return szDetails;
    }

    public float getAcceptableDistance() {
        return fAcceptableDistance;
    }

    public float getPerfectDistance()
    {
        return fPerfectDistance;
    }

    public CommandActionType getCommandActionType()
    {
        return commandActionType;
    }

    public void setType(FundamentalTaskType taskType)
    {
        if (editable)
            this.type = taskType;
    }

    public void setOrder(int iOrder)
    {
        if (editable)
            this.iOrder = iOrder;
    }

    /**
     * Updates the details string based on the task type and details
     */
    private void updateDetails()
    {
        if (editable)
        {
            switch (type)
            {
                case tpll:
                    if (!(fAcceptableDistance < 0 || fPerfectDistance < 0 || fAcceptableDistance < fPerfectDistance))
                        szDetails = fPerfectDistance+";"+fAcceptableDistance;
                    break;
                case command:
                    if (commandActionType != null)
                        szDetails = commandActionType.name();
                    break;
            }
        }
    }

    public void setAcceptableDistance(float fAcceptableDistance)
    {
        if (editable)
            this.fAcceptableDistance = fAcceptableDistance;
    }

    public void setPerfectDistance(float fPerfectDistance)
    {
        if (editable)
            this.fPerfectDistance = fPerfectDistance;
    }

    public void setCommandActionType(CommandActionType commandActionType)
    {
        if (editable)
            this.commandActionType = commandActionType;
    }

    //---------------------------------------------------
    //---------------------- Utils ----------------------
    //---------------------------------------------------
    /**
     * @return whether this task is fully filled and ready for adding
     */
    public boolean isComplete()
    {
        //Check task type
        if (type == null)
            return false;

        //Check order
        if (iOrder < 1)
            return false;

        //Check tpll accuracies if necessary and update details string
        if (type.equals(FundamentalTaskType.tpll))
        {
            if (fAcceptableDistance < 0 || fPerfectDistance < 0 || fAcceptableDistance < fPerfectDistance)
            {
                return false;
            }
            else
                updateDetails();
        }

        //Check command type if necessary and update details string
        if (type.equals(FundamentalTaskType.command))
        {
            if (commandActionType == null)
            {
                return false;
            }
            else
                updateDetails();
        }

        return true;
    }

    /**
     * Inserts this task into the database
     * @param dbConnection A DB connection to the tutorials database
     * @param logger A logger to output to
     * @param iGroupID The GroupID of the group which these tasks will belong to
     * @return Whether the task was successfully added to the database
     */
    public boolean addTaskToDB(DBConnection dbConnection, Logger logger, int iGroupID)
    {
        //Catch if task info not completely set
        if (!isComplete())
        {
            logger.warning("Task information not fully set, aborting insert");
            return false;
        }

        //Add task
        String sql;
        Statement SQL = null;
        ResultSet resultSet;

        logger.log(Level.INFO, "Adding task to database");

        //Insert the new task into the Task table
        try
        {
            //Update the details string
            updateDetails();

            //Insert the task
            SQL = dbConnection.getConnection().createStatement();
            sql = "INSERT INTO `Tasks` (`GroupID`, `TaskType`, `Order`, `Details`)" +
                    " VALUES (" +iGroupID+", '"+type.name()+"', "+iOrder +", '" +szDetails +"')";
            SQL.executeUpdate(sql);
            return true;
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "Could not insert new task into DB", e);
            return false;
        }
    }
}
