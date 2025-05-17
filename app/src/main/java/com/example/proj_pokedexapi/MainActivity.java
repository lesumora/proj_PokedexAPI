package com.example.proj_pokedexapi;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ImageView ivPlay, ivSettings;
    private FirebaseAuth mAuth;
    private RecyclerView recyclerView;
    private Button btnViewMore;
    private RequestQueue queue;
    private List<String> pokemonNames = new ArrayList<>();
    private List<String> pokemonUrls = new ArrayList<>();
    private PokemonAdapter adapter;
    private String nextUrl = "https://pokeapi.co/api/v2/pokemon?limit=5";
    private boolean isLoading = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ivPlay = findViewById(R.id.ivPlay);
        ivSettings = findViewById(R.id.ivSettings);
        recyclerView = findViewById(R.id.recyclerView);
        btnViewMore = findViewById(R.id.btnViewMore);
        queue = Volley.newRequestQueue(this);
        adapter = new PokemonAdapter(pokemonNames);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        ivPlay.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, QuizPage.class));
        });

        ivSettings.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsPage.class));
        });

        mAuth = FirebaseAuth.getInstance();

        adapter.setOnPokemonClickListener(position -> {
            String url = pokemonUrls.get(position);
            fetchPokemonDetails(url);
        });

        loadPokemons();

        btnViewMore.setOnClickListener(v -> loadPokemons());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginPage.class));
            finish();
        }
    }

    private void loadPokemons() {
        if (isLoading || nextUrl == null) {
            if (nextUrl == null) {
                Toast.makeText(this, "No more Pokémon to load", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        isLoading = true;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, nextUrl, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        int start = pokemonNames.size();
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject pokemon = results.getJSONObject(i);
                            String name = pokemon.getString("name");
                            String url = pokemon.getString("url");
                            pokemonNames.add(name);
                            pokemonUrls.add(url);
                        }
                        adapter.notifyItemRangeInserted(start, results.length());
                        nextUrl = response.optString("next", null);
                        btnViewMore.setEnabled(nextUrl != null);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Error parsing Pokémon data", Toast.LENGTH_SHORT).show();
                    } finally {
                        isLoading = false;
                    }
                },
                error -> {
                    Toast.makeText(MainActivity.this, "Error loading Pokémon data", Toast.LENGTH_SHORT).show();
                    isLoading = false;
                }
        );
        queue.add(request);
    }

    private void fetchPokemonDetails(String url) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String name = response.getString("name");
                        String spriteUrl = response.getJSONObject("sprites").getString("front_default");
                        JSONArray stats = response.getJSONArray("stats");
                        StringBuilder statsBuilder = new StringBuilder();
                        for (int i = 0; i < stats.length(); i++) {
                            JSONObject statObj = stats.getJSONObject(i);
                            String statName = statObj.getJSONObject("stat").getString("name");
                            int baseStat = statObj.getInt("base_stat");
                            statsBuilder.append(statName).append(": ").append(baseStat).append("\n");
                        }
                        showPokemonDialog(name, spriteUrl, statsBuilder.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Error parsing Pokémon details", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(MainActivity.this, "Failed to load Pokémon details", Toast.LENGTH_SHORT).show();
                }
        );
        queue.add(request);
    }

    private void showPokemonDialog(String name, String spriteUrl, String stats) {
        View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_pokemon_stats, null);
        ImageView ivPokemon = dialogView.findViewById(R.id.ivPokemon);
        TextView tvStats = dialogView.findViewById(R.id.tvStats);
        Glide.with(this).load(spriteUrl).into(ivPokemon);
        tvStats.setText(stats);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        String capitalizedName = name.substring(0,1).toUpperCase() + name.substring(1);
        builder.setTitle(capitalizedName);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

}