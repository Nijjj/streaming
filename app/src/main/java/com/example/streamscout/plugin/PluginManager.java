package com.example.streamscout.plugin;

import android.content.Context;
import android.content.res.AssetManager;

import com.example.streamscout.plugin.json.JsonStreamPlugin;
import com.example.streamscout.plugin.json.PluginDescriptor;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PluginManager {
    private static final String ASSET_PLUGIN_ROOT = "plugins";
    private final Context context;
    private final Map<String, StreamPlugin> plugins = new LinkedHashMap<>();

    public PluginManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public List<StreamPlugin> loadPlugins() {
        plugins.clear();
        copyBundledPluginsIfMissing();
        scanPluginRoot(internalPluginDir());
        File external = context.getExternalFilesDir("plugins");
        if (external != null) scanPluginRoot(external);
        return new ArrayList<>(plugins.values());
    }

    public StreamPlugin get(String id) {
        return plugins.get(id);
    }

    public File internalPluginDir() {
        return new File(context.getFilesDir(), "plugins");
    }

    private void scanPluginRoot(File root) {
        if (root == null || !root.exists() || !root.isDirectory()) return;
        File[] folders = root.listFiles();
        if (folders == null) return;
        for (File folder : folders) {
            if (!folder.isDirectory()) continue;
            File config = new File(folder, "plugin.json");
            if (!config.exists()) continue;
            try {
                PluginDescriptor descriptor = PluginDescriptor.fromJson(new JSONObject(readAll(config)));
                if (descriptor.isValid()) plugins.put(descriptor.id, new JsonStreamPlugin(descriptor));
            } catch (Exception ignored) {
                // Bad third-party plugins are skipped so the app can still launch.
            }
        }
    }

    private void copyBundledPluginsIfMissing() {
        try {
            AssetManager assets = context.getAssets();
            String[] pluginIds = assets.list(ASSET_PLUGIN_ROOT);
            if (pluginIds == null) return;
            File root = internalPluginDir();
            if (!root.exists()) root.mkdirs();
            for (String pluginId : pluginIds) {
                String assetFolder = ASSET_PLUGIN_ROOT + "/" + pluginId;
                File targetFolder = new File(root, pluginId);
                if (!targetFolder.exists()) targetFolder.mkdirs();
                String[] files = assets.list(assetFolder);
                if (files == null) continue;
                for (String fileName : files) {
                    File target = new File(targetFolder, fileName);
                    if (target.exists()) continue;
                    copyAsset(assetFolder + "/" + fileName, target);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void copyAsset(String assetPath, File target) throws Exception {
        InputStream input = context.getAssets().open(assetPath);
        FileOutputStream output = new FileOutputStream(target);
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        input.close();
        output.close();
    }

    private static String readAll(File file) throws Exception {
        InputStream input = new java.io.FileInputStream(file);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        input.close();
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}
