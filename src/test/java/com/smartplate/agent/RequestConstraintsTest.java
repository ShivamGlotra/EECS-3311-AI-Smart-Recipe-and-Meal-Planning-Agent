package com.smartplate.agent;

import com.smartplate.domain.SmartPlateException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestConstraintsTest {

    @Test
    void parsesTheReportExampleRequest() {
        RequestConstraints c = RequestConstraints.parse(
                "Plan dinners for this week, use some of my pantry items, avoid mushrooms, keep meals under 30 minutes");
        assertEquals(30, c.getMaxTime());
        assertEquals(List.of("mushrooms"), c.getAvoid());
        assertEquals(7, c.planDays());
    }

    @Test
    void parsesDaysCountAndServings() {
        assertEquals(5, RequestConstraints.parse("weekday dinners please").planDays());
        assertEquals(6, RequestConstraints.parse("plan 6 dinners").planDays());
        assertEquals(4, RequestConstraints.parse("give me 4 vegetarian recipes").recommendationCount(3));
        assertEquals(3, RequestConstraints.parse("something quick").recommendationCount(3));
        assertEquals(4, RequestConstraints.parse("dinners for 4 people").servings(2));
        assertNull(RequestConstraints.parse("no rush").getMaxTime());
    }

    @Test
    void rejectsPlansOutsideFiveToSevenDays() {
        assertThrows(SmartPlateException.class, () -> RequestConstraints.parse("plan 3 days").planDays());
        assertThrows(SmartPlateException.class, () -> RequestConstraints.parse("plan 10 days").planDays());
    }

    @Test
    void dietsNamedInTheRequestBecomeHardConstraints() {
        RequestConstraints c = RequestConstraints.parse("swap Tuesday for something vegetarian and gluten-free");
        assertEquals(List.of("vegetarian", "gluten-free"), c.getDiets());
        assertEquals(List.of(), RequestConstraints.parse("non-vegan is fine").getDiets());
    }

    @Test
    void ignoresNonIngredientWordsAfterNo() {
        assertEquals(List.of("dairy"), RequestConstraints.parse("no more than 20 min, no dairy").getAvoid());
    }
}
