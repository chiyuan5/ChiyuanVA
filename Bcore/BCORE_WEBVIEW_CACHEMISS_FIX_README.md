Bcore WebView Cache-Miss Fix

This patch focuses on the new root cause seen in runtime logs:
- KRWebView receives `net::ERR_CACHE_MISS`
- process is `com.kurogame.mingchao:WebView`
- network state itself is still WIFI

Changes in this package:
1. Added `WebViewProcessFix`
   - installs a stable per-user / per-package / per-process WebView storage directory
   - sets `webview.data.dir`, `webview.cache.dir`, `webview.cookies.dir`, `webview.database.path`
   - calls `WebView.setDataDirectorySuffix(...)` with a stable suffix (no PID)
   - warms `CookieManager` after the data dir is fixed

2. `BActivityThread.bindApplication(...)`
   - now installs `WebViewProcessFix` before app WebView usage

3. `SocialMediaAppCrashPrevention`
   - now reuses the same stable WebView environment helper instead of ad-hoc properties

4. `HookManager`
   - removes `IDnsResolverProxy` injection to avoid DNS semantic corruption

5. `IWebViewUpdateServiceProxy`
   - no longer forces `isMultiProcessEnabled()` to false; it now prefers the real system result

6. `WebViewProxy`
   - changed PID-based WebView paths to stable package-based paths to avoid cache churn after process recreation

Important:
- This patch reduces WebView storage/cache instability from the Bcore side.
- If KRWebView still treats `ERR_CACHE_MISS` as a fatal page error, the remaining direct fix is still inside the target app's `KRWebView` error handling.
