package org.btuk.teachingtutorials.newlocation;

import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.projection.GeographicProjection;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;
import net.buildtheearth.terraminusminus.projection.dymaxion.BTEDymaxionProjection;

public class ProjectionTester
{
    private static final EarthGeneratorSettings bteGeneratorSettings = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS);
    private static final GeographicProjection projection = bteGeneratorSettings.projection();

    public static void main (String[] args) throws OutOfProjectionBoundsException {
        double latitude = 51.43764;
        double longitude = 0.384391;


        double xz[] = projection.fromGeo(0 , 51);

        System.out.println(xz[0] +", "+xz[1]);
    }


}
