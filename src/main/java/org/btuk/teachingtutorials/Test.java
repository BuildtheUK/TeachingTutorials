package org.btuk.teachingtutorials;

import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.projection.GeographicProjection;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;

import java.util.Stack;

public class Test
{
    public static void main (String[] args)
    {
//        final GeographicProjection projection = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS).projection();
//        try
//        {
//            double[] longLat = projection.toGeo(2810731+0.5d, -5391084+0.5d);
//            System.out.println(longLat[1]);
//            System.out.println(longLat[0]);
//        }
//        catch (OutOfProjectionBoundsException e)
//        {
//            //Player has selected an area outside of the projection
//            return;
//        }

        Stack<String> stack = new Stack<>();
        stack.push("1");
        stack.push("2");
        stack.push("3");
        stack.push("4");
        stack.push("5");

        stack.push("Hello");

        String[] array = stack.toArray(String[]::new);

        for (int i = array.length-1 ; i >= 0 ; i--)
        {
            System.out.println(array[i]);
        }

        System.out.println();

        System.out.println(stack.pop());
        System.out.println(stack.pop());




    }
}
