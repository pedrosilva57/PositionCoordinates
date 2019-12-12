package pt.ulisboa.tecnico.cmov.positioncoordinates;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ReadAndWriteFiles {
    private static final int REQUESTCODE_STORAGE_PERMISSION = 1;    // For requesting permission to write onto a file

    // Reads all the addresses of the relevant BLE beacons from the beacons_address.txt file
    // Source: https://stackoverflow.com/questions/9544737/read-file-from-assets
    public static ArrayList<String> fileReader (InputStream inputStream){
        ArrayList<String> bleAddress = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(inputStream));

            // do reading, usually loop until end of file reading
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                bleAddress.add(mLine);      // Store the MAC address of the relevant beacon in array
            }
        } catch (IOException e) {
            Log.d("File", e.getLocalizedMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.d("File", "Error closing the reader.");
                }
            }
        }
        return bleAddress;
    }

    // Records the string buit onto a file in the Downloads directory
    // Source: http://codetheory.in/android-saving-files-on-internal-and-external-storage/
    public static void writeToFile(String data, String fileName, Context context) {
        if (storagePermitted((Activity) context)) {
            File file;
            FileOutputStream outputStream;
            try {
                file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName + ".txt");

                outputStream = new FileOutputStream(file);
                outputStream.write(data.getBytes());
                outputStream.close();
            } catch (IOException e) {
                Log.d("File", e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }

    // Checks if there is permission to write and read in memory
    // Requests permission to the user if not
    private static boolean storagePermitted(Activity activity) {

        // Check read and write permissions
        Boolean readPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        Boolean writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        if (readPermission && writePermission) {
            return true;
        }

        // Request permission to the user
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUESTCODE_STORAGE_PERMISSION);

        return false;
    }
}
