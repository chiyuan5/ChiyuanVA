# WebView 分身修复说明

本补丁主要修复了容器分身场景下 WebView 的目录隔离和错误代理问题：

1. 移除了 `HookManager` 中未真正生效且返回值语义错误的 WebView 代理注册：
   - `WebViewProxy`
   - `WebViewFactoryProxy`
   - `IWebViewUpdateServiceProxy`
2. 新增 `WebViewEnv`，统一负责：
   - Android 9+ 的 `WebView.setDataDirectorySuffix()`
   - guest WebView 目录创建
3. 新增 `GuestAppContext`，补齐 guest app 的：
   - `getApplicationInfo()`
   - `getDataDir()/getFilesDir()/getCacheDir()/getCodeCacheDir()`
   - `getDir()/getDatabasePath()`
4. 在 `BActivityThread` 中：
   - 更早调用 `WebViewEnv.prepare(...)`
   - 同步 `LoadedApk` 的数据目录字段
   - `createPackageContext()` 统一返回 guest 语义 Context
5. 在 `IOCore` 中增加 WebView 目录重定向，兼容 Android P 以下场景。
6. 把 `SocialMediaAppCrashPrevention.initialize()` 从类加载期移动到 `doAttachBaseContext()` 之后，避免上下文未准备好时提前初始化。
