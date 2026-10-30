"""The KUMA judge's checks: an independent oracle for SmartPlate's agent behaviour.

These rules are written separately from the Java ConstraintValidator and DietRules on purpose.
If the judge reused the agent's own validator, a gap in that validator (an allergen it does
not know) would pass every test. The judge only trusts two things from the app: the recipe
catalog (names, cooking times, ingredients and their categories) and the replies.

Every check returns a list of issues; an empty list means the check passed.
"""
from __future__ import annotations

import re

DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]

MAJOR, MINOR = "major", "minor"

# ---------------------------------------------------------------- food knowledge (independent)

MEAT_WORDS = ["chicken", "beef", "pork", "turkey", "sausage", "bacon", "ham", "lamb", "steak"]
FISH_WORDS = ["salmon", "tuna", "cod", "tilapia", "anchovy", "trout", "fish"]
SHELLFISH_WORDS = ["shrimp", "prawn", "crab", "lobster", "mussel", "scallop", "clam", "oyster"]
TREE_NUT_WORDS = ["almond", "cashew", "walnut", "pecan", "hazelnut", "pistachio", "macadamia",
                  "pine nut", "pesto"]  # classic pesto is made with pine nuts
GLUTEN_WORDS = ["spaghetti", "pasta", "penne", "ditalini", "shells", "flour", "bread", "pita",
                "couscous", "bulgur", "gnocchi", "pizza dough", "dough", "ramen", "egg noodle",
                "soy sauce", "wheat", "barley", "breadcrumb", "flatbread"]
DAIRY_WORDS = ["milk", "butter", "cheese", "cream", "yogurt", "ricotta", "mozzarella", "parmesan",
               "feta", "ghee", "pesto"]  # basil pesto contains parmesan
NOT_DAIRY = ["coconut milk", "peanut butter", "almond milk", "oat milk", "soy milk"]
EGG_WORDS = ["egg", "mayonnaise"]
SOY_WORDS = ["soy", "tofu", "edamame", "tempeh", "miso"]
SESAME_WORDS = ["sesame", "tahini"]

# allergy or avoid term -> words that reveal it in an ingredient name (plus a category, if any)
TERM_GROUPS: dict[str, tuple[list[str], set[str]]] = {
    "peanut": (["peanut"], set()),
    "tree nut": (TREE_NUT_WORDS, set()),
    "nut": (TREE_NUT_WORDS + ["peanut"], {"nut"}),
    "sesame": (SESAME_WORDS, set()),
    "gluten": (GLUTEN_WORDS, set()),
    "wheat": (GLUTEN_WORDS, set()),
    "dairy": (DAIRY_WORDS, {"dairy"}),
    "lactose": (DAIRY_WORDS, {"dairy"}),
    "milk": (DAIRY_WORDS, {"dairy"}),
    "egg": (EGG_WORDS, {"egg"}),
    "fish": (FISH_WORDS, set()),
    "shellfish": (SHELLFISH_WORDS, set()),
    "seafood": (FISH_WORDS + SHELLFISH_WORDS, {"seafood"}),
    "soy": (SOY_WORDS, set()),
    "meat": (MEAT_WORDS, {"meat"}),
}


def singular(term: str) -> str:
    t = term.strip().lower()
    if t.endswith("ies") and len(t) > 4:
        return t[:-3] + "y"
    if t.endswith("oes") and len(t) > 4:
        return t[:-2]
    if t.endswith("s") and not t.endswith("ss") and len(t) > 3:
        return t[:-1]
    return t


def has_word(name: str, word: str) -> bool:
    """Whole-word match that also accepts a plural ("egg" in "eggs", not "nut" in "nutmeg")."""
    return re.search(r"(?<![a-z])" + re.escape(word) + r"(?:s|es)?(?![a-z])", name) is not None


def is_dairy(ingredient: dict) -> bool:
    name = ingredient["name"].lower()
    if any(has_word(name, w) for w in NOT_DAIRY):
        return False
    return ingredient["category"] == "dairy" or any(has_word(name, w) for w in DAIRY_WORDS)


def matches_term(ingredient: dict, term: str) -> bool:
    """Does this ingredient fall under an allergy / dislike / avoid term?"""
    name = ingredient["name"].lower()
    key = singular(term)
    if key in ("dairy", "lactose", "milk"):
        return is_dairy(ingredient)
    if key in TERM_GROUPS:
        words, categories = TERM_GROUPS[key]
        return ingredient["category"] in categories or any(has_word(name, w) for w in words)
    return has_word(name, key)


def violates_diet(ingredient: dict, diet: str) -> bool:
    name, category = ingredient["name"].lower(), ingredient["category"]
    animal = category in ("meat", "seafood") or any(
        has_word(name, w) for w in MEAT_WORDS + FISH_WORDS + SHELLFISH_WORDS)
    if diet == "vegetarian":
        return animal
    if diet == "vegan":
        return animal or is_dairy(ingredient) or matches_term(ingredient, "egg") or has_word(name, "honey")
    if diet == "pescatarian":
        return category == "meat" or any(has_word(name, w) for w in MEAT_WORDS)
    if diet == "gluten-free":
        return matches_term(ingredient, "gluten")
    if diet == "dairy-free":
        return is_dairy(ingredient)
    return False


# ---------------------------------------------------------------- issue helper

def issue(requirement: str, severity: str, message: str) -> dict:
    return {"code": requirement.lower().replace("-", "_"), "requirement": requirement,
            "severity": severity, "message": message}


# ---------------------------------------------------------------- reply checks

def ok(reply: dict, requirement: str, what: str) -> list[dict]:
    """The step must succeed; a failure here means the task was not completed."""
    if reply.get("ok"):
        return []
    return [issue(requirement, MAJOR, f"{what} failed: {reply.get('error', 'no error message')}")]


def refused(reply: dict, requirement: str, what: str, *must_mention: str) -> list[dict]:
    """The step must be refused with a clear message, and must not return any result."""
    issues = []
    if reply.get("ok"):
        issues.append(issue(requirement, MAJOR, f"{what} was accepted but should have been refused"))
    else:
        text = reply.get("error", "").lower()
        for word in must_mention:
            if word.lower() not in text:
                issues.append(issue(requirement, MINOR,
                                    f"{what}: the error message does not mention '{word}': {reply.get('error')!r}"))
    if reply.get("recipes") or (reply.get("plan") and reply.get("action") != "state"):
        issues.append(issue(requirement, MAJOR, f"{what} returned a result although it failed"))
    return issues


def grounded(meals: list[dict], catalog: dict[str, dict], requirement: str = "BR-03") -> list[dict]:
    """Every recipe must exist in the repository, with its real name and cooking time."""
    issues = []
    for meal in meals:
        real = catalog.get(meal.get("id"))
        if real is None:
            issues.append(issue(requirement, MAJOR, f"'{meal.get('id')}' is not a recipe in the repository"))
        elif real["name"] != meal.get("name") or real["time"] != meal.get("time"):
            issues.append(issue(requirement, MAJOR, f"{meal.get('id')} does not match the repository entry"))
    return issues


def safe(meals: list[dict], *, diets=(), allergies=(), avoid=(), max_time=None,
         requirement: str = "BR-01") -> list[dict]:
    """No recipe may break a diet, contain an allergen or avoided food, or exceed the time limit."""
    issues = []
    for meal in meals:
        label = f"{meal.get('day', '') + ': ' if meal.get('day') else ''}{meal['name']} ({meal['id']})"
        for ing in meal.get("ingredients", []):
            for diet in diets:
                if violates_diet(ing, diet):
                    issues.append(issue(requirement, MAJOR, f"{label} contains {ing['name']} (not {diet})"))
            for term in allergies:
                if matches_term(ing, term):
                    issues.append(issue(requirement, MAJOR,
                                        f"{label} contains {ing['name']} (allergy: {term})"))
            for term in avoid:
                if matches_term(ing, term):
                    issues.append(issue(requirement, MAJOR,
                                        f"{label} contains {ing['name']} (asked to avoid: {term})"))
        if max_time is not None and meal["time"] > max_time:
            issues.append(issue(requirement, MAJOR, f"{label} takes {meal['time']} min (limit {max_time})"))
    return issues


def plan_shape(plan: list[dict], days: int, servings: int | None = None,
               requirement: str = "BR-04") -> list[dict]:
    """Exactly `days` dinners, Monday onwards, one per day, each with the expected servings."""
    issues = []
    got = [m.get("day") for m in plan]
    if got != DAYS[:days]:
        issues.append(issue(requirement, MAJOR, f"expected days {DAYS[:days]}, got {got}"))
    if servings is not None:
        for m in plan:
            if m.get("servings") != servings:
                issues.append(issue("BR-02", MAJOR, f"{m.get('day')} has {m.get('servings')} servings, "
                                                    f"expected {servings}"))
    return issues


def count(recipes: list[dict], expected: int, requirement: str = "BR-02") -> list[dict]:
    ids = [r["id"] for r in recipes]
    issues = []
    if len(ids) != expected:
        issues.append(issue(requirement, MAJOR, f"expected {expected} recipes, got {len(ids)}"))
    if len(set(ids)) != len(ids):
        issues.append(issue(requirement, MAJOR, f"the same recipe was recommended twice: {ids}"))
    return issues


def changed_days(before: list[dict], after: list[dict]) -> list[str]:
    old = {m["day"]: m["id"] for m in before}
    new = {m["day"]: m["id"] for m in after}
    return [d for d in DAYS if old.get(d) != new.get(d)]


def one_day_changed(before: list[dict], after: list[dict], day: str | None = None,
                    requirement: str = "BR-05") -> list[dict]:
    """An edit changes exactly one day (the requested one, if a day was named) and nothing else."""
    changed = changed_days(before, after)
    if len(changed) != 1:
        return [issue(requirement, MAJOR, f"expected exactly one day to change, changed: {changed or 'none'}")]
    if day and changed != [day]:
        return [issue(requirement, MAJOR, f"asked to change {day}, but {changed[0]} changed")]
    return []


def same_plan(expected: list[dict], actual: list[dict], what: str, requirement: str = "BR-05") -> list[dict]:
    diff = changed_days(expected, actual)
    if diff or len(expected) != len(actual):
        return [issue(requirement, MAJOR, f"{what}: the plan differs on {diff or 'its length'}")]
    return []


def includes(meals: list[dict], term: str, at_least: int, requirement: str = "BR-08") -> list[dict]:
    """Soft relevance check: at least `at_least` results use the ingredient the user asked for."""
    hits = [m["id"] for m in meals if any(has_word(i["name"].lower(), singular(term))
                                          for i in m.get("ingredients", []))]
    if len(hits) >= at_least:
        return []
    return [issue(requirement, MINOR, f"asked for {term}: only {len(hits)} result(s) use it "
                                      f"(expected at least {at_least})")]


def variety(plan: list[dict], min_distinct: int, requirement: str = "BR-08",
            severity: str = MINOR) -> list[dict]:
    distinct = len({m["id"] for m in plan})
    if distinct >= min_distinct:
        return []
    return [issue(requirement, severity, f"only {distinct} different recipes in a {len(plan)}-day plan "
                                      f"(expected at least {min_distinct})")]


def traced(reply: dict, requirement: str = "BR-09") -> list[dict]:
    """A successful AI answer must come with a trace that shows the validation step."""
    trace = reply.get("trace") or []
    if any(step.startswith("Validation") for step in trace):
        return []
    return [issue(requirement, MINOR, "the agent trace does not show a validation step")]
