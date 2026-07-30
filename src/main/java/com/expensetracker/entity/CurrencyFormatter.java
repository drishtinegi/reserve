package com.expensetracker.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * CurrencyFormatter — a tiny static helper that turns a Currency enum
 * into its display symbol and formats amounts with that symbol.
 *
 * This is not a Spring bean — it has no dependencies and needs no injection.
 * Any class can call CurrencyFormatter.symbol(currency) directly.
 */
public class CurrencyFormatter {

    /** Returns the currency symbol: $ for USD, ₹ for INR. */
    public static String symbol(Currency currency) {
        return currency == Currency.INR ? "₹" : "$";
    }

    /** Formats an amount with the currency symbol: "$123.45" or "₹987.00". */
    public static String format(Currency currency, BigDecimal amount) {
        return symbol(currency) + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
