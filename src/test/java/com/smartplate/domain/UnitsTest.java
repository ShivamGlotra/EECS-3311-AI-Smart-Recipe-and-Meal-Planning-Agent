package com.smartplate.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnitsTest {

    @Test
    void convertsWithinAFamily() {
        assertEquals(1500, Units.convert(1.5, "kg", "g"), 1e-9);
        assertEquals(0.25, Units.convert(250, "ml", "l"), 1e-9);
        assertTrue(Units.compatible("g", "kg"));
        assertFalse(Units.compatible("g", "ml"));
    }

    @Test
    void rejectsUnsupportedOrMixedUnits() {
        assertThrows(SmartPlateException.class, () -> Units.family("tbsp"));
        assertThrows(SmartPlateException.class, () -> Units.convert(1, "g", "ml"));
    }

    @Test
    void formatsNicely() {
        assertEquals("200 g", Units.format(200.0, "g"));
        assertEquals("1.5 kg", Units.format(1.5, "KG"));
    }
}
