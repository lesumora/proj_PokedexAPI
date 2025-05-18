package com.example.proj_pokedexapi;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
import org.json.JSONObject;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ImageView ivPlay, ivSettings, ivSearch, ivFavorites;
    private EditText etSearch;
    private Spinner spinnerTypeFilter;
    private FirebaseAuth mAuth;
    private RecyclerView recyclerView;
    private Button btnViewMore;
    private RequestQueue requestQueue;
    private ArrayList<JSONObject> pokemonList;
    private PokemonAdapter adapter;
    private int offset = 0;
    private final int limit = 5;
    private String selectedType = "all";

    String[] pokemonTypes = {"All", "Normal", "Fighting", "Flying", "Poison", "Ground", "Rock", "Bug", "Ghost",
            "Steel", "Fire", "Water", "Grass", "Electric", "Psychic", "Ice", "Dragon", "Dark", "Fairy"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ivPlay = findViewById(R.id.ivPlay);
        ivSettings = findViewById(R.id.ivSettings);
        ivFavorites = findViewById(R.id.ivFavorites);
        ivSearch = findViewById(R.id.ivSearch);
        etSearch = findViewById(R.id.etSearch);
        spinnerTypeFilter = findViewById(R.id.spinnerTypeFilter);
        recyclerView = findViewById(R.id.recyclerView);
        btnViewMore = findViewById(R.id.btnViewMore);
        requestQueue = Volley.newRequestQueue(this);
        pokemonList = new ArrayList<>();
        adapter = new PokemonAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        ivPlay.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, QuizPage.class)));
        ivSettings.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SettingsPage.class)));
        ivFavorites.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, FavoritesPage.class)));

        mAuth = FirebaseAuth.getInstance();

        // Set up Spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, pokemonTypes);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTypeFilter.setAdapter(spinnerAdapter);
        spinnerTypeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedType = parent.getItemAtPosition(position).toString().toLowerCase();
                offset = 0;
                pokemonList.clear();
                adapter.notifyDataSetChanged();
                fetchPokemon();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedType = "all";
                fetchPokemon();
            }
        });

        // Search button click listener
        ivSearch.setOnClickListener(v -> performSearch());

        // Search on Enter key in EditText
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        fetchPokemon();

        btnViewMore.setOnClickListener(v -> {
            offset += limit;
            fetchPokemon();
        });

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
            startActivity(new Intent(this, LandingPage.class));
            finish();
        }
    }

    private void performSearch() {
        String query = etSearch.getText().toString().trim().toLowerCase();
        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a Pokémon name", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(MainActivity.this, PokemonDetailsPage.class);
        intent.putExtra("pokemon_name", query);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error launching details page", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchPokemon() {
        if (selectedType.equals("all")) {
            String url = "https://pokeapi.co/api/v2/pokemon?limit=" + limit + "&offset=" + offset;
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        try {
                            JSONArray results = response.getJSONArray("results");
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject pokemon = results.getJSONObject(i);
                                String detailUrl = pokemon.getString("url");
                                fetchPokemonDetails(detailUrl);
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "Error parsing data", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show());
            requestQueue.add(request);
        } else {
            String url = "https://pokeapi.co/api/v2/type/" + selectedType;
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        try {
                            JSONArray pokemonArray = response.getJSONArray("pokemon");
                            int start = offset;
                            int end = Math.min(offset + limit, pokemonArray.length());
                            for (int i = start; i < end; i++) {
                                JSONObject pokemon = pokemonArray.getJSONObject(i);
                                String detailUrl = pokemon.getJSONObject("pokemon").getString("url");
                                fetchPokemonDetails(detailUrl);
                            }
                            // Disable View More button if no more Pokémon to load
                            btnViewMore.setEnabled(end < pokemonArray.length());
                        } catch (Exception e) {
                            Toast.makeText(this, "Error parsing type data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show());
            requestQueue.add(request);
        }
    }

    private void fetchPokemonDetails(String url) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    pokemonList.add(response);
                    adapter.notifyDataSetChanged();
                },
                error -> Toast.makeText(this, "Error fetching details", Toast.LENGTH_SHORT).show());
        requestQueue.add(request);
    }

    private class PokemonAdapter extends RecyclerView.Adapter<PokemonAdapter.PokemonViewHolder> {

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
            textView.setTextColor(Color.WHITE);
            layout.addView(textView);

            return new PokemonViewHolder(layout, imageView, textView);
        }

        @Override
        public void onBindViewHolder(PokemonViewHolder holder, int position) {
            try {
                JSONObject pokemon = pokemonList.get(position);
                String name = pokemon.getString("name");
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
                holder.textView.setText(name);
                JSONObject sprites = pokemon.getJSONObject("sprites");
                String imageUrl = sprites.getString("front_default");
                Glide.with(MainActivity.this).load(imageUrl).into(holder.imageView);

                // Set click listener on the layout
                holder.itemView.setOnClickListener(v -> {
                    try {
                        String pokemonName = pokemon.getString("name").toLowerCase();
                        Intent intent = new Intent(MainActivity.this, PokemonDetailsPage.class);
                        intent.putExtra("pokemon_name", pokemonName);
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Error launching details page", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                holder.textView.setText("Error loading Pokémon");
            }
        }

        @Override
        public int getItemCount() {
            return pokemonList.size();
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