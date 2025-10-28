package dev.lmv.lmvac.api.implement.checks.type;

public enum DescType {
    PREMIUM("&#FF8656"),
    NEW("&#1467E9"),
    RELEASE("&#6514E9"),
    ALPHA("&#14E9A4"),
    BETA("&#E9A714"),
    OLD("&#D5E914"),
    BUG("&#B82626");

    private final String color;

    DescType(String color) {
        this.color = color;
    }

    public String getColor() {
        return color;
    }
}