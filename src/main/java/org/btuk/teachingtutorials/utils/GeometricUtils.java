package org.btuk.teachingtutorials.utils;

import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.projection.GeographicProjection;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;
import net.buildtheearth.terraminusminus.util.geo.LatLng;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Contains a set of methods for handling geometry
 */
public class GeometricUtils
{
    /** The BTE Earth projection */
    final static GeographicProjection projection = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS).projection();

    /**
     * Calculates the distance between two sets of geographical coordinates
     * @param testCoords The first set of coordinates
     * @param dTargetCoords The second set of coordinates, in (latitude, longitude) order
     * @return The distance in metres between the two sets of coordinates
     */
    public static float geometricDistance(LatLng testCoords, double[] dTargetCoords)
    {
        double dLatitude1 = testCoords.getLat();
        double dLatitude2 = dTargetCoords[0];

        double dLongitude1 = testCoords.getLng();
        double dLongitude2 = dTargetCoords[1];

        //Performs some geometry

        //Radius of the sphere of the Earth in meters
        int iRadius = 6371000;
        double φ1 = dLatitude1 * Math.PI/180; // φ, λ in radians
        double φ2 = dLatitude2 * Math.PI/180;
        double Δφ = (dLatitude2-dLatitude1) * Math.PI/180;
        double Δλ = (dLongitude2-dLongitude1) * Math.PI/180;

        double a = Math.sin(Δφ/2) * Math.sin(Δφ/2) +
                Math.cos(φ1) * Math.cos(φ2) *
                        Math.sin(Δλ/2) * Math.sin(Δλ/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        float fDistance = (float) (iRadius * c); // in metres

        return fDistance;
    }

    /**
     * Converts a set of geographical coordinates to minecraft coordinates and creates a bukkit Location object for this
     * given a bukkit world
     * @param world The world for the Location
     * @param dLatitude The latitude of the location
     * @param dLongitude The longitude of the location
     * @return A location object for the given world and geographical coordinates
     */
    public static Location convertToBukkitLocation(World world, double dLatitude, double dLongitude)
    {
        Location location = null;

        //Coverts the geographical coordinates to mc coordinates
        double[] xz = convertToMCCoordinates(dLatitude, dLongitude);

        if (xz != null)
        {
            //Creates a new location for these coordinates
            location = new Location(world, xz[0], world.getHighestBlockYAt((int) xz[0], (int) xz[1]), xz[1]);
        }
        return location;
    }

    /**
     * Convert a set of geographical coordinates to minecraft coordinates
     * @param dLatitude The latitude of the location
     * @param dLongitude The longitude of the location
     * @return A double array of the minecraft coordinates, in x, z form
     */
    public static double[] convertToMCCoordinates(double dLatitude, double dLongitude)
    {
        double[] xz = null;

        try
        {
            //Converts the coordinates using the projection
            xz = projection.fromGeo(dLongitude, dLatitude);
        }
        catch (OutOfProjectionBoundsException e)
        {
            //Player has selected an area outside of the projection
        }
        return xz;
    }

    /**
     * Convert a set of minecraft coordinates to geographical coordinates
     * @param X The x value of the minecraft coordinates
     * @param Z The z value of the minecraft coordinates
     * @return A double array of the form (longitude, latitude)
     */
    public static double[] convertToGeometricCoordinates(double X, double Z)
    {
        double[] longLat = null;

        try
        {
            longLat = projection.toGeo(X, Z);
        }
        catch (OutOfProjectionBoundsException e)
        {
            //Player has selected an area outside of the projection
        }
        return longLat;
    }

    /**
     * Teleports a player to a given latitude and longitude for a given world
     * @param world The world to teleport the player to
     * @param latitude The latitude of the location to teleport the player to
     * @param longitude The longitude of the location to teleport the player to
     * @param player The player to teleport
     * @return Whether the teleportation completed successfully
     */
    public static boolean tpllPlayer(World world, double latitude, double longitude, Player player)
    {
        try
        {
            //Gets the minecraft coordinates of the earth location
            double[] xz = projection.fromGeo(longitude, latitude);

            //Creates a Bukkit Location object and sets the location
            org.bukkit.Location tpLocation;
            tpLocation = new org.bukkit.Location(world, xz[0], world.getHighestBlockYAt((int) xz[0], (int) xz[1]) + 1, xz[1]);

            //Sets yaw and pitch from the player's current yaw and pitch
            Location playerCurrentLocation = player.getLocation();
            tpLocation.setPitch(playerCurrentLocation.getPitch());
            tpLocation.setYaw(playerCurrentLocation.getYaw());

            //Teleports the player
            player.teleport(tpLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);

            return true;
        }
        catch (OutOfProjectionBoundsException e)
        {
            player.sendMessage(Display.errorText("Coordinates not on the earth"));
            return false;
        }
    }
}
