package com.example.streamscout.util;

import android.net.Uri;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Template {
    private static final Pattern TOKEN = Pattern.compile("\\{([a-zA-Z0-9_]+)}");

    private Template() {
    }

    public static String render(String template, Map<String, String> values) {
        if (template == null) return "";
        Matcher matcher = TOKEN.matcher(template);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = values.get(key);
            if (value == null) value = "";
            matcher.appendReplacement(out, Matcher.quoteReplacement(Uri.encode(value)));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
