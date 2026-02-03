package com.example.pawfindermy;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView recyclerFavorites;
    private FavoritesAdapter adapter;
    private List<FavoriteVet> favList;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        recyclerFavorites = findViewById(R.id.recyclerFavorites);
        recyclerFavorites.setLayoutManager(new LinearLayoutManager(this));

        favList = new ArrayList<>();
        adapter = new FavoritesAdapter(favList);
        recyclerFavorites.setAdapter(adapter);


        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            mDatabase = FirebaseDatabase.getInstance().getReference("favorites").child(user.getUid());
            loadFavorites();
        } else {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadFavorites() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                favList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    FavoriteVet vet = data.getValue(FavoriteVet.class);
                    favList.add(vet);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FavoritesActivity.this, "Error loading data", Toast.LENGTH_SHORT).show();
            }
        });
    }


    class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {
        private List<FavoriteVet> list;

        public FavoritesAdapter(List<FavoriteVet> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FavoriteVet vet = list.get(position);
            holder.textName.setText(vet.name);
            holder.textAddress.setText(vet.address);


            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(FavoritesActivity.this, PlaceDetailActivity.class);
                intent.putExtra("place_id", vet.placeId);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textName, textAddress;
            ViewHolder(View itemView) {
                super(itemView);
                textName = itemView.findViewById(android.R.id.text1);
                textAddress = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}