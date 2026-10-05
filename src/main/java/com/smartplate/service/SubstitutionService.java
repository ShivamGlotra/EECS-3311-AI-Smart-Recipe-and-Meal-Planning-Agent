package com.smartplate.service;

import com.smartplate.domain.DietRules;
import com.smartplate.domain.Ingredient;
import com.smartplate.domain.Recipe;
import com.smartplate.domain.RecipeIngredient;
import com.smartplate.domain.SmartPlateException;
import com.smartplate.domain.Substitution;
import com.smartplate.domain.UserProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * F08: finds and applies a substitution from the predefined list, skipping substitutes that
 * clash with the user's allergies, dislikes or diet. No AI is involved.
 */
public class SubstitutionService {

    private final List<Substitution> substitutions;

    public SubstitutionService(List<Substitution> substitutions) {
        this.substitutions = List.copyOf(substitutions);
    }

    public static SubstitutionService withDefaultSubstitutions() {
        return new SubstitutionService(SampleData.substitutions());
    }

    /** All predefined substitutions for this ingredient (before profile filtering). */
    public List<Substitution> getSubstitutionsFor(Ingredient ingredient) {
        List<Substitution> result = new ArrayList<>();
        for (Substitution s : substitutions) {
            if (s.getOriginal().equals(ingredient)) {
                result.add(s);
            }
        }
        return result;
    }

    /** Returns the first substitute that does not conflict with the profile. */
    public Substitution findSubstitute(Ingredient ingredient, UserProfile profile) {
        List<Substitution> options = getSubstitutionsFor(ingredient);
        if (options.isEmpty()) {
            throw new SmartPlateException("No substitute is known for " + ingredient.getName() + ".");
        }
        for (Substitution s : options) {
            if (DietRules.conflicts(s.getSubstitute(), profile).isEmpty()) {
                return s;
            }
        }
        throw new SmartPlateException("Every known substitute for " + ingredient.getName()
                + " conflicts with your allergies or preferences.");
    }

    /** Returns a copy of the recipe with the ingredient replaced and its quantity converted. */
    public Recipe applySubstitution(Recipe recipe, Substitution substitution) {
        RecipeIngredient line = recipe.findIngredient(substitution.getOriginal().getName());
        if (line == null) {
            throw new SmartPlateException(recipe.getName() + " does not contain "
                    + substitution.getOriginal().getName() + ".");
        }
        String unit = substitution.getUnit() == null ? line.getUnit() : substitution.getUnit();
        RecipeIngredient replacement = new RecipeIngredient(substitution.getSubstitute(),
                line.getQuantity() * substitution.getConversionFactor(), unit);
        List<RecipeIngredient> ingredients = new ArrayList<>();
        for (RecipeIngredient ri : recipe.getIngredients()) {
            ingredients.add(ri == line ? replacement : ri);
        }
        return recipe.withIngredients(ingredients);
    }
}
