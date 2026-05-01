# StreamScout

StreamScout is a lightweight Android streaming aggregator prototype built without a backend server. It uses local, folder-based plugins to search media sources, cache structured JSON responses, and hand stream URLs to external Android players.

## Features

- Native dark Android UI
- Movies and TV Shows sections
- Cross-plugin search
- Detail screen with poster, description, and stream links
- Folder-based plugin loader
- Offline-first JSON cache for previous searches and details
- External playback through Android intents
- No heavyweight app dependencies

## Build

```sh
gradle :app:assembleDebug
```

The project intentionally has no backend service. All scraping/search requests run on-device from loaded plugins.

## Plugins

See [PLUGINS.md](PLUGINS.md). A working Internet Archive example plugin is included at `app/src/main/assets/plugins/internet_archive/plugin.json`.
