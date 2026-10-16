package com.smartplate.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShoppingListTest {

    @Test
    void consolidateMergesCompatibleAndKeepsIncompatibleSeparate() {
        ShoppingList list = new ShoppingList();
        Ingredient onion = new Ingredient("onion");
        list.addItem(new ShoppingItem(onion, 600, "g"));
        list.addItem(new ShoppingItem(onion, 0.5, "kg"));
        list.addItem(new ShoppingItem(onion, 2, "pcs"));
        list.consolidate();
        assertEquals(2, list.getItems().size());
        assertEquals(1.1, list.getItems().get(0).getQuantity(), 1e-9);
        assertEquals("kg", list.getItems().get(0).getUnit());
        assertEquals("pcs", list.getItems().get(1).getUnit());
    }
}
