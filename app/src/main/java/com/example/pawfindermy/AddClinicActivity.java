package com.example.pawfindermy;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddClinicActivity extends AppCompatActivity {


    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_GALLERY_IMAGE = 2;
    static final int REQUEST_CAMERA_PERMISSION = 100;
    static final int REQUEST_LOCATION_PERMISSION = 44;


    private ImageView imgPreview;
    private EditText etName, etNotes;
    private Button btnCapture, btnCheckIn;
    private RecyclerView recyclerHistory;


    private Bitmap currentImageBitmap = null;
    private DatabaseReference mDatabase;
    private FusedLocationProviderClient fusedLocationClient;


    private HistoryAdapter adapter;
    private List<Visit> visitList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_clinic);


        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            mDatabase = FirebaseDatabase.getInstance().getReference("visits").child(user.getUid());
        } else {
            Toast.makeText(this, "Please Login First", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        imgPreview = findViewById(R.id.imgPreview);
        etName = findViewById(R.id.etName);
        etNotes = findViewById(R.id.etNotes);
        btnCapture = findViewById(R.id.btnCapture);
        btnCheckIn = findViewById(R.id.btnCheckIn);
        recyclerHistory = findViewById(R.id.recyclerHistory);


        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        visitList = new ArrayList<>();
        adapter = new HistoryAdapter(visitList);
        recyclerHistory.setAdapter(adapter);


        loadHistory();



        btnCapture.setOnClickListener(v -> showImageSourceDialog());

        btnCheckIn.setOnClickListener(v -> saveVisitWithLocation());
    }


    private void showImageSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {

                checkCameraPermissionAndOpen();
            } else {

                openGallery();
            }
        });
        builder.show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY_IMAGE);
    }


    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            try {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } catch (Exception e) {
                Toast.makeText(this, "Camera not found", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    currentImageBitmap = (Bitmap) extras.get("data");
                    imgPreview.setImageBitmap(currentImageBitmap);
                }
            }

            else if (requestCode == REQUEST_GALLERY_IMAGE && data != null) {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    try {
                        InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        currentImageBitmap = BitmapFactory.decodeStream(imageStream);
                        imgPreview.setImageBitmap(currentImageBitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }


    private void deleteVisit(Visit visit) {
        if (visit.id == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Delete Record")
                .setMessage("Are you sure you want to delete this visit to " + visit.clinicName + "?")
                .setPositiveButton("Delete", (dialog, which) -> {

                    mDatabase.child(visit.id).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(AddClinicActivity.this, "Record Deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(AddClinicActivity.this, "Error deleting", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void saveVisitWithLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return;
        }

        Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show();

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                saveToFirebase(location.getLatitude(), location.getLongitude());
            } else {
                Toast.makeText(this, "GPS Error. Open Google Maps first.", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Location Error", Toast.LENGTH_SHORT).show());
    }

    private void saveToFirebase(double lat, double lng) {
        String name = etName.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Required");
            return;
        }

        String imageString = (currentImageBitmap != null) ? bitmapToString(currentImageBitmap) : "";
        String id = mDatabase.push().getKey();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

        Visit newVisit = new Visit(id, name, notes, timestamp, lat, lng, imageString);

        if (id != null) {
            mDatabase.child(id).setValue(newVisit)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AddClinicActivity.this, "Saved!", Toast.LENGTH_SHORT).show();


                        scheduleFollowUpNotification(name);

                        // Clear inputs
                        etName.setText("");
                        etNotes.setText("");
                        imgPreview.setImageResource(android.R.drawable.ic_menu_camera);
                        currentImageBitmap = null;
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }


    private void showVisitDetails(Visit visit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(visit.clinicName);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 20);

        TextView tvDate = new TextView(this);
        tvDate.setText("Visited on: " + visit.timestamp);
        tvDate.setTextSize(14);
        layout.addView(tvDate);

        TextView tvLoc = new TextView(this);
        tvLoc.setText("GPS: " + visit.latitude + ", " + visit.longitude);
        tvLoc.setTextSize(14);
        tvLoc.setTextColor(android.graphics.Color.GRAY);
        tvLoc.setPadding(0, 0, 0, 30);
        layout.addView(tvLoc);

        TextView tvNotes = new TextView(this);
        tvNotes.setText("Notes:\n" + visit.notes);
        tvNotes.setTextSize(16);
        layout.addView(tvNotes);

        if (visit.imageBase64 != null && !visit.imageBase64.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(visit.imageBase64, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                ImageView imageView = new ImageView(this);
                imageView.setImageBitmap(decodedByte);
                imageView.setAdjustViewBounds(true);
                imageView.setMaxHeight(600);
                layout.addView(imageView);

            } catch (Exception e) {}
        }

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(layout);
        builder.setView(scrollView);
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void scheduleFollowUpNotification(String clinicName) {
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("clinicName", clinicName);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long triggerTime = System.currentTimeMillis() + (10 * 1000);
        if (alarmManager != null) {
            try {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } catch (SecurityException e) {}
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) openCamera();
        }
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) saveVisitWithLocation();
        }
    }

    // --- HELPERS ---
    private String bitmapToString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    private void loadHistory() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                visitList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Visit visit = postSnapshot.getValue(Visit.class);
                    visitList.add(0, visit);
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<Visit> list;
        public HistoryAdapter(List<Visit> list) { this.list = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Visit visit = list.get(position);
            holder.text1.setText(visit.clinicName + " (" + visit.timestamp + ")");
            holder.text2.setText(visit.notes);


            holder.itemView.setOnClickListener(v -> showVisitDetails(visit));


            holder.itemView.setOnLongClickListener(v -> {
                deleteVisit(visit);
                return true;
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}