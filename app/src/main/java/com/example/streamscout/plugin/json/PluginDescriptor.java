package com.example.streamscout.plugin.json;

import com.example.streamscout.model.MediaType;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PluginDescriptor {
    public final int schema;
    public final String id;
    public final String name;
    public final String version;
    public final String siteUrl;
    public final Set<MediaType> mediaTypes;
    public final SearchRequest search;
    public final DetailRequest details;

    private PluginDescriptor(int schema, String id, String name, String version, String siteUrl, Set<MediaType> mediaTypes, SearchRequest search, DetailRequest details) {
        this.schema = schema;
        this.id = clean(id);
        this.name = clean(name);
        this.version = clean(version);
        this.siteUrl = clean(siteUrl);
        this.mediaTypes = mediaTypes;
        this.search = search;
        this.details = details;
    }

    public static PluginDescriptor fromJson(JSONObject json) {
        Set<MediaType> mediaTypes = new HashSet<>();
        JSONArray types = json.optJSONArray("mediaTypes");
        if (types == null || types.length() == 0) {
            mediaTypes.add(MediaType.MOVIE);
            mediaTypes.add(MediaType.TV);
        } else {
            for (int i = 0; i < types.length(); i++) {
                mediaTypes.add(MediaType.fromId(types.optString(i)));
            }
        }

        return new PluginDescriptor(
                json.optInt("schema", 1),
                json.optString("id"),
                json.optString("name"),
                json.optString("version", "1.0"),
                json.optString("siteUrl"),
                mediaTypes,
                SearchRequest.fromJson(json.optJSONObject("search")),
                DetailRequest.fromJson(json.optJSONObject("details"))
        );
    }

    public boolean isValid() {
        return schema == 1 && !id.isEmpty() && !name.isEmpty() && search != null && !search.url.isEmpty();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    static List<String> stringList(JSONObject json, String key) {
        List<String> values = new ArrayList<>();
        JSONArray array = json == null ? null : json.optJSONArray(key);
        if (array == null) return values;
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i, "").trim();
            if (!value.isEmpty()) values.add(value);
        }
        return values;
    }

    public static final class SearchRequest {
        public final String url;
        public final String resultsPath;
        public final String idPath;
        public final String titlePath;
        public final String posterPath;
        public final String posterTemplate;
        public final String descriptionPath;

        private SearchRequest(String url, String resultsPath, String idPath, String titlePath, String posterPath, String posterTemplate, String descriptionPath) {
            this.url = clean(url);
            this.resultsPath = clean(resultsPath);
            this.idPath = clean(idPath);
            this.titlePath = clean(titlePath);
            this.posterPath = clean(posterPath);
            this.posterTemplate = clean(posterTemplate);
            this.descriptionPath = clean(descriptionPath);
        }

        static SearchRequest fromJson(JSONObject json) {
            if (json == null) return null;
            return new SearchRequest(
                    json.optString("url"),
                    json.optString("resultsPath"),
                    json.optString("idPath", "id"),
                    json.optString("titlePath", "title"),
                    json.optString("posterPath"),
                    json.optString("posterTemplate"),
                    json.optString("descriptionPath", "description")
            );
        }
    }

    public static final class DetailRequest {
        public final String url;
        public final String titlePath;
        public final String posterPath;
        public final String posterTemplate;
        public final String descriptionPath;
        public final String streamsPath;
        public final String streamUrlPath;
        public final String streamUrlTemplate;
        public final String streamQualityPath;
        public final String streamTypePath;
        public final String streamType;
        public final List<String> streamExtensions;

        private DetailRequest(String url, String titlePath, String posterPath, String posterTemplate, String descriptionPath, String streamsPath, String streamUrlPath, String streamUrlTemplate, String streamQualityPath, String streamTypePath, String streamType, List<String> streamExtensions) {
            this.url = clean(url);
            this.titlePath = clean(titlePath);
            this.posterPath = clean(posterPath);
            this.posterTemplate = clean(posterTemplate);
            this.descriptionPath = clean(descriptionPath);
            this.streamsPath = clean(streamsPath);
            this.streamUrlPath = clean(streamUrlPath);
            this.streamUrlTemplate = clean(streamUrlTemplate);
            this.streamQualityPath = clean(streamQualityPath);
            this.streamTypePath = clean(streamTypePath);
            this.streamType = clean(streamType);
            this.streamExtensions = streamExtensions;
        }

        static DetailRequest fromJson(JSONObject json) {
            if (json == null) return null;
            return new DetailRequest(
                    json.optString("url"),
                    json.optString("titlePath", "title"),
                    json.optString("posterPath"),
                    json.optString("posterTemplate"),
                    json.optString("descriptionPath", "description"),
                    json.optString("streamsPath", "streams"),
                    json.optString("streamUrlPath", "url"),
                    json.optString("streamUrlTemplate"),
                    json.optString("streamQualityPath", "quality"),
                    json.optString("streamTypePath", "type"),
                    json.optString("streamType", "stream"),
                    stringList(json, "streamExtensions")
            );
        }
    }
}
