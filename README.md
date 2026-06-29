<p align="center"><img src="screenshots/icon.png" width="96" height="96" alt="DTV Mobile" /></p>

<h3 align="center">DTV Mobile</h3>

<p align="center">
  抖音、B站、斗鱼、虎牙轻量化安卓客户端（非官方）
</p>

> **基于 [chen-zeong/dtv](https://github.com/chen-zeong/dtv) 二次开发**，在原版基础上新增了以下功能。桌面端请前往原项目[仓库](https://github.com/chen-zeong/dtv)。

---

## 新增特性（v0.1.3）

- **播放页优化**：进入直播间后隐藏底部 Tab；播放器快捷按钮 3 秒自动隐藏，点击视频重新显示
- **自动发布**：GitHub Actions 支持按 tag 构建 Release，并自动同步 App 版本号

## 历史特性（v0.1.2）

- **抖音链接解析**：搜索框支持直接粘贴抖音直播间链接（短链接 / 直播页面链接），自动提取房间号
- **音频模式**：播放界面点击耳机图标进入，展示仿网易云音乐 CD 旋转封面，支持播放/暂停
- **通知栏控制**：音频模式自动注册前台通知服务，通知栏显示主播头像和控制按钮，锁屏可见
- **定时关闭**：支持预设时间（15/30/45/60/90/120 分钟）和自定义输入，到期自动暂停
- **画中画**：视频播放时按 Home 键自动进入 PiP 模式，纯视频无多余控件
- **省电优化**：音频模式允许熄屏，通知服务保证后台播放

## 基础功能

- 支持平台：斗鱼 / 虎牙 / 抖音 / B站（直播）
- 分区浏览：按平台分类浏览直播列表，支持订阅常用分区（快速入口）
- 关注管理：一键关注/取消关注；首页支持置顶与长按拖拽排序
- 搜索：按平台搜索主播/直播间；B站支持登录/退出
- 播放：基于 Android Media3（ExoPlayer）播放；全屏/横竖屏适配；清晰度/线路选择
- 弹幕：实时弹幕展示；关键词屏蔽；字号/透明度/显示区域可调
- 同步：局域网共享/导入（mDNS 发现、手动输入、扫码导入），增量同步关注、分区订阅、屏蔽词
- 主题：浅色 / 深色 / 跟随系统

## 发布 Release

仓库内置 GitHub Actions 发布流程：推送 `v*` tag 时会自动构建 APK、生成 GitHub Release，并上传 APK 与 `SHA256SUMS.txt` 校验文件；同时也会在 Actions 运行页的 Artifacts 区域提供 APK 下载。Release 正文会自动收集上一个 tag 到当前 tag 之间的 commit subject，并生成固定格式的 `### 主要更新内容：` 列表；同时会把 tag 同步写入 APK 的 `versionName` / `versionCode`。

```bash
git tag v0.1.3
git push origin v0.1.3
```

也可以在 GitHub Actions 页面手动运行 `Release Android APK`，填写 `tag_name`（例如 `v0.1.3`）后生成对应 Release。

如需产出正式可安装的 release APK，请在仓库 Settings -> Secrets and variables -> Actions 中配置以下 Secrets：

- `ANDROID_KEYSTORE_BASE64`：release keystore 文件的 Base64 内容
- `ANDROID_KEYSTORE_PASSWORD`：keystore 密码
- `ANDROID_KEY_ALIAS`：签名 key alias
- `ANDROID_KEY_PASSWORD`：签名 key 密码

未配置完整签名 Secrets 时，Actions 会退回生成 debug APK 作为 Release Asset，方便测试下载流程。

## 说明

- 本项目仅用于学习与技术交流
- 播放内容与相关数据来自第三方平台接口，版权归属于第三方，可能随平台变更而失效

## 截图

| 首页                                                       | 播放                                                         | 分区（斗鱼）                                                    |
| -------------------------------------------------------- | ---------------------------------------------------------- | --------------------------------------------------------- |
| <img src="screenshots/home.jpeg" width="240" alt="首页" /> | <img src="screenshots/player.jpeg" width="240" alt="播放" /> | <img src="screenshots/douyu.jpeg" width="240" alt="斗鱼" /> |

| 共享/导入                                                     | B站                                                    |
| --------------------------------------------------------- | ----------------------------------------------------- |
| <img src="screenshots/share.jpeg" width="240" alt="共享" /> | <img src="screenshots/b.jpeg" width="240" alt="B站" /> |
