package pt.ulisboa.tecnico.cmov.positioncoordinates;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static pt.ulisboa.tecnico.cmov.positioncoordinates.Compass.estimateUserOrientation;
import static pt.ulisboa.tecnico.cmov.positioncoordinates.Compass.signToValue;
import static pt.ulisboa.tecnico.cmov.positioncoordinates.Pedometer.calculateSqaure;
import static pt.ulisboa.tecnico.cmov.positioncoordinates.ReadAndWriteFiles.fileReader;
import static pt.ulisboa.tecnico.cmov.positioncoordinates.ReadAndWriteFiles.writeToFile;

public class MainActivity extends AppCompatActivity implements SensorService.Callbacks, BeaconsService.Callbacks {

    // Pedometer
    private int numberSteps = 0;

    // Walk orientation
    private float azimuthValue;      // Saves the azimuth values returned by the SensorService. Used in setting the azimuth value for the walk.

    private float mainAzimuth;      // Stores the azimuth registered when the user is facing the front direction of the room
    private float turnAzimuth;     // Has to be set to the entrance measured azimuth (current value set for experiment purposes)
    private float savedAzimuth;   // Stores the azimuth value when the user has taken a turn (normal or diagonal)

    private boolean straightWalk=true;    // Indicates if the user is walking parallel to the y axis


    // Turn detection
    private boolean currSign = true;
    private boolean turnDetected = false;       // Indicates if the user has taken a turn
    private boolean diagonalDirection;         // Indicates the direction of the user's diagonal turn
    private boolean diagonalWalk = false;

    // Position (Pedometer based)
    private int x = 0, y = 0;            // User position estimation
    private int realX = 0, realY = 0;    // Actual x, y position of the user
    private String direction="O";      // User's actual direction: L- left, R - right, F - forward, B - backwards
    private static Timer stepTimer;    // Timer for detecting when the user stops

    // Bluetooth Low Energy devices detected and respective measured RSSI
    private Map<String, Integer> BLeDevices = new HashMap<>();          // Stores the device and measured RSSI
    private ArrayList<String> bleAddress = new ArrayList<>();     // Stores the addresses of all the BLE beacons
    private boolean bleWorking = false;     // Indicates if the BLE scanning is working

    // Server communication and position correction
    private boolean waitingResponse=false;      // Indicates if the app is waiting for a response from the serve
    private int dispX=0, dispY=0;       // While waiting for response, if the user walks, his displacement is saved here

    // Views
    TextView numberStepsText;
    TextView pedometerPositionText;

    TextView realUserPositionText;

    EditText heightEdit;
    Switch genderSwitch;
    Button saveButton;

    private GlobalClass globalClass;
    private Context context = this;

    // Related to experiments
    private StringBuilder infoStr = new StringBuilder();
    boolean recording = false;
    private SimpleDateFormat formatter;   // To get the date and time of the experiment

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String header = "TYPE,TIME,R_X,R_Y,R_DIR,E_X,E_Y,E_DIR,BLE1,BLE2,BLE3,BLE4,ACCEL_X,ACCEL_Y,ACCEL_Z,GYRO_X,GYRO_Y,GYRO_Z,MAG_X,MAG_Y,MAG_Z,AZT";
        infoStr.append(header).append("\n");

        // UI related
        setUpUIViews();     // UI setup
        setOnClickListenersForExperiments();    // Button OnCLickListeners (Experiment related)

        // Global Class
        globalClass = (GlobalClass) getApplicationContext();
        //globalClass.setStraightWalk(true);   // User starts by walking in the y aix, that is straight
        globalClass.setContext(this);       // Set context for use in BeaconsServices (necessary to check permissions)

        loadBleDevicesArray();
        servicesStartUp();      // Services startup

        formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm");       // To name the experiment results file
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    // Initializes the various view components in the UI
    public void setUpUIViews() {
        // TextViews
        numberStepsText = findViewById(R.id.numberStepsTextView);
        pedometerPositionText = findViewById(R.id.pedometerTextView);
        //closestBeaconText = findViewById(R.id.closestBeaconTextView);
        realUserPositionText = findViewById(R.id.realPosition);

        // Interaction views (EditTexts, Switch, Button)
        heightEdit = findViewById(R.id.heightEditText);
        genderSwitch = findViewById(R.id.genderSwitch);
        saveButton = findViewById(R.id.saveButton);

        // Button OnClickListeners (Height and gender related)
        onClickListenerForSwitch();
        onClickListenerForButton();
        onClickListenerForAzimuthButton();
    }

    // ************************************************* ON CLICK LISTENERS ************************************************

    // Button OnCLickListeners (Experiment related)
    public void setOnClickListenersForExperiments() {
        findViewById(R.id.recordButton).setOnClickListener(listenerRegisterPosition);
        findViewById(R.id.saveFileButton).setOnClickListener(listenerSaveToFile);
        findViewById(R.id.startExpButton).setOnClickListener(startExpButtonListener);
        findViewById(R.id.defaultSettingsButton).setOnClickListener(listenerDeafultSettingsButton);

        // x coordinate
        findViewById(R.id.xAdd).setOnClickListener(addXButtonListener);
        findViewById(R.id.xSub).setOnClickListener(subXButtonListener);
        // y coordinate
        findViewById(R.id.yAdd).setOnClickListener(addYButtonListener);
        findViewById(R.id.ySub).setOnClickListener(subYButtonListener);

        // Directions
        findViewById(R.id.up).setOnClickListener(forwardButtonListener);
        findViewById(R.id.down).setOnClickListener(backwardButtonListener);
        findViewById(R.id.left).setOnClickListener(leftButtonListener);
        findViewById(R.id.right).setOnClickListener(rightButtonListener);
    }

    // Listener for the gender switch
    // If selected the user is a female (false), else the user is a male (true)
    public void onClickListenerForSwitch() {
        genderSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    globalClass.setGender(false);
                } else {
                    globalClass.setGender(true);
                }
            }
        });
    }

    // Listener for save info button
    public void onClickListenerForButton() {
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                globalClass.setHeight(Float.valueOf(String.valueOf(heightEdit.getText())));
                Log.d("Position", "Height= " + Float.valueOf(String.valueOf(heightEdit.getText())) + "  Step size=" + globalClass.getStepSize());
            }
        });
    }

    // Listener for set azimuth button
    public void onClickListenerForAzimuthButton() {
        findViewById(R.id.setAzimuthButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnAzimuth = azimuthValue;
                mainAzimuth = azimuthValue;

                // For experiments
                String measure = "SET," + System.currentTimeMillis() + ",";
                for (int k = 0; k < 19; k++) {
                    measure = measure + "0,";
                }
                infoStr.append(measure + mainAzimuth + "\n");

                Toast.makeText(MainActivity.this, "Initial azimuth value set to " + azimuthValue + ".", Toast.LENGTH_LONG).show();
            }
        });
    }

    // ************************************************* SERVICE CONNECTION ****************************************************

    // Starts up the services and binds them so as to allow communication between the service and the activity
    private void servicesStartUp() {
        // SensorService start up
        Intent sensorServiceIntent = new Intent(context, SensorService.class);
        startService(sensorServiceIntent);

        // BeaconService start up
        Intent beaconsServiceIntent = new Intent(context, BeaconsService.class);
        startService(beaconsServiceIntent);

        // Service binding
        getApplicationContext().bindService(sensorServiceIntent, sensorServiceConnection, Context.BIND_AUTO_CREATE);
        getApplicationContext().bindService(beaconsServiceIntent, beaconServiceConnection, Context.BIND_AUTO_CREATE);

    }

    private ServiceConnection sensorServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            // We've binded to LocalService, cast the IBinder and get LocalService instance
            SensorService.LocalBinder binder = (SensorService.LocalBinder) service;
            SensorService sensorService = binder.getServiceInstance();
            sensorService.registerClient(MainActivity.this); //Activity register in the service as client for callabcks!
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d("SensorService", "Service disconnected");
        }
    };

    private ServiceConnection beaconServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            // We've binded to LocalService, cast the IBinder and get LocalService instance
            BeaconsService.LocalBinder binderOne = (BeaconsService.LocalBinder) service;
            BeaconsService beaconsService = binderOne.getServiceInstance();
            beaconsService.registerClient(MainActivity.this);   //Activity register in the service as client for callabcks!
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("BeaconService", "Service disconnected");
        }
    };

    // INTERFACE METHODS IMPLEMENTED ******************************************

    // The user has taken a step
    @Override
    public void stepDetected() {
        numberSteps++;      // Increment the number of steps taken
        numberStepsText.setText(String.valueOf(numberSteps));       // Update TextView with the new number of total steps taken

        // The main azimuth value is step when the user takes his first step
        if (numberSteps==1){
            mainAzimuth = azimuthValue;
            turnAzimuth = azimuthValue;
            savedAzimuth = azimuthValue;
        } else {
            cancelStepTimer();
        }

        if (!(globalClass.getStepSize()==0.0f)) {
            estimateNewUserPosition(azimuthValue);       // Estimates the new user position
        }

        savedAzimuth = azimuthValue;
        setStepTimer();
    }

    // The user has changed direction
    @Override
    public void notifyDirectionChange(boolean direction, float azimuth) {
        turnDetected = true;
        diagonalWalk = false;

        determineDirectionChangeParameters(direction);
    }

    // The azimuth value has changed
    @Override
    public void updateAzimuth(long timestamp, float azimuth) {
        azimuthValue = azimuth;

        if (recording) {
            String measure = "RAW," + timestamp + ",";
            //infoStr.append("RAW,").append(timestamp).append(",");
            for (int k = 0; k < 19; k++) {
                measure = measure + "0,";
                //infoStr.append("0,");
            }
            infoStr.append(measure + azimuth + "\n");
        }
    }

    @Override
    public void updateInfoBeacons(long timestamp, String address, int rssi) {
        Log.d("BLE", "New/update beacon " + address + " with measured RSSI of " + rssi + " dBm.");

        if (bleAddress.contains(address)) {
            BLeDevices.put(address, rssi);

            // For experiment purposes
            if (recording) {
                String measure = "RAW," + timestamp + ",0,0,0,0,0,0";
                for (String beaconAddr : bleAddress) {
                    if (BLeDevices.containsKey(beaconAddr)) {
                        int aux_rssi = BLeDevices.get(beaconAddr);      // Get the respective measured RSSI for the beacon

                        measure = measure + "," + aux_rssi;
                        //infoStr.append(",").append(aux_rssi);     // Write the latest measured RSSI to the StringBuilder
                    }
                }
                for (int k = 0; k < 10; k++) {measure = measure + ",0";}

                infoStr.append(measure + "\n");
            }
        }

        bleWorking=true;
    }

    @Override
    public void updateSensorData(SensorEvent event) {
        int startOn = 0;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {startOn = 10;}
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {startOn = 13;}
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {startOn = 16;}

        // For experiment purposes
        if (recording) {
            long timeInMillis = (new Date()).getTime() + (event.timestamp - System.nanoTime()) / 1000000L;
            String measure = "RAW," + timeInMillis;
            for (int i = 0; i < 20; i++) {
                if (i >= startOn && i < startOn + 3) {
                    //infoStr.append(",").append(event.values[i - startOn]);
                    measure = measure + "," + event.values[i - startOn];
                } else {
                    //infoStr.append(",0");
                    measure = measure + ",0";
                }
            }
            infoStr.append(measure + "\n");
        }
    }

    // **************************************** DETERMINE AND SOLVE DIRECTION SITUATION *****************************************

    // Estimates the user position based on the turn detector and the compass
    // First, it is checks if the user has changed direction since the last turn (normal or diagonally)
    // If not, the new position is estimated as before
    // If yes, the new orientation situation is determined (orientationSituation) and based on the assessment
    // the new way of calculating the user position is chosen (resolveOrientationSituation)
    public void estimateNewUserPosition(float azimuth) {
        float azimuthDifference = Compass.adjustAzimuth(savedAzimuth, azimuth);     // Difference between the current value and the one measured after last turn

        // Check if the azimuth value has changed significantly (difference is between -π/8 and π/8)
        if (!turnDetected) {
            if (azimuthDifference < -(Math.PI / 8) || azimuthDifference > (Math.PI / 8)) {
                int directionSituation = Compass.orientationSituation(azimuth, turnAzimuth);       // Determine the new orientation situation
                resolveOrientationSituation(directionSituation);       // Resolve based on the direction situation how to estimate the new user position
            }
        } else {
            turnAzimuth = azimuthValue;          // Save the azimuth of the last turn
            turnDetected = false;
        }

        if (diagonalWalk) {
            estimateCurrentPositionBasedOnCompass();
        } else {
            estimateCurrentPositionBasedOnTurnDetection();
        }
    }

    // Based on whether the axis the user was walking, the turn direction and the previous direction, it is estimated how the user position will be updated
    // It is determined the axis the user is walking and the sign to use to update the position
    public void determineDirectionChangeParameters(boolean direction) {
        boolean sign = (straightWalk) == direction;     // The users walking vector in relation to the axis influences the direction of the new walking vector
        currSign = (currSign) == sign;      // The previous direction of the user (previous sign) influences the current direction (current sign)
        straightWalk = !straightWalk;      // User is now walking on a vector perpendicular to the one before
    }

    // Based on the direction situation determined before, the right way to estimate the new user position is chosen
    // Two situations are considered:
    // 1) The user is walking diagonally with respected to the chosen referential
    // 2) The user returned to a parallel path to one of the referential axes, after walking diagonally
    public void resolveOrientationSituation(int directionSituation) {

        // Turn to the perpendicular aix, after walking diagonally
        if (directionSituation % 2 == 1) {
            diagonalDirection = directionSituation != 1;         // If the user has made a slight left turn after walking diagonally
            determineDirectionChangeParameters(diagonalDirection);   // Determine the parameters for the turn

            diagonalWalk = false;
        }
        // Soft turn (diagonal walk)
        else if (directionSituation % 2 == 0) {
            diagonalDirection = directionSituation != 2;

            diagonalWalk = true;
        } else {
            Toast.makeText(MainActivity.this, "Direction is undefined!", Toast.LENGTH_SHORT).show();
            Log.d("Direction", "The user has changed his walking direction to one not forseen by the porgram. (-1)");
        }
    }

    // ********************************************* PEDOMETER POSITION ESTIMATION **********************************************

    // Based on the last turn detected, the x, y position is estimated
    // The direction of the turn and the relation to the aixs in the museum referential is given by the notifyDirectionChange() method
    public void estimateCurrentPositionBasedOnTurnDetection() {

        int square = calculateSqaure(globalClass.getStepSize(), false);

        if (straightWalk) {
            y += signToValue(currSign, square);
        } else {
            x += signToValue(currSign, square);
        }
    }

    // Based on the change of the user direction, the x,y position is estimated
    // The new direction is given by comparing the current azimuth value with the one saved when the user last turned
    public void estimateCurrentPositionBasedOnCompass() {

        // Related to the position update
        int square = calculateSqaure(globalClass.getStepSize(), true);  // Estimates how many floor squares the user has walked

        // Update x,y position
        if (straightWalk) {
            diagonalDirection = (currSign) == diagonalDirection;    // For the cases where the previous direction was negative

            y += signToValue(currSign, square);
            x += signToValue(diagonalDirection, square);

        } else {
            diagonalDirection = (currSign) != diagonalDirection;    // For the cases where the previous direction was negative

            x += signToValue(currSign, square);
            y += signToValue(diagonalDirection, square);
        }
    }

    // ************************************************** POSITION CORRECTION ***********************************************

    // Creates the timer for detecting when the user stops walking
    // If no steps are detected during the this time, a fingerprint message is sent to server, so as to receive another estimate
    // which will then be used to correct the position estimated
    public void setStepTimer () {
        TimerTask timerTask = new TimerTask() {

            @Override
            public void run() {
                // TODO: send message to server

                // TODO: save current position and reset position to (0,0)
                // If no message is received back, then new position is the sum

                cancelStepTimer();
            }
        };
        stepTimer = new Timer();
        stepTimer.schedule(timerTask, 1000);

    }

    private void cancelStepTimer() {
        stepTimer.cancel();
    }

    // RELATED TO EXPERIMENTS AND TESTS ******************************************************************************************************************************

    // Get current date and time (used to name the resulting file in experiments)
    // Source: https://stackabuse.com/how-to-get-current-date-and-time-in-java/
    public String getCurrentTimeAndDate() {
        Date date = new Date(System.currentTimeMillis());
        return formatter.format(date);
    }

    // BUTTON CLICK LISTENERS ******************************************
    // Sets default height (mine = 1,77 m)
    private View.OnClickListener listenerDeafultSettingsButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            globalClass.setHeight(1.77f);
            Toast.makeText(MainActivity.this, "Default values set.", Toast.LENGTH_LONG).show();
        }
    };

    private View.OnClickListener startExpButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (bleWorking) {
                Toast.makeText(MainActivity.this, "Recording started.", Toast.LENGTH_LONG).show();
                recording = true;
            } else {
                Toast.makeText(MainActivity.this, "BLE scanning not working! Recording cannot be started!!", Toast.LENGTH_LONG).show();
            }
        }
    };


    // Listener for the register position button
    // When pressed the x, y position is saved on the infoStr string builder
    private View.OnClickListener listenerRegisterPosition = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            long timestamp = System.currentTimeMillis();
            String estimatedDirection = estimateUserOrientation(azimuthValue, mainAzimuth);      // estimate current orientation

            if (recording) {
                String measure;
                // For experiments (Request)
                measure = "REQ," + timestamp + "," + realX + "," + realY + "," + direction;
                for (int o = 0; o < 17; o++) {measure = measure + ",0";}
                infoStr.append(measure + "\n");

                // For experiments (Response)
                // TODO: remove this from here when implementing server connection
                measure = "";
                measure = "RESP," + System.currentTimeMillis() + ",0,0,0," + x + "," + y + "," + estimatedDirection;
                for (int k = 0; k < 14; k++) {measure = measure + ",0";}
                infoStr.append(measure + "\n");

                Toast.makeText(MainActivity.this, "Position recorded.", Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(MainActivity.this, "Not recording.", Toast.LENGTH_LONG).show();
            }

            pedometerPositionText.setText("X= " + x + "     Y= " + y + "     Direction: " + estimatedDirection);
        }
    };

    // Listener for the save file button
    // When pressed the string built is recorded onto a file
    // This is done by calling the writeToFile method, built to do such
    private View.OnClickListener listenerSaveToFile = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            writeToFile(infoStr.toString(), "Museum_exp_" + getCurrentTimeAndDate(), context);
            infoStr.setLength(0);
            Toast.makeText(MainActivity.this, "File saved.", Toast.LENGTH_LONG).show();
            recording = false;
        }
    };

    // X, Y POSITION *******************************************

    // Add +1 to the X coordinate
    private View.OnClickListener addXButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            realX++;
            realUserPositionText.setText("X= " + realX + "     Y= " + realY + "     Direction: " + direction);
        }
    };

    // Add +1 to the Y coordinate
    private View.OnClickListener addYButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            realY++;
            realUserPositionText.setText("X= " + realX + "     Y= " + realY + "     Direction: " + direction);
        }
    };

    // Subtract -1 to the X coordinate
    private View.OnClickListener subXButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            realX--;
            realUserPositionText.setText("X= " + realX + "     Y= " + realY + "     Direction: " + direction);
        }
    };

    // Subtract -1 to the Y coordinate
    private View.OnClickListener subYButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            realY--;
            realUserPositionText.setText("X= " + realX + "     Y= " + realY + "     Direction: " + direction);
        }
    };

    // USER'S ACTUAL DIRECTION ****************************************

    private View.OnClickListener forwardButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            direction = "F";
            realUserPositionText.setText("X= " + realX + "     Y= " + realY + "     Direction: " + direction);
        }
    };

    private View.OnClickListener backwardButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            direction = "B";
            realUserPositionText.setText("X= " + realX + "     Y= " + realY + "     Direction: " + direction);
        }
    };

    private View.OnClickListener leftButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            direction = "L";
            realUserPositionText.setText("X= " + realX + "     Y= " + realY + "     Direction: " + direction);
        }
    };

    private View.OnClickListener rightButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            direction = "R";
            realUserPositionText.setText("X= " + realX + "     Y= " + realY + "     Direction: " + direction);
        }
    };

    // ******************************************************* READ DATA FROM FILE ***************************************************

    // Reads the file that contains the addresses of all the relevant beacons and loads it up to the bleAddresses array
    public void loadBleDevicesArray() {
        try {
            InputStream inputStream = getAssets().open("beacons_positions.txt");
            bleAddress = fileReader(inputStream);

            for (String k: bleAddress){
                Log.d("Beacon", "Beacon BLE with address " + k);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("Error", "Problem reading file: " + e.getMessage());
        }
    }

}
