package pt.ulisboa.tecnico.cmov.positioncoordinates;

import android.app.Application;
import android.content.Context;

public class GlobalClass extends Application {
    // Pedometer
    private int numberSteps=0;
    private boolean gender;
    private float stepSize;

    // Direction change | Turn detection
    private boolean straightWalk;
    private boolean turnDirection;

    // Geographic orientation
    private float azimuth;  // in radians

    // Activity
    private Context context;

    public GlobalClass() {}

    public int getNumberSteps() {
        return numberSteps;
    }

    public void setNumberSteps(int numberSteps) {
        this.numberSteps = numberSteps;
    }

    public boolean isGender() {
        return gender;
    }

    public void setGender(boolean g) {
        gender=g;
    }


    public void setHeight(float height) {
        stepSize = Pedometer.calculateStepSize(height, gender);
    }

    public float getStepSize() {
        return stepSize;
    }

    public void setStepSize(float stepSize) {
        this.stepSize = stepSize;
    }

    public boolean isStraightWalk() {
        return straightWalk;
    }

    public void setStraightWalk(boolean straight) {
        this.straightWalk = straight;
    }

    public boolean isTurnDirection() {
        return turnDirection;
    }

    public void setTurnDirection(boolean turnDirection) {
        this.turnDirection = turnDirection;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public float getAzimuth() {
        return azimuth;
    }

    public void setAzimuth(float azimuth) {
        this.azimuth = azimuth;
    }
}
