package dev.hendrikhoemberg.witchfirerandomizer.model;

public enum Element {
    Fire("#ff4d4d"),
    Water("#4da6ff"),
    Earth("#66cc66"),
    Air("#ffcc00");

    private final String color;

    Element(String color) {
        this.color = color;
    }

    public String getColor() {
        return color;
    }

    public static Element fromString(String val) {
        if (val == null || val.isBlank()) return null;
        for (Element e : values()) {
            if (e.name().equalsIgnoreCase(val.trim())) {
                return e;
            }
        }
        return null;
    }
}
