package com.inventory.vehicle.common.util;

import java.math.BigDecimal;

public final class MoneyFormat {

    private MoneyFormat() {
    }

    public static String peso(BigDecimal amount) {
        return "₱" + (amount == null ? "0.00" : amount.toPlainString());
    }
}
