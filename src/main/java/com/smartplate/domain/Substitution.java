package com.smartplate.domain;

/**
 * A predefined substitution: replace {@code original} with {@code substitute},
 * multiplying the quantity by {@code conversionFactor} and using {@code unit}
 * (null keeps the original unit).
 */
public final class Substitution {

    private final Ingredient original;
    private final Ingredient substitute;
    private final double conversionFactor;
    private final String unit;

    public Substitution(Ingredient original, Ingredient substitute, double conversionFactor, String unit) {
        if (conversionFactor <= 0) {
            throw new SmartPlateException("Conversion factor must be positive.");
        }
        this.original = original;
        this.substitute = substitute;
        this.conversionFactor = conversionFactor;
        this.unit = unit == null ? null : Units.normalize(unit);
    }

    public Substitution(Ingredient original, Ingredient substitute, double conversionFactor) {
        this(original, substitute, conversionFactor, null);
    }

    public Ingredient getOriginal() {
        return original;
    }

    public Ingredient getSubstitute() {
        return substitute;
    }

    public double getConversionFactor() {
        return conversionFactor;
    }

    /** Unit of the substitute, or null to keep the original unit. */
    public String getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return original + " -> " + substitute + " (x" + conversionFactor + ")";
    }
}
