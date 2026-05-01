package com.example.streamscout.model;

import org.json.JSONException;
import org.json.JSONObject;

public final class StreamInfo {
    public final String quality;
    public final String url;
    public final String type;

    public StreamInfo(String quality, String url, String type) {
        this.quality = quality == null ? "Direct" : quality;
        this.url = url == null ? "" : url;
        this.type = type == null ? "stream" : type;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("quality", quality);
        json.put("url", url);
        json.put("type", type);
        return json;
    }

    public static StreamInfo fromJson(JSONObject json) {
        return new StreamInfo(
                json.optString("quality", "Direct"),
                json.optString("url", ""),
                json.optString("type", "stream")
        );
    }
}
