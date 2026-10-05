package com.smartplate.service;

import com.smartplate.domain.Ingredient;
import com.smartplate.domain.PantryItem;
import com.smartplate.domain.Recipe;
import com.smartplate.domain.Substitution;

import java.util.List;

/**
 * Built-in sample data: 30 recipes, a starter pantry and the predefined substitution list.
 * Nutrition values are approximate per-serving figures for demonstration.
 */
public final class SampleData {

    private SampleData() {
    }

    public static List<Recipe> recipes() {
        return List.of(
                Recipe.builder("r01", "Vegetable Fried Rice").time(20).servings(2)
                        .tags("vegetarian", "dairy-free", "quick")
                        .ingredient("rice", "grain", 300, "g").ingredient("egg", "egg", 2, "pcs")
                        .ingredient("frozen peas", "vegetable", 150, "g").ingredient("carrot", "vegetable", 1, "pcs")
                        .ingredient("soy sauce", "condiment", 30, "ml").ingredient("vegetable oil", "oil", 15, "ml")
                        .ingredient("green onion", "vegetable", 2, "pcs")
                        .nutrition(420, 12, 68, 11)
                        .step("Cook the rice and let it cool.").step("Scramble the eggs in oil, then set aside.")
                        .step("Fry the peas and carrot, add rice, soy sauce, eggs and green onion.").build(),
                Recipe.builder("r02", "Chickpea Spinach Curry").time(30).servings(2)
                        .tags("vegan", "vegetarian", "gluten-free", "dairy-free")
                        .ingredient("chickpeas", "legume", 400, "g").ingredient("spinach", "vegetable", 200, "g")
                        .ingredient("onion", "vegetable", 1, "pcs").ingredient("garlic", "vegetable", 2, "pcs")
                        .ingredient("coconut milk", "other", 400, "ml").ingredient("curry powder", "spice", 10, "g")
                        .ingredient("rice", "grain", 200, "g")
                        .nutrition(520, 16, 70, 20)
                        .step("Soften onion and garlic, add curry powder.").step("Add chickpeas and coconut milk; simmer 15 min.")
                        .step("Stir in spinach and serve with rice.").build(),
                Recipe.builder("r03", "Spaghetti Aglio e Olio").time(15).servings(2)
                        .tags("vegan", "vegetarian", "dairy-free", "quick")
                        .ingredient("spaghetti", "grain", 250, "g").ingredient("garlic", "vegetable", 4, "pcs")
                        .ingredient("olive oil", "oil", 60, "ml").ingredient("chili flakes", "spice", 2, "g")
                        .ingredient("parsley", "herb", 10, "g")
                        .nutrition(560, 14, 80, 22)
                        .step("Boil the spaghetti.").step("Gently fry sliced garlic and chili in olive oil.")
                        .step("Toss with pasta and parsley.").build(),
                Recipe.builder("r04", "Chicken Stir-Fry").time(25).servings(2)
                        .tags("dairy-free", "high-protein")
                        .ingredient("chicken breast", "meat", 400, "g").ingredient("bell pepper", "vegetable", 2, "pcs")
                        .ingredient("broccoli", "vegetable", 250, "g").ingredient("soy sauce", "condiment", 40, "ml")
                        .ingredient("garlic", "vegetable", 2, "pcs").ingredient("rice", "grain", 250, "g")
                        .ingredient("vegetable oil", "oil", 15, "ml")
                        .nutrition(540, 42, 60, 12)
                        .step("Cook the rice.").step("Stir-fry sliced chicken until cooked.")
                        .step("Add vegetables, garlic and soy sauce; serve over rice.").build(),
                Recipe.builder("r05", "Beef Tacos").time(20).servings(3)
                        .tags("high-protein")
                        .ingredient("ground beef", "meat", 400, "g").ingredient("flour tortilla", "grain", 6, "pcs")
                        .ingredient("lettuce", "vegetable", 100, "g").ingredient("tomato", "vegetable", 2, "pcs")
                        .ingredient("cheddar cheese", "dairy", 100, "g").ingredient("sour cream", "dairy", 60, "g")
                        .ingredient("taco seasoning", "spice", 15, "g")
                        .nutrition(610, 34, 40, 34)
                        .step("Brown the beef with taco seasoning.").step("Warm the tortillas.")
                        .step("Fill with beef, lettuce, tomato, cheese and sour cream.").build(),
                Recipe.builder("r06", "Mushroom Risotto").time(45).servings(3)
                        .tags("vegetarian", "gluten-free")
                        .ingredient("arborio rice", "grain", 300, "g").ingredient("mushroom", "vegetable", 300, "g")
                        .ingredient("onion", "vegetable", 1, "pcs").ingredient("vegetable broth", "other", 1, "l")
                        .ingredient("parmesan", "dairy", 50, "g").ingredient("butter", "dairy", 30, "g")
                        .nutrition(510, 14, 78, 15)
                        .step("Fry onion and mushrooms in butter.").step("Toast the rice, then add broth a ladle at a time.")
                        .step("Finish with parmesan.").build(),
                Recipe.builder("r07", "Baked Salmon with Asparagus").time(25).servings(2)
                        .tags("pescatarian", "gluten-free", "dairy-free", "high-protein")
                        .ingredient("salmon", "seafood", 400, "g").ingredient("asparagus", "vegetable", 300, "g")
                        .ingredient("lemon", "fruit", 1, "pcs").ingredient("olive oil", "oil", 20, "ml")
                        .ingredient("garlic", "vegetable", 2, "pcs")
                        .nutrition(450, 38, 8, 28)
                        .step("Put salmon and asparagus on a tray with oil, garlic and lemon.")
                        .step("Bake at 200 C for 15-18 minutes.").build(),
                Recipe.builder("r08", "Black Bean Quesadillas").time(15).servings(2)
                        .tags("vegetarian", "quick")
                        .ingredient("flour tortilla", "grain", 4, "pcs").ingredient("black beans", "legume", 400, "g")
                        .ingredient("cheddar cheese", "dairy", 150, "g").ingredient("bell pepper", "vegetable", 1, "pcs")
                        .ingredient("salsa", "condiment", 100, "g")
                        .nutrition(580, 26, 62, 24)
                        .step("Fill tortillas with beans, pepper and cheese.").step("Toast both sides in a pan.")
                        .step("Serve with salsa.").build(),
                Recipe.builder("r09", "Lentil Soup").time(40).servings(4)
                        .tags("vegan", "vegetarian", "gluten-free", "dairy-free")
                        .ingredient("lentils", "legume", 300, "g").ingredient("carrot", "vegetable", 2, "pcs")
                        .ingredient("celery", "vegetable", 2, "pcs").ingredient("onion", "vegetable", 1, "pcs")
                        .ingredient("canned tomatoes", "vegetable", 400, "g").ingredient("vegetable broth", "other", 1, "l")
                        .ingredient("cumin", "spice", 5, "g")
                        .nutrition(340, 20, 52, 4)
                        .step("Soften onion, carrot and celery.").step("Add lentils, tomatoes, broth and cumin.")
                        .step("Simmer 30 minutes.").build(),
                Recipe.builder("r10", "Shrimp Garlic Pasta").time(20).servings(2)
                        .tags("pescatarian")
                        .ingredient("spaghetti", "grain", 250, "g").ingredient("shrimp", "seafood", 300, "g")
                        .ingredient("garlic", "vegetable", 4, "pcs").ingredient("butter", "dairy", 40, "g")
                        .ingredient("parsley", "herb", 10, "g").ingredient("lemon", "fruit", 1, "pcs")
                        .nutrition(620, 36, 72, 20)
                        .step("Boil the pasta.").step("Cook shrimp and garlic in butter.")
                        .step("Toss with pasta, lemon and parsley.").build(),
                Recipe.builder("r11", "Tofu Veggie Stir-Fry").time(20).servings(2)
                        .tags("vegan", "vegetarian", "dairy-free")
                        .ingredient("tofu", "legume", 400, "g").ingredient("broccoli", "vegetable", 200, "g")
                        .ingredient("bell pepper", "vegetable", 1, "pcs").ingredient("carrot", "vegetable", 1, "pcs")
                        .ingredient("soy sauce", "condiment", 30, "ml").ingredient("rice", "grain", 250, "g")
                        .ingredient("vegetable oil", "oil", 15, "ml")
                        .nutrition(480, 24, 62, 15)
                        .step("Cook the rice.").step("Brown cubed tofu in oil.")
                        .step("Add vegetables and soy sauce; serve over rice.").build(),
                Recipe.builder("r12", "Greek Salad with Chickpeas").time(10).servings(2)
                        .tags("vegetarian", "gluten-free", "quick", "no-cook")
                        .ingredient("cucumber", "vegetable", 1, "pcs").ingredient("tomato", "vegetable", 3, "pcs")
                        .ingredient("red onion", "vegetable", 1, "pcs").ingredient("feta cheese", "dairy", 100, "g")
                        .ingredient("olives", "vegetable", 60, "g").ingredient("chickpeas", "legume", 200, "g")
                        .ingredient("olive oil", "oil", 30, "ml")
                        .nutrition(410, 15, 30, 26)
                        .step("Chop the vegetables.").step("Toss with chickpeas, olives, feta and olive oil.").build(),
                Recipe.builder("r13", "Spinach and Cheese Omelette").time(10).servings(2)
                        .tags("vegetarian", "gluten-free", "quick")
                        .ingredient("egg", "egg", 4, "pcs").ingredient("spinach", "vegetable", 100, "g")
                        .ingredient("cheddar cheese", "dairy", 60, "g").ingredient("milk", "dairy", 50, "ml")
                        .ingredient("butter", "dairy", 10, "g")
                        .nutrition(380, 26, 4, 29)
                        .step("Whisk eggs with milk.").step("Cook in butter, add spinach and cheese, fold.").build(),
                Recipe.builder("r14", "Turkey Chili").time(50).servings(4)
                        .tags("gluten-free", "dairy-free", "high-protein")
                        .ingredient("ground turkey", "meat", 500, "g").ingredient("kidney beans", "legume", 400, "g")
                        .ingredient("canned tomatoes", "vegetable", 400, "g").ingredient("onion", "vegetable", 1, "pcs")
                        .ingredient("bell pepper", "vegetable", 1, "pcs").ingredient("chili powder", "spice", 10, "g")
                        .nutrition(430, 38, 34, 14)
                        .step("Brown the turkey with onion and pepper.").step("Add beans, tomatoes and chili powder.")
                        .step("Simmer 35 minutes.").build(),
                Recipe.builder("r15", "Peanut Noodles").time(20).servings(2)
                        .tags("vegan", "vegetarian", "dairy-free")
                        .ingredient("rice noodles", "grain", 250, "g").ingredient("peanut butter", "nut", 80, "g")
                        .ingredient("soy sauce", "condiment", 30, "ml").ingredient("lime", "fruit", 1, "pcs")
                        .ingredient("cucumber", "vegetable", 1, "pcs").ingredient("carrot", "vegetable", 1, "pcs")
                        .ingredient("green onion", "vegetable", 2, "pcs")
                        .nutrition(590, 18, 76, 24)
                        .step("Cook the noodles.").step("Whisk peanut butter, soy sauce and lime juice.")
                        .step("Toss noodles with sauce and sliced vegetables.").build(),
                Recipe.builder("r16", "Margherita Flatbread").time(25).servings(2)
                        .tags("vegetarian")
                        .ingredient("pizza dough", "grain", 300, "g").ingredient("tomato sauce", "condiment", 150, "ml")
                        .ingredient("mozzarella", "dairy", 150, "g").ingredient("basil", "herb", 10, "g")
                        .ingredient("olive oil", "oil", 15, "ml")
                        .nutrition(640, 26, 78, 24)
                        .step("Stretch the dough and spread with sauce.").step("Top with mozzarella; bake at 230 C for 12 min.")
                        .step("Finish with basil and olive oil.").build(),
                Recipe.builder("r17", "Quinoa Buddha Bowl").time(30).servings(2)
                        .tags("vegan", "vegetarian", "gluten-free", "dairy-free")
                        .ingredient("quinoa", "grain", 200, "g").ingredient("sweet potato", "vegetable", 2, "pcs")
                        .ingredient("chickpeas", "legume", 200, "g").ingredient("spinach", "vegetable", 100, "g")
                        .ingredient("tahini", "condiment", 40, "g").ingredient("lemon", "fruit", 1, "pcs")
                        .nutrition(550, 18, 74, 20)
                        .step("Roast cubed sweet potato and chickpeas.").step("Cook the quinoa.")
                        .step("Assemble with spinach and a tahini-lemon dressing.").build(),
                Recipe.builder("r18", "Lemon Herb Chicken with Potatoes").time(45).servings(2)
                        .tags("gluten-free", "dairy-free", "high-protein")
                        .ingredient("chicken thigh", "meat", 500, "g").ingredient("potato", "vegetable", 500, "g")
                        .ingredient("lemon", "fruit", 1, "pcs").ingredient("olive oil", "oil", 30, "ml")
                        .ingredient("garlic", "vegetable", 3, "pcs").ingredient("rosemary", "herb", 5, "g")
                        .nutrition(610, 40, 45, 28)
                        .step("Toss chicken and potatoes with oil, lemon, garlic and rosemary.")
                        .step("Roast at 200 C for 40 minutes.").build(),
                Recipe.builder("r19", "Tuna Salad Wraps").time(10).servings(2)
                        .tags("pescatarian", "dairy-free", "quick", "no-cook")
                        .ingredient("canned tuna", "seafood", 300, "g").ingredient("mayonnaise", "egg", 60, "g")
                        .ingredient("flour tortilla", "grain", 4, "pcs").ingredient("lettuce", "vegetable", 80, "g")
                        .ingredient("celery", "vegetable", 1, "pcs")
                        .nutrition(470, 32, 36, 20)
                        .step("Mix tuna, mayonnaise and chopped celery.").step("Wrap in tortillas with lettuce.").build(),
                Recipe.builder("r20", "Caprese Pasta Salad").time(15).servings(2)
                        .tags("vegetarian", "quick")
                        .ingredient("penne pasta", "grain", 250, "g").ingredient("cherry tomatoes", "vegetable", 250, "g")
                        .ingredient("mozzarella", "dairy", 125, "g").ingredient("basil", "herb", 10, "g")
                        .ingredient("olive oil", "oil", 30, "ml")
                        .nutrition(560, 20, 70, 22)
                        .step("Cook and cool the pasta.").step("Toss with tomatoes, mozzarella, basil and oil.").build(),
                Recipe.builder("r21", "Beef and Broccoli").time(30).servings(2)
                        .tags("dairy-free", "high-protein")
                        .ingredient("beef sirloin", "meat", 400, "g").ingredient("broccoli", "vegetable", 300, "g")
                        .ingredient("soy sauce", "condiment", 45, "ml").ingredient("garlic", "vegetable", 2, "pcs")
                        .ingredient("ginger", "spice", 10, "g").ingredient("rice", "grain", 250, "g")
                        .nutrition(590, 40, 64, 16)
                        .step("Cook the rice.").step("Sear sliced beef; remove.")
                        .step("Cook broccoli with garlic and ginger, return beef with soy sauce.").build(),
                Recipe.builder("r22", "Vegetable Minestrone").time(40).servings(4)
                        .tags("vegan", "vegetarian", "dairy-free")
                        .ingredient("canned tomatoes", "vegetable", 400, "g").ingredient("zucchini", "vegetable", 1, "pcs")
                        .ingredient("carrot", "vegetable", 2, "pcs").ingredient("celery", "vegetable", 2, "pcs")
                        .ingredient("kidney beans", "legume", 400, "g").ingredient("ditalini pasta", "grain", 100, "g")
                        .ingredient("vegetable broth", "other", 1, "l")
                        .nutrition(310, 12, 54, 4)
                        .step("Soften the vegetables.").step("Add tomatoes, beans and broth; simmer 20 min.")
                        .step("Add pasta and cook until tender.").build(),
                Recipe.builder("r23", "Sheet-Pan Sausage and Vegetables").time(35).servings(2)
                        .tags("gluten-free", "dairy-free")
                        .ingredient("smoked sausage", "meat", 400, "g").ingredient("potato", "vegetable", 400, "g")
                        .ingredient("bell pepper", "vegetable", 2, "pcs").ingredient("red onion", "vegetable", 1, "pcs")
                        .ingredient("olive oil", "oil", 20, "ml").ingredient("paprika", "spice", 5, "g")
                        .nutrition(640, 24, 40, 42)
                        .step("Chop everything and toss with oil and paprika.").step("Roast at 220 C for 30 minutes.").build(),
                Recipe.builder("r24", "Teriyaki Salmon Rice Bowl").time(25).servings(2)
                        .tags("pescatarian", "dairy-free", "high-protein")
                        .ingredient("salmon", "seafood", 300, "g").ingredient("rice", "grain", 250, "g")
                        .ingredient("soy sauce", "condiment", 40, "ml").ingredient("honey", "sweetener", 20, "g")
                        .ingredient("edamame", "legume", 150, "g").ingredient("cucumber", "vegetable", 1, "pcs")
                        .nutrition(620, 36, 72, 18)
                        .step("Cook the rice.").step("Glaze and pan-fry salmon with soy sauce and honey.")
                        .step("Serve over rice with edamame and cucumber.").build(),
                Recipe.builder("r25", "Spinach and Ricotta Stuffed Shells").time(55).servings(4)
                        .tags("vegetarian")
                        .ingredient("jumbo pasta shells", "grain", 250, "g").ingredient("ricotta", "dairy", 250, "g")
                        .ingredient("spinach", "vegetable", 200, "g").ingredient("tomato sauce", "condiment", 400, "ml")
                        .ingredient("mozzarella", "dairy", 100, "g").ingredient("egg", "egg", 1, "pcs")
                        .nutrition(520, 26, 56, 20)
                        .step("Boil the shells.").step("Mix ricotta, spinach and egg; fill the shells.")
                        .step("Bake in tomato sauce topped with mozzarella for 30 min.").build(),
                Recipe.builder("r26", "Sweet Potato Black Bean Tacos").time(25).servings(3)
                        .tags("vegan", "vegetarian", "gluten-free", "dairy-free")
                        .ingredient("sweet potato", "vegetable", 2, "pcs").ingredient("black beans", "legume", 400, "g")
                        .ingredient("corn tortilla", "grain", 6, "pcs").ingredient("avocado", "fruit", 1, "pcs")
                        .ingredient("lime", "fruit", 1, "pcs").ingredient("cumin", "spice", 5, "g")
                        .nutrition(480, 14, 78, 13)
                        .step("Roast cubed sweet potato with cumin.").step("Warm beans and tortillas.")
                        .step("Fill with sweet potato, beans, avocado and lime.").build(),
                Recipe.builder("r27", "Garlic Butter Shrimp with Rice").time(15).servings(2)
                        .tags("pescatarian", "gluten-free", "quick", "high-protein")
                        .ingredient("shrimp", "seafood", 400, "g").ingredient("butter", "dairy", 40, "g")
                        .ingredient("garlic", "vegetable", 4, "pcs").ingredient("rice", "grain", 250, "g")
                        .ingredient("parsley", "herb", 10, "g").ingredient("lemon", "fruit", 1, "pcs")
                        .nutrition(540, 34, 62, 16)
                        .step("Cook the rice.").step("Saute shrimp and garlic in butter.")
                        .step("Finish with lemon and parsley.").build(),
                Recipe.builder("r28", "Pesto Gnocchi with Peas").time(15).servings(2)
                        .tags("vegetarian", "quick")
                        .ingredient("potato gnocchi", "grain", 500, "g").ingredient("basil pesto", "dairy", 100, "g")
                        .ingredient("frozen peas", "vegetable", 150, "g").ingredient("parmesan", "dairy", 30, "g")
                        .nutrition(590, 16, 80, 22)
                        .step("Boil gnocchi and peas together.").step("Toss with pesto and parmesan.").build(),
                Recipe.builder("r29", "Chicken Fajita Bowl").time(30).servings(2)
                        .tags("gluten-free", "high-protein")
                        .ingredient("chicken breast", "meat", 400, "g").ingredient("bell pepper", "vegetable", 2, "pcs")
                        .ingredient("onion", "vegetable", 1, "pcs").ingredient("rice", "grain", 250, "g")
                        .ingredient("salsa", "condiment", 100, "g").ingredient("sour cream", "dairy", 60, "g")
                        .ingredient("fajita seasoning", "spice", 15, "g")
                        .nutrition(600, 42, 66, 16)
                        .step("Cook the rice.").step("Fry chicken, peppers and onion with seasoning.")
                        .step("Serve in bowls with salsa and sour cream.").build(),
                Recipe.builder("r30", "Coconut Lentil Dal").time(35).servings(4)
                        .tags("vegan", "vegetarian", "gluten-free", "dairy-free")
                        .ingredient("red lentils", "legume", 300, "g").ingredient("coconut milk", "other", 400, "ml")
                        .ingredient("onion", "vegetable", 1, "pcs").ingredient("garlic", "vegetable", 3, "pcs")
                        .ingredient("ginger", "spice", 10, "g").ingredient("turmeric", "spice", 5, "g")
                        .ingredient("rice", "grain", 200, "g")
                        .nutrition(450, 18, 62, 15)
                        .step("Soften onion, garlic and ginger with turmeric.").step("Add lentils and coconut milk; simmer 20 min.")
                        .step("Serve with rice.").build());
    }

    /** A small starter pantry so the demo has something to work with. */
    public static List<PantryItem> pantry() {
        return List.of(
                new PantryItem(new Ingredient("rice", "grain"), 1, "kg"),
                new PantryItem(new Ingredient("garlic", "vegetable"), 6, "pcs"),
                new PantryItem(new Ingredient("olive oil", "oil"), 500, "ml"),
                new PantryItem(new Ingredient("onion", "vegetable"), 3, "pcs"),
                new PantryItem(new Ingredient("canned tomatoes", "vegetable"), 800, "g"),
                new PantryItem(new Ingredient("soy sauce", "condiment"), 250, "ml"),
                new PantryItem(new Ingredient("egg", "egg"), 6, "pcs"),
                new PantryItem(new Ingredient("spaghetti", "grain"), 500, "g"));
    }

    /** The predefined substitutions (F08). The AI never invents substitutions. */
    public static List<Substitution> substitutions() {
        return List.of(
                sub("butter", "dairy", "olive oil", "oil", 0.8, "ml"),
                sub("butter", "dairy", "vegan butter", "oil", 1.0, null),
                sub("milk", "dairy", "oat milk", "other", 1.0, null),
                sub("milk", "dairy", "soy milk", "other", 1.0, null),
                sub("cheddar cheese", "dairy", "nutritional yeast", "other", 0.25, null),
                sub("parmesan", "dairy", "nutritional yeast", "other", 0.5, null),
                sub("mozzarella", "dairy", "vegan mozzarella", "other", 1.0, null),
                sub("feta cheese", "dairy", "firm tofu", "legume", 1.0, null),
                sub("ricotta", "dairy", "firm tofu", "legume", 1.0, null),
                sub("sour cream", "dairy", "greek yogurt", "dairy", 1.0, null),
                sub("sour cream", "dairy", "coconut yogurt", "other", 1.0, null),
                sub("egg", "egg", "flaxseed", "other", 15, "g"),
                sub("mayonnaise", "egg", "mashed avocado", "fruit", 1.0, null),
                sub("chicken breast", "meat", "tofu", "legume", 1.0, null),
                sub("chicken breast", "meat", "chickpeas", "legume", 1.0, null),
                sub("chicken thigh", "meat", "tofu", "legume", 1.0, null),
                sub("ground beef", "meat", "lentils", "legume", 1.0, null),
                sub("ground beef", "meat", "ground turkey", "meat", 1.0, null),
                sub("ground turkey", "meat", "lentils", "legume", 1.0, null),
                sub("beef sirloin", "meat", "tofu", "legume", 1.0, null),
                sub("smoked sausage", "meat", "chickpeas", "legume", 1.0, null),
                sub("shrimp", "seafood", "tofu", "legume", 1.0, null),
                sub("salmon", "seafood", "tofu", "legume", 1.0, null),
                sub("canned tuna", "seafood", "chickpeas", "legume", 1.0, null),
                sub("spaghetti", "grain", "gluten-free spaghetti", "grain", 1.0, null),
                sub("penne pasta", "grain", "gluten-free penne", "grain", 1.0, null),
                sub("flour tortilla", "grain", "corn tortilla", "grain", 1.0, null),
                sub("soy sauce", "condiment", "coconut aminos", "condiment", 1.0, null),
                sub("peanut butter", "nut", "sunflower seed butter", "other", 1.0, null),
                sub("honey", "sweetener", "maple syrup", "sweetener", 1.0, "ml"),
                sub("rice", "grain", "quinoa", "grain", 1.0, null),
                sub("basil pesto", "dairy", "olive oil and basil", "oil", 0.6, "ml"),
                sub("mushroom", "vegetable", "zucchini", "vegetable", 1.0, null));
    }

    private static Substitution sub(String from, String fromCategory, String to, String toCategory,
                                    double factor, String unit) {
        return new Substitution(new Ingredient(from, fromCategory), new Ingredient(to, toCategory), factor, unit);
    }
}
