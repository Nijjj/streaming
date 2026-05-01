package com.example.streamscout.data;

import android.content.Context;

import com.example.streamscout.model.MediaItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class PluginCache {
    private static final int MAX_CACHE_FILES = 160;
    private final File root;

    public PluginCache(Context context) {
        root = new File(context.getCacheDir(), "plugin-json");
        if (!root.exists()) root.mkdirs();
    }

    public void putSearch(String pluginId, String type, String query, List<MediaItem> items) {
        try {
            JSONArray array = new JSONArray();
            for (MediaItem item : items) array.put(item.toJson());
            write(fileFor("search", pluginId, type, query), array.toString());
            prune();
        } catch (Exception ignored) {
        }
    }

    public List<MediaItem> getSearch(String pluginId, String type, String query) {
        List<MediaItem> items = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(read(fileFor("search", pluginId, type, query)));
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item != null) items.add(MediaItem.fromJson(item));
            }
        } catch (Exception ignored) {
        }
        return items;
    }

    public void putDetails(MediaItem item) {
        try {
            write(fileFor("detail", item.pluginId, item.type.id, item.id), item.toJson().toString());
            prune();
        } catch (Exception ignored) {
        }
    }

    public MediaItem getDetails(String pluginId, String type, String id) {
        try {
            return MediaItem.fromJson(new JSONObject(read(fileFor("detail", pluginId, type, id))));
        } catch (Exception ignored) {
            return null;
        }
    }

    private File fileFor(String prefix, String pluginId, String type, String key) throws Exception {
        return new File(root, prefix + "-" + sha1(pluginId + "|" + type + "|" + key) + ".json");
    }

    private void write(File file, String text) throws Exception {
        FileOutputStream output = new FileOutputStream(file);
        output.write(text.getBytes(StandardCharsets.UTF_8));
        output.close();
    }

    private String read(File file) throws Exception {
        InputStream input = new java.io.FileInputStream(file);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
        input.close();
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private void prune() {
        File[] files = root.listFiles();
        if (files == null || files.length <= MAX_CACHE_FILES) return;
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File left, File right) {
                return Long.compare(left.lastModified(), right.lastModified());
            }
        });
        for (int i = 0; i < files.length - MAX_CACHE_FILES; i++) files[i].delete();
    }

    private static String sha1(String text) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        for (byte value : bytes) {
            String hex = Integer.toHexString(value & 0xff);
            if (hex.length() == 1) out.append('0');
            out.append(hex);
        }
        return out.toString();
    }
}
