package pt.ulisboa.tecnico.cmov.positioncoordinates;

public class Compass {
    // Converts the measured azimuth into the bearing (N, S, E, W)
    // Works with degrees (º)
    // Source of info: http://chrisechterling.com/blog/2009/09/07/what-is-the-difference-between-an-azimuth-and-bearing/
    public static int[] convertAzimuthToRotation (float azimuthRadians) {
        int[] bearing = new int[2];

        int azimuthDegrees = (int) Math.round(Math.toDegrees(azimuthRadians));     // Converts the azimuth in radians to degrees

        // +360 for implementations where mod returns negative numbers
        if (azimuthDegrees < 0){
            azimuthDegrees = (azimuthDegrees + 360) % 360;
        }

        //azimuthDegrees = convertIfInLandscape(azimuthDegrees);

        // With 0 to 90 degree azimuth = 0 to 90 degree bearing
        if (azimuthDegrees >= 0 && azimuthDegrees <= 90){
            bearing[0]=1;
            bearing[1]=azimuthDegrees;
        }
        // With 90 to 180 degree azimuth, your bearing = 180 – azimuth
        else if (azimuthDegrees > 90 && azimuthDegrees <= 180){
            bearing[0]=2;
            bearing[1]=180-azimuthDegrees;
        }
        // With 180 to 270 degree azimuth, your bearing =  azimuth – 180
        else if (azimuthDegrees > 180 && azimuthDegrees <= 270){
            bearing[0]=3;
            bearing[1]=azimuthDegrees-180;
        }
        // With 270 to 360 degree azimuth, your bearing = 360 – azimuth
        else if (azimuthDegrees > 270 && azimuthDegrees <= 360){
            bearing[0]=4;
            bearing[1]=360-azimuthDegrees;
        }

        return bearing;
    }

    // Checks if phone is in landscape mode and if so corrects the bearing
    // Works with radians (rad)
    // Partial source: https://stackoverflow.com/questions/4727800/detect-android-orientation-landscape-left-v-landscape-right
    public static float convertIfInLandscape(float azimuth, int deviceOrientation){
        // Divide by 90 into an int to round, then multiply out to one of 5 positions, either 0,90,180,270,360.
        int orientation = 90*Math.round(deviceOrientation / 90);

        // Convert 360 to 0
        if(orientation == 360) {
            orientation = 0;
        }

        // Correct azimuth if in landscape mode
        if (orientation == 90){
            azimuth = (float) (azimuth - (Math.PI/2));   // Top of device points to the right, so subtract π/2

        } else if (orientation == 180){
            azimuth = (float) (azimuth + (Math.PI/2));   // Top of device points to the left, so add π/2

        }

        // If the bearing is in que upper quadrants and the final result is > π or < -π
        if (azimuth < -(Math.PI)){
            azimuth = (float) (azimuth + (2*Math.PI));  // If result is lower than -π, then add 2π

        } else if (azimuth > Math.PI){
            azimuth = (float) (azimuth - (2*Math.PI));  // If result is higher than π, then subtract 2π

        }

        return azimuth;
    }

    // Based on the azimuth information, the user's orientation ias estimated
    // Works with radians (rad)
    static String estimateUserOrientation(float azimuth, float forwardAzimuth){
        float difference = adjustAzimuth(forwardAzimuth, azimuth);

        // The user is oriented to the left of the room
        if (difference < -(Math.PI/8) && difference >= -((3*Math.PI)/4)){
            return "L";
        }
        // The user is oriented to the right of the room
        else if (difference >= (Math.PI/8) && difference < ((3*Math.PI)/4)){
            return "R";
        }
        // The user is oriented to the front of the room
        else if (difference >= -(Math.PI/4) && difference < Math.PI/4) {
            return "F";
        }
        // The user is oriented to the back of the room
        else if (difference >= ((3*Math.PI)/4) || difference < -((3*Math.PI)/4)) {
            return "B";
        }

        return "O";     // The user orientation is not considered
    }

    // Determines the current situation of the user's walking direction
    // If it is the same or it has changed his direction
    // It returns a number that identifies the current situation
    // Works with radians (rad)
    public static int orientationSituation (float azimuth, float turnAzimuth){

        float difference = adjustAzimuth(turnAzimuth, azimuth);

        // The user is walking on a perpendicular aix to the one after the turn (left side)
        if (difference < -((3*Math.PI)/8) && difference > -((5*Math.PI)/8)){
            return 1;
        }
        // The user is walking on a perpendicular aix to the one after the turn (right side)
        else if (difference > ((3*Math.PI)/8) && difference < ((5*Math.PI)/8)){
            return 3;
        }
        // The user has strayed of the original straight path and is walking diagonally to the left
        else if (difference < -(Math.PI/8) && difference > -((3*Math.PI)/8)) {
            return 2;
        }
        // The user has strayed of the original straight path and is walking diagonally to the right
        else if (difference > (Math.PI/8) && difference < ((3*Math.PI)/8)) {
            return 4;
        } else {
            return -1;
        }
    }

    // If the azimuth value is above or below the limits of the defined referential
    // The value has to be adjusted if is greater that π and lower than -π
    // Works with radians (rad)
    public static float adjustAzimuth (float previousAzimuth, float azimuth){

        float azimuthDiff = azimuth - previousAzimuth;      // Calculate the difference between the two recorded instances

        // Checks first if the result is contained in the -π - 0 - π referential
        if (azimuthDiff < -(Math.PI)){
            azimuthDiff = (float) (azimuthDiff + (2*Math.PI));  // If result is lower than -π, then add 2π

        } else if (azimuthDiff > Math.PI){
            azimuthDiff = (float) (azimuthDiff - (2*Math.PI));  // If result is higher than π, then subtract 2π
        }

        return azimuthDiff;
    }

    // Return TRUE if the number is greater than 0 (positive) and FALSE if the number is smaller than 0
    public static boolean positiveOrNegative (float number){
        return number >= 0;
    }

    // If the sign is TRUE (+) then the value will be returned positive, if the sign is FALSE (-) then the value will be returned negative
    static int signToValue(boolean sign, int value){
        return (!sign) ? -1 * value : value;
    }
}
