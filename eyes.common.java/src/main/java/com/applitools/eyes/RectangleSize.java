package com.applitools.eyes;

import com.applitools.utils.ArgumentGuard;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a 2D size.
 */
public class RectangleSize {
    private int width;
    private int height;

    /**
     * Creates a new RectangleSize instance.
     * @param width The width of the rectangle.
     * @param height The height of the rectangle.
     */
    @JsonCreator
    public RectangleSize(@JsonProperty("width") int width, @JsonProperty("height") int height) {
        ArgumentGuard.greaterThanOrEqualToZero(width, "width");
        ArgumentGuard.greaterThanOrEqualToZero(height, "height");

        this.width = width;
        this.height = height;
    }

    /**
     * @return The rectangle's width.
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return The rectangle's height.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Parses a string into a {link RectangleSize} instance.
     * @param size A string representing width and height separated by "x".
     * @return An instance representing the input size.
     */
    public static RectangleSize parse(String size) {
        ArgumentGuard.notNull(size, "size");
        String[] parts = size.split("x");
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "Not a valid size string: " + size);
        }

        return new RectangleSize(
                Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }


    /**
     * Get a scaled version of the current size.
     *
     * @param scaleRatio The ratio by which to scale.
     * @return A scaled version of the current size.
     */
    public RectangleSize scale(double scaleRatio) {
        return new RectangleSize((int) Math.round(width * scaleRatio),
                (int) Math.round(height * scaleRatio));
    }

    /**
     * @param obj A {@link com.applitools.eyes.RectangleSize} instance to be
     *            checked for equality with the current instance.
     * @return {@code true} if and only if the input objects are equal by
     *          value, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof RectangleSize)) {
            return false;
        }

        RectangleSize other = (RectangleSize) obj;
        return width == other.width && height == other.height;
    }

    @Override
    public int hashCode() {
        return width ^ height;
    }


    @Override
    public String toString() {
        return width + "x" + height;
    }
}
