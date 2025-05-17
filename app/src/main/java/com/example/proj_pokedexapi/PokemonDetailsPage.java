package com.example.proj_pokedexapi;

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
import org.json.JSONArray;
import org.json.JSONObject;

public class PokemonDetailsPage extends AppCompatActivity {

    private static final String TAG = "PokemonDetailsPage";
    private TextView tvName, tvIdNo, tvType, tvResHp, tvResSpeed, tvResAttack, tvResDefense, tvResSpecialAtt, tvResSpecialDef;
    private ImageView imgPokemon;
    private Button backBtn;
    private RequestQueue requestQueue;

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
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
            Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Volley
        requestQueue = Volley.newRequestQueue(this);

        // Handle back button
        backBtn.setOnClickListener(v -> finish());

        // Get Pokémon name from Intent
        String pokemonName = getIntent().getStringExtra("pokemon_name");
        if (pokemonName == null || pokemonName.isEmpty()) {
            Log.e(TAG, "No Pokémon name received");
            Toast.makeText(this, "No Pokémon name provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Fetch Pokémon data
        String url = "https://pokeapi.co/api/v2/pokemon/" + pokemonName;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        // Name
                        String name = response.getString("name");
                        name = name.substring(0, 1).toUpperCase() + name.substring(1);
                        tvName.setText(name);

                        // ID
                        String id = "#" + response.getInt("id");
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
}