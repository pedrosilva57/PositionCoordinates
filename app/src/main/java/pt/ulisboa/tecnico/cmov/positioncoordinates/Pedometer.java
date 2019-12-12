package pt.ulisboa.tecnico.cmov.positioncoordinates;

public class Pedometer {

    // Calculates the time and value difference between the maximum and minimum found
    // If these values are within limits, then the function return true,
    // considering the signature was caused by a user step
    public static boolean checkMaxAndMinPair (float maxValue, long maxTimestamp, float minValue, long minTimestamp){

        // Calculate the time and value difference between the maximum and minimum
        float valueDifference = maxValue - minValue;
        float timeDifference = minTimestamp - maxTimestamp;

        if ((valueDifference > 4 && valueDifference < 9) && (timeDifference > 60)) {
            return true;
        }

        return false;
    }

    // Checks if the gyroscope latest data is between -1 and 1
    // Return true or false, depending on the values in the array
    // Gyroscope is more sensible to motions on the device so it is used here to discard false positives
    public static boolean ratifyStep(float[] gyroMeasures){
        // The gyroMeasures array created for sensor fusion is used in here
        for (float value : gyroMeasures){
            if (!(value > -0.5 && value < 0.5)){
                return false;
            }
        }
        return true;
    }

    // Estimates the persons step size based on the height and the gender
    // It multiplies the person's height with a constant that's different for man and woman
    // Source of info: https://www.wikihow.com/Measure-Stride-Length
    public static float calculateStepSize (float height, boolean gender){
        // gender==true for males | gender==false for woman
        return (gender) ? height * 0.415f : height * 0.413f;
    }

    // Calculates the number of floor squares the user as walked, based on its direction and step size
    // Length of the side of a floor square measure 0.598 m, and in turn the diagonal measures 0.846 m
    public static int calculateSqaure (float stepSize, boolean diagonal){
        return (diagonal) ? (int) Math.ceil(stepSize / 0.846) : (int) Math.floor(stepSize / 0.598);
    }
}
