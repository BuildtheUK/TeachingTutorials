package teachingtutorials.tutorialobjects;

import teachingtutorials.TeachingTutorials;
import teachingtutorials.utils.Category;
import teachingtutorials.utils.DBConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a tutorial of the Tutorials system
 */
public class Tutorial
{
    public static final String[] szCategoryEnumsInOrder = new String[]{"tpll", "worldedit", "colouring", "detail", "terraforming"};
    private static final int iCategories = szCategoryEnumsInOrder.length;

    /** The ID of the tutorial as in the DB */
    private final int iTutorialID;

    /** The name of the tutorial */
    private String szTutorialName;

    /** The UUID of the author of the tutorial */
    private final UUID authorUUID;

    /** Whether this tutorial is compulsory */
    private boolean bCompulsory;

    /** Whether this tutorial is in use */
    private boolean bInUse;

    /** The relevance of this tutorial to each skill */
    private final int[] categoryUsage = new int[Category.values().length];

    //Used when creating a new tutorial
    /** The stages forming this tutorial */
    public ArrayList<Stage> stages = new ArrayList<>();

    //--------------------------------------------------
    //-------------------Constructors-------------------
    //--------------------------------------------------
    /**
     * Constructs a tutorial object from information in the DB
     * @param iTutorialID The ID of the tutorial in the DB
     * @param szTutorialName The name of the tutorial
     * @param authorUUID The uuid of the author of the tutorial
     * @param bCompulsory Whether the tutorial is compulsory
     * @param bInUse Whether the tutorial is in use
     */
    public Tutorial(int iTutorialID, String szTutorialName, String authorUUID, boolean bCompulsory, boolean bInUse)
    {
        this.iTutorialID = iTutorialID;
        this.szTutorialName = szTutorialName;
        this.authorUUID = UUID.fromString(authorUUID);
        this.bCompulsory = bCompulsory;
        this.bInUse = bInUse;
    }

    /**
     * Constructs a tutorial object for use whilst creating a new tutorial
     * @param szTutorialName The name of the tutorial
     * @param authorUUID The uuid of the author of the tutorial
     */
    public Tutorial(String szTutorialName, String authorUUID)
    {
        this.iTutorialID = -1;
        this.szTutorialName = szTutorialName;
        this.authorUUID = UUID.fromString(authorUUID);
        this.bCompulsory = false;
        this.bInUse = false;
    }


    //---------------------------------------------------
    //----------------------Getters----------------------
    //---------------------------------------------------
    public int getTutorialID()
    {
        return iTutorialID;
    }

    public String getTutorialName()
    {
        return szTutorialName;
    }

    public UUID getUUIDOfAuthor()
    {
        return authorUUID;
    }

    public boolean isCompulsory()
    {
        return bCompulsory;
    }

    public boolean isInUse()
    {
        return bInUse;
    }
    /**
     *
     * @param iCategoryIndex 0 indexed
     * @return
     */
    public int getCategoryUsage(int iCategoryIndex)
    {
        if (iCategoryIndex > iCategories-1)
            return 0;
        else
            return categoryUsage[iCategoryIndex];
    }

    //---------------------------------------------------
    //----------------------Setters----------------------
    //---------------------------------------------------
    public void setTutorialName(String szTutorialName)
    {
        this.szTutorialName = szTutorialName;
    }

    //---------------------------------------------------
    //---------------------- Utils ----------------------
    //---------------------------------------------------

    /**
     * @return whether this tutorial and all of its child stages are fully filled and ready for adding
     */
    public boolean isComplete()
    {
        //Check name
        if (szTutorialName.equals(""))
            return false;

        //Check whether there are any stages
        int iNumStages = stages.size();
        if (iNumStages == 0)
            return false;

        //Check the stages themselves
        boolean bAllStagesComplete = true;
        for (Stage stage : stages)
        {
            if (!stage.isComplete())
            {
                bAllStagesComplete = false;
                break;
            }
        }
        return bAllStagesComplete;
    }

    //---------------------------------------------------
    //--------------------SQL Fetches--------------------
    //---------------------------------------------------

    /**
     * Fetches all of the Tutorials in the DB which satisfy the provided conditions, loads them with all relevant data.
     * @param bInUseOnly Whether to only select in use locations
     * @param bCompulsoryOnly Whether to only select compulsory locations
     * @param uuid The UUID of the user to select tutorials for, leave null to avoid this condition
     * @param dbConnection A DB connection
     * @return A list of Tutorials, with all information loaded
     */
    public static Tutorial[] fetchAll(boolean bInUseOnly, boolean bCompulsoryOnly, UUID uuid, DBConnection dbConnection, Logger logger)
    {
        //Declare variables
        Tutorial[] tutorials;

        int iCount = 0;

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;
        ResultSet resultSet2 = null;

        boolean bWhereClausePresent = false;

        try
        {
            //Compiles the command to fetch tutorials
            if (bInUseOnly && bCompulsoryOnly)
            {
                bWhereClausePresent = true;
                sql = "SELECT * FROM `Tutorials` WHERE `Tutorials`.`InUse` = 1 AND `Tutorials`.`Compulsory` = 1";
            }
            else if (bInUseOnly)
            {
                bWhereClausePresent = true;
                sql = "SELECT * FROM `Tutorials` WHERE `Tutorials`.`InUse` = 1";
            }
            else if (bCompulsoryOnly)
            {
                bWhereClausePresent = true;
                sql = "SELECT * FROM `Tutorials` WHERE `Tutorials`.`Compulsory` = 1";
            }
            else
                sql = "SELECT * FROM `Tutorials`";

            //Add the user condition
            if (uuid != null)
            {
                if (bWhereClausePresent)
                    sql = sql +" AND `Tutorials`.`Author` = \'"+uuid.toString()+"\'";
                else
                    sql = "SELECT * FROM `Tutorials` WHERE `Tutorials`.`Author` = \'"+uuid.toString()+"\'";
            }

            SQL = dbConnection.getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);

            //Counts the amount of tutorials
            while (resultSet.next())
            {
                iCount++;
            }

            //Initiates the array with length of the number of tutorials fetched
            tutorials = new Tutorial[iCount];

            //Executes the query again
            resultSet = SQL.executeQuery(sql);
            for (int i = 0 ; i < iCount ; i++)
            {
                //Moves to the next tutorial
                resultSet.next();

                //Creates the tutorial object for this entry
                tutorials[i] = new Tutorial(resultSet.getInt("TutorialID"), resultSet.getString("TutorialName"),
                        resultSet.getString("Author"), resultSet.getBoolean("Compulsory"),
                        resultSet.getBoolean("InUse"));
            }

            //Compiles the command to fetch category relevancies
            if (bInUseOnly)
                sql = "SELECT `CategoryPoints`.`TutorialID`,`Category`,`CategoryPoints`.`Relevance` FROM `Tutorials` JOIN `CategoryPoints` ON `Tutorials`.`TutorialID`=`CategoryPoints`.`TutorialID` WHERE `Tutorials`.`InUse` = 1";
            else
                sql = "SELECT `CategoryPoints`.`TutorialID`,`Category`,`CategoryPoints`.`Relevance` FROM `Tutorials` JOIN `CategoryPoints` ON `Tutorials`.`TutorialID`=`CategoryPoints`.`TutorialID`";

            SQL = dbConnection.getConnection().createStatement();

            //Executes the query
            resultSet2 = SQL.executeQuery(sql);

            //Adds the category points
            addCategoryPointsToTutorials(tutorials, resultSet2);
        }
        catch (SQLException se)
        {
            logger.log(Level.SEVERE, "SQL - SQL Error fetching all in use tutorials", se);
            tutorials = new Tutorial[0];
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "SQL - Non-SQL Error fetching all in use tutorials", e);
            tutorials = new Tutorial[0];
        }
        return tutorials;
    }

    /**
     *
     * @return All in use tutorials which have at least one location
     */
    public static Tutorial[] getInUseTutorialsWithLocations(DBConnection dbConnection, Logger logger)
    {
        int iAvailableTutorials;
        int iNumInUseTutorials;
        int i;

        //Fetches all in use tutorials
        Tutorial[] allInUseTutorials = Tutorial.fetchAll(true, false, null, dbConnection, logger);
        iNumInUseTutorials = allInUseTutorials.length;

        //Boolean array storing whether each tutorial has at least one location
        boolean[] tutorialHasLocation = new boolean[iNumInUseTutorials];

        //Counts the amount of in use tutorials with at least one location
        iAvailableTutorials = 0;
        for (i = 0 ; i < iNumInUseTutorials ; i++)
        {
            //Determines whether tutorial i has any locations
            tutorialHasLocation[i] = Location.getAreThereInUseLocationsForTutorial(allInUseTutorials[i].getTutorialID(), dbConnection, logger);

            if (tutorialHasLocation[i])
            {
                iAvailableTutorials++;
            }
        }

        //A list of all tutorials which are in use and have at least one location
        Tutorial[] allAvailableTutorials = new Tutorial[iAvailableTutorials];

        //Compiles a list of just tutorials with locations
        iAvailableTutorials = 0;
        for (i = 0 ; i < iNumInUseTutorials ; i++)
        {
            if (tutorialHasLocation[i])
            {
                allAvailableTutorials[iAvailableTutorials] = allInUseTutorials[i];
                iAvailableTutorials++;
            }
        }
        return allAvailableTutorials;
    }

    /**
     * Fetches a tutorial by a tutorial ID and loads it with the relevant data
     * @param iTutorialID The ID of the tutorial to fetch
     * @param dbConnection A DB connection
     * @return A tutorials object loaded with all relevant data, or null if the tutorial couldn't be fetched
     */
    public static Tutorial fetchByTutorialID(int iTutorialID, DBConnection dbConnection, Logger logger)
    {
        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        Tutorial tutorial;

        try
        {
            //Compiles the command to fetch the tutorial
            sql = "SELECT * FROM `Tutorials` WHERE `Tutorials`.`TutorialID` = " +iTutorialID;

            SQL = dbConnection.getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);

            //Moves to the next tutorial
            if (resultSet.next())
            {
                //Creates the tutorial object for this entry
                tutorial = new Tutorial(resultSet.getInt("TutorialID"), resultSet.getString("TutorialName"),
                        resultSet.getString("Author"), resultSet.getBoolean("Compulsory"),
                        resultSet.getBoolean("InUse"));

                //Gets the skill use scores
                Tutorial[] tutorials = new Tutorial[]{tutorial};

                sql = "SELECT `CategoryPoints`.`TutorialID`,`Category`,`CategoryPoints`.`Relevance` FROM `Tutorials` JOIN `CategoryPoints` ON `Tutorials`.`TutorialID`=`CategoryPoints`.`TutorialID`";

                SQL = dbConnection.getConnection().createStatement();

                //Executes the query
                resultSet = SQL.executeQuery(sql);

                addCategoryPointsToTutorials(tutorials, resultSet);
                return tutorial;
            }
            else
            {
                logger.log(Level.WARNING, "Error fetching by TutorialID: Could not find a Tutorial with an ID of " +iTutorialID);
                return null;
            }
        }
        catch (SQLException se)
        {
            logger.log(Level.SEVERE, "SQL Error fetching tutorial details by tutorial id: " +iTutorialID, se);
            return null;
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "SQL Error fetching tutorial details by tutorial id: " +iTutorialID, e);
            return null;
        }
    }

    /**
     * Extracts the category points from a results set add adds them to the list of tutorials
     * @param tutorials A list of tutorials to add category points to
     * @param resultSet An SQL results set containing category information
     * @throws SQLException
     */
    private static void addCategoryPointsToTutorials(Tutorial[] tutorials, ResultSet resultSet) throws SQLException
    {
        //Goes through each entry - there will be 5 per tutorial
        while (resultSet.next())
        {
            int i;
            int iCount = tutorials.length;
            //For each entry, go through the list of tutorials are work out which tutorial it is relevant to
            for (i = 0 ; i < iCount ; i++)
            {
                if (tutorials[i].iTutorialID == resultSet.getInt("CategoryPoints.TutorialID"))
                {
                    switch (resultSet.getString("Category"))
                    {
                        case "tpll":
                            tutorials[i].categoryUsage[0] = resultSet.getInt("CategoryPoints.Relevance");
                            break;
                        case "we":
                            tutorials[i].categoryUsage[1] = resultSet.getInt("CategoryPoints.Relevance");
                            break;
                        case "colouring":
                            tutorials[i].categoryUsage[2] = resultSet.getInt("CategoryPoints.Relevance");
                            break;
                        case "detail":
                            tutorials[i].categoryUsage[3] = resultSet.getInt("CategoryPoints.Relevance");
                            break;
                        case "terraforming":
                            tutorials[i].categoryUsage[4] = resultSet.getInt("CategoryPoints.Relevance");
                            break;
                        default:
                    }
                    break;
                }
            }
        }
    }

    //---------------------------------------------------
    //--------------------SQL Updates--------------------
    //---------------------------------------------------

    /**
     * Changes the boolean value of whether a tutorial is compulsory or not. If was compulsory, will set it to false.
     * If it was not compulsory, will set it to true.
     * Will ensure that only one tutorial is compulsory at a time by setting all others to not compulsory if it is changed to compulsory
     */
    public void toggleCompulsory(TeachingTutorials plugin)
    {
        //Declare variables
        String szSql;
        Statement SQL;

        //Attempts to update the database
        try
        {
            SQL = plugin.getDBConnection().getConnection().createStatement();
            if (this.bCompulsory)
                szSql = "UPDATE `Tutorials` SET `Compulsory` = 0 WHERE `TutorialID` = "+ this.iTutorialID;
            else
            {
                setAllTutorialsNotCompulsory(plugin);
                szSql = "UPDATE `Tutorials` SET `Compulsory` = 1 WHERE `TutorialID` = "+ this.iTutorialID;
            }
            SQL.executeUpdate(szSql);
            plugin.getLogger().log(Level.INFO, "Set tutorial "+this.iTutorialID +" as compulsory");
            //Updates the value of the boolean stored in memory
            this.bCompulsory = !this.bCompulsory;
        }
        catch (SQLException se)
        {
            plugin.getLogger().log(Level.SEVERE, "SQL - SQL error toggling tutorial compulsory-ness", se);
        }
        catch (Exception e)
        {
            plugin.getLogger().log(Level.SEVERE, "SQL - Non-SQL error toggling tutorial compulsory-ness", e);
        }
    }

    /**
     * Sets all tutorials as not compulsory in the DB
     */
    private static void setAllTutorialsNotCompulsory(TeachingTutorials plugin)
    {
        //Declare variables
        String szSql;
        Statement SQL;

        try
        {
            SQL = plugin.getDBConnection().getConnection().createStatement();
            szSql = "UPDATE `Tutorials` SET `Compulsory` = 0";
            SQL.executeUpdate(szSql);
        }
        catch (SQLException se)
        {
            plugin.getLogger().log(Level.SEVERE, "SQL - SQL error setting all tutorials not compulsory", se);
        }
        catch (Exception e)
        {
            plugin.getLogger().log(Level.SEVERE, "SQL - Non-SQL error setting all tutorials not compulsory", e);
        }
    }

    /**
     * Changes the boolean value of whether this tutorial is "in-use" or not in the DB. Negates the current value.
     * @return Whether it was changed successfully
     */
    public boolean toggleInUse(TeachingTutorials plugin)
    {
        //Declare variables
        String szSql;
        Statement SQL;

        try
        {
            //Updates the database
            SQL = plugin.getDBConnection().getConnection().createStatement();
            if (this.bInUse)
            {
                szSql = "UPDATE `Tutorials` SET `InUse` = 0 WHERE `TutorialID` = "+ this.iTutorialID;
            }
            else if (Location.getAreThereInUseLocationsForTutorial(this.iTutorialID, plugin.getDBConnection(), plugin.getLogger()))
            {
                szSql = "UPDATE `Tutorials` SET `InUse` = 1 WHERE `TutorialID` = "+ this.iTutorialID;
            }
            else
            {
                return false;
            }

            //Updates the database
            SQL.executeUpdate(szSql);

            //Updates the value of the boolean stored in memory
            this.bInUse = !this.bInUse;
        }
        catch (SQLException se)
        {
            plugin.getLogger().log(Level.SEVERE, "SQL - SQL error toggling the in use status", se);
        }
        catch (Exception e)
        {
            plugin.getLogger().log(Level.SEVERE, "SQL - Non-SQL error toggling the in use status", e);
        }
        return true;
    }

    /**
     * Updates the name of this tutorial in the DB
     * @return whether the update was made successfully
     */
    public boolean updateName(DBConnection dbConnection, Logger logger, String szNewName)
    {
        //Declare variables
        String szSql;
        Statement SQL;

        try
        {
            //Updates the database
            SQL = dbConnection.getConnection().createStatement();
            szSql = "UPDATE `Tutorials` SET `TutorialName` = '"+szNewName +"' WHERE `TutorialID` = "+ this.iTutorialID;

            //Updates the database
            SQL.executeUpdate(szSql);

            //Updates the value of the name stored in memory
            this.szTutorialName = szNewName;
        }
        catch (SQLException se)
        {
            logger.log(Level.SEVERE, "SQL - SQL error updating tutorial name", se);
            return false;
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "SQL - Non-SQL error updating tutorial name", e);
            return false;
        }
        return true;
    }

    /**
     * Inserts this tutorial and all of its child stages, steps, groups and tasks into the database
     * @param dbConnection A db connection
     * @param logger A reference to a logger to output to
     * @return Whether the tutorial was fully added successfully
     */
    public boolean addTutorialToDB(DBConnection dbConnection, Logger logger)
    {
        //Catch if tutorial info not completely set
        if (!isComplete())
        {
            logger.warning("Tutorial information not fully set, aborting insert");
            return false;
        }

        //Add tutorial
        //Notes the ID of the current tutorial objects we are within
        int iTutorialID;

        String sql;
        Statement SQL = null;
        ResultSet resultSet;

        logger.log(Level.INFO, "Adding tutorial to database");

        //Insert the new tutorial into the tutorials table
        try
        {
            SQL = dbConnection.getConnection().createStatement();
            sql = "INSERT INTO `Tutorials` (`TutorialName`, `Author`) VALUES ('"+szTutorialName.replace("'", "\\'")+"', '"+authorUUID +"')";
            SQL.executeUpdate(sql);

            //Gets the TutorialID of the tutorial just inserted
            sql = "Select LAST_INSERT_ID()";
            resultSet = SQL.executeQuery(sql);
            resultSet.next();
            iTutorialID = resultSet.getInt(1);
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "Could not insert new tutorial into DB. Name: "+szTutorialName, e);
            return false;
        }

        //Add the relevances into the DB
        int i;
        for (i = 0 ; i < 5 ; i++)
        {
            try
            {
                sql = "INSERT INTO `CategoryPoints` (`TutorialID`, `Category`, `Relevance`) VALUES (" + iTutorialID + ", '" + szCategoryEnumsInOrder[i] + "', " +((float) getCategoryUsage(i))/100f+ ")";
                SQL.executeUpdate(sql);
            }
            catch (Exception e)
            {
                logger.log(Level.SEVERE, "Could not insert relevances into DB. Name: "+szTutorialName, e);
                //Reverse tutorial addition
                try {
                    SQL = dbConnection.getConnection().createStatement();
                    sql = "DELETE FROM Tutorials WHERE TutorialID = "+iTutorialID;
                    SQL.executeUpdate(sql);
                }
                catch (SQLException se)
                {
                    return false;
                }
                break;
            }
        }

        //Add the child stages
        logger.log(Level.INFO, stages.size()+" stages in this tutorial");

        boolean bAllStagesAdded = true;
        for (Stage newStage : stages)
        {
            if (!newStage.addStageToDB(dbConnection, logger, iTutorialID))
            {
                bAllStagesAdded = false;
                break;
            }
        }

        if (!bAllStagesAdded)
        {
            //Reverse tutorial addition
            try {
                SQL = dbConnection.getConnection().createStatement();
                sql = "DELETE FROM Tutorials WHERE TutorialID = "+iTutorialID;
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
