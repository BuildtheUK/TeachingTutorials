package teachingtutorials.tutorialobjects;

import org.bukkit.Bukkit;
import org.bukkit.World;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.utils.DBConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a Tutorials Location
 */
public class Location
{
    /** The ID of this location in the DB */
    private int iLocationID;

    /** The ID of the tutorial for which this is a location of */
    private final int iTutorialID;

    /** Whether this location is marked as in use */
    private boolean bInUse;

    /** The name for this location */
    private String szLocationName;

    /** A reference to the bukkit world for this location */
    private World world;

    //--------------------------------------------------
    //-------------------Constructors-------------------
    //--------------------------------------------------
    /**
     * Used for creating a new location
     * @param iTutorialID The ID of the tutorial for which this is a location of
     */
    public Location(int iTutorialID)
    {
        this.iTutorialID = iTutorialID;
    }

    /**
     * Used for loading a location from the DB
     * @param iLocationID The locationID of the location to load from the DB
     * @param iTutorialID The tutorialID of the location's tutorial
     * @param bInUse Whether the Location is in use
     * @param szLocationName The name of the Location
     */
    public Location(int iLocationID, int iTutorialID, boolean bInUse, String szLocationName)
    {
        this.iLocationID = iLocationID;
        this.iTutorialID = iTutorialID;
        this.bInUse = bInUse;
        this.szLocationName = szLocationName;

        //Loads the bukkit world for this location
        this.world = Bukkit.getWorld(this.getLocationID()+"");
    }

    //---------------------------------------------------
    //----------------------Getters----------------------
    //---------------------------------------------------

    /**
     * Care must be taken using this method, as this class may not always be initialised
     *<p>
     * Use Stage.getLocationID() to be safe
     * @return The LocationID
     */
    public int getLocationID()
    {
        return iLocationID;
    }

    /**
     * @return The ID of the Tutorial which this Location is of
     */
    public int getTutorialID()
    {
        return iTutorialID;
    }

    /**
     *
     * @return Whether this location is in use or not
     */
    public boolean isInUse()
    {
        return bInUse;
    }

    public String getLocationName()
    {
        return szLocationName;
    }

    /**
     * Gets the bukkit world for this location if it is not already loaded
     * @return The bukkit world for this location
     */
    public World getWorld()
    {
        if (world == null)
            world = Bukkit.getWorld(iLocationID+"");
        return world;
    }

    //---------------------------------------------------
    //----------------------Setters----------------------
    //---------------------------------------------------
    /**
     * Sets the bukkit world object for this location
     * @param world
     */
    public void setWorld(World world)
    {
        this.world = world;
    }

    //---------------------------------------------------
    //--------------------SQL Fetches--------------------
    //---------------------------------------------------

    /**
     * Searches whether there are any In-Use locations for the Tutorial with the given Tutorial ID
     * @param iTutorialID The tutorial ID of the tutorial for which to fetch the locations for
     * @param dbConnection A DB connection object
     * @param logger A reference to a logger to use
     * @return Whether there are any In-Use locations for the Tutorial with the given Tutorial ID
     */
    public static boolean getAreThereInUseLocationsForTutorial(int iTutorialID, DBConnection dbConnection, Logger logger)
    {
        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch all the in use locations for the tutorial
            sql = "SELECT * FROM `Locations` WHERE `TutorialID` = " +iTutorialID +" AND InUse = 1";
            SQL = dbConnection.getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            return resultSet.next();
        }
        catch (SQLException se)
        {
            logger.log(Level.SEVERE, "SQL - SQL Error fetching location IDs for tutorial with ID: "+iTutorialID, se);
            return false;
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "SQL - Non-SQL Error fetching location IDs for tutorial with ID: "+iTutorialID, e);
            return false;
        }
    }

    /**
     * Fetches a list of Locations for all of the In Use Locations of a given Tutorial
     * @param iTutorialID The tutorial ID of the tutorial for which to fetch the locations for
     * @param dbConnection A DB connection object
     * @return A list of locationIDs
     */
    public static Location[] getAllInUseLocationsForTutorial(int iTutorialID, DBConnection dbConnection, Logger logger)
    {
        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;
        int iLocations = 0;
        Location[] locations;
        int i;

        try
        {
            //Compiles the command to fetch all the in use locations for the tutorial
            sql = "SELECT * FROM `Locations` WHERE `TutorialID` = " +iTutorialID +" AND InUse = 1";
            SQL = dbConnection.getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                iLocations++;
            }

            locations = new Location[iLocations];

            //Move to start and get the location information
            resultSet = SQL.executeQuery(sql);
            for (i = 0 ; i < iLocations ; i++)
            {
                resultSet.next();
                locations[i] = new Location(resultSet.getInt("LocationID"),
                        iTutorialID,
                        resultSet.getBoolean("InUse"),
                        resultSet.getString("LocationName"));
            }
        }
        catch (SQLException se)
        {
            logger.log(Level.SEVERE, "SQL - SQL Error fetching locations for tutorial with ID: "+iTutorialID, se);
            locations = new Location[0];
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "SQL - Non-SQL Error fetching locations for tutorial with ID: "+iTutorialID, e);
            locations = new Location[0];
        }
        return locations;
    }

    /**
     * Fetches a list of all Locations for a tutorial, will all Location information filled
     * @param iTutorialID The tutorial ID of the tutorial for which to fetch the locations for
     * @param dbConnection A DB connection object
     * @param logger A reference to a logger to log output to
     * @return A list of locationIDs
     */
    public static Location[] getAllLocationForTutorial(int iTutorialID, DBConnection dbConnection, Logger logger)
    {
        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;
        int iLocations = 0;
        Location[] locations;
        int i;

        try
        {
            //Compiles the command to fetch all the in use locations for the tutorial
            sql = "SELECT * FROM `Locations` WHERE `TutorialID` = " +iTutorialID;
            SQL = dbConnection.getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);
            while (resultSet.next())
            {
                iLocations++;
            }

            locations = new Location[iLocations];

            //Move to start and get the location information
            resultSet = SQL.executeQuery(sql);
            for (i = 0 ; i < iLocations ; i++)
            {
                resultSet.next();
                locations[i] = new Location(resultSet.getInt("LocationID"),
                                            iTutorialID,
                                            resultSet.getBoolean("InUse"),
                                            resultSet.getString("LocationName"));
            }
        }
        catch (SQLException se)
        {
            logger.log(Level.SEVERE, "SQL - SQL Error fetching locations for tutorial with ID: "+iTutorialID, se);
            locations = new Location[0];
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "SQL - Non-SQL Error fetching locations for tutorial with ID: "+iTutorialID, e);
            locations = new Location[0];
        }
        return locations;
    }

    /**
     * Get the start location of a Location by getting the start location of the first step.
     * @param dbConnection A reference to a database connection
     * @param logger A reference to a logger to which to log output
     * @param iLocationID The ID of the location to get the start location of
     * @return An array containing latitude, longitude, start yaw, start pitch, or null if the location could not be fetched
     */
    public static double[] getStartCoordinatesOfLocation(DBConnection dbConnection, Logger logger, int iLocationID)
    {
        double[] coordinates = null;

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch all the in use locations for the tutorial
            sql = "SELECT Latitude,Longitude,StartYaw,StartPitch FROM `LocationSteps` WHERE `Location` = " +iLocationID +" Order By Step";
            SQL = dbConnection.getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);

            if (resultSet.next())
                coordinates = new double[]{resultSet.getDouble("Latitude"), resultSet.getDouble("Longitude"),
                        resultSet.getDouble("StartYaw"), resultSet.getDouble("StartPitch")};
        }
        catch (SQLException se)
        {
            logger.log(Level.SEVERE, "SQL - SQL Error fetching location start from location steps for location: "+iLocationID, se);
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "SQL - Non-SQL Error fetching location start from location steps for location: "+iLocationID, e);
        }
        return coordinates;
    }


    /**
     * Fetches the Location from the database base with the given LocationID.
     * @param dbConnection A reference to a database connection
     * @param logger A reference to a logger to which to log output
     * @param iLocationID The ID of the location to get the start location of
     * @return A Location containing the information of the Location with the LocationID provided, or null if no location
     * could be found with the LocationID provided
     */
    public static Location getLocationByLocationID(DBConnection dbConnection, Logger logger, int iLocationID)
    {
        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        try
        {
            //Compiles the command to fetch all the in use locations for the tutorial
            sql = "SELECT * FROM `Locations` WHERE `LocationID` = " +iLocationID;
            SQL = dbConnection.getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);

            if (resultSet.next())
                return new Location(iLocationID, resultSet.getInt("TutorialID"), resultSet.getBoolean("InUse"), resultSet.getString("LocationName"));
            else
                return null;

        }
        catch (SQLException se)
        {
            logger.log(Level.SEVERE, "SQL - SQL Error fetching location start from location steps for location: "+iLocationID, se);
            return null;
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "SQL - Non-SQL Error fetching location start from location steps for location: "+iLocationID, e);
            return null;
        }
    }

    //---------------------------------------------------
    //--------------------SQL Updates--------------------
    //---------------------------------------------------

    /**
     * Inserts a new location into the database based on the data in this object
     * @param dbConnection A database connection to a tutorials database
     * @param logger A logger to output to
     * @return Whether the new location was successfully added to the database
     */
    public boolean insertNewLocation(DBConnection dbConnection, Logger logger)
    {
        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        int iCount;

        try
        {
            SQL = dbConnection.getConnection().createStatement();
            sql = "INSERT INTO `Locations` (`TutorialID`) VALUES (" +iTutorialID+")";
            iCount = SQL.executeUpdate(sql);

            if (iCount != 1)
            {
                return false;
            }

            //Gets the LocationID of the newly inserted location
            sql = "SELECT LAST_INSERT_ID()";
            resultSet = SQL.executeQuery(sql);
            resultSet.next();
            iLocationID = resultSet.getInt(1);
            return true;
        }
        catch (SQLException se)
        {
            logger.log(Level.SEVERE, "SQL - SQL Error adding new location", se);
            return false;
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "SQL - Non-SQL Error adding new location", e);
            return false;
        }
    }

    /**
     * Updates the name of this location in the DB
     * @return whether the update was made successfully
     */
    public boolean updateName(DBConnection dbConnection, Logger logger, String szNewName)
    {
        String sql;
        Statement SQL = null;

        int iCount;

        try
        {
            SQL = dbConnection.getConnection().createStatement();
            sql = "UPDATE `Locations` SET LocationName = '" +szNewName  +"' WHERE LocationID = "+iLocationID;
            iCount = SQL.executeUpdate(sql);

            //Updates the value of the name stored in memory
            this.szLocationName = szNewName;

            return iCount == 1;
        }
        catch (SQLException se)
        {
            logger.log(Level.SEVERE, "SQL - SQL Error updating name of location", se);
            return false;
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "SQL - Non-SQL Error updating name of location", e);
            return false;
        }
    }

    /**
     * Changes the boolean value of whether this tutorial is "in-use" or not in the DB. Negates the current value.
     * @param dbConnection A database connection to a tutorials database
     * @param logger A logger to output to
     * @return Whether the update completed
     */
    public boolean toggleInUse(DBConnection dbConnection, Logger logger)
    {
        String sql;
        Statement SQL = null;

        int iCount;

        try
        {
            SQL = dbConnection.getConnection().createStatement();
            if (bInUse)
                sql = "UPDATE `Locations` SET InUse = 0 WHERE LocationID = "+iLocationID;
            else
                sql = "UPDATE `Locations` SET InUse = 1 WHERE LocationID = "+iLocationID;

            iCount = SQL.executeUpdate(sql);

            bInUse = !bInUse;

            return iCount == 1;
        }
        catch (SQLException se)
        {
            logger.log(Level.SEVERE, "SQL - SQL Error updating name of location", se);
            return false;
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "SQL - Non-SQL Error updating name of location", e);
            return false;
        }
    }

    /**
     * Deletes a location from the DB
     * @param iLocationID The location ID of the location to delete
     * @param dbConnection A database connection to a tutorials database
     * @param logger A logger to output to
     * @return Whether the location was successfully deleted
     */
    public static boolean deleteLocationByID(int iLocationID, DBConnection dbConnection, Logger logger)
    {
        logger.log(Level.INFO, "Removing location " +iLocationID +" from the DB");
        String sql;
        Statement SQL = null;

        int iCount;

        try
        {
            SQL = dbConnection.getConnection().createStatement();

            //Removes the answers
            sql = "DELETE FROM `LocationTasks` WHERE `LocationID` = " +iLocationID;
            iCount = SQL.executeUpdate(sql);
            logger.log(Level.INFO, iCount +" LocationTasks were deleted");

            //Removes the location specific step details
            sql = "DELETE FROM `LocationSteps` WHERE `Location` = " +iLocationID;
            iCount = SQL.executeUpdate(sql);
            logger.log(Level.INFO, iCount +" LocationSteps were deleted");

            //Removes the location
            sql = "DELETE FROM `Locations` WHERE `LocationID` = " +iLocationID;
            iCount = SQL.executeUpdate(sql);

            return iCount == 1;
        }
        catch (SQLException se)
        {
            logger.log(Level.SEVERE, "SQL Error deleting location with LocationID = "+iLocationID, se);
            return false;
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "Non-SQL Error deleting location with LocationID = "+iLocationID, e);
            return false;
        }
    }
}
