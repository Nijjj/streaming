package com.example.streamscout.util;

import org.json.JSONArray;
import org.json.JSONObject;

public final class JsonPath {
    private JsonPath() {
    }

    public static Object read(Object root, String path) {
        if (root == null || path == null || path.trim().isEmpty()) return root;
        Object current = root;
        String[] parts = path.split("\\.");
        for (String part : parts) {
            if (current == null) return null;
            if (current instanceof JSONObject) {
                current = ((JSONObject) current).opt(part);
            } else if (current instanceof JSONArray) {
                try {
                    current = ((JSONArray) current).opt(Integer.parseInt(part));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return current;
    }

    public static String readString(Object root, String path) {
        Object value = read(root, path);
        return value == null || JSONObject.NULL.equals(value) ? "" : String.valueOf(value);
    }

    public static JSONArray readArray(Object root, String path) {
        Object value = read(root, path);
        return value instanceof JSONArray ? (JSONArray) value : new JSONArray();
    }
}
