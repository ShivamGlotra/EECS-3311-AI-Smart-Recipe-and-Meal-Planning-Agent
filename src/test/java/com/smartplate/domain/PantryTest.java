package com.smartplate.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PantryTest {

    @Test
    void addMergesCompatibleUnits() {
        Pantry pantry = new Pantry();
        pantry.addItem(new PantryItem("Rice", 500, "g"));
        pantry.addItem(new PantryItem("rice", 1, "kg"));
        assertEquals(1, pantry.getItems().size());
        assertEquals(1500, pantry.findItem("RICE").getQuantity(), 1e-9);
        assertEquals("g", pantry.findItem("rice").getUnit());
    }

    @Test
    void addRejectsIncompatibleUnitForExistingItem() {
        Pantry pantry = new Pantry();
        pantry.addItem(new PantryItem("egg", 6, "pcs"));
        assertThrows(SmartPlateException.class, () -> pantry.addItem(new PantryItem("egg", 100, "g")));
    }

    @Test
    void updateAndRemove() {
        Pantry pantry = new Pantry();
        pantry.addItem(new PantryItem("milk", 1, "l"));
        pantry.updateItem(new PantryItem("milk", 250, "ml"));
        assertEquals(250, pantry.findItem("milk").getQuantity(), 1e-9);
        pantry.removeItem("milk");
        assertNull(pantry.findItem("milk"));
    }

    @Test
    void invalidInputIsRejected() {
        Pantry pantry = new Pantry();
        assertThrows(SmartPlateException.class, () -> pantry.addItem(new PantryItem("salt", 0, "g")));
        assertThrows(SmartPlateException.class, () -> pantry.addItem(new PantryItem("salt", -1, "g")));
        assertThrows(SmartPlateException.class, () -> pantry.addItem(new PantryItem("salt", 1, "cup")));
        assertThrows(SmartPlateException.class, () -> pantry.updateItem(new PantryItem("salt", 1, "g")));
        assertThrows(SmartPlateException.class, () -> pantry.removeItem("salt"));
    }
}
