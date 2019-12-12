package pt.ulisboa.tecnico.cmov.positioncoordinates;

import java.util.Map;

public class BeaconArea {

    // Takes the devices and respective RSSI measured to determine the closest beacon
    // Compares the values to identify the maximum
    public static String estimateClosestBeaconsBasedOnRSSI (Map<String, Integer> BleDevices){
        Map.Entry<String, Integer> maxEntry = null;

        // Loop through hashmap in order to find the device with highest RSSI
        for (Map.Entry<String, Integer> entry : BleDevices.entrySet() ) {
            if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0){
                maxEntry = entry;
            }
        }

        return maxEntry.getKey();
    }

    // Calculates the distances between the device and each of the beacons
    // Then compares them and return the MAC address of the closest beacon
    // This is done using the x,y coordinates of both the device and the beacons
    public static String estimateClosestBasedOnDistance (Map<String, int[]> BleDevices, int[] userPosition){
        String beacon="";
        float minDistance=-1;

        for (Map.Entry<String, int[]> entry : BleDevices.entrySet()){
            float distance = distanceBetweenTwoPoints(entry.getValue(), userPosition);
            if (minDistance==-1 || distance < minDistance){
                beacon = entry.getKey();
                distance = distance;
            }
        }

        return beacon;
    }

    // Calculates the distance between two points (x1,y1) and (x2,y2)
    // Formula: d = ((x1-x2)^2 + (y1-y2)^2)^(1/2))
    private static float distanceBetweenTwoPoints (int[] pointOne, int[] pointTwo){
        float sum=0;
        for (int i=0; i<2; i++){
            sum+=Math.pow(pointOne[i]-pointTwo[i], 2);
        }

        return (float) Math.sqrt(sum);
    }

}


