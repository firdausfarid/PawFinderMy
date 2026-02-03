package com.example.pawfindermy;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient client;
    private LatLng currentLatLng;
    private SearchView searchView;


    private static final String API_KEY = "PLACE_YOUR_API_KEY_HERE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps2);


        searchView = findViewById(R.id.sv_location);
        FloatingActionButton btnMapType = findViewById(R.id.btnMapType);

        client = LocationServices.getFusedLocationProviderClient(this);


        btnMapType.setOnClickListener(v -> {
            if (mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            } else {
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            }
        });


        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String location = searchView.getQuery().toString();
                List<Address> addressList = null;

                if (location != null || !location.equals("")) {
                    Geocoder geocoder = new Geocoder(MapsActivity.this);
                    try {

                        addressList = geocoder.getFromLocationName(location, 1);

                        if (addressList != null && !addressList.isEmpty()) {
                            Address address = addressList.get(0);
                            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());


                            currentLatLng = latLng;


                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));


                            mMap.clear();
                            mMap.addMarker(new MarkerOptions().position(latLng).title(location));
                            searchNearby("veterinary_care");
                        } else {
                            Toast.makeText(MapsActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(MapsActivity.this, "Search error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) { return false; }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Permission Check
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
        }




        mMap.setOnMarkerClickListener(marker -> {
            if ("You are here".equals(marker.getTitle())) return false;

            if (marker.getTitle().equals(searchView.getQuery().toString())) return false;

            Object tag = marker.getTag();
            if (tag != null && tag instanceof String) {
                String placeId = (String) tag;
                Intent intent = new Intent(MapsActivity.this, PlaceDetailActivity.class);
                intent.putExtra("place_id", placeId);
                startActivity(intent);
            }
            return true;
        });
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        mMap.setMyLocationEnabled(true);

        client.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14));
                mMap.addMarker(new MarkerOptions().position(currentLatLng).title("You are here"));


                searchNearby("veterinary_care");
            }
        });
    }

    private void searchNearby(String type) {
        if (currentLatLng == null) return;

        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=" + currentLatLng.latitude + "," + currentLatLng.longitude +
                "&radius=5000" +
                "&type=" + type +
                "&key=" + API_KEY;

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");

                        for (int i = 0; i < Math.min(results.length(), 20); i++) {
                            JSONObject place = results.getJSONObject(i);

                            String placeId = place.getString("place_id");
                            String name = place.getString("name");
                            String vicinity = place.optString("vicinity");

                            JSONObject loc = place.getJSONObject("geometry").getJSONObject("location");
                            LatLng latLng = new LatLng(loc.getDouble("lat"), loc.getDouble("lng"));

                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(name)
                                    .snippet(vicinity)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))); // BLUE color

                            if (marker != null) {
                                marker.setTag(placeId);
                            }


                            checkDistance(currentLatLng.latitude, currentLatLng.longitude, loc.getDouble("lat"), loc.getDouble("lng"), name);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show());

        queue.add(request);
    }

    private void checkDistance(double userLat, double userLng, double destLat, double destLng, String placeName) {
        float[] results = new float[1];
        Location.distanceBetween(userLat, userLng, destLat, destLng, results);
        if (results[0] < 1000) {
            showNotification(placeName);
        }
    }

    private void showNotification(String placeName) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "pawfinder_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "PawFinder Alerts", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Vet Nearby!")
                .setContentText("You are near " + placeName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        notificationManager.notify(placeName.hashCode(), builder.build());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 44 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        }
    }
}