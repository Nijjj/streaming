package com.example.streamscout.plugin;

import com.example.streamscout.model.MediaItem;
import com.example.streamscout.model.MediaType;

import java.util.List;

public interface StreamPlugin {
    String id();

    String name();

    boolean supports(MediaType type);

    List<MediaItem> search(String query, MediaType type) throws Exception;

    MediaItem loadDetails(MediaItem item) throws Exception;
}
