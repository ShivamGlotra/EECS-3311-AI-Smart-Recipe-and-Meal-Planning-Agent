package com.smartplate.domain;

import java.util.List;
import java.util.Locale;

/**
 * The only units SmartPlate supports: g, kg, ml, l and pcs (report section 1.3, F02, F10).
 * Converts between units of the same family so quantities can be added and subtracted.
 */
public final class Units {

    public static final List<String> SUPPORTED = List.of("g", "kg", "ml", "l", "pcs");

    /** Unit families. Only units of the same family can be combined. */
    public enum Family { MASS, VOLUME, COUNT }

    private Units() {
    }

    public static String normalize(String unit) {
        return unit == null ? "" : unit.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isSupported(String unit) {
        return SUPPORTED.contains(normalize(unit));
    }

    public static Family family(String unit) {
        switch (normalize(unit)) {
            case "g":
            case "kg":
                return Family.MASS;
            case "ml":
            case "l":
                return Family.VOLUME;
            case "pcs":
                return Family.COUNT;
            default:
                throw new SmartPlateException("Unsupported unit '" + unit + "'. Use one of " + SUPPORTED + ".");
        }
    }

    public static boolean compatible(String a, String b) {
        return isSupported(a) && isSupported(b) && family(a) == family(b);
    }

    /** Converts a quantity to the base unit of its family (g, ml or pcs). */
    public static double toBase(double quantity, String unit) {
        String u = normalize(unit);
        if (u.equals("kg") || u.equals("l")) {
            return quantity * 1000.0;
        }
        family(u); // throws for unsupported units
        return quantity;
    }

    public static String baseUnit(Family family) {
        switch (family) {
            case MASS:
                return "g";
            case VOLUME:
                return "ml";
            default:
                return "pcs";
        }
    }

    /** Converts {@code quantity} in {@code fromUnit} into {@code toUnit} (same family only). */
    public static double convert(double quantity, String fromUnit, String toUnit) {
        if (!compatible(fromUnit, toUnit)) {
            throw new SmartPlateException("Cannot convert " + fromUnit + " to " + toUnit + ".");
        }
        return toBase(quantity, fromUnit) / toBase(1.0, toUnit);
    }

    /** Formats a quantity for display, e.g. "1.5 kg", "200 g", "3 pcs". */
    public static String format(double quantity, String unit) {
        double rounded = Math.round(quantity * 100.0) / 100.0;
        if (rounded == Math.rint(rounded)) {
            return String.format(Locale.ROOT, "%d %s", (long) rounded, normalize(unit));
        }
        return String.format(Locale.ROOT, "%s %s", rounded, normalize(unit));
    }
}
