package com.example.rpsbbs_security;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {


    FusedLocationProviderClient client;
    SupportMapFragment mapFragment;
    FirebaseDatabase rootnode;
    DatabaseReference reference;
    String userId;
    FirebaseAuth firebaseAuth;
    EditText startpoint;
    EditText endpoint;
    Button btn_journey;
    Boolean danger=false;
    Boolean safe=true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        startpoint=findViewById(R.id.start);
        endpoint=findViewById(R.id.end);
        btn_journey=findViewById(R.id.journey_tracker);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Danger Mode On,Our Agents will be reacing you soon", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                danger=true;
                safe=false;
            }
        });
        FloatingActionButton fab1 = findViewById(R.id.fab1);
        fab1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Safer Mode On", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                danger=false;
                safe=true;
            }
        });

        btn_journey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sSource=startpoint.getText().toString().trim();
                String eSource=endpoint.getText().toString().trim();

                if (eSource.equals("")&& sSource.equals("")){
                    Toast.makeText(getApplicationContext()
                            ,"Enter Both Fields",Toast.LENGTH_SHORT).show();
                }else{
                    DisplayTrack(sSource,eSource);
                }
            }
        });

        client = LocationServices.getFusedLocationProviderClient(MapsActivity.this);
        firebaseAuth=FirebaseAuth.getInstance();
        userId=firebaseAuth.getCurrentUser().getUid();

        Dexter.withContext(getApplicationContext())
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        getmylocation(userId,danger,safe);

                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                }).check();

    }

    private void DisplayTrack(String sSource, String eSource) {
        try{
            Uri uri=Uri.parse("https://www.google.co.in/maps/dir/"+sSource+"/"+eSource);
            Intent intent=new Intent(Intent.ACTION_VIEW,uri);
            intent.setPackage("com.google.android.apps.maps");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }catch (ActivityNotFoundException e){
            Uri uri=Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.maps");
            Intent intent=new Intent(Intent.ACTION_VIEW,uri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private void getmylocation(String userId, Boolean danger, Boolean safe) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        String userid=userId;
        Task<Location> task = client.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                mapFragment.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        LatLng latLng=new LatLng(location.getLatitude(),location.getLongitude());
                        MarkerOptions markerOptions=new MarkerOptions().position(latLng).title("you are here");
                        googleMap.addMarker(markerOptions);
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,17));
                    }
                });
                updateDataBase(userid,location.getLatitude(),location.getLongitude(),danger,safe);

            }
        });
    }

    private void updateDataBase(String userid, double latitude, double longitude, Boolean danger, Boolean safe) {
        rootnode=FirebaseDatabase.getInstance();
        reference=rootnode.getReference(userid);
        reference.child("users").child(userid).child("Latitude").setValue(latitude);
        reference.child("users").child(userid).child("Longitude").setValue(longitude);
        reference.child("users").child(userid).child("DangerMode").setValue(danger);
        reference.child("users").child(userid).child("SafeMode").setValue(safe);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

    }
}