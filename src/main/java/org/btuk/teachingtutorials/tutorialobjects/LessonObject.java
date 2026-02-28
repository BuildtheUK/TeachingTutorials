package org.btuk.teachingtutorials.tutorialobjects;

import org.bukkit.Bukkit;
import org.btuk.teachingtutorials.utils.DBConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a Lesson record from the database. Contains methods for loading Lesson data from the Lessons table
 */
public class LessonObject
{
    /** Holds the LessonID of the lesson as stored in the DB */
    private final int iLessonID;

    private final UUID uuid;

    private final int iTutorialID;

    private final boolean bFinished;

    private final int StageAt;

    private final int StepAt;

    private final int iHighestStageCompleted;

    private final int iHighestStepCompleted;

    /** Contains the location information for this lesson */
    private final Location location;

    /** Contains the tutorial information for this lesson */
    private final Tutorial tutorial;

    public LessonObject(int iLessonID, UUID uuid, int iTutorialID, boolean bFinished, int iStageAt, int iStepAt, int iHighestStage, int iHighestStep, Location location, Tutorial tutorial)
    {
        this.iLessonID = iLessonID;
        this.uuid = uuid;
        this.iTutorialID = iTutorialID;
        this.bFinished = bFinished;
        this.StageAt = iStageAt;
        this.StepAt = iStepAt;
        this.iHighestStageCompleted = iHighestStage;
        this.iHighestStepCompleted = iHighestStep;
        this.location = location;
        this.tutorial = tutorial;
    }

    //---------------------------------------------------
    //--------------------- Getters ---------------------
    //---------------------------------------------------
    public int getStageAt()
    {
        return StageAt;
    }

    public int getStepAt()
    {
        return StepAt;
    }

    public int getHighestStageCompleted()
    {
        return iHighestStageCompleted;
    }

    public int getHighestStepCompleted()
    {
        return iHighestStepCompleted;
    }

    public Location getLocation()
    {
        return location;
    }

    public Tutorial getTutorial()
    {
        return tutorial;
    }

    public int getTutorialID()
    {
        return iTutorialID;
    }

    public int getLessonID()
    {
        return iLessonID;
    }

    public boolean isFinished()
    {
        return bFinished;
    }

    //---------------------------------------------------
    //--------------------SQL Fetches--------------------
    //---------------------------------------------------

    /**
     * Fetches the Lessons of the current in progress lessons for a player.
     * @param playerUUID The UUID of the player to get the Lessons of
     * @param dbConnection A connection to the database
     * @param logger A reference to a plugin logger
     * @return An array of LessonObjects representing the Lessons in the database
     */
    public static LessonObject[] getUnfinishedLessonsOfPlayer(UUID playerUUID, DBConnection dbConnection, Logger logger)
    {
        LessonObject[] lessons;

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        int iNumLessons = 0;
        int i;

        try
        {
            //Sets up the statement
            SQL = dbConnection.getConnection().createStatement();
            sql = "SELECT * FROM `Lessons` " +
                    "JOIN `Locations` ON `Lessons`.LocationID = `Locations`.`LocationID` " +
                    "JOIN `Tutorials` ON `Lessons`.`TutorialID` = `Tutorials`.`TutorialID` " +
                    " WHERE `UUID` = '"+playerUUID+"' AND `Finished` = 0";

            //Executes the query to count number of lessons
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                iNumLessons++;
            }

            if (iNumLessons == 0)
                logger.log(Level.FINE, "An unfinished Lesson could not be found in the DB for user " +playerUUID +" (" + Bukkit.getOfflinePlayer(playerUUID).getName() +")");

            //Initiates the array with length of the number of lessons fetched
            lessons = new LessonObject[iNumLessons];

            //Executes the query again
            resultSet = SQL.executeQuery(sql);
            for (i = 0 ; i < iNumLessons ; i++)
            {
                //Moves to the next lesson
                resultSet.next();

                //Extracts the information
                int iLessonID = resultSet.getInt("LessonID");
                int iTutorialID = resultSet.getInt("TutorialID");
                int iStageAt = resultSet.getInt("StageAt");
                int iStepAt = resultSet.getInt("StepAt");
                int iHighestStepCompleted = resultSet.getInt("HighestStepCompleted");
                int iHighestStageCompleted = resultSet.getInt("HighestStageCompleted");

                //Initialises the location - The Location constructor will fetch the location details as well
                Location location = new Location(resultSet.getInt("Lessons.LocationID"), resultSet.getInt("TutorialID"), resultSet.getBoolean("InUse"),
                        resultSet.getString("LocationName"));

                //Initialises the tutorial
                Tutorial tutorial = new Tutorial(iTutorialID, resultSet.getString("TutorialName"), resultSet.getString("Author"),
                        resultSet.getBoolean("Compulsory"), resultSet.getBoolean("InUse"));

                //Creates the Location object for this entry
                lessons[i] = new LessonObject(iLessonID, playerUUID, iTutorialID, false, iStageAt, iStepAt, iHighestStageCompleted, iHighestStepCompleted, location, tutorial);
            }
        }
        catch(SQLException se)
        {
            logger.log(Level.SEVERE, "Tutorials - SQL Error fetching unfinished lessons for " +playerUUID, se);
            return new LessonObject[0];
        }
        return lessons;
    }

    /**
     * Fetches the Lesson with the given LessonID
     * @param iLessonID The LessonID of the Lesson to fetch from the DB
     * @param dbConnection A connection to the database
     * @param logger A reference to a plugin logger
     * @return A LessonObject for the Lesson with the LessonID specified, or null if no Lesson was found with that LessonID
     */
    public static LessonObject getLessonByLessonID(int iLessonID, DBConnection dbConnection, Logger logger)
    {
        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        int iNumLessons = 0;
        int i;

        try
        {
            //Sets up the statement
            SQL = dbConnection.getConnection().createStatement();
            sql = "SELECT * FROM `Lessons` " +
                    "JOIN `Locations` ON `Lessons`.LocationID = `Locations`.`LocationID` " +
                    "JOIN `Tutorials` ON `Lessons`.`TutorialID` = `Tutorials`.`TutorialID` " +
                    " WHERE `LessonID` = " +iLessonID;

            //Executes the query to count number of lessons
            resultSet = SQL.executeQuery(sql);
            if (!resultSet.next())
            {
                logger.log(Level.INFO, "A Lesson could not be found in the DB with the LessonID " +iLessonID);
                return null;
            }
            else
            {
                //Extracts the information
                int iTutorialID = resultSet.getInt("TutorialID");
                int iStageAt = resultSet.getInt("StageAt");
                int iStepAt = resultSet.getInt("StepAt");
                int iHighestStepCompleted = resultSet.getInt("HighestStepCompleted");
                int iHighestStageCompleted = resultSet.getInt("HighestStageCompleted");
                UUID playerUUID = UUID.fromString(resultSet.getString("UUID"));
                boolean bFinished = resultSet.getBoolean("Finished");

                //Initialises the location - The Location constructor will fetch the location details as well
                Location location = new Location(resultSet.getInt("Lessons.LocationID"), resultSet.getInt("TutorialID"), resultSet.getBoolean("InUse"),
                        resultSet.getString("LocationName"));

                //Initialises the tutorial
                Tutorial tutorial = new Tutorial(iTutorialID, resultSet.getString("TutorialName"), resultSet.getString("Author"),
                        resultSet.getBoolean("Compulsory"), resultSet.getBoolean("InUse"));

                //Creates the Location object for this entry and return
                return new LessonObject(iLessonID, playerUUID, iTutorialID, bFinished, iStageAt, iStepAt, iHighestStageCompleted, iHighestStepCompleted, location, tutorial);
            }

        }
        catch(SQLException se)
        {
            logger.log(Level.SEVERE, "Tutorials - SQL Error fetching lesson with LessonID " +iNumLessons, se);
            return null;
        }
    }
}
