# DTV 桌面端 -> 移动端迁移说明

## 目录结构

- `DTV-heroui/`：桌面端（Tauri + Rust 后端 + Web UI）
- `shared/`：Compose Multiplatform 共享 UI/逻辑（目前 Android 先跑通）
- `androidApp/`：Android 应用入口（`MainActivity`）

## UI 设计继承点（来自桌面端）

桌面端的设计 token 在 `DTV-heroui/web/src/app/legacy-global.css`（vibestream-day/night）。
移动端已按同一套主色（绿色 `--accent`）与浅/深色背景、描边色进行映射：

- 主题：`shared/src/commonMain/kotlin/dtv/mobile/theme/DtvTheme.kt`
- 网格背景：`shared/src/commonMain/kotlin/dtv/mobile/ui/components/DtvBackground.kt`
- 底部平台栏：`shared/src/commonMain/kotlin/dtv/mobile/ui/PlatformBottomBar.kt`

## Rust 后端迁移策略（Kotlin Native Backend）

桌面端 Rust 的入口命令列表在 `DTV-heroui/src-tauri/src/main.rs` 的 `generate_handler![...]`。

移动端不会通过 JNI/桥接调用 Rust；建议按“可跑通播放链路”为主线分阶段迁移：

1. **拉流 URL**：`get_stream_url_cmd` / `get_stream_url_with_quality_cmd`
2. **平台首页列表**：Douyu/Huya/Douyin/Bilibili 的 live list
3. **房间详情/主播信息**：room info / streamer info
4. **弹幕监听**：把桌面端的 listener 状态机迁移为 Kotlin 协程 + Flow
5. **代理**（如仍需要）：移动端改为 Android 本地代理实现（前台服务 + 通知）

目前仅建立了 Kotlin 接口骨架，方便 UI 与后端解耦并逐步替换：

- `shared/src/commonMain/kotlin/dtv/mobile/backend/DtvBackend.kt`

