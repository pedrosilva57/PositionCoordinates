package pt.ulisboa.tecnico.cmov.positioncoordinates;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class BeaconsService extends Service {

    private static final String TAG = BeaconsService.class.getSimpleName();

    // Constants
    private static final int REQUEST_ENABLE_BT = 1;     // For requesting the user to turn Bluetooth on
    private static final int PERMISSION_ACCESS_FINE_LOCATION = 1;
    private int PERMISSION_ACCESS_COURSE_LOCATION=1;    // For requesting the user to allow location to be used

    private BluetoothAdapter adapter;       // Bluetooth adapter

    // Activity
    private GlobalClass globalClass;
    private Context activityContext;
    private Activity mainActivity;

    // For activity communication
    private final IBinder mBinder = new BeaconsService.LocalBinder();
    private BeaconsService.Callbacks activity;

    private Map<BluetoothDevice, Integer> BLeDevices = new HashMap<BluetoothDevice, Integer>();     // Stores the device and measured RSSI

    public BeaconsService() {}

    // ******************************************* ACTIVITY COMMUNICATION RELATED METHODS ****************************************
    // Source: https://stackoverflow.com/questions/20594936/communication-between-activity-and-service
    @Override
    public IBinder onBind(Intent intent) {return mBinder;}

    //returns the instance of the service
    public class LocalBinder extends Binder {
        public BeaconsService getServiceInstance(){
            return BeaconsService.this;
        }
    }

    //Here Activity register to the service as Callbacks client
    public void registerClient(Activity activity){
        this.activity = (Callbacks) activity;
        Log.d(TAG, activity + " client registered.");
    }

    //callbacks interface for communication with service clients!
    public interface Callbacks{
        void updateInfoBeacons(long timestamp, String address, int rssi);
    }

    // ***********************************************************************************************************************

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Global Class instance
        globalClass = (GlobalClass) getApplicationContext();

        // Activity and context
        activityContext = globalClass.getContext();
        mainActivity = (Activity) activityContext;

        // Bluetooth Low Energy initialization and permission checking
        checkLocationPermissions();                            // Checks if the app has permissions for location usage
        adapter = BluetoothAdapter.getDefaultAdapter();       // Bluetooth adapter initialization
        checkBluetoothPermissions();                         // Checks if the app has permissions for Bluetooth utilization



        scanBeaconsBLE();   // To start the BLE beacon scanning

        return super.onStartCommand(intent, flags, startId);
    }


    // *********************************************** BLE BEACON SCANNING AND ANALYSIS *********************************************
    // Scans all the BLE beacons near the device and puts that information in a HashMap
    public void scanBeaconsBLE (){
        BluetoothLeScanner bluetoothLeScanner = adapter.getBluetoothLeScanner();    // Bluetooh Low Energy Scanner adapter initializing
        Log.d("BLE", "BLE adpater initialized");

        // All the devices detected are stored in a hashmap along with the respective RSSI measured
        ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

                Log.d("BLE", "Device " + result.getDevice().getName() + " with MAC address " + result.getDevice().getAddress() + " registers a RSSI of " + result.getRssi());
                activity.updateInfoBeacons(System.currentTimeMillis(), result.getDevice().getAddress(), result.getRssi());
            }
        };

        bluetoothLeScanner.startScan(scanCallback);     // Start the scan
        Log.d("BLE", "Scanning stated");
    }

    // Takes the devices and respective RSSI measured to determine the closest beacon
    // Compares the values to identify the maximum
    public void estimateClosestBeacon (){
        Map.Entry<BluetoothDevice, Integer> maxEntry = null;
        ArrayList<Map.Entry<BluetoothDevice, Integer>> fingerprint = new ArrayList<>();

        // Loop through hashmap in order to find the device with highest RSSI
        for (Map.Entry<BluetoothDevice, Integer> entry : BLeDevices.entrySet() ) {
            if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0){
                maxEntry = entry;
            }
        }

        BluetoothDevice bleDevice = maxEntry.getKey();
        int rssi = maxEntry.getValue();
        //activity.updateInfoBeacons(bleDevice.getName(), bleDevice.getAddress(), rssi);
    }


    // ************************************************* BLUETOOTH PERMISSIONS *****************************************************
    // Request the user to turn Bluetooh on if turned off
    // Source: https://stackoverflow.com/questions/34878071/adapter-getbluetoothlescanner-returns-null-on-some-android-6-0-devices
    public void checkBluetoothPermissions (){
        if (!adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mainActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    // Check (and request if necessary) for user location permission (for some reason)
    // Necessary for the Bluetooth Low Energy beacons
    // Source: https://stackoverflow.com/questions/32491960/android-check-permission-for-locationmanager
    public void checkLocationPermissions (){
        int permissionCheckFineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int permissionCheckCoarseLocation = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION);

        if (permissionCheckFineLocation != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(mainActivity, new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ACCESS_FINE_LOCATION);
        }

        if (permissionCheckCoarseLocation != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(mainActivity, new String[] {android.Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_ACCESS_COURSE_LOCATION);
        }
    }

}
