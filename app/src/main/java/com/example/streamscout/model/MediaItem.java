package com.example.streamscout.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class MediaItem {
    public final String pluginId;
    public final String id;
    public final MediaType type;
    public final String title;
    public final String poster;
    public final String description;
    public final List<StreamInfo> streams;

    public MediaItem(String pluginId, String id, MediaType type, String title, String poster, String description, List<StreamInfo> streams) {
        this.pluginId = pluginId == null ? "" : pluginId;
        this.id = id == null ? "" : id;
        this.type = type == null ? MediaType.MOVIE : type;
        this.title = title == null || title.trim().isEmpty() ? "Untitled" : title.trim();
        this.poster = poster == null ? "" : poster;
        this.description = description == null ? "" : description.trim();
        this.streams = streams == null ? new ArrayList<StreamInfo>() : streams;
    }

    public MediaItem withDetails(String poster, String description, List<StreamInfo> streams) {
        return new MediaItem(
                pluginId,
                id,
                type,
                title,
                poster == null || poster.isEmpty() ? this.poster : poster,
                description == null || description.isEmpty() ? this.description : description,
                streams == null ? this.streams : streams
        );
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("pluginId", pluginId);
        json.put("id", id);
        json.put("type", type.id);
        json.put("title", title);
        json.put("poster", poster);
        json.put("description", description);
        JSONArray streamJson = new JSONArray();
        for (StreamInfo stream : streams) {
            streamJson.put(stream.toJson());
        }
        json.put("streams", streamJson);
        return json;
    }

    public static MediaItem fromJson(JSONObject json) {
        List<StreamInfo> streams = new ArrayList<>();
        JSONArray streamJson = json.optJSONArray("streams");
        if (streamJson != null) {
            for (int i = 0; i < streamJson.length(); i++) {
                JSONObject item = streamJson.optJSONObject(i);
                if (item != null) streams.add(StreamInfo.fromJson(item));
            }
        }
        return new MediaItem(
                json.optString("pluginId"),
                json.optString("id"),
                MediaType.fromId(json.optString("type", "movie")),
                json.optString("title"),
                json.optString("poster"),
                json.optString("description"),
                streams
        );
    }
}
