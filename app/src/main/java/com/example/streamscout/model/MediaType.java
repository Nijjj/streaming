package com.example.streamscout.model;

public enum MediaType {
    MOVIE("movie", "Movies"),
    TV("tv", "TV Shows");

    public final String id;
    public final String label;

    MediaType(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public static MediaType fromId(String id) {
        for (MediaType type : values()) {
            if (type.id.equals(id)) return type;
        }
        return MOVIE;
    }
}
