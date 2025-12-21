package teachingtutorials.tutorialobjects;

import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorialplaythrough.FundamentalTaskType;
import teachingtutorials.utils.Category;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

/**
 * A type of task where the location information is known
 */
public class LocationTask extends Task
{
    /** The difficulties of the location task, in the same order as the categories in the Category enum */
    private float[] fDifficulties;

    /** The answers of the location task */
    private String szAnswers;

    /** The locationID of the location task */
    private final int iLocationID;

    /** Whether or not the Location task is already saved
     * <p></p>
     * This is used to determine whether to edit or update the location task
     */
    private boolean bIsSaved;

    /**
     * Constructs a location task during a lesson playthrough
     * @param type The type of the task
     * @param iTaskID The ID of the task in the DB
     * @param iOrder The order in which the task should be completed within its parent group
     * @param szDetails The extra details associated with the task
     * @param iLocationID A copy of the locationID for the location with which the Location Task is associated
     * @param szAnswers The answers
     * @param fDifficulties The difficulties in each skills category for this location task
     */
    public LocationTask(FundamentalTaskType type, int iTaskID, int iOrder, String szDetails, int iLocationID, String szAnswers, float[] fDifficulties)
    {
        super(type, iTaskID, iOrder, szDetails);
        this.iLocationID = iLocationID;
        this.szAnswers = szAnswers;
        this.fDifficulties = fDifficulties;
        this.bIsSaved = true;
    }

    /**
     * Constructs a location task during a new location playthrough
     * @param task A reference to the task
     * @param iLocationID A copy of the locationID for the location with which the Location Task is associated
     */
    public LocationTask(Task task, int iLocationID)
    {
        super(task.getType(), task.iTaskID, task.getOrder(), task.getDetails());
        this.iLocationID = iLocationID;

        this.fDifficulties = new float[]{0f, 0f, 0f, 0f, 0f};
        this.bIsSaved = false;
    }

    /**
     * Sets the difficulty in the category specified
     * @param category The category to set the difficulty in
     */
    public void setDifficulty(Category category, float fDifficulty)
    {
        switch (category)
        {
            case tpll -> fDifficulties[0] = fDifficulty;
            case worldedit -> fDifficulties[1] = fDifficulty;
            case colouring -> fDifficulties[2] = fDifficulty;
            case detail -> fDifficulties[3] = fDifficulty;
            case terraforming -> fDifficulties[4] = fDifficulty;
        }
    }

    /**
     * Sets all of the difficulties together
     * @param fDifTpll
     * @param fDifWE
     * @param fDifColour
     * @param fDifDetail
     * @param fDifTerra
     */
    public void setDifficulties(float fDifTpll, float fDifWE, float fDifColour, float fDifDetail, float fDifTerra)
    {
        this.fDifficulties = new float[]{fDifTpll, fDifWE, fDifColour, fDifDetail, fDifTerra};
    }

    /**
     *
     * @return The answer
     */
    public String getAnswer()
    {
        return szAnswers;
    }

    /**
     *
     * @return Gets the difficulty of this location task in the category specified
     */
    public float getDifficulty(Category category)
    {
        switch (category)
        {
            case tpll ->
            {
                return fDifficulties[0];
            }
            case worldedit ->
            {
                return fDifficulties[1];
            }
            case colouring ->
            {
                return fDifficulties[2];
            }
            case detail ->
            {
                return fDifficulties[3];
            }
            //Including terraforming:
            default ->
            {
                return fDifficulties[4];
            }
        }
    }

    /**
     * Sets the answers
     * @param szAnswers The answers to set
     */
    public void setAnswers(String szAnswers)
    {
        this.szAnswers = szAnswers;
    }

    /**
     * Uses the data within this object to create a new location task in the DB
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @return Whether the LocationTask was added successfully
     */
    public boolean storeNewData(TeachingTutorials plugin)
    {
        //Diverts to the update
        if (bIsSaved)
            return updateData(plugin);

        boolean bSuccess = false;

        String sql;
        Statement SQL = null;

        try
        {
            sql = "INSERT INTO `LocationTasks` (`LocationID`, `TaskID`, `Answers`, `TpllDifficulty`, `WEDifficulty`, `ColouringDifficulty`, `DetailingDifficulty`, `TerraDifficulty`) " +
                    "VALUES (" +iLocationID+", " +iTaskID+", '" +szAnswers+"'";

            SQL = plugin.getDBConnection().getConnection().createStatement();

            for (int i = 0 ; i <=4 ; i++)
            {
                sql = sql +", "+fDifficulties[i];
            }

            sql = sql +")";
            SQL.executeUpdate(sql);
            bSuccess = true;
        }
        catch (SQLException se)
        {
            plugin.getLogger().log(Level.SEVERE, "SQL - SQL Error adding LocationTask", se);
            bSuccess = false;
        }
        catch (Exception e)
        {
            plugin.getLogger().log(Level.SEVERE, "Non SQL Error adding LocationTask", e);
            bSuccess = false;
        }
        if (bSuccess)
            bIsSaved = true;
        return bSuccess;
    }

    /**
     * Updates the location task in the DB with the data within this object DB
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     * @return Whether the LocationTask was updated successfully
     */
    public boolean updateData(TeachingTutorials plugin)
    {
        //Diverts to the add
        if (!bIsSaved)
            return storeNewData(plugin);

        boolean bSuccess = false;

        String sql;
        Statement SQL = null;

        try
        {
            SQL = plugin.getDBConnection().getConnection().createStatement();

            sql = "UPDATE `LocationTasks` SET `Answers` = '"+szAnswers+"'"
                    + ", `TpllDifficulty` = " +fDifficulties[0]
                    + ", `WEDifficulty` = " +fDifficulties[1]
                    + ", `ColouringDifficulty` = " +fDifficulties[2]
                    + ", `DetailingDifficulty` = " +fDifficulties[3]
                    + ", `TerraDifficulty` = " +fDifficulties[4];

            sql = sql +" WHERE LocationID = "+this.iLocationID +" AND TaskID = "+this.iTaskID +";";

            SQL.executeUpdate(sql);
            bSuccess = true;
        }
        catch (SQLException se)
        {
            plugin.getLogger().log(Level.SEVERE, "SQL - SQL Error updating LocationTask", se);
            bSuccess = false;
        }
        catch (Exception e)
        {
            plugin.getLogger().log(Level.SEVERE, "Non SQL Error updating LocationTask", e);
            bSuccess = false;
        }
        return bSuccess;
    }
}
