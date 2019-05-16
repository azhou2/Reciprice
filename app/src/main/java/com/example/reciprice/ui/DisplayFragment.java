package com.example.reciprice.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.reciprice.R;
import com.example.reciprice.model.Recipe;
import com.example.reciprice.model.RecipeWrapper;
import com.example.reciprice.model.RecipeResponse;
import com.example.reciprice.repo.RecipeService;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DisplayFragment extends Fragment {
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private RecipeAdapter recipeAdapter;
    private List<Recipe> recipes;
    private List<Recipe> savedRecipes;

    private EditText searchText;
    private Button searchButton;
    private ProgressBar loadingProgressbar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_display, container, false);

        wireWidgets(rootView);

        recipes = new ArrayList<>();
        savedRecipes = new ArrayList<>();
        layoutManager = new LinearLayoutManager(getActivity());
        recipeAdapter = new RecipeAdapter(recipes);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(recipeAdapter);

        registerForContextMenu(recyclerView);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recipes.clear();
                loadingProgressbar.setVisibility(View.VISIBLE);
                searchRecipes();
                // TODO: Hide keyboard
            }
        });

        return rootView;
    }

    private void wireWidgets(View rootView) {
        recyclerView = rootView.findViewById(R.id.recyclerView_display);
        searchText = rootView.findViewById(R.id.editText_display_search);
        searchButton = rootView.findViewById(R.id.button_display_search);
        loadingProgressbar = rootView.findViewById(R.id.progressbar_display_loading);
    }

    private void searchRecipes() {
        Retrofit retrofit = new Retrofit.Builder().
                baseUrl("https://api.edamam.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RecipeService service = retrofit.create(RecipeService.class);
        Call<RecipeResponse> recipeResponseCall = service.searchByKeyWord(searchText.getText().toString(), "255f9b26", "94b4e1023e6be9907d1210dfdcbfa935");

        recipeResponseCall.enqueue(new Callback<RecipeResponse>() {
            @Override
            public void onResponse(Call<RecipeResponse> call, Response<RecipeResponse> response) {
                Log.e("hits", response.body().getHits() + "");
                if (!response.body().getHits().isEmpty()) {
                    List<RecipeWrapper> recipeWrappers = response.body().getHits();

                    List<Recipe> newRecipes = addRecipes(recipeWrappers);
                    recipes.addAll(newRecipes);

                    loadingProgressbar.setVisibility(View.INVISIBLE);
                    recipeAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getContext(), "No recipes were found. Please enter another search.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<RecipeResponse> call, Throwable t) {
                Log.d("ENQUEUE", "onFailure: " + t.getMessage());
            }
        });

    }

    private List<Recipe> addRecipes(List<RecipeWrapper> recipeWrappers) {
        List<Recipe> newRecipes = new ArrayList<>();
        for(RecipeWrapper recipeWrapper : recipeWrappers){
            newRecipes.add(recipeWrapper.getRecipe());
        }
        return newRecipes;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.save:
                Recipe recipe = recipes.get(recipeAdapter.getPosition());
                savedRecipes.add(recipe);
                writeToFile(savedRecipes);
                Toast.makeText(getContext(), "Successfully Saved", Toast.LENGTH_SHORT).show();
                break;
        }
        return true;
    }

    public void writeToFile(List<Recipe> savedRecipes) {
        // check if the file already exists...if it doesn't:
        String fileName = "favoriteRecipes.txt";
        File file = new File(getActivity().getFilesDir(), fileName);
        Gson gson = new Gson();

        String s = gson.toJson(savedRecipes);

        FileOutputStream outputStream;

        try {
            outputStream = getActivity().openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(s.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();

        }
    }
}