package pt.ulisboa.tecnico.cmov.positioncoordinates;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import static pt.ulisboa.tecnico.cmov.positioncoordinates.Compass.signToValue;
import static pt.ulisboa.tecnico.cmov.positioncoordinates.Pedometer.calculateSqaure;

public class PositionEstimationService extends Service {
    private GlobalClass globalClass;

    // User position
    private int x, y;

    // Direction/orientation
    private boolean currSign;

    public PositionEstimationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Global Class instance
        globalClass = (GlobalClass) getApplicationContext();

        return super.onStartCommand(intent, flags, startId);
    }

    // **************************************** DETERMINE AND SOLVE DIRECTION SITUATION *****************************************

    // Based on whether the axis the user was walking, the turn direction and the previous direction, it is estimated how the user position will be updated
    // It is determined the axis the user is walking and the sign to use to update the position
    public void determineDirectionChangeParameters (boolean direction){
        boolean straight, sign;

        // User is walking on the y axis
        if (globalClass.isStraightWalk()){
            if (direction){
                sign=true;      // User is walking on +y
            } else {
                sign=false;     // User is walking on -y
            }
            straight = false;    // User is now walking on the x aix
        }
        // User is walking on the x axis
        else {
            if (direction){
                sign = false;   // User is walking on -x
            } else {
                sign = true;    // User is walking on +x
            }
            straight=true;      // User is now walking on the x aix
        }


        // If the previous direction was -x or -y, then that affects the sign
        if (!currSign){
            sign = !sign;
        }

        // Update values
        globalClass.setStraightWalk(straight);
        globalClass.setTurnDirection(direction);
        currSign = sign;
    }

    // ********************************************* PEDOMETER POSITION ESTIMATION **********************************************

    // Based on the last turn detected, the x, y position is estimated
    // The direction of the turn and the relation to the aixs in the museum referential is given by the notifyDirectionChange() method
    public void estimateCurrentPositionBasedOnTurnDetection (){
        //boolean sign = globalClass.isSign();

        int square = calculateSqaure(globalClass.getStepSize(), false);

        if (globalClass.isStraightWalk()){
            y+=signToValue(currSign, square);
        } else {
            x+=signToValue(currSign, square);
        }
    }

    // Based on the change of the user direction, the x,y position is estimated
    // The new direction is given by comparing the current azimuth value with the one saved when the user last turned
    public void estimateCurrentPositionBasedOnCompass (float azimuth, boolean signum){

        // Related to the position update
        int square = calculateSqaure(globalClass.getStepSize(), true);  // Estimates how many floor squares the user has walked

        // Update x,y position
        if (globalClass.isStraightWalk()){
            y+=signToValue(currSign, square);
            // For the cases where the previous direction was negative
            if (!currSign){
                signum =! signum;
            }
            x+=signToValue(signum, square);
        } else {
            x+=signToValue(currSign, square);
            // For the cases where the previous direction was negative
            if (currSign){
                signum =! signum;
            }
            y+=signToValue(signum, square);
        }
    }
}
