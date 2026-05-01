package com.example.streamscout.plugin.json;

import com.example.streamscout.model.MediaItem;
import com.example.streamscout.model.MediaType;
import com.example.streamscout.model.StreamInfo;
import com.example.streamscout.plugin.StreamPlugin;
import com.example.streamscout.util.JsonPath;
import com.example.streamscout.util.Network;
import com.example.streamscout.util.Template;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class JsonStreamPlugin implements StreamPlugin {
    private final PluginDescriptor descriptor;

    public JsonStreamPlugin(PluginDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public String id() {
        return descriptor.id;
    }

    @Override
    public String name() {
        return descriptor.name;
    }

    @Override
    public boolean supports(MediaType type) {
        return descriptor.mediaTypes.contains(type);
    }

    @Override
    public List<MediaItem> search(String query, MediaType type) throws Exception {
        Map<String, String> values = new HashMap<>();
        values.put("query", query == null ? "" : query);
        values.put("type", type.id);

        Object root = parse(Network.getText(Template.render(descriptor.search.url, values)));
        JSONArray results = JsonPath.readArray(root, descriptor.search.resultsPath);
        List<MediaItem> items = new ArrayList<>();
        for (int i = 0; i < results.length(); i++) {
            JSONObject itemJson = results.optJSONObject(i);
            if (itemJson == null) continue;
            String id = read(itemJson, descriptor.search.idPath);
            String title = read(itemJson, descriptor.search.titlePath);
            if (id.isEmpty() || title.isEmpty()) continue;

            Map<String, String> itemValues = valuesFor(itemJson);
            itemValues.put("id", id);
            itemValues.put("title", title);
            itemValues.put("query", query == null ? "" : query);
            itemValues.put("type", type.id);

            String poster = read(itemJson, descriptor.search.posterPath);
            if (poster.isEmpty() && !descriptor.search.posterTemplate.isEmpty()) {
                poster = Template.render(descriptor.search.posterTemplate, itemValues);
            }

            items.add(new MediaItem(
                    descriptor.id,
                    id,
                    type,
                    title,
                    poster,
                    stripHtml(read(itemJson, descriptor.search.descriptionPath)),
                    new ArrayList<StreamInfo>()
            ));
        }
        return items;
    }

    @Override
    public MediaItem loadDetails(MediaItem item) throws Exception {
        if (descriptor.details == null || descriptor.details.url.isEmpty()) return item;

        Map<String, String> values = new HashMap<>();
        values.put("id", item.id);
        values.put("title", item.title);
        values.put("type", item.type.id);

        Object root = parse(Network.getText(Template.render(descriptor.details.url, values)));
        String title = first(read(root, descriptor.details.titlePath), item.title);
        String description = first(stripHtml(read(root, descriptor.details.descriptionPath)), item.description);
        String poster = read(root, descriptor.details.posterPath);
        if (poster.isEmpty() && !descriptor.details.posterTemplate.isEmpty()) {
            poster = Template.render(descriptor.details.posterTemplate, values);
        }

        JSONArray streamArray = JsonPath.readArray(root, descriptor.details.streamsPath);
        List<StreamInfo> streams = new ArrayList<>();
        for (int i = 0; i < streamArray.length(); i++) {
            JSONObject streamJson = streamArray.optJSONObject(i);
            if (streamJson == null) continue;
            String url = read(streamJson, descriptor.details.streamUrlPath);
            if (url.isEmpty() && !descriptor.details.streamUrlTemplate.isEmpty()) {
                Map<String, String> streamValues = valuesFor(streamJson);
                streamValues.putAll(values);
                url = Template.render(descriptor.details.streamUrlTemplate, streamValues);
            }
            if (url.isEmpty() || !matchesAllowedExtension(url)) continue;

            String quality = first(read(streamJson, descriptor.details.streamQualityPath), inferQuality(url));
            String streamType = first(read(streamJson, descriptor.details.streamTypePath), descriptor.details.streamType);
            if (url.startsWith("magnet:")) streamType = "magnet";
            streams.add(new StreamInfo(quality, url, streamType));
        }

        return new MediaItem(item.pluginId, item.id, item.type, title, first(poster, item.poster), description, streams);
    }

    private boolean matchesAllowedExtension(String url) {
        if (descriptor.details == null || descriptor.details.streamExtensions.isEmpty()) return true;
        String lower = url.toLowerCase(Locale.US);
        int queryStart = lower.indexOf('?');
        if (queryStart >= 0) lower = lower.substring(0, queryStart);
        for (String extension : descriptor.details.streamExtensions) {
            if (lower.endsWith(extension.toLowerCase(Locale.US))) return true;
        }
        return false;
    }

    private static Object parse(String text) throws Exception {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.startsWith("[")) return new JSONArray(trimmed);
        return new JSONObject(trimmed);
    }

    private static String read(Object root, String path) {
        return JsonPath.readString(root, path).trim();
    }

    private static String first(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? (fallback == null ? "" : fallback) : value.trim();
    }

    private static Map<String, String> valuesFor(JSONObject json) {
        Map<String, String> values = new HashMap<>();
        JSONArray keys = json.names();
        if (keys == null) return values;
        for (int i = 0; i < keys.length(); i++) {
            String key = keys.optString(i);
            Object value = json.opt(key);
            if (value != null && !JSONObject.NULL.equals(value)) values.put(key, String.valueOf(value));
        }
        return values;
    }

    private static String stripHtml(String text) {
        if (text == null) return "";
        return text.replaceAll("(?is)<br\\s*/?>", "\n")
                .replaceAll("(?is)<[^>]+>", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String inferQuality(String url) {
        String lower = url == null ? "" : url.toLowerCase(Locale.US);
        if (lower.contains("2160") || lower.contains("4k")) return "4K";
        if (lower.contains("1080")) return "1080p";
        if (lower.contains("720")) return "720p";
        if (lower.contains("480")) return "480p";
        if (lower.contains("360")) return "360p";
        return "Direct";
    }
}
