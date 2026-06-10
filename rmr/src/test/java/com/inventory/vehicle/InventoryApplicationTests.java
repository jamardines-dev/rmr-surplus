package com.inventory.vehicle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class InventoryApplicationTests {

    @Test
    void applicationClassCanBeCreated() {
        assertDoesNotThrow(InventoryApplication::new);
    }
}
