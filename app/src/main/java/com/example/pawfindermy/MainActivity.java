package com.example.pawfindermy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private Button btnFindVet, btnAddClinic, btnViewFavorites, btnLogout;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();


        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }


        btnFindVet = findViewById(R.id.btnFindVet);
        btnAddClinic = findViewById(R.id.btnAddClinic);
        btnViewFavorites = findViewById(R.id.btnViewFavorites); // NEW BUTTON
        btnLogout = findViewById(R.id.btnLogout);


        btnFindVet.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
            startActivity(intent);
        });


        btnAddClinic.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddClinicActivity.class);
            startActivity(intent);
        });


        btnViewFavorites.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FavoritesActivity.class);
            startActivity(intent);
        });


        btnLogout.setOnClickListener(v -> {
            // Sign out from Firebase
            mAuth.signOut();



            Toast.makeText(MainActivity.this, "Signed Out", Toast.LENGTH_SHORT).show();


            Intent intent = new Intent(MainActivity.this, LoginActivity.class);

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        Button btnAboutUs = findViewById(R.id.btnAboutUs);

        btnAboutUs.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AboutUsActivity.class);
            startActivity(intent);
        });
    }
}