package com.example.streamscout;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.streamscout.data.PluginCache;
import com.example.streamscout.model.MediaItem;
import com.example.streamscout.model.MediaType;
import com.example.streamscout.model.StreamInfo;
import com.example.streamscout.plugin.PluginManager;
import com.example.streamscout.plugin.StreamPlugin;
import com.example.streamscout.ui.ImageLoader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(8, 10, 15);
    private static final int SURFACE = Color.rgb(18, 23, 34);
    private static final int SURFACE_2 = Color.rgb(26, 33, 48);
    private static final int TEXT = Color.rgb(239, 244, 255);
    private static final int MUTED = Color.rgb(143, 154, 176);
    private static final int ACCENT = Color.rgb(66, 211, 146);

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final ImageLoader imageLoader = new ImageLoader();
    private final Map<String, String> pluginNames = new LinkedHashMap<>();

    private PluginCache cache;
    private PluginManager pluginManager;
    private List<StreamPlugin> plugins = new ArrayList<>();
    private MediaType selectedType = MediaType.MOVIE;
    private LinearLayout root;
    private LinearLayout content;
    private TextView status;
    private EditText search;
    private Button moviesButton;
    private Button tvButton;
    private TextView subtitle;
    private int searchGeneration = 0;

    private final Runnable searchRunnable = new Runnable() {
        @Override
        public void run() {
            runSearch();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        buildShell();
        initializePlugins();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        imageLoader.shutdown();
        super.onDestroy();
    }

    private void buildShell() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), 0);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(0, 0, 0, dp(12));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        TextView title = new TextView(this);
        title.setText("StreamScout");
        title.setTextColor(TEXT);
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title);

        subtitle = new TextView(this);
        subtitle.setText(plugins.size() + " plugins loaded - external playback");
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(13);
        subtitle.setPadding(0, dp(2), 0, dp(10));
        header.addView(subtitle);

        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextColor(TEXT);
        search.setHintTextColor(MUTED);
        search.setTextSize(15);
        search.setHint("Search movies and TV shows");
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setPadding(dp(14), 0, dp(14), 0);
        search.setBackground(rounded(SURFACE, dp(10), 0, 0));
        header.addView(search, new LinearLayout.LayoutParams(-1, dp(48)));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { scheduleSearch(); }
            @Override public void afterTextChanged(Editable s) { }
        });

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(0, dp(12), 0, 0);
        header.addView(tabs);
        moviesButton = tabButton("Movies", MediaType.MOVIE);
        tvButton = tabButton("TV Shows", MediaType.TV);
        tabs.addView(moviesButton, new LinearLayout.LayoutParams(0, dp(42), 1));
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        tvParams.leftMargin = dp(8);
        tabs.addView(tvButton, tvParams);
        refreshTabs();

        status = new TextView(this);
        status.setTextColor(MUTED);
        status.setTextSize(13);
        status.setPadding(0, dp(2), 0, dp(10));
        header.addView(status);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, dp(24));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);
    }

    private void initializePlugins() {
        try {
            cache = new PluginCache(this);
            pluginManager = new PluginManager(this);
            plugins = pluginManager.loadPlugins();
            pluginNames.clear();
            for (StreamPlugin plugin : plugins) pluginNames.put(plugin.id(), plugin.name());
            subtitle.setText(plugins.size() + " plugins loaded - external playback");
            runSearch();
        } catch (Exception error) {
            plugins = new ArrayList<>();
            pluginNames.clear();
            subtitle.setText("Plugin startup failed");
            status.setText("Startup error");
            content.removeAllViews();
            content.addView(emptyState("Could not load plugins", friendlyError(error)));
        }
    }

    private Button tabButton(String label, final MediaType type) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedType = type;
                refreshTabs();
                runSearch();
            }
        });
        return button;
    }

    private void refreshTabs() {
        styleTab(moviesButton, selectedType == MediaType.MOVIE);
        styleTab(tvButton, selectedType == MediaType.TV);
    }

    private void styleTab(Button button, boolean active) {
        button.setTextColor(active ? BG : TEXT);
        button.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
        button.setBackground(rounded(active ? ACCENT : SURFACE, dp(10), 0, 0));
    }

    private void scheduleSearch() {
        main.removeCallbacks(searchRunnable);
        main.postDelayed(searchRunnable, 450);
    }

    private void runSearch() {
        final int generation = ++searchGeneration;
        final String typedQuery = search.getText() == null ? "" : search.getText().toString().trim();
        final String effectiveQuery = typedQuery.isEmpty() ? (selectedType == MediaType.MOVIE ? "public domain" : "classic tv") : typedQuery;

        content.removeAllViews();
        content.addView(loadingRow("Searching " + selectedType.label.toLowerCase() + "..."));
        status.setText("Searching " + supportedPluginCount(selectedType) + " plugin(s)");

        executor.execute(new Runnable() {
            @Override
            public void run() {
                final List<MediaItem> all = new ArrayList<>();
                boolean usedCache = false;
                try {
                    for (StreamPlugin plugin : plugins) {
                        if (!plugin.supports(selectedType)) continue;
                        try {
                            List<MediaItem> items = plugin.search(effectiveQuery, selectedType);
                            if (cache != null) cache.putSearch(plugin.id(), selectedType.id, effectiveQuery, items);
                            all.addAll(items);
                        } catch (Exception error) {
                            List<MediaItem> cached = cache == null ? new ArrayList<MediaItem>() : cache.getSearch(plugin.id(), selectedType.id, effectiveQuery);
                            if (!cached.isEmpty()) {
                                usedCache = true;
                                all.addAll(cached);
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
                final boolean cacheResult = usedCache;
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        if (generation != searchGeneration) return;
                        showSearchResults(all, typedQuery, cacheResult);
                    }
                });
            }
        });
    }

    private int supportedPluginCount(MediaType type) {
        int count = 0;
        for (StreamPlugin plugin : plugins) if (plugin.supports(type)) count++;
        return count;
    }

    private void showSearchResults(List<MediaItem> items, String typedQuery, boolean cacheResult) {
        content.removeAllViews();
        String prefix = cacheResult ? "Offline cache" : "Results";
        status.setText(prefix + " - " + items.size() + " item(s)" + (typedQuery.isEmpty() ? " - default discovery" : ""));
        if (plugins.isEmpty()) {
            content.addView(emptyState("No plugins found", "Add plugin folders under the app's files/plugins directory."));
            return;
        }
        if (items.isEmpty()) {
            content.addView(emptyState("No results", "Try a different title or add another plugin."));
            return;
        }
        for (MediaItem item : items) content.addView(mediaRow(item));
    }

    private View mediaRow(final MediaItem item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(10), dp(10), dp(10));
        row.setBackground(rounded(SURFACE, dp(8), 1, Color.rgb(31, 39, 56)));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowParams);

        ImageView poster = new ImageView(this);
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        poster.setBackgroundColor(SURFACE_2);
        row.addView(poster, new LinearLayout.LayoutParams(dp(86), dp(128)));
        imageLoader.load(item.poster, poster);

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.VERTICAL);
        meta.setPadding(dp(12), 0, 0, 0);
        row.addView(meta, new LinearLayout.LayoutParams(0, -2, 1));

        TextView title = label(item.title, TEXT, 17, true);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        meta.addView(title);

        TextView plugin = label(pluginName(item.pluginId) + " - " + item.type.label, ACCENT, 12, false);
        plugin.setPadding(0, dp(4), 0, dp(6));
        meta.addView(plugin);

        TextView description = label(item.description.isEmpty() ? "Details and streams load on selection." : item.description, MUTED, 13, false);
        description.setMaxLines(3);
        description.setEllipsize(TextUtils.TruncateAt.END);
        meta.addView(description);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDetails(item);
            }
        });
        return row;
    }

    private void showDetails(final MediaItem item) {
        final int generation = ++searchGeneration;
        content.removeAllViews();
        content.addView(loadingRow("Loading streams..."));
        status.setText(pluginName(item.pluginId));

        executor.execute(new Runnable() {
            @Override
            public void run() {
                MediaItem details = null;
                try {
                    StreamPlugin plugin = pluginManager.get(item.pluginId);
                    if (plugin != null) {
                        details = plugin.loadDetails(item);
                        if (details != null && cache != null) cache.putDetails(details);
                    }
                } catch (Exception ignored) {
                    details = cache == null ? null : cache.getDetails(item.pluginId, item.type.id, item.id);
                }
                if (details == null) details = item;
                final MediaItem finalDetails = details;
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        if (generation != searchGeneration) return;
                        renderDetails(finalDetails);
                    }
                });
            }
        });
    }

    private void renderDetails(final MediaItem item) {
        content.removeAllViews();
        status.setText(pluginName(item.pluginId) + " - " + item.streams.size() + " stream(s)");

        Button back = new Button(this);
        back.setText("Back");
        back.setAllCaps(false);
        back.setTextColor(TEXT);
        back.setBackground(rounded(SURFACE, dp(8), 0, 0));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runSearch();
            }
        });
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(dp(96), dp(42));
        backParams.setMargins(0, 0, 0, dp(12));
        content.addView(back, backParams);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setPadding(dp(10), dp(10), dp(10), dp(10));
        hero.setBackground(rounded(SURFACE, dp(8), 1, Color.rgb(31, 39, 56)));
        content.addView(hero, new LinearLayout.LayoutParams(-1, -2));

        ImageView poster = new ImageView(this);
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        poster.setBackgroundColor(SURFACE_2);
        hero.addView(poster, new LinearLayout.LayoutParams(dp(118), dp(176)));
        imageLoader.load(item.poster, poster);

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.VERTICAL);
        meta.setPadding(dp(12), 0, 0, 0);
        hero.addView(meta, new LinearLayout.LayoutParams(0, -2, 1));
        TextView title = label(item.title, TEXT, 20, true);
        title.setMaxLines(3);
        title.setEllipsize(TextUtils.TruncateAt.END);
        meta.addView(title);
        TextView source = label(pluginName(item.pluginId) + " - " + item.type.label, ACCENT, 12, false);
        source.setPadding(0, dp(6), 0, dp(8));
        meta.addView(source);
        TextView description = label(item.description.isEmpty() ? "No description available." : item.description, MUTED, 13, false);
        description.setMaxLines(8);
        description.setEllipsize(TextUtils.TruncateAt.END);
        meta.addView(description);

        TextView streamTitle = label("Available streams", TEXT, 18, true);
        streamTitle.setPadding(0, dp(18), 0, dp(8));
        content.addView(streamTitle);

        if (item.streams.isEmpty()) {
            content.addView(emptyState("No playable streams", "The plugin returned metadata but no supported stream links."));
            return;
        }
        for (final StreamInfo stream : item.streams) content.addView(streamRow(stream));
    }

    private View streamRow(final StreamInfo stream) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(10), dp(10));
        row.setBackground(rounded(SURFACE, dp(8), 1, Color.rgb(31, 39, 56)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(params);

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.VERTICAL);
        row.addView(meta, new LinearLayout.LayoutParams(0, -2, 1));
        TextView quality = label(stream.quality, TEXT, 15, true);
        meta.addView(quality);
        TextView url = label(stream.type + " - " + compactUrl(stream.url), MUTED, 12, false);
        url.setMaxLines(1);
        url.setEllipsize(TextUtils.TruncateAt.END);
        meta.addView(url);

        ImageButton open = new ImageButton(this);
        open.setImageResource(android.R.drawable.ic_media_play);
        open.setColorFilter(BG);
        open.setBackground(rounded(ACCENT, dp(8), 0, 0));
        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openExternal(stream.url);
            }
        });
        row.addView(open, new LinearLayout.LayoutParams(dp(44), dp(44)));
        return row;
    }

    private void openExternal(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(intent, "Open stream"));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "No external player can open this stream", Toast.LENGTH_LONG).show();
        } catch (Exception error) {
            Toast.makeText(this, "Invalid stream URL", Toast.LENGTH_LONG).show();
        }
    }

    private View loadingRow(String text) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, dp(48), 0, dp(48));
        ProgressBar progress = new ProgressBar(this);
        row.addView(progress, new LinearLayout.LayoutParams(dp(36), dp(36)));
        TextView label = label(text, MUTED, 14, false);
        label.setPadding(dp(12), 0, 0, 0);
        row.addView(label);
        return row;
    }

    private View emptyState(String title, String message) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(20), dp(16), dp(20));
        box.setBackground(rounded(SURFACE, dp(8), 1, Color.rgb(31, 39, 56)));
        box.addView(label(title, TEXT, 17, true));
        TextView body = label(message, MUTED, 13, false);
        body.setPadding(0, dp(6), 0, 0);
        box.addView(body);
        return box;
    }

    private TextView label(String text, int color, int sp, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(sp);
        view.setIncludeFontPadding(true);
        if (bold) view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }

    private String pluginName(String pluginId) {
        String name = pluginNames.get(pluginId);
        return name == null ? pluginId : name;
    }

    private String compactUrl(String url) {
        if (url == null) return "";
        try {
            Uri uri = Uri.parse(url);
            if (uri.getHost() != null) return uri.getHost();
        } catch (Exception ignored) {
        }
        return url.length() > 42 ? url.substring(0, 42) : url;
    }

    private String friendlyError(Exception error) {
        String message = error == null ? "" : error.getMessage();
        if (message == null || message.trim().isEmpty()) return "Unexpected startup failure. Try reinstalling the debug APK.";
        return message;
    }

    private GradientDrawable rounded(int color, int radius, int strokeWidth, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) drawable.setStroke(dp(strokeWidth), strokeColor);
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
