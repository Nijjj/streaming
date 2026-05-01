package com.example.streamscout.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.widget.ImageView;

import com.example.streamscout.util.Network;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ImageLoader {
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final LruCache<String, Bitmap> cache;

    public ImageLoader() {
        int maxKb = (int) (Runtime.getRuntime().maxMemory() / 1024);
        cache = new LruCache<String, Bitmap>(Math.max(2048, maxKb / 12)) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };
    }

    public void load(final String url, final ImageView target) {
        target.setImageDrawable(null);
        target.setTag(url == null ? "" : url);
        if (url == null || url.trim().isEmpty()) return;

        Bitmap cached = cache.get(url);
        if (cached != null) {
            target.setImageBitmap(cached);
            return;
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] bytes = Network.getBytes(url);
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (bitmap == null) return;
                    cache.put(url, bitmap);
                    target.post(new Runnable() {
                        @Override
                        public void run() {
                            if (url.equals(target.getTag())) target.setImageBitmap(bitmap);
                        }
                    });
                } catch (Exception ignored) {
                }
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
