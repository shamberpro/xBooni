package com.swiftoffice.cuas;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.OnFailureListener;

import java.util.HashMap;
import java.util.Map;

public class HomeActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener, LocationListener, AdapterView.OnItemSelectedListener {
    private static final int REQUEST_CODE = 101;
    //Create Log
    private static final String TAG = "HomeActivity";
    private static final String KEY_Altitude = "Altitude";
    private static final String KEY_Bearing = "Bearing";
    private static final String KEY_Longitude = "Longitude";
    private static final String KEY_Latitude = "Latitude";
    private static final String KEY_Color = "Color";
    private static final String KEY_Payload = "Payload";
    private static final String KEY_Light = "Light";
    private static final String KEY_Speed = "Speed";
    private static final String KEY_Landmark = "Nearest Landmark";
    private static final String KEY_TimeStamp = "Timestamp";
    private static final String KEY_AndroidID = "Android ID";
    //Map
    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;
    //Bearing
    int mAzimuth;
    String where;
    TextView txt_compass;
    boolean haveSensor = false, haveSensor2 = false;
    float[] rMat = new float[9];
    float[] orientation = new float[3];
    String latitude = "0", longitude = "0";
    //Create Timestamp
    Long tsLong = System.currentTimeMillis();
    String date = android.text.format.DateFormat.format("yyyy-MM-dd hh:mm:ss", tsLong).toString();
    // Access a Cloud Firestore instance from your Activity
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    //Button
    Button btnLogout;
    Button btnSubmit;
    FirebaseAuth mFirebaseAuth;
    String altitude, color, speed, payload, light, landmark = "";
    //IMEI
    String id;
    private SensorManager mSensorManager;
    private Sensor mRotationV, mAccelerometer, mMagnetometer;
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    //Latitude and Longtitude
    private TextView LatLng;
    private LocationManager locationManager;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    //Spinner
    private Spinner spinnerAltitude, spinnerColor, spinnerSpeed, spinnerPayload, spinnerLight, spinnerLandmark;

    private GoogleMap mMap;
    SupportMapFragment mapFragment;
    ProgressDialog mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        //IMEI
        id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        //Map
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fetchLastLocation();

        //Bearing
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        txt_compass = (TextView) findViewById(R.id.txt_azimuth);

        start();

        //Latitude and Longtitude
        LatLng = (TextView) findViewById(R.id.txt_latlng);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        onLocationChanged(location);

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000, 1, this);

        //Button
        btnLogout = findViewById(R.id.LogoutButton);
        btnSubmit = findViewById(R.id.submit_info);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intToLogin = new Intent(HomeActivity.this, PhoneLoginActivity.class);
                startActivity(intToLogin);
            }
        });

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                initView();
            }
        });
    }

    private void initView() {
        //Spinner
        spinnerAltitude = (Spinner) findViewById(R.id.altitudeSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.altitude_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAltitude.setAdapter(adapter);

        spinnerColor = (Spinner) findViewById(R.id.colorSpinner);
        ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(this, R.array.color_array, android.R.layout.simple_spinner_item);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerColor.setAdapter(adapter1);

        spinnerSpeed = (Spinner) findViewById(R.id.speedSpinner);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this, R.array.speed_array, android.R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSpeed.setAdapter(adapter2);

        spinnerPayload = (Spinner) findViewById(R.id.payloadSpinner);
        ArrayAdapter<CharSequence> adapter3 = ArrayAdapter.createFromResource(this, R.array.payload_array, android.R.layout.simple_spinner_item);
        adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPayload.setAdapter(adapter3);

        spinnerLight = (Spinner) findViewById(R.id.lightSpinner);
        ArrayAdapter<CharSequence> adapter4 = ArrayAdapter.createFromResource(this, R.array.light_array, android.R.layout.simple_spinner_item);
        adapter4.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLight.setAdapter(adapter4);

        spinnerLandmark = (Spinner) findViewById(R.id.landmarkSpinner);
        ArrayAdapter<CharSequence> adapter5 = ArrayAdapter.createFromResource(this, R.array.landmark_array, android.R.layout.simple_spinner_item);
        adapter5.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLandmark.setAdapter(adapter5);

        spinnerAltitude.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                altitude = spinnerAltitude.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinnerColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                color = spinnerColor.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinnerSpeed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                speed = spinnerSpeed.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinnerPayload.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                payload = spinnerPayload.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinnerLight.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                light = spinnerLight.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinnerLandmark.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                landmark = spinnerLandmark.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    public void saveInfo(View v) {

        final Map<String, Object> note = new HashMap<>();
        note.put(KEY_Altitude, altitude);
        note.put(KEY_Bearing, mAzimuth);
        note.put(KEY_Longitude, longitude);
        note.put(KEY_Latitude, latitude);
        note.put(KEY_Color, color);
        note.put(KEY_Payload, payload);
        note.put(KEY_Light, light);
        note.put(KEY_Speed, speed);
        note.put(KEY_Landmark, landmark);
        note.put(KEY_TimeStamp, date);
        note.put(KEY_AndroidID, id);


        //Getting EmailID
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            mProgressBar = new ProgressDialog(HomeActivity.this);
            mProgressBar.setCancelable(false);
            mProgressBar.setMessage("Waiting ...");
            mProgressBar.show();
            final String phone = user.getPhoneNumber();

            db.collection("Information").document(phone).set(note)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            postToSQL(note, phone);
                            Toast.makeText(HomeActivity.this, "BCP Notified", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(HomeActivity.this, "Error. Please Try Again", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, e.toString());
                            mProgressBar.cancel();
                        }
                    });
        }
    }

    private void postToSQL(final Map<String, Object> note,final String phone) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        final StringBuffer restURL = new StringBuffer("https://cuas-dev-api.azurewebsites.net/report");
        restURL.append("?AndroidID=" + note.get("Android ID").toString());
        restURL.append("&Altitude=" + note.get("Altitude").toString());
        restURL.append("&Bearing=" + note.get("Bearing").toString());
        restURL.append("&Colour=" + note.get("Color").toString());
        restURL.append("&Latitude=" + note.get("Latitude").toString());
        restURL.append("&Longitude=" + note.get("Longitude").toString());
        restURL.append("&Light=" + note.get("Light").toString());
        restURL.append("&NearestLandmark=" + note.get("Nearest Landmark").toString());
        restURL.append("&Payload=" + note.get("Payload").toString());
        restURL.append("&Speed=" + note.get("Speed").toString());
        restURL.append("&Timestamp=" + note.get("Timestamp").toString());
        restURL.append("&UserID=" + phone);

        StringRequest postRequest = new StringRequest(com.android.volley.Request.Method.POST, restURL.toString(),
                new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // response
                        Log.d("Response XXX", response);
                        mProgressBar.cancel();
                    }
                },
                new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.d("Error.Response XXX", error.getMessage() == null ? "" : error.getMessage());
                        mProgressBar.cancel();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                return params;
            }
        };
        queue.add(postRequest);
    }

    private void start() {
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) {
            if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null || mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
                noSensorAlert();
            } else {
                mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                haveSensor = mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
                haveSensor2 = mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
            }
        } else {
            mRotationV = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            haveSensor = mSensorManager.registerListener(this, mRotationV, SensorManager.SENSOR_DELAY_UI);
        }
    }

    private void noSensorAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("Your device does not support bearing.")
                .setCancelable(false)
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        alertDialog.show();
    }

    public void stop() {
        if (haveSensor && haveSensor2) {
            mSensorManager.unregisterListener(this, mAccelerometer);
            mSensorManager.unregisterListener(this, mMagnetometer);
        } else {
            if (haveSensor)
                mSensorManager.unregisterListener(this, mRotationV);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        start();
    }

    private void fetchLastLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLocation = location;
                    //Toast.makeText(getApplicationContext(),currentLocation.getLatitude()
                    //+""+currentLocation.getLongitude(),Toast.LENGTH_SHORT).show();
                    mapFragment = (SupportMapFragment)
                            getSupportFragmentManager().findFragmentById(R.id.google_map);
                    mapFragment.getMapAsync(HomeActivity.this);

                    double latti = location.getLatitude();
                    double longi = location.getLongitude();
                    latitude = String.valueOf(latti);
                    longitude = String.valueOf(longi);
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions().position(latLng)
                .title("Current Location");
        mMap.addMarker(markerOptions);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)
                .zoom(17)
                .bearing(mAzimuth)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    fetchLastLocation();
                }
                break;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rMat, event.values);
            mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastMagnetometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(rMat, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(rMat, orientation);
            mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
        }

        mAzimuth = Math.round(mAzimuth);

        where = "NW";

        if (mAzimuth >= 350 || mAzimuth <= 10)
            where = "N";
        if (mAzimuth < 350 && mAzimuth > 280)
            where = "NW";
        if (mAzimuth <= 280 && mAzimuth > 260)
            where = "W";
        if (mAzimuth <= 260 && mAzimuth > 190)
            where = "SW";
        if (mAzimuth <= 190 && mAzimuth > 170)
            where = "S";
        if (mAzimuth <= 170 && mAzimuth > 100)
            where = "SE";
        if (mAzimuth <= 100 && mAzimuth > 80)
            where = "E";
        if (mAzimuth <= 80 && mAzimuth > 10)
            where = "NE";

        txt_compass.setText("Bearing: " + mAzimuth + "° " + where);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //not in used
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;

        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        this.latitude = String.valueOf(latitude);
        this.longitude = String.valueOf(longitude);
        LatLng.setText("Lat: " + latitude + ", " + "Lng: " + longitude);

        LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)
                .zoom(17)
                .bearing(mAzimuth)
                .build();
        if (mMap != null) {
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else {
            mapFragment = (SupportMapFragment)
                    getSupportFragmentManager().findFragmentById(R.id.google_map);
            mapFragment.getMapAsync(HomeActivity.this);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
