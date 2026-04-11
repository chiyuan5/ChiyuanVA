# WebView Fix Notes v5

This package includes the v5 WebView isolation fix:

- Keep `WebView.setDataDirectorySuffix(...)` as the only WebView isolation mechanism on Android 9+
- Skip `app_webview` and `cache/WebView` IO redirection on Android 9+ to avoid Chromium state/cache mismatch
- Clear legacy unsuffixed WebView state once per process on Android 9+
- Keep legacy `app_webview` / `cache/WebView` redirects only for Android 8.x and below

Why this change matters:

Android 9+ WebView already isolates per-app data using the suffix API. Applying additional file-level redirection for `app_webview` and `cache/WebView` can make Chromium believe its state lives in one place while the filesystem redirects it to another, which often manifests as repeated `net::ERR_CACHE_MISS` errors.
