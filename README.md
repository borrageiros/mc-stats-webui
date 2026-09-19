# ⛏️ mc-stats-webui

**Live competitive rankings for your Minecraft survival server.**

A **Fabric server mod** that embeds a small web UI + API. Players open it in a browser — no client mod needed.

![Ranking home screenshot](screenshots/screenshot.png)

## [MORE SCREENSHOTS](screenshots/README.md)

## ✨ Features

- 🏆 Home podium with **Champion**, **Most dedicated**, and **Most efficient** crowns
- 📋 Player profiles with skins, grades, and full vanilla stats
- 🆚 Side-by-side player compare
- 📊 Leaderboards for the whole vanilla stats catalog (custom, mined, killed, …)
- 🧮 Transparent scoring (“How we rank”) — farms inflate some boards on purpose
- 🌐 UI in **English / Español / Français** · light / dark / system theme
- 🏷️ Branding via `config/mc-stats-webui.json` (title, slogan, tagline, window title, bind, port)
- ⚡ In-memory stats cache (see below)

## 🧠 Stats cache

On world start the mod loads every player stats file into RAM. The API serves that snapshot — it does not re-scan the disk on each request.

When a connected player gains stats, their UUID is marked dirty. Every ~15 s (and before each API snapshot) dirty players are flushed with vanilla `save()`, then that one entry is reloaded into the cache. Offline players drop out of the dirty set and reload from disk if needed.

| URL | What |
| --- | --- |
| `http://localhost:25580/` | Web UI |
| `http://localhost:25580/api/players` | JSON API |

More detail: [endpoints](docs/endpoints.md) · [stats & champion](docs/stats-analysis.md)

## 🛠️ Development

**Needs:** Java 25 · Node 24+ · [Yarn via Corepack](https://yarnpkg.com/corepack) · Fabric 26.2

```bash
cd web
corepack yarn install
corepack yarn dev
```

UI: `http://localhost:5173` (proxies `/api` → `25580`)

```bash
./gradlew build
```

The SvelteKit UI is built static and shipped **inside the JAR**.
