package com.example.proj_pokedexapi;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

public class QuizPage extends AppCompatActivity {

    private ImageView ivBack, ivNext, ivSilhouette, btnHint;
    private EditText etGuess;
    private Button btnGuess;
    private TextView tvPokemonName;
    private Spinner spnrTypes;
    private String currentPokemonName = "";
    private boolean isGuessPhase = true;
    private String selectedType = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz_page);

        ivSilhouette = findViewById(R.id.ivSilhouette);
        btnHint = findViewById(R.id.btnHint);
        btnGuess = findViewById(R.id.btnGuess);
        tvPokemonName = findViewById(R.id.tvPokemonName);
        etGuess = findViewById(R.id.etGuess);
        spnrTypes = findViewById(R.id.spnrTypes);
        ivBack = findViewById(R.id.ivBack);
        ivNext = findViewById(R.id.ivNext);

        ivBack.setOnClickListener(v -> {
            finish();
        });

        ivNext.setOnClickListener(v -> {
            generateRandomPokemon();
        });

        String[] types = new String[]{
                "all", "normal", "fire", "water", "electric", "grass", "ice", "fighting",
                "poison", "ground", "flying", "psychic", "bug", "rock", "ghost", "dragon",
                "dark", "steel", "fairy"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spnrTypes.setAdapter(adapter);

        spnrTypes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedType = types[position];
                generateRandomPokemon();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnHint.setOnClickListener(view -> showFirstLetterClue());
        btnGuess.setOnClickListener(this::onGuessOrNext);

        generateRandomPokemon();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void onGuessOrNext(View view) {
        if (isGuessPhase) {
            checkGuess(view);
        } else {
            resetForNextPokemon();
        }
    }

    private void generateRandomPokemon() {
        if (selectedType.equals("all")) {
            int maxPokemon = 898;
            int randomId = new Random().nextInt(maxPokemon) + 1;
            fetchPokemonSilhouette(randomId);
        } else {
            fetchPokemonByType(selectedType);
        }
    }

    private void fetchPokemonByType(String type) {
        String url = "https://pokeapi.co/api/v2/type/" + type;

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject data = new JSONObject(response);
                        var pokemonArray = data.getJSONArray("pokemon");

                        int count = pokemonArray.length();
                        if (count == 0) {
                            Toast.makeText(this, "No Pokémon of this type found!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int randomIndex = new Random().nextInt(count);
                        JSONObject pokemonObj = pokemonArray.getJSONObject(randomIndex).getJSONObject("pokemon");
                        String pokemonUrl = pokemonObj.getString("url");

                        fetchPokemonByUrl(pokemonUrl);

                    } catch (JSONException e) {
                        Toast.makeText(this, "JSON parsing error", Toast.LENGTH_LONG).show();
                    }
                },
                error -> Toast.makeText(this, "Failed to load Pokémon of type " + type, Toast.LENGTH_LONG).show()
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void fetchPokemonByUrl(String url) {
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject data = new JSONObject(response);
                        String img = data.getJSONObject("sprites").getString("front_default");
                        currentPokemonName = data.getString("name");

                        Glide.with(this).load(img).into(ivSilhouette);
                        ivSilhouette.setColorFilter(0xFF000000, android.graphics.PorterDuff.Mode.MULTIPLY);
                        etGuess.setText("");
                        tvPokemonName.setText("Who's this lil shit?");

                        btnHint.setVisibility(View.VISIBLE);
                        etGuess.setVisibility(View.VISIBLE);
                        isGuessPhase = true;

                    } catch (JSONException e) {
                        Toast.makeText(this, "JSON parsing error", Toast.LENGTH_LONG).show();
                    }
                },
                error -> Toast.makeText(this, "Failed to load Pokémon details", Toast.LENGTH_LONG).show()
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void fetchPokemonSilhouette(int id) {
        String url = "https://pokeapi.co/api/v2/pokemon/" + id;
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject data = new JSONObject(response);
                        String img = data.getJSONObject("sprites").getString("front_default");
                        currentPokemonName = data.getString("name");

                        Glide.with(QuizPage.this).load(img).into(ivSilhouette);
                        ivSilhouette.setColorFilter(0xFF000000, android.graphics.PorterDuff.Mode.MULTIPLY);
                        etGuess.setText("");
                        tvPokemonName.setText("Who's this lil shit?");

                        btnHint.setVisibility(View.VISIBLE);
                        etGuess.setVisibility(View.VISIBLE);
                        isGuessPhase = true;

                    } catch (JSONException e) {
                        Toast.makeText(QuizPage.this, "JSON parsing error", Toast.LENGTH_LONG).show();
                    }
                },
                error -> Toast.makeText(QuizPage.this, "Failed to load Pokémon", Toast.LENGTH_LONG).show()
        );

        RequestQueue queue = Volley.newRequestQueue(QuizPage.this);
        queue.add(request);
    }

    private void checkGuess(View view) {
        String guess = etGuess.getText().toString().trim().toLowerCase();
        if (guess.isEmpty()) {
            Toast.makeText(this, "Please enter your guess", Toast.LENGTH_SHORT).show();
            return;
        }
        if (guess.equals(currentPokemonName.toLowerCase())) {
            ivSilhouette.clearColorFilter();
            tvPokemonName.setText("It's " + capitalize(currentPokemonName) + "!");
            Toast.makeText(this, "Correct! It's " + capitalize(currentPokemonName) + "!", Toast.LENGTH_LONG).show();

            btnGuess.setText("Next");
            btnHint.setVisibility(View.GONE);
            etGuess.setVisibility(View.GONE);

            isGuessPhase = false;
        } else {
            Toast.makeText(this, "Wrong! Try again!", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetForNextPokemon() {
        generateRandomPokemon();
    }

    private void showFirstLetterClue() {
        if (!currentPokemonName.isEmpty()) {
            String firstLetter = currentPokemonName.substring(0, 1).toUpperCase();
            String message = "Clue: It starts with \"" + firstLetter + "\"";


            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 40, 50, 40);

            TextView messageView = new TextView(this);
            messageView.setText(message);
            messageView.setTextSize(18);
            messageView.setTextColor(Color.BLACK);

            layout.addView(messageView);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(layout)
                    .setPositiveButton("OK", null)
                    .create()
                    .show();
        } else {
            Toast.makeText(this, "Load a Pokémon first!", Toast.LENGTH_SHORT).show();
        }
    }

    private String capitalize(String str) {
        if (str == null || str.length() == 0) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}