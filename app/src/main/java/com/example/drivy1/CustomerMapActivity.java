package com.example.drivy1;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import com.google.android.libraries.places.api.Places;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    private GoogleMap mMap;
    public static final int MY_PERMISSION_LOCATION =99;

    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;
    private RadioGroup radioGroup;
    private Button logout;
    private Button mRequest;
    private LatLng pickupLocation;
    private Boolean requestBol = false;
    private Marker pickupMarker;
    private Button mSettings;
    private String destination, requestService;
    private LinearLayout driverInfo;

    private ImageView driverProfileImage;
    private TextView driverName,driverPhone,driverCar;
    private PlacesClient placesClient;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_LOCATION:
                if (grantResults.length> 0) {
                    boolean internet = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                    boolean fine = grantResults[0] ==  PackageManager.PERMISSION_GRANTED;
                    boolean coarse = grantResults[1] ==  PackageManager.PERMISSION_GRANTED;
                    if (internet && fine && coarse) {
                        Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(),"Permission Denied",Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }
    public boolean CheckPermissions() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION);
        int result2 = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), INTERNET);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED && result2 == PackageManager.PERMISSION_GRANTED;
    }
    private void RequestPermissions() {
        ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, INTERNET}, MY_PERMISSION_LOCATION);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        while (!CheckPermissions())
            RequestPermissions();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        Places.initialize(getApplicationContext(),"AIzaSyDw8AuOiFHkLmwhdbwkYPO7nWXDFrzYrcw");
        placesClient = Places.createClient(this);
        logout = (Button) findViewById(R.id.logout);
        mRequest = (Button) findViewById(R.id.requests);
        mSettings = (Button) findViewById(R.id.settings);
        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.check(R.id.DrivyX);

        driverInfo = (LinearLayout) findViewById(R.id.driverInfo);
        driverProfileImage = (ImageView) findViewById(R.id.driverProfileImage);
        driverName= (TextView) findViewById(R.id.driverName);
        driverPhone= (TextView) findViewById(R.id.driverPhone);
        driverCar = (TextView) findViewById(R.id.driverCar);

        //Logout
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerMapActivity.this,MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        //Request
        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(requestBol){
                    requestBol = false;
                    geoQuery.removeAllListeners();
                    driverLocationRef.removeEventListener(driverLocationRefListener);

                    if(driverFoundId !=null)
                    {
                        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(driverFoundId);
                        driverRef.setValue(true);
                        driverFoundId = null;
                    }
                    driverFound = false;
                    radius = 1;
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.removeLocation(userId);
                    if(pickupMarker !=null)
                        pickupMarker.remove();
                    mRequest.setText("Call Drivy");

                    driverInfo.setVisibility(View.GONE);
                    driverName.setText("");
                    driverPhone.setText("");
                    driverCar.setText("");
                    driverProfileImage.setImageResource(R.mipmap.default_user);

                }
                else {
                    int selectID = radioGroup.getCheckedRadioButtonId();
                    final RadioButton radioButton = (RadioButton) findViewById(selectID);
                    if(radioButton.getText() == null)
                        return;
                    requestService = radioButton.getText().toString();
                    requestBol = true;
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userId,new GeoLocation(lastLocation.getLatitude(),lastLocation.getLongitude()));

                    pickupLocation = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                    pickupMarker=mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.icon)));
                    mRequest.setText("Getting your driver...");
                    getClosestDriver();

                }

            }
        });

        //Settings
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerMapActivity.this,CustomerSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });


        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                destination=place.getName().toString();
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
            }
        });

    }
    private int radius = 1;
    private Boolean driverFound = false;
    private String driverFoundId;
    GeoQuery geoQuery;
    private void getClosestDriver() {
        final DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");
        GeoFire geoFire = new GeoFire(driverLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude,pickupLocation.longitude),radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound && requestBol)
                {

                DatabaseReference customerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(key);
                customerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                            Map<String,Object> driverMap = (Map<String,Object>) dataSnapshot.getValue();
                            if(driverFound){
                                return;
                            }
                            if(driverMap.get("service").equals(requestService))
                            {
                                driverFound = true;
                                driverFoundId = dataSnapshot.getKey();
                                DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(dataSnapshot.getKey()).child("customerRequest");
                                String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                HashMap map = new HashMap();
                                map.put("customerRideId",customerId);
                                //destination = placesClient.toString(); Toast.makeText(getApplicationContext(), destination, Toast.LENGTH_SHORT).show();
                                map.put("destination",destination);
                                driverRef.updateChildren(map);
                                getDriverLocation();
                                getDriverInfo();
                                mRequest.setText("Loooking for Driver Location");
                            }

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
            if(!driverFound)
            {
                radius++;
                getClosestDriver();
            }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }


            private Marker mDriverMarker ;
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;
    private void getDriverLocation() {
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("driversWorking").child(driverFoundId).child("l");
        driverLocationRefListener= driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && requestBol)
                {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLong = 0;

                    mRequest.setText("Driver Found");
                    if(map.get(0)!=null)
                    {
                        locationLat =Double.parseDouble(map.get(0).toString());}
                    if(map.get(1)!=null){
                        locationLong =Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatLng = new LatLng(locationLat,locationLong);
                    if(mDriverMarker !=null)
                    {
                        mDriverMarker.remove();
                    }

                    Location loc1 = new Location("");
                    loc1.setLatitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(driverLatLng.latitude);
                    loc2.setLongitude(driverLatLng.longitude);

                    float distance = loc1.distanceTo(loc2);
                    if(distance < 1) {
                        mRequest.setText("Driver is here " );
                    }
                    else
                    {
                        mRequest.setText("Driver Found: " + String.valueOf(distance));
                    }
                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Your Driver").icon(BitmapDescriptorFactory.fromResource(R.mipmap.drivy_icon)));
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void getDriverInfo()
    {
        driverInfo.setVisibility(View.VISIBLE);
        DatabaseReference customerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(driverFoundId);
        customerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0)
                {
                    Map<String,Object> map = (Map<String,Object>) dataSnapshot.getValue();
                    if(map.get("name")!=null)
                    {
                        driverName.setText(map.get("name").toString());
                    }
                    if(map.get("phone")!=null)
                    {
                        driverPhone.setText(map.get("phone").toString());
                    }
                    if(map.get("car")!=null)
                    {
                        driverCar.setText(map.get("car").toString());
                    }
                    if(map.get("profileImageUrl")!=null)
                    {
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(driverProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


            /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
        //    return;
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleApiClient(){
        googleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        googleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(18)); //camera

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
        //    return;
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,locationRequest,this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
