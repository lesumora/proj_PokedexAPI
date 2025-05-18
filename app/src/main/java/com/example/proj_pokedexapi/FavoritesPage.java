package com.example.proj_pokedexapi;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FavoritesPage extends AppCompatActivity {

    private static final String TAG = "FavoritesPage";
    private RecyclerView recyclerView;
    private ImageView ivBack;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ArrayList<Map<String, Object>> favoritePokemonList;
    private FavoritePokemonAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites_page);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        ivBack = findViewById(R.id.ivBack);

        // Set up RecyclerView
        favoritePokemonList = new ArrayList<>();
        adapter = new FavoritePokemonAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Handle back button
        ivBack.setOnClickListener(v -> finish());

        // Fetch favorites
        fetchFavorites();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LandingPage.class));
            finish();
        }
    }

    private void fetchFavorites() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in to view favorites", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        db.collection("users").document(userId).collection("favorites")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    favoritePokemonList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, Object> pokemon = new HashMap<>();
                        pokemon.put("name", document.getString("name"));
                        pokemon.put("id", document.getLong("id"));
                        favoritePokemonList.add(pokemon);
                    }
                    adapter.notifyDataSetChanged();
                    if (favoritePokemonList.isEmpty()) {
                        Toast.makeText(this, "No favorite Pokémon found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching favorites: " + e.getMessage());
                    Toast.makeText(this, "Error loading favorites", Toast.LENGTH_SHORT).show();
                });
    }

    private class FavoritePokemonAdapter extends RecyclerView.Adapter<FavoritePokemonAdapter.PokemonViewHolder> {

        @Override
        public PokemonViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LinearLayout layout = new LinearLayout(parent.getContext());
            layout.setOrientation(LinearLayout.VERTICAL);

            ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            marginParams.setMargins(100, 32, 100, 32);

            layout.setLayoutParams(marginParams);
            layout.setGravity(Gravity.CENTER);
            layout.setBackground(ContextCompat.getDrawable(parent.getContext(), R.drawable.custom_quit_btn));
            layout.setPadding(100, 32, 100, 32);
            layout.setClickable(true);
            layout.setFocusable(true);

            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new LinearLayout.LayoutParams(450, 450));
            imageView.setId(View.generateViewId());
            layout.addView(imageView);

            TextView textView = new TextView(parent.getContext());
            textView.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            textView.setGravity(Gravity.CENTER);
            textView.setTextSize(16);
            textView.setTextColor(android.graphics.Color.WHITE);
            layout.addView(textView);

            return new PokemonViewHolder(layout, imageView, textView);
        }

        @Override
        public void onBindViewHolder(PokemonViewHolder holder, int position) {
            Map<String, Object> pokemon = favoritePokemonList.get(position);
            String name = (String) pokemon.get("name");
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            holder.textView.setText(name);

            // Load Pokémon image
            String imageUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/" + pokemon.get("id") + ".png";
            Glide.with(FavoritesPage.this).load(imageUrl).into(holder.imageView);

            // Set click listener to open details page
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(FavoritesPage.this, PokemonDetailsPage.class);
                intent.putExtra("pokemon_name", (String) pokemon.get("name"));
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return favoritePokemonList.size();
        }

        class PokemonViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView textView;

            PokemonViewHolder(View itemView, ImageView imageView, TextView textView) {
                super(itemView);
                this.imageView = imageView;
                this.textView = textView;
            }
        }
    }
}