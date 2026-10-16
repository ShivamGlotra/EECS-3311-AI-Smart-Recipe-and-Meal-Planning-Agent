package com.smartplate;

import com.smartplate.domain.Pantry;
import com.smartplate.domain.Recipe;
import com.smartplate.domain.UserProfile;

import java.util.List;

/** Small fixtures shared by the tests. */
public final class TestData {

    private TestData() {
    }

    public static UserProfile profile(List<String> diet, List<String> allergies, List<String> dislikes, int time) {
        return new UserProfile(diet, allergies, dislikes, time, 2);
    }

    public static UserProfile noRestrictions() {
        return UserProfile.defaultProfile();
    }

    public static Recipe pasta() {
        return Recipe.builder("t01", "Test Pasta").time(20).servings(2).tags("vegetarian")
                .ingredient("spaghetti", "grain", 200, "g")
                .ingredient("butter", "dairy", 40, "g")
                .ingredient("garlic", "vegetable", 2, "pcs")
                .nutrition(500, 15, 70, 18).build();
    }

    public static Pantry emptyPantry() {
        return new Pantry();
    }
}
