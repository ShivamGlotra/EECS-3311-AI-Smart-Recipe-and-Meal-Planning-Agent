package com.smartplate.service;

import com.smartplate.domain.Recipe;
import com.smartplate.domain.RecipeIngredient;
import com.smartplate.domain.SmartPlateException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Data layer: the collection of recipes. The AI may only choose recipes that are in here.
 */
public class RecipeRepository {

    private final Map<String, Recipe> recipes = new LinkedHashMap<>();

    public RecipeRepository(List<Recipe> initialRecipes) {
        for (Recipe r : initialRecipes) {
            add(r);
        }
    }

    /** A repository loaded with the built-in sample recipes. */
    public static RecipeRepository withSampleRecipes() {
        return new RecipeRepository(SampleData.recipes());
    }

    public void add(Recipe recipe) {
        if (recipes.containsKey(recipe.getId())) {
            throw new SmartPlateException("Duplicate recipe id '" + recipe.getId() + "'.");
        }
        recipes.put(recipe.getId(), recipe);
    }

    /**
     * Returns recipes whose name, tags or ingredient names contain every word of the query
     * (case-insensitive). An empty query returns no recipes.
     */
    public List<Recipe> search(String query) {
        List<Recipe> result = new ArrayList<>();
        if (query == null || query.isBlank()) {
            return result;
        }
        String[] words = query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        for (Recipe r : recipes.values()) {
            String text = searchableText(r);
            boolean all = true;
            for (String w : words) {
                if (!text.contains(w)) {
                    all = false;
                    break;
                }
            }
            if (all) {
                result.add(r);
            }
        }
        return result;
    }

    /** Returns the recipe with this id (case-insensitive), or null. */
    public Recipe findById(String id) {
        if (id == null) {
            return null;
        }
        return recipes.get(id.trim().toLowerCase(Locale.ROOT));
    }

    public List<Recipe> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(recipes.values()));
    }

    public int size() {
        return recipes.size();
    }

    private static String searchableText(Recipe r) {
        StringBuilder sb = new StringBuilder(r.getId()).append(' ').append(r.getName().toLowerCase(Locale.ROOT));
        for (String tag : r.getDietaryTags()) {
            sb.append(' ').append(tag);
        }
        for (RecipeIngredient ri : r.getIngredients()) {
            sb.append(' ').append(ri.getName());
        }
        return sb.toString();
    }
}
