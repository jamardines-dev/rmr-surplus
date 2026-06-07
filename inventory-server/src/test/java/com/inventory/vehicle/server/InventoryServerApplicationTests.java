package com.inventory.vehicle.server;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class InventoryServerApplicationTests {

    @Test
    void applicationClassCanBeCreated() {
        assertDoesNotThrow(InventoryServerApplication::new);
    }
}
