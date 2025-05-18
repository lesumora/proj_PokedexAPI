package com.example.proj_pokedexapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class PokemonDetailsPage extends AppCompatActivity {

    private static final String TAG = "PokemonDetailsPage";
    private TextView tvName, tvIdNo, tvType, tvResHp, tvResSpeed, tvResAttack, tvResDefense, tvResSpecialAtt, tvResSpecialDef;
    private ImageView imgPokemon;
    private Button backBtn, favoriteBtn;
    private RequestQueue requestQueue;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String pokemonName;
    private int pokemonId;
    private boolean isFavorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_pokemon_details_page);
        } catch (Exception e) {
            Log.e(TAG, "Error setting content view: " + e.getMessage());
            Toast.makeText(this, "Error loading layout", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        try {
            tvName = findViewById(R.id.tv_name);
            tvIdNo = findViewById(R.id.tv_idNo);
            tvType = findViewById(R.id.tv_type);
            imgPokemon = findViewById(R.id.img_pokemon);
            tvResHp = findViewById(R.id.tv_resHp);
            tvResSpeed = findViewById(R.id.tv_resSpeed);
            tvResAttack = findViewById(R.id.tv_resAttack);
            tvResDefense = findViewById(R.id.tv_resDefense);
            tvResSpecialAtt = findViewById(R.id.tv_resSpecialAtt);
            tvResSpecialDef = findViewById(R.id.tv_resSpecialDef);
            backBtn = findViewById(R.id.back_btn);
            favoriteBtn = findViewById(R.id.btnFave);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
            Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Volley
        requestQueue = Volley.newRequestQueue(this);

        // Handle back button
        backBtn.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("refresh_favorites", true);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        });

        // Get Pokémon name from Intent
        pokemonName = getIntent().getStringExtra("pokemon_name");
        if (pokemonName == null || pokemonName.isEmpty()) {
            Log.e(TAG, "No Pokémon name received");
            Toast.makeText(this, "No Pokémon name provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set up favorite button listener
        favoriteBtn.setOnClickListener(v -> {
            if (isFavorite) {
                removeFromFavorites();
            } else {
                addToFavorites();
            }
        });

        // Fetch Pokémon data
        fetchPokemonData();
    }

    private void fetchPokemonData() {
        String url = "https://pokeapi.co/api/v2/pokemon/" + pokemonName;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        // Name
                        String name = response.getString("name");
                        name = name.substring(0, 1).toUpperCase() + name.substring(1);
                        tvName.setText(name);

                        // ID
                        pokemonId = response.getInt("id");
                        String id = "#" + pokemonId;
                        tvIdNo.setText(id);

                        // Type(s)
                        JSONArray types = response.getJSONArray("types");
                        StringBuilder typeText = new StringBuilder();
                        for (int i = 0; i < types.length(); i++) {
                            String type = types.getJSONObject(i).getJSONObject("type").getString("name");
                            type = type.substring(0, 1).toUpperCase() + type.substring(1);
                            typeText.append(type);
                            if (i < types.length() - 1) typeText.append(", ");
                        }
                        tvType.setText(typeText.toString());

                        // Image
                        String imageUrl = response.getJSONObject("sprites").getString("front_default");
                        if (!isFinishing()) {
                            Glide.with(this).load(imageUrl).into(imgPokemon);
                        }

                        // Base Stats
                        JSONArray stats = response.getJSONArray("stats");
                        for (int i = 0; i < stats.length(); i++) {
                            JSONObject stat = stats.getJSONObject(i);
                            int value = stat.getInt("base_stat");
                            String statName = stat.getJSONObject("stat").getString("name");
                            switch (statName) {
                                case "hp":
                                    tvResHp.setText(String.valueOf(value));
                                    break;
                                case "speed":
                                    tvResSpeed.setText(String.valueOf(value));
                                    break;
                                case "attack":
                                    tvResAttack.setText(String.valueOf(value));
                                    break;
                                case "defense":
                                    tvResDefense.setText(String.valueOf(value));
                                    break;
                                case "special-attack":
                                    tvResSpecialAtt.setText(String.valueOf(value));
                                    break;
                                case "special-defense":
                                    tvResSpecialDef.setText(String.valueOf(value));
                                    break;
                            }
                        }

                        // Check favorite status after fetching pokemonId
                        checkFavoriteStatus();
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing Pokémon data: " + e.getMessage());
                        tvName.setText("Error loading Pokémon");
                        Toast.makeText(this, "Error loading Pokémon data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching Pokémon data: " + error.getMessage());
                    Toast.makeText(this, "Pokémon not found", Toast.LENGTH_SHORT).show();
                    finish();
                });
        requestQueue.add(request);
    }

    private void checkFavoriteStatus() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            favoriteBtn.setText("Add to Favorites");
            isFavorite = false;
            favoriteBtn.setEnabled(false);
            Toast.makeText(this, "Please log in to manage favorites", Toast.LENGTH_SHORT).show();
            return;
        }

        favoriteBtn.setEnabled(true);
        String userId = user.getUid();
        db.collection("users").document(userId).collection("favorites").document(String.valueOf(pokemonId))
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    isFavorite = documentSnapshot.exists();
                    favoriteBtn.setText(isFavorite ? "Remove from Favorites" : "Add to Favorites");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking favorite status: " + e.getMessage());
                    Toast.makeText(this, "Error checking favorites", Toast.LENGTH_SHORT).show();
                    favoriteBtn.setText("Add to Favorites");
                    isFavorite = false;
                });
    }

    private void addToFavorites() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in to save favorites", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        Map<String, Object> favorite = new HashMap<>();
        favorite.put("name", pokemonName);
        favorite.put("id", pokemonId);

        db.collection("users").document(userId).collection("favorites").document(String.valueOf(pokemonId))
                .set(favorite)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Pokémon added to favorites");
                    Toast.makeText(this, pokemonName + " added to favorites", Toast.LENGTH_SHORT).show();
                    isFavorite = true;
                    favoriteBtn.setText("Remove from Favorites");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding to favorites: " + e.getMessage());
                    Toast.makeText(this, "Error saving to favorites", Toast.LENGTH_SHORT).show();
                });
    }

    private void removeFromFavorites() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in to manage favorites", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        db.collection("users").document(userId).collection("favorites").document(String.valueOf(pokemonId))
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Pokémon removed from favorites");
                    Toast.makeText(this, pokemonName + " removed from favorites", Toast.LENGTH_SHORT).show();
                    isFavorite = false;
                    favoriteBtn.setText("Add to Favorites");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error removing from favorites: " + e.getMessage());
                    Toast.makeText(this, "Error removing from favorites", Toast.LENGTH_SHORT).show();
                });
    }
}