package com.dulceprime.antitheft.services;

import android.Manifest;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dulceprime.antitheft.other_components.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class Background_Service extends Service {
    private Handler mHandler;
    private Runnable mRunnable;
    private int mInterval = 1000;

    private static final String TAG = "HelloService";

    private boolean isRunning = false;


    SomeComponents myNewComponents;
    public SQLiteDatabase db;
    public SharedPreferences user_details;
    private String getTableState;

    PrefManager prefManager;

    SQLController sqlController;

    private Background_Service mActivity;
    private Context context;


    private LocationManager locationManager;
    private LocationListener listener;


    public String longitude = "";
    public String latitude = "";


    private String dbSerialNumber1 = "";
    private String dbSerialNumber2 = "";

    private static final int MY_PERMISSIONS_REQUEST_CODE = 12345;
    private Cursor managedCursor;

    boolean isSim1Removed = false;
    boolean isSim2Removed = false;

    boolean isSim1Swapped = false;
    boolean isSim2Swapped = false;
    private boolean isBothSimsPresentAtProtection = false;
    private boolean submitAReport = false;


    ArrayList<String> phoneNumberArrayList, emailArrayList;

    String gpsCoordinateX;
    String gpsCoordinateY;

    JSONObject phoneNumbers;
    JSONObject emailAddresses;

    JSONObject EverythingJSON;

    String everythingConvertToString;


    @Override
    public void onCreate() {
//        Log.i(TAG, "Service onCreate");
        isRunning = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

//        db = openOrCreateDatabase(DBhelper.DB_NAME, MODE_PRIVATE, null);
        sqlController = new SQLController(getApplicationContext());
        prefManager = new PrefManager(this);
        context = getApplicationContext();
        mActivity = Background_Service.this;


//        Log.i(TAG, "Service onStartCommand");
//        Log.i("BOOT THINGS", "BOOT COMPLETE");

        //Creating new thread for my service
        //Always write your long running tasks in a separate thread, to avoid ANR
        final Handler handler = new Handler(Looper.getMainLooper());

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            //do your stuff here
//                            Log.d("ECHO ", "SERVICE RUNNING");

                            Toast.makeText(getApplicationContext(), "BOOT COMPLETED", Toast.LENGTH_SHORT).show();

                            if (prefManager.isProtected()) {
//                                Log.d("PROTECTION ", "DEVICE PROTECTED");

                                if (prefManager.isPermissionGranted()) {
                                    db = openOrCreateDatabase(DBhelper.DB_NAME, MODE_PRIVATE, null);

                                    try {
                                        Cursor c = db.rawQuery("SELECT * FROM " + DBhelper.SIM_DETAILS_TABLE + " WHERE " + DBhelper.SIM_TABLE_ID + " = '1'", null);
                                        if (c.moveToNext()) {
                                            dbSerialNumber1 = c.getString(c.getColumnIndex(DBhelper.SERIAL_NUMBER_1));
                                            dbSerialNumber2 = c.getString(c.getColumnIndex(DBhelper.SERIAL_NUMBER_2));
                                        }
                                        c.close();

                                    } catch (Exception e) {
                                        e.printStackTrace();

                                        Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                                    }


                                    // FROM HERE
                                    if (!((dbSerialNumber1.trim().isEmpty()) || (dbSerialNumber2.trim().isEmpty()))) {
                                        // BOTH SIMS WERE PRESENT AT PROTECTION


                                        isBothSimsPresentAtProtection = true;
                                        if (!((dbSerialNumber1.equalsIgnoreCase(getDeviceSerialNumber1())) || (dbSerialNumber1.equalsIgnoreCase(getDeviceSerialNumber2())))) {
                                            // SIM 1 IS REMOVED
                                            isSim1Removed = true;
                                        } else {
                                            isSim1Removed = false;
                                        }


                                        if (!((dbSerialNumber2.equalsIgnoreCase(getDeviceSerialNumber2())) || (dbSerialNumber2.equalsIgnoreCase(getDeviceSerialNumber1())))) {
                                            // SIM 1 IS REMOVED
                                            isSim2Removed = true;
                                        } else {
                                            isSim2Removed = false;
                                        }

                                        if (isBothSimsPresentAtProtection && (isSim1Removed && isSim2Removed)) {
                                            // If both sims present at protection and one of them is removed at check, submit report
                                            submitAReport = true;
                                        }
                                    } else {
                                        // One or both sims were not present at protection
                                        if (dbSerialNumber1.isEmpty() && dbSerialNumber2.isEmpty()) {
                                            // Both slots are empty at protection
                                            if (!(getDeviceSerialNumber1().isEmpty()) && (getDeviceSerialNumber2().isEmpty())) {
                                                // Both sim empty at protection, but sim currently detected
                                                submitAReport = true;
                                            }
                                        }


                                        if (dbSerialNumber1.isEmpty()) {
                                            // Slot 1 empty at protection
                                            if (!((dbSerialNumber2.equalsIgnoreCase(getDeviceSerialNumber1())) || (dbSerialNumber2.equalsIgnoreCase(getDeviceSerialNumber2())))) {
                                                // The sim 2 that was present at protection is not found on the device
                                                submitAReport = true;
                                            }
                                        }

                                        if (dbSerialNumber2.isEmpty()) {
                                            // Slot 2 empty at protection
                                            if (!((dbSerialNumber1.equalsIgnoreCase(getDeviceSerialNumber1())) || (dbSerialNumber1.equalsIgnoreCase(getDeviceSerialNumber2())))) {
                                                // The sim 1 that was present at protection is not found on the device
                                                submitAReport = true;
                                            }
                                        }
                                    }

                                    // TO HERE
                                    if (submitAReport) {

                                        if(prefManager.isFreshRequest()) {
                                            fetchReportFromDBToSend();  // Check whether there's a report to submit to the internet
                                        }

                                        submitAReport = false;  // re-initializing the variable

                                    } else {
//                                        Toast.makeText(mActivity, "Do not submit a report ", Toast.LENGTH_SHORT).show();
                                    }


                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                            // Access fine location permission granted


                                            try {
                                                locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                                                listener = new LocationListener() {
                                                    @Override
                                                    public void onLocationChanged(Location location) {
//                                            textView.append("n " + location.getLongitude() + " " + location.getLatitude());
                                                        longitude = location.getLongitude() + "";
                                                        latitude = location.getLatitude() + "";

//                                                        Toast.makeText(getApplicationContext(), "Long: " + location.getLongitude() + "\n Lat: " + location.getLatitude(), Toast.LENGTH_SHORT).show();
                                                    }

                                                    @Override
                                                    public void onStatusChanged(String s, int i, Bundle bundle) {

                                                    }

                                                    @Override
                                                    public void onProviderEnabled(String s) {

                                                    }

                                                    @Override
                                                    public void onProviderDisabled(String s) {

                                                        Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                                        startActivity(i);
                                                    }
                                                };

                                                gpsLocationMethod();
                                            } catch (Exception e) {
                                                Toast.makeText(getApplicationContext(), "Exception: " + e.toString(), Toast.LENGTH_SHORT).show();
                                            }


//                                            Toast.makeText(getApplicationContext(), "LOCATION Permission granted.", Toast.LENGTH_SHORT).show();
                                        } else {
                                            // Camera permission not granted
//                                            Toast.makeText(getApplicationContext(), "LOCATION Permission not granted.", Toast.LENGTH_SHORT).show();
                                        }
                                    }


                                    db.close();
                                } else {
                                    Toast.makeText(getApplicationContext(), "Permission not granted!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });
                    try {
//                        30000  = 30 seconds
                        Thread.sleep(120000); // 2 min
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        new Thread(runnable).start();
        return Service.START_STICKY;
    }


    void gpsLocationMethod() {
        // this code won'textView execute IF permissions are not allowed, because in the line above there is return statement.
        //noinspection Missing Permission
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates("gps", 10000, 0, listener); // 5000 for 5 seconds
    }


    public void fetchReportFromDBToSend() {
//        if (!(prefManager.isReportSent())) {
//                prefManager.setIsReportSent(false);
            // WE HAVE AN UNSENT REPORT

            if (!(longitude.equalsIgnoreCase(""))) {
                // If there is a value for the coordinates yet
//                    Toast.makeText(Background_Service.this, "Longitude to send", Toast.LENGTH_LONG).show();
                fetchAllRecoveryDetailsToArrayLists(); // Must be called before using the values of the arraylist (contact recovery details)

                if (!(phoneNumberArrayList.size() < 0)) {
                    // If the arraylist of contact details is not empty

                    // WE SEND OUR REPORT
                    phoneNumbers = new JSONObject();
                    emailAddresses = new JSONObject();

                    //Loop through array of contacts and put them to a JSONcontact object
                    for (int i = 0; i < phoneNumberArrayList.size(); i++) {
                        try {
                            phoneNumbers.put("index_" + String.valueOf(i + 1), phoneNumberArrayList.get(i));
                            emailAddresses.put("index_" + String.valueOf(i + 1), emailArrayList.get(i));

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    EverythingJSON = new JSONObject();
                    try {
                        EverythingJSON.put("phoneNumbers", phoneNumbers);
                        EverythingJSON.put("emailAddress", emailAddresses);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                    everythingConvertToString = EverythingJSON.toString();

                    BackGround backWork = new BackGround();
                    backWork.execute(everythingConvertToString, longitude, latitude);

                }
            }
//        }
    }


    public void fetchAllRecoveryDetailsToArrayLists() {
        // This method should be called before using the values of the arraylists
        db = openOrCreateDatabase(DBhelper.DB_NAME, MODE_PRIVATE, null);

        phoneNumberArrayList = new ArrayList<String>();
        emailArrayList = new ArrayList<String>();

        try {
            Cursor c = db.rawQuery("SELECT * FROM " + DBhelper.RECOVERY_DETAILS_TABLE + "", null);
            while (c.moveToNext()) {
                // PUT ALL VALUES INTO ARRAY LISTS
                phoneNumberArrayList.add(c.getString(c.getColumnIndex(DBhelper.RECOVERY_DETAILS_PHONE)));
                emailArrayList.add(c.getString(c.getColumnIndex(DBhelper.RECOVERY_DETAILS_NAME)));

            }
            c.close();

        } catch (Exception e) {
            e.printStackTrace();

            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
        }
        db.close();
    }


    // getSimInfo
    public List<SubscriptionInfo> getSimInfo(Context context) {
        SubscriptionManager subManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        List<SubscriptionInfo> subscriptionInfoList = new ArrayList<>();
        subscriptionInfoList = subManager.getActiveSubscriptionInfoList();
        Log.d("LIST LIST", subscriptionInfoList.toString());
        if (subscriptionInfoList == null) {
            Toast.makeText(context, "address not found", Toast.LENGTH_SHORT).show();
        }
        return subscriptionInfoList;
    }

    // getNetworkOperator
    public List<String> getNetworkOperator(final Context context) {
        // Get System TELEPHONY service reference
        List<String> carrierNames = new ArrayList<>();
        try {
            final String permission = android.Manifest.permission.READ_PHONE_STATE;
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) && (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)) {
                final List<SubscriptionInfo> subscriptionInfos = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
                for (int i = 0; i < subscriptionInfos.size(); i++) {
                    carrierNames.add(subscriptionInfos.get(i).getCarrierName().toString());
                }

            } else {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                // Get carrier name (Network Operator Name)
                carrierNames.add(telephonyManager.getNetworkOperatorName());
                //enter code here
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return carrierNames;
    }


    public String getDeviceSerialNumber1() {
// Getting the current sim 1 serial number
        try {
            int subId = getSimInfo(context).get(0).getSubscriptionId();
            String subscriberId1 = subId + "";
            return subscriberId1;
        } catch (Exception e) {
            return "";
        }
    }

    public String getDeviceSerialNumber2() {
// Getting the current sim 2 serial number
        try {
            int subId = getSimInfo(context).get(1).getSubscriptionId();
            String subscriberId2 = subId + "";
            return subscriberId2;
        } catch (Exception e) {
            return "";
        }
    }


    @Override
    public IBinder onBind(Intent arg0) {
//        Log.i(TAG, "Service onBind");
        return null;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
//        Log.i(TAG, "Service onDestroy");
    }


    class BackGround extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            String parameterSent = params[0];
            String coordinateX = params[1];
            String coordinateY = params[2];
            String data = "";
            int tmp;

            try {
                URL url = new URL("http://project.edostatenews.com.ng/anti-theft-app-backend/send_email_and_sms.php"); // online
                String urlParams = "json_brought=" + parameterSent + "&gpsCoordinateX=" + coordinateX + "&gpsCoordinateY=" + coordinateY;

                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setDoOutput(true);
                OutputStream os = httpURLConnection.getOutputStream();
                os.write(urlParams.getBytes());
                os.flush();
                os.close();
                InputStream is = httpURLConnection.getInputStream();
                while ((tmp = is.read()) != -1) {
                    data += (char) tmp;
                }
                is.close();
                httpURLConnection.disconnect();

                return data;

            } catch (MalformedURLException e) {
                e.printStackTrace();
                return "Exception: " + e.getMessage();
            } catch (IOException e) {
                e.printStackTrace();
                return "Exception: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String s) {

            if (s.trim().equalsIgnoreCase("OK")) { // Message sent


//                prefManager.setIsReportSent(true);
                prefManager.setIsFreshRequest(false);
                Toast.makeText(Background_Service.this, "SENT", Toast.LENGTH_LONG).show();
            }


        }
    }


}