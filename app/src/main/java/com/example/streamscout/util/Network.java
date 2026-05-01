package com.example.streamscout.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class Network {
    private Network() {
    }

    public static String getText(String url) throws IOException {
        return new String(getBytes(url), "UTF-8");
    }

    public static byte[] getBytes(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(9000);
        connection.setReadTimeout(12000);
        connection.setRequestProperty("User-Agent", "StreamScout/1.0 Android");
        connection.setRequestProperty("Accept", "*/*");
        int status = connection.getResponseCode();
        InputStream input = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        byte[] body = readAll(input);
        connection.disconnect();
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP " + status + " for " + url);
        }
        return body;
    }

    private static byte[] readAll(InputStream input) throws IOException {
        if (input == null) return new byte[0];
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        input.close();
        return output.toByteArray();
    }
}
