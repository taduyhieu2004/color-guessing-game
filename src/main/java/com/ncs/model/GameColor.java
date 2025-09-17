package com.ncs.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model class đại diện cho một màu trong game
 * Lưu trữ thông tin RGB của màu sắc
 */
public class GameColor {
    private int red;
    private int green;
    private int blue;

    // Constructors
    public GameColor() {}

    @JsonCreator
    public GameColor(@JsonProperty("r") int red, 
                     @JsonProperty("g") int green, 
                     @JsonProperty("b") int blue) {
        this.red = Math.max(0, Math.min(255, red));
        this.green = Math.max(0, Math.min(255, green));
        this.blue = Math.max(0, Math.min(255, blue));
    }

    // Getters and Setters
    @JsonProperty("r")
    public int getRed() {
        return red;
    }

    public void setRed(int red) {
        this.red = Math.max(0, Math.min(255, red));
    }

    @JsonProperty("g")
    public int getGreen() {
        return green;
    }

    public void setGreen(int green) {
        this.green = Math.max(0, Math.min(255, green));
    }

    @JsonProperty("b")
    public int getBlue() {
        return blue;
    }

    public void setBlue(int blue) {
        this.blue = Math.max(0, Math.min(255, blue));
    }

    // Utility methods
    public String toHexString() {
        return String.format("#%02X%02X%02X", red, green, blue);
    }

    public String toRgbString() {
        return String.format("rgb(%d, %d, %d)", red, green, blue);
    }

    // Removed JavaFX dependency

    public static GameColor fromHex(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() != 6) {
            throw new IllegalArgumentException("Invalid hex color format");
        }
        
        int red = Integer.parseInt(hex.substring(0, 2), 16);
        int green = Integer.parseInt(hex.substring(2, 4), 16);
        int blue = Integer.parseInt(hex.substring(4, 6), 16);
        
        return new GameColor(red, green, blue);
    }

    public static GameColor random() {
        return new GameColor(
            (int) (Math.random() * 256),
            (int) (Math.random() * 256),
            (int) (Math.random() * 256)
        );
    }

    // Predefined colors for game
    public static final GameColor RED = new GameColor(255, 0, 0);
    public static final GameColor GREEN = new GameColor(0, 255, 0);
    public static final GameColor BLUE = new GameColor(0, 0, 255);
    public static final GameColor YELLOW = new GameColor(255, 255, 0);
    public static final GameColor PURPLE = new GameColor(128, 0, 128);
    public static final GameColor ORANGE = new GameColor(255, 165, 0);
    public static final GameColor PINK = new GameColor(255, 192, 203);
    public static final GameColor CYAN = new GameColor(0, 255, 255);
    public static final GameColor BROWN = new GameColor(165, 42, 42);
    public static final GameColor GRAY = new GameColor(128, 128, 128);
    public static final GameColor BLACK = new GameColor(0, 0, 0);
    public static final GameColor WHITE = new GameColor(255, 255, 255);

    // Override methods
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GameColor gameColor = (GameColor) obj;
        return red == gameColor.red && green == gameColor.green && blue == gameColor.blue;
    }

    @Override
    public int hashCode() {
        return (red << 16) | (green << 8) | blue;
    }

    @Override
    public String toString() {
        return String.format("GameColor{r=%d, g=%d, b=%d}", red, green, blue);
    }
}
