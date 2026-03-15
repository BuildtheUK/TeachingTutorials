package org.btuk.teachingtutorials.utils;

import java.sql.*;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Represents a database connection
 * @date 10 Aug 2020
 * @time 11:41:25
 * @author Provided by Mr Singh (Gravesend, Kent, UK) and modified by George112n
 */
public class DBConnection
{
    /** The address of the DB connection */
    private String DB_CON;

    /** The IP address of the DB host */
    private String HOST;

    /** The port of the database on the host */
    private int PORT;

    /** The name of the schema to access */
    public String Database;

    /** A username for access */
    private String USER;

    /** A password for the given user */
    private String PASSWORD;

    /** An SQL connection object to the DB */
    private Connection connection = null;

    /** Whether there is an active connection to the DB */
    private boolean bIsConnected;


    // ----------------------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------------------
    public DBConnection()
    {
        reset() ;
        return ;
    }

    //Setters
    private void reset()
    {
        this.bIsConnected = false ;
        return ;
    }

    /**
     * Loads the database connection information from the config
     * @param config A reference to the config of the TeachingTutorials plugin
     */
    public void mysqlSetup(FileConfiguration config)
    {
        this.HOST = config.getString("MySQL_Database_Information.host");
        this.PORT = config.getInt("MySQL_Database_Information.port");
        this.Database = config.getString("MySQL_Database_Information.database");
        this.USER = config.getString("MySQL_Database_Information.username");
        this.PASSWORD = config.getString("MySQL_Database_Information.password");

        this.DB_CON = "jdbc:mysql://" + this.HOST + ":"
                + this.PORT + "/" + this.Database + "?&useSSL=false&";
    }

    /**
     * Loads the database connection information from the given details
     * @param szHost The IP address of the DB host
     * @param iPort The port of the database on the host
     * @param szDatabaseName The name of the schema to access
     * @param szUsername A username for access
     * @param szPassword A password for the given user
     */
    public void externalMySQLSetup(String szHost, int iPort, String szDatabaseName, String szUsername, String szPassword)
    {
        this.HOST = szHost;
        this.PORT = iPort;
        this.Database = szDatabaseName;
        this.USER = szUsername;
        this.PASSWORD = szPassword;

        this.DB_CON = "jdbc:mysql://" + this.HOST + ":"
                + this.PORT + "/" + this.Database + "?&useSSL=false&";
    }

    /**
     * Makes a connection to the database and stores this connection object
     * @return Whether the connection was made successfully
     */
    public boolean connect()
    {
        try
        {
            Bukkit.getLogger().log(Level.INFO, "Connecting to the MySql database");
            DriverManager.getDriver(DB_CON);
            connection = DriverManager.getConnection(DB_CON, USER, PASSWORD);
            if (this.connection != null)
            {
                this.bIsConnected = true;
                return true;
            }
            else
                return false;
        }
        catch (SQLException e)
        {
            if (e.toString().contains("Access denied"))
                Bukkit.getLogger().log(Level.SEVERE, "SQL Error - Access denied", e);
            else if (e.toString().contains("Communications link failure"))
                Bukkit.getLogger().log(Level.SEVERE, "SQL Error - Communications link failure", e);
            else
                Bukkit.getLogger().log(Level.SEVERE, "SQL Error - Other SQLException - "+e.getMessage(), e);
            return false;
        }
        catch (Exception e)
        {
            Bukkit.getLogger().log(Level.SEVERE, "SQL Error - Other Exception whilst connecting to database- "+e.getMessage(), e);
            return false;
        }
    }

    /**
     * Closes the connection to the database
     */
    public void disconnect()
    {
        try
        {
            this.connection.close() ;
            this.bIsConnected = false ;
        }
        catch ( SQLException se )
        {
            Bukkit.getLogger().log(Level.SEVERE, "SQL Error whilst disconnecting from the DB", se);
        }
        catch ( Exception e )
        {
            Bukkit.getLogger().log(Level.SEVERE, "Error whilst disconnecting from the DB", e);
        }
    }

    /**
     * Returns a reference to the connection. If the connection is not valid or null, then it will attempt to make a
     * connection to the database.
     * @return A reference to the connection object
     */
    public Connection getConnection()
    {
        try
        {
            if (connection.isValid(10) && connection != null)
            {

            }
            else
            {
                Bukkit.getLogger().log(Level.INFO, "Not currently connection to the DB, attempting to make a connection");
                if (connect())
                    Bukkit.getLogger().log(Level.INFO, "Connection made successfully");
                else
                    Bukkit.getLogger().log(Level.INFO, "Failed to connect to the DB, see above for potential errors");
            }
        }
        catch (SQLException e)
        {
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred whilst attempting to get the connection to the DB", e);
        }
        return connection;
    }
}