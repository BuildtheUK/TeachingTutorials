package teachingtutorials.guis;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialobjects.Tutorial;
import teachingtutorials.utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles tutorial Events. Data fields refer to those of the events table of the database
 */
public class Event
{
    /** The player whom the event relates to */
    private final Player player;

    /** The event type. Defines what the action is */
    private final EventType eventType;

    /** The time that the event was added */
    private final Timestamp timestamp;

    /** Data associated with the event. This could for example refer to a tutorial ID to play when with a Library event */
    private final int iData;

    /**
     * Constructs a new event
     * @param player The player object for the event
     * @param eventType The event type
     * @param timestamp The time that the event was added to the DB
     * @param iData The data of the event - The tutorial ID of the tutorial the event includes a reference to
     */
    public Event(Player player, EventType eventType, Timestamp timestamp, int iData)
    {
        this.player = player;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.iData = iData;
    }

    /** Returns a reference to the player */
    public Player getPlayer()
    {
        return player;
    }

    /** Returns a reference to the event type */
    public EventType getEventType()
    {
        return eventType;
    }

    /**
     * Returns a copy of the data value
     * @return
     */
    public int getData()
    {
        return iData;
    }

    /**
     * Gets a list of the current events in the Events table and remove any duplicate events in the DB
     * @param dbConnection The database connection object
     * @param logger A logger to output errors to
     * @return An ArrayList of all events in the Events table currently
     */
    public static ArrayList<Event> getLatestEvents(DBConnection dbConnection, Logger logger)
    {
        ArrayList<Event> events = new ArrayList<>();

        String sql;
        Statement SQL = null;
        ResultSet resultSet = null;

        boolean bDoNotAdd;

        try
        {
            //Compiles the command to fetch steps
            sql = "Select * FROM Events";
            SQL = dbConnection.getConnection().createStatement();

            //Executes the query
            resultSet = SQL.executeQuery(sql);

            //Goes through all entries in the DB
            while (resultSet.next())
            {
                bDoNotAdd = false;

                //Extracts the event details
                Player player = Bukkit.getPlayer(UUID.fromString(resultSet.getString("UUID")));
                if (player == null)
                    continue;
                int iData = resultSet.getInt("Data");

                EventType eventType;
                String szEventType = resultSet.getString("EventName");
                logger.log(Level.INFO, "Event name = " +szEventType);
                //Handle depreciated value
                if (szEventType.equalsIgnoreCase("COMPULSORY"))
                {
                    logger.log(Level.INFO, "Event detected as compulsory, handling this old mechanism. Note: " +
                            "please ask the author(s) of the event sender plugin to use the new values");

                    //Get compulsory tutorial ID
                    int iCompulsoryTutorialID;

                    Tutorial[] compulsoryTutorials = Tutorial.fetchAll(true, true, null, dbConnection, logger);
                    if (compulsoryTutorials.length == 0)
                        iCompulsoryTutorialID = -1;
                    else
                        iCompulsoryTutorialID = compulsoryTutorials[0].getTutorialID();

                    iData = iCompulsoryTutorialID;
                    eventType = EventType.RESTART_LESSON;
                }
                else
                {
                    eventType = EventType.valueOf(szEventType);
                }
                Timestamp timestamp = resultSet.getTimestamp("Timestamp");

                //Creates an event object to store the details of this entry
                Event event = new Event(player, eventType, timestamp, iData);

                //Go through entire event list and check whether there is already an event found for this player
                for (int i = 0 ; i < events.size() ; i++)
                {
                    Event previousEvent = events.get(i);
                    if (previousEvent.player.getUniqueId().equals(event.player.getUniqueId()))
                    {
                        //If an event for this player already exists, check whether it was added before or after
                        if (previousEvent.timestamp.before(event.timestamp))
                        {
                            //If the old one was added after, delete the old one from the DB
                            previousEvent.remove(dbConnection, logger);

                            //Delete the old one from the list to return
                            events.remove(i);
                        }
                        else
                        {
                            //If the previous event was added after the current one, skip adding this one to the list to return, and remove this from the DB
                            bDoNotAdd = true;

                            //Delete the new one from the DB
                            event.remove(dbConnection, logger);

                            //We do not want to break the loop here because we want to sanitise the whole list
                        }
                    }
                }

                //Add the event to the list to return
                if (!bDoNotAdd)
                    events.add(event);
            }
        }
        catch (SQLException se)
        {
            logger.log(Level.SEVERE, "[TeachingTutorials] - SQL - SQL Error fetching all Events", se);
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "[TeachingTutorials] - SQL - Non SQL Error fetching all Events", e);
        }
        return events;
    }

    /**
     * Removes this event from the database
     * @param dbConnection A database connection to a tutorials database
     * @param logger A logger to output to
     */
    public void remove(DBConnection dbConnection, Logger logger)
    {
        String sql;
        Statement SQL = null;

        int iCount;

        try
        {
            SQL = dbConnection.getConnection().createStatement();

            //Removes the answers
            sql = "Delete FROM Events WHERE UUID = '" +this.player.getUniqueId() +"' AND Timestamp = '"+this.timestamp+"'";
            iCount = SQL.executeUpdate(sql);
        }
        catch (SQLException se)
        {
            logger.log(Level.SEVERE, "SQL - SQL Error deleting event", se);
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "SQL - Non-SQL Error deleting event", e);
        }
    }

    /**
     * Adds an event to the events table
     * @param eventType The event that is to take place
     * @param userUUID The UUID of the player undergoing to event
     * @param iData Any data - refers to the TutorialID of any tutorials the event refers to
     * @param dbConnection The database connection object
     * @param logger A logger to output errors to
     * @return True if the event was successfully added to the DB, false if not
     */
    public static boolean addEvent(EventType eventType, UUID userUUID, int iData, DBConnection dbConnection, Logger logger)
    {
        String sql;
        Statement SQL = null;

        int iCount = 0;

        try
        {
            SQL = dbConnection.getConnection().createStatement();
            sql = "INSERT INTO `Events` (`UUID`,`EventName`,`Data`) VALUES('" +userUUID +"', '"+eventType.toString()+"', "+iData +")";
            iCount = SQL.executeUpdate(sql);

            if (iCount != 1)
            {
                logger.log(Level.WARNING, "[TeachingTutorials] SQL - Update failed, count not equal to 1");
                return false;
            }
        }
        catch (SQLException se)
        {
            logger.log(Level.SEVERE, "[TeachingTutorials] SQL - SQL Error adding event", se);
            return false;
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "[TeachingTutorials] SQL - Non-SQL Error adding event", e);
            return false;
        }
        return true;
    }
}
