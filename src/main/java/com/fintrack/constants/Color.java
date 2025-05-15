package com.fintrack.constants;

import java.util.*;
import java.util.stream.Collectors;

public enum Color {
    RED("#FF0000"),
    GREEN("#00FF00"),
    BLUE("#0000FF"),
    ORANGE("#FFA500"),
    PURPLE("#800080"),
    CYAN("#00FFFF"),

    // Darker colors
    DARK_CYAN("#008B8B"),
    TEAL("#008080"),
    DARK_OLIVE_GREEN("#556B2F"),
    STEEL_BLUE("#4682B4"),
    MEDIUM_SLATE_BLUE("#7B68EE"),
    INDIAN_RED("#CD5C5C"),
    GOLDENROD("#DAA520"),
    SIENNA("#A0522D"),
    DARK_KHAKI("#BDB76B"),
    CADET_BLUE("#5F9EA0");

    private final String hexCode;

    Color(String hexCode) {
        this.hexCode = hexCode;
    }

    public String getHexCode() {
        return hexCode;
    }

    public static List<Color> getAllColors() {
        return Arrays.asList(Color.values());
    }

    public static boolean exists(String hexCode) {
        if (hexCode == null || !hexCode.matches("^#[0-9A-Fa-f]{6}$")) {
            return false;
        }
        String upperHexCode = hexCode.toUpperCase();
        return Arrays.stream(Color.values())
            .anyMatch(color -> color.getHexCode().equals(upperHexCode));
    }

    public static String getFormattedColorList() {
        return Arrays.stream(Color.values())
            .map(color -> color.name() + " (" + color.getHexCode() + ")")
            .collect(Collectors.joining(", "));
    }
}
