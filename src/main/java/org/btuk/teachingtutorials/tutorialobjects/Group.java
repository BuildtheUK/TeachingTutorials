package org.btuk.teachingtutorials.tutorialobjects;

import org.btuk.teachingtutorials.utils.DBConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a Tutorials Group as stored in the DB
 */
public class Group
{
    /** The database's ID of the group */
    private final int iGroupID;

    /** The tasks forming this group */
    public ArrayList<Task> tasks = new ArrayList<>();

    /**
     * Used to construct a Group from the database
     * @param iGroupID The ID of the group in the DB
     */
    public Group(int iGroupID)
    {
        this.iGroupID = iGroupID;
    }

    /**
     * Constructs a Group object for use whilst creating a new tutorial
     */
    public Group()
    {
        this.iGroupID = -1;
    }

    /**
     * @return A copy of the group ID of this group
     */
    public int getGroupID()
    {
        return this.iGroupID;
    }

    //---------------------------------------------------
    //---------------------- Utils ----------------------
    //---------------------------------------------------
    /**
     * @return whether this group and all of its child tasks are fully filled and ready for adding
     */
    public boolean isComplete()
    {
        //Check whether there are any task
        if (tasks.size() == 0)
            return false;

        //Check the tasks themselves
        boolean bAllTasksComplete = true;
        for (Task task : tasks)
        {
            if (!task.isComplete())
            {
                bAllTasksComplete = false;
                break;
            }
        }

        return bAllTasksComplete;
    }

    /**
     * Inserts this group and all of its child tasks into the database
     * @param dbConnection A DB connection to the tutorials database
     * @param logger A logger to output to
     * @param iStepID The StepID of the step which these groups will belong to
     * @return Whether the group and all of its tasks were successfully added to the database
     */
    public boolean addGroupToDB(DBConnection dbConnection, Logger logger, int iStepID)
    {
        //Catch if group info is not completely set
        if (!isComplete())
        {
            logger.warning("Group information not fully set, aborting insert");
            return false;
        }

        //Add group
        //Notes the ID of the current group object we are within
        int iGroupID;

        String sql;
        Statement SQL = null;
        ResultSet resultSet;

        logger.log(Level.INFO, "Adding group to database");

        //Insert the new group into the Groups table
        try
        {
            SQL = dbConnection.getConnection().createStatement();
            sql = "INSERT INTO `Groups` (`StepID`) VALUES (" +iStepID+")";
            SQL.executeUpdate(sql);

            //Gets the GroupID of the group just inserted
            sql = "Select LAST_INSERT_ID()";
            resultSet = SQL.executeQuery(sql);
            resultSet.next();
            iGroupID = resultSet.getInt(1);
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "Could not insert new group into DB");
            return false;
        }

        //Add the child tasks
        logger.log(Level.INFO, tasks.size()+" tasks in this group");

        boolean bAllTasksAdded = true;
        for (Task newTask : tasks)
        {
            if (!newTask.addTaskToDB(dbConnection, logger, iGroupID))
            {
                bAllTasksAdded = false;
                break;
            }
        }

        if (!bAllTasksAdded)
        {
            //Reverse group addition
            try {
                SQL = dbConnection.getConnection().createStatement();
                sql = "DELETE FROM Groups WHERE GroupID = "+iGroupID;
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
