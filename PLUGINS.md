# StreamScout Plugin API

Plugins are folder-based. Each plugin lives in its own directory and must contain a `plugin.json` file:

```text
plugins/
  my_plugin/
    plugin.json
```

At runtime StreamScout scans:

- App-private internal folder: `files/plugins`
- App-specific external folder: `Android/data/com.example.streamscout/files/plugins`

The bundled Internet Archive plugin is copied into the internal plugin folder on first launch.

## `plugin.json`

```json
{
  "schema": 1,
  "id": "my_plugin",
  "name": "My Plugin",
  "version": "1.0.0",
  "siteUrl": "https://example.com",
  "mediaTypes": ["movie", "tv"],
  "search": {
    "url": "https://example.com/search?q={query}",
    "resultsPath": "results",
    "idPath": "id",
    "titlePath": "title",
    "posterPath": "poster",
    "descriptionPath": "description"
  },
  "details": {
    "url": "https://example.com/title/{id}",
    "titlePath": "title",
    "posterPath": "poster",
    "descriptionPath": "description",
    "streamsPath": "streams",
    "streamUrlPath": "url",
    "streamQualityPath": "quality",
    "streamTypePath": "type",
    "streamExtensions": [".mp4", ".m3u8", ".webm"]
  }
}
```

Supported template tokens include `{query}`, `{type}`, `{id}`, `{title}`, plus top-level fields from the current JSON object. Search returns app models shaped as:

```json
{
  "title": "Title",
  "poster": "https://example.com/poster.jpg",
  "description": "Description",
  "streams": [
    { "quality": "1080p", "url": "https://example.com/video.mp4", "type": "stream" }
  ]
}
```

## Adding A Plugin

1. Create a new folder under one of the scanned plugin roots.
2. Add a `plugin.json` matching schema `1`.
3. Restart the app.
4. Search from Movies or TV Shows. If the network request fails, StreamScout falls back to cached JSON from previous successful searches/details.

StreamScout does not ship an internal player. Stream URLs are opened with Android `ACTION_VIEW`, so VLC, mpv, or another installed external player handles playback.
