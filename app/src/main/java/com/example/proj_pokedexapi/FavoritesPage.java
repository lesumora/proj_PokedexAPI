package com.example.proj_pokedexapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FavoritesPage extends AppCompatActivity {

    private static final String TAG = "FavoritesPage";
    private RecyclerView recyclerView;
    private ImageView ivBack;
    private Spinner spinnerTypeFilter;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RequestQueue requestQueue;
    private ArrayList<Map<String, Object>> favoritePokemonList;
    private ArrayList<Map<String, Object>> filteredPokemonList;
    private FavoritePokemonAdapter adapter;
    private ActivityResultLauncher<Intent> pokemonDetailsLauncher;
    private String selectedType = "all";

    private final String[] pokemonTypes = {"All", "Normal", "Fighting", "Flying", "Poison", "Ground", "Rock", "Bug", "Ghost",
            "Steel", "Fire", "Water", "Grass", "Electric", "Psychic", "Ice", "Dragon", "Dark", "Fairy"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites_page);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        requestQueue = Volley.newRequestQueue(this);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        ivBack = findViewById(R.id.ivBack);
        spinnerTypeFilter = findViewById(R.id.spinnerTypeFilter);

        // Set up RecyclerView
        favoritePokemonList = new ArrayList<>();
        filteredPokemonList = new ArrayList<>();
        adapter = new FavoritePokemonAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Set up Spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, pokemonTypes);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTypeFilter.setAdapter(spinnerAdapter);
        spinnerTypeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedType = parent.getItemAtPosition(position).toString().toLowerCase();
                filterFavoritesByType();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedType = "all";
                filterFavoritesByType();
            }
        });

        // Handle back button
        ivBack.setOnClickListener(v -> finish());

        // Set up ActivityResultLauncher for PokemonDetailsPage
        pokemonDetailsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getBooleanExtra("refresh_favorites", false)) {
                            fetchFavorites(); // Refresh favorites list
                        }
                    }
                });

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
                    filteredPokemonList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, Object> pokemon = new HashMap<>();
                        pokemon.put("name", document.getString("name"));
                        // Safely handle the ID
                        Object idObj = document.get("id");
                        if (idObj != null) {
                            try {
                                Long pokemonId;
                                if (idObj instanceof Long) {
                                    pokemonId = (Long) idObj;
                                } else if (idObj instanceof Integer) {
                                    pokemonId = ((Integer) idObj).longValue();
                                } else if (idObj instanceof String) {
                                    pokemonId = Long.parseLong((String) idObj);
                                } else {
                                    Log.e(TAG, "Invalid ID type for Pokémon: " + document.getString("name"));
                                    continue; // Skip invalid entry
                                }
                                pokemon.put("id", pokemonId);
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Error parsing ID for Pokémon: " + document.getString("name"), e);
                                continue; // Skip invalid entry
                            }
                        } else {
                            Log.e(TAG, "Missing ID for Pokémon: " + document.getString("name"));
                            continue; // Skip invalid entry
                        }
                        favoritePokemonList.add(pokemon);
                    }
                    filteredPokemonList.addAll(favoritePokemonList);
                    adapter.notifyDataSetChanged();
                    if (favoritePokemonList.isEmpty()) {
                        Toast.makeText(this, "No favorite Pokémon found", Toast.LENGTH_SHORT).show();
                    } else {
                        filterFavoritesByType(); // Apply filter after fetching
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching favorites: " + e.getMessage());
                    Toast.makeText(this, "Error loading favorites", Toast.LENGTH_SHORT).show();
                });
    }

    private void filterFavoritesByType() {
        filteredPokemonList.clear();
        if (selectedType.equals("all")) {
            filteredPokemonList.addAll(favoritePokemonList);
            adapter.notifyDataSetChanged();
            if (filteredPokemonList.isEmpty()) {
                Toast.makeText(this, "No favorite Pokémon found", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Fetch Pokémon type data from PokeAPI
        String url = "https://pokeapi.co/api/v2/type/" + selectedType;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray pokemonArray = response.getJSONArray("pokemon");
                        ArrayList<String> pokemonNamesInType = new ArrayList<>();
                        for (int i = 0; i < pokemonArray.length(); i++) {
                            JSONObject pokemon = pokemonArray.getJSONObject(i);
                            String name = pokemon.getJSONObject("pokemon").getString("name");
                            pokemonNamesInType.add(name);
                        }

                        // Filter favorites that match the selected type
                        for (Map<String, Object> pokemon : favoritePokemonList) {
                            String name = (String) pokemon.get("name");
                            if (name != null && pokemonNamesInType.contains(name)) {
                                filteredPokemonList.add(pokemon);
                            }
                        }

                        adapter.notifyDataSetChanged();
                        if (filteredPokemonList.isEmpty()) {
                            Toast.makeText(this, "No favorite Pokémon of type " + selectedType, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing type data: " + e.getMessage());
                        Toast.makeText(this, "Error filtering by type", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Network error: " + error.getMessage());
                    Toast.makeText(this, "Network error while filtering", Toast.LENGTH_SHORT).show();
                });
        requestQueue.add(request);
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

            Button removeButton = new Button(parent.getContext());
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            buttonParams.setMargins(50, 30, 50, 0);
            removeButton.setLayoutParams(buttonParams);
            removeButton.setText("Remove from Favorites");
            removeButton.setTextSize(14);
            removeButton.setBackgroundResource(R.drawable.custom_btn);
            removeButton.setId(View.generateViewId());
            layout.addView(removeButton);

            return new PokemonViewHolder(layout, imageView, textView, removeButton);
        }

        @Override
        public void onBindViewHolder(PokemonViewHolder holder, int position) {
            Map<String, Object> pokemon = filteredPokemonList.get(position);
            String name = (String) pokemon.get("name");
            if (name != null) {
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
                holder.textView.setText(name);
            } else {
                holder.textView.setText("Unknown Pokémon");
            }

            // Load Pokémon image
            Long pokemonId = null;
            Object idObj = pokemon.get("id");
            if (idObj != null) {
                try {
                    if (idObj instanceof Long) {
                        pokemonId = (Long) idObj;
                    } else if (idObj instanceof Integer) {
                        pokemonId = ((Integer) idObj).longValue();
                    } else if (idObj instanceof String) {
                        pokemonId = Long.parseLong((String) idObj);
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing ID for Pokémon: " + name, e);
                }
            }
            if (pokemonId != null) {
                String imageUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/" + pokemonId + ".png";
                Glide.with(FavoritesPage.this).load(imageUrl).into(holder.imageView);
            } else {
                holder.imageView.setImageDrawable(null); // Clear image if ID is invalid
                Log.e(TAG, "Invalid or missing ID for Pokémon: " + name);
            }

            // Set click listener to open details page
            holder.itemView.setOnClickListener(v -> {
                String pokemonName = (String) pokemon.get("name");
                if (pokemonName != null) {
                    Intent intent = new Intent(FavoritesPage.this, PokemonDetailsPage.class);
                    intent.putExtra("pokemon_name", pokemonName);
                    pokemonDetailsLauncher.launch(intent);
                } else {
                    Toast.makeText(FavoritesPage.this, "Invalid Pokémon data", Toast.LENGTH_SHORT).show();
                }
            });

            // Set click listener for remove button
            Long finalPokemonId = pokemonId; // Use final variable for lambda
            holder.removeButton.setOnClickListener(v -> {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user == null) {
                    Toast.makeText(FavoritesPage.this, "Please log in to manage favorites", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (finalPokemonId == null) {
                    Toast.makeText(FavoritesPage.this, "Invalid Pokémon ID", Toast.LENGTH_SHORT).show();
                    return;
                }

                String userId = user.getUid();
                holder.removeButton.setEnabled(false);

                db.collection("users").document(userId).collection("favorites").document(String.valueOf(finalPokemonId))
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Pokémon removed from favorites: ");
                            Toast.makeText(FavoritesPage.this," removed from favorites", Toast.LENGTH_SHORT).show();
                            favoritePokemonList.remove(pokemon);
                            filteredPokemonList.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, filteredPokemonList.size());
                            if (filteredPokemonList.isEmpty()) {
                                Toast.makeText(FavoritesPage.this, "No favorite Pokémon found", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error removing from favorites: " + e.getMessage());
                            Toast.makeText(FavoritesPage.this, "Error removing from favorites", Toast.LENGTH_SHORT).show();
                            holder.removeButton.setEnabled(true);
                        });
            });
        }

        @Override
        public int getItemCount() {
            return filteredPokemonList.size();
        }

        class PokemonViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView textView;
            Button removeButton;

            PokemonViewHolder(View itemView, ImageView imageView, TextView textView, Button removeButton) {
                super(itemView);
                this.imageView = imageView;
                this.textView = textView;
                this.removeButton = removeButton;
            }
        }
    }
}