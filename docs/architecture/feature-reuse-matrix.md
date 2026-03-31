# Feature Reuse Matrix (Phase 0 Draft)

| Feature | Source Modules | Android Coupling | Reuse Decision | Priority | Notes |
|---|---|---|---|---|---|
| Steam auth/session | `service/SteamService.kt`, `api/` | High | Refactor | P0 | Extract `SteamSessionManager` first |
| Steam library sync | `service/SteamService.kt`, `data/SteamApp.kt` | High | Refactor | P0 | Move protocol logic to `core/store-steam` |
| Steam download/install | `service/SteamService.kt`, `service/DownloadService.kt`, `data/DownloadInfo.kt` | High | Refactor | P0 | Unify under portable download manager |
| Steam cloud saves | `service/SteamAutoCloud.kt` | Medium | Refactor | P1 | Replace Android I/O wrappers |
| Game launch orchestration | `service/SteamService.kt`, `gamefixes/` | High | Refactor | P0 | Split launch planning from service lifecycle |
| Game fixes registry | `gamefixes/` | Low | Reuse | P0 | Copy with zero logic changes |
| Preferences/settings | `PrefManager.kt` | Medium | Refactor | P0 | DataStore to file-backed config service |
| Database layer | `db/`, `app/schemas/` | Medium | Refactor | P0 | Room to SQLDelight migration scripts |
| UI shell/navigation | `ui/PluviaMain.kt`, `ui/screen/`, `ui/component/` | High | Replace | P0 | Compose Desktop app shell |
| Android services | `service/*` | High | Replace/Refactor | P0 | Remove Android lifecycle assumptions |
| External display bridge | `externaldisplay/` | High | Replace | P0 | Use host X11/Wayland/XWayland stack |
| Notifications | `service/NotificationHelper.kt` | High | Replace | P1 | D-Bus/libnotify backend |
| Network monitor | `NetworkMonitor.kt` | High | Replace | P1 | Linux network state backend |
| Achievements polling | `service/AchievementWatcher.kt` | Low | Reuse | P2 | Portable logic |
| Analytics | `statsgen/` | Medium | Replace | P1 | PostHog JVM SDK |
| Crypto utilities | `Crypto.kt` | Low | Reuse with swap | P0 | Spongycastle to BouncyCastle |
| GOG integration | `service/gog/`, `data/GOGGame.kt` | High | Replace service | P1 | Keep models, rewrite host logic |
| Epic integration | `service/epic/`, `data/EpicGame.kt` | High | Replace service | P1 | Keep models, rewrite host logic |
| Amazon integration | `service/amazon/`, `data/AmazonGame.kt` | High | Replace service | P2 | Keep models, rewrite host logic |

## Verification Notes

- Android import audit completed for `app/src/main/java/app/gamenative/data`: `0` matches for `import android.`.
- Next matrix update trigger: after `SteamService.kt` section-level extraction map and launch trace capture.
