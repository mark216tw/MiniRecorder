# MiniRecorder（mini錄音機）

MiniRecorder 是一款以 Kotlin 與 Jetpack Compose 打造的輕量 Android 錄音應用程式。
主打「快速開始錄音、清楚整理內容」：提供前景服務穩定錄音、即時聲波視覺化、
標記要點與時間軸跳轉、標籤分類、匯入外部音樂檔，以及可自訂的主題色彩。

> 本專案採用 **MIT License** 授權。

---

## 專案特色

- **穩定錄音**：以 `ForegroundService` 於背景持續錄音，支援暫停 / 繼續與螢幕休眠時持續運作（WakeLock）。
- **音訊處理**：以 `AudioRecord` + `MediaCodec`（AAC-LC）+ `MediaMuxer` 直接輸出 `.m4a`（MP4 容器）。
- **平台音效**：可啟用降噪（NoiseSuppressor）、回音消除（AcousticEchoCanceler）與自動增益控制（AGC）。
- **靜音跳過**：可設定靜音門檻與最短靜音長度，於錄音時自動略過靜音片段。
- **即時視覺化**：錄音畫面提供即時聲波（RMS）視覺化與等寬計時器。
- **標記要點**：錄音中可即時加入時間戳標記，播放時可於時間軸上看到標記並快速跳轉。
- **標籤分類**：支援標籤新增 / 重新命名 / 上色 / 刪除，並可於清單以標籤過濾。
- **搜尋與排序**：依標題搜尋，並可依時間、長度、標題排序。
- **播放控制**：0.5x～2.0x 倍速、前後 10 秒快轉、前後要點跳轉、分享與刪除。
- **快速入口**：支援快速設定磚（QS Tile）、桌面小工具（Glance AppWidget）與捷徑意圖快速錄音。
- **自訂外觀**：淺色 / 深色 / 跟隨系統，並可透過色相滑桿自訂整體主題色。
- **最長錄音與提醒**：可設定 30～180 分鐘上限，並在前 10 / 5 / 3 分鐘發出語音與震動提醒。

---

## 系統需求

| 項目 | 需求 |
| --- | --- |
| 作業系統 | Android 8.0（API 26）以上 |
| 目標 SDK | Android 15（API 35） |
| 硬體 | 需具備麥克風 |
| 建置環境 | JDK 17、Android SDK（compileSdk 35） |

---

## 開發環境與建置

本專案使用 Gradle（Kotlin DSL）與 Version Catalog 管理相依套件。

```powershell
# 建置 Debug 版本
.\gradlew :app:assembleDebug

# 建置測試發行版本（啟用 R8 壓縮、以 Debug 金鑰簽署）
.\gradlew :app:assemblePrerelease

# 執行單元測試（若有）
.\gradlew test
```

建置產物位置：

- Debug：`app/build/outputs/apk/debug/app-debug.apk`
- 測試發行版：`app/build/outputs/apk/prerelease/app-prerelease.apk`

> 請於 `local.properties` 設定 `sdk.dir` 指向本機 Android SDK 路徑。

---

## 文件導覽

| 文件 | 說明 |
| --- | --- |
| [docs/使用指南.md](docs/使用指南.md) | 安裝、權限、各功能操作與常見問題 |
| [docs/系統架構與技術文件.md](docs/系統架構與技術文件.md) | 技術棧、模組分層、資料流、音訊管線、建置設定 |
| [docs/系統設計文件.md](docs/系統設計文件.md) | 需求、資料模型、狀態機、循序圖、設計決策 |
| [CHANGELOG.md](CHANGELOG.md) | 版本變更紀錄 |
| [LICENSE](LICENSE) | MIT 授權條款 |

---

## 授權

本專案以 [MIT License](LICENSE) 釋出。

Copyright (c) 2026 mark216tw
