package com.example.pawfindermy;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageRequest; // NEW IMPORT
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PlaceDetailActivity extends AppCompatActivity {


    private TextView txtName, txtAddress, txtCoordinates, txtPhone;
    private Button btnGoThere;
    private FloatingActionButton btnFavorite;
    private ImageView imagePlace;


    private String placeId;
    private String currentName;
    private String currentAddress;
    private double currentLat, currentLng;


    private DatabaseReference favRef;
    private boolean isFavorite = false;
    private FirebaseUser user;


    private static final String API_KEY = "PLACE_YOUR_API_KEY_HERE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_detail);


        txtName = findViewById(R.id.txtName);
        txtAddress = findViewById(R.id.txtAddress);
        txtCoordinates = findViewById(R.id.txtCoordinates);
        txtPhone = findViewById(R.id.txtPhone);
        btnGoThere = findViewById(R.id.btnGoThere);
        btnFavorite = findViewById(R.id.btnFavorite);
        imagePlace = findViewById(R.id.imagePlace);


        if (getIntent() != null && getIntent().hasExtra("place_id")) {
            placeId = getIntent().getStringExtra("place_id");
            fetchPlaceDetails(placeId);
        } else {
            Toast.makeText(this, "Error: No Place ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && placeId != null) {
            favRef = FirebaseDatabase.getInstance().getReference("favorites")
                    .child(user.getUid())
                    .child(placeId);
            checkFavoriteStatus();
        }


        btnFavorite.setOnClickListener(v -> toggleFavorite());


        btnGoThere.setOnClickListener(v -> {
            String uri = "google.navigation:q=" + currentLat + "," + currentLng;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "Google Maps not installed", Toast.LENGTH_SHORT).show();
            }
        });


        txtPhone.setOnClickListener(v -> {
            String phone = txtPhone.getText().toString();
            if (!phone.isEmpty() && !phone.equals("No contact info")) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phone));
                startActivity(intent);
            }
        });
    }


    private void fetchPlaceDetails(String placeId) {

        String url = "https://maps.googleapis.com/maps/api/place/details/json?" +
                "place_id=" + placeId +
                "&fields=name,formatted_address,geometry,formatted_phone_number,photos" +
                "&key=" + API_KEY;

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject result = response.getJSONObject("result");

                        // 1. Name
                        currentName = result.getString("name");
                        txtName.setText(currentName);

                        // 2. Address
                        if (result.has("formatted_address")) {
                            currentAddress = result.getString("formatted_address");
                            txtAddress.setText(currentAddress);
                        } else {
                            currentAddress = "Address not available";
                            txtAddress.setText(currentAddress);
                        }

                        // 3. Phone
                        if (result.has("formatted_phone_number")) {
                            txtPhone.setText(result.getString("formatted_phone_number"));
                        } else {
                            txtPhone.setText("No contact info");
                        }


                        JSONObject loc = result.getJSONObject("geometry").getJSONObject("location");
                        currentLat = loc.getDouble("lat");
                        currentLng = loc.getDouble("lng");
                        txtCoordinates.setText("Lat: " + currentLat + ", Lng: " + currentLng);


                        if (result.has("photos")) {
                            JSONArray photosArray = result.getJSONArray("photos");
                            JSONObject firstPhoto = photosArray.getJSONObject(0);
                            String photoReference = firstPhoto.getString("photo_reference");
                            // Fetch the actual image using the reference
                            fetchPhoto(photoReference, queue);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show()
        );

        queue.add(request);
    }

    private void fetchPhoto(String photoReference, RequestQueue queue) {

        String photoUrl = "https://maps.googleapis.com/maps/api/place/photo?" +
                "maxwidth=800" +
                "&photo_reference=" + photoReference +
                "&key=" + API_KEY;


        ImageRequest imageRequest = new ImageRequest(photoUrl,
                bitmap -> {

                    imagePlace.setImageBitmap(bitmap);
                },
                0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565,
                error -> {

                }
        );

        queue.add(imageRequest);
    }


    private void checkFavoriteStatus() {
        if (favRef == null) return;

        favRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    isFavorite = true;
                    btnFavorite.setImageResource(android.R.drawable.btn_star_big_on);
                } else {
                    isFavorite = false;
                    btnFavorite.setImageResource(android.R.drawable.btn_star_big_off);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void toggleFavorite() {
        if (user == null) {
            Toast.makeText(this, "Please login to save favorites", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentName == null || currentName.isEmpty()) {
            Toast.makeText(this, "Please wait for details to load...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isFavorite) {
            favRef.removeValue()
                    .addOnSuccessListener(aVoid -> Toast.makeText(PlaceDetailActivity.this, "Removed from Favorites", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(PlaceDetailActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            FavoriteVet vet = new FavoriteVet(placeId, currentName, currentAddress);
            favRef.setValue(vet)
                    .addOnSuccessListener(aVoid -> Toast.makeText(PlaceDetailActivity.this, "Added to Favorites!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(PlaceDetailActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }
}