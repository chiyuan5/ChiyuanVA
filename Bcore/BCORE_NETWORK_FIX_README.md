# Bcore 网络兼容修复说明

这份压缩包主要做了两类调整，目标是减少分身环境里 WebView / SDK 误判“无网络”的情况。

## 已修改内容

### 1. `IConnectivityManagerProxy.java`
改成了“宿主真实网络优先”的实现：
- 优先透传宿主 `ConnectivityManager` 的真实 `Network / NetworkInfo / NetworkCapabilities / LinkProperties`
- 只有宿主拿不到时才做兜底构造
- `registerNetworkCallback` / `registerDefaultNetworkCallback` / `requestNetwork` 注册成功或失败后，都会主动补一次：
  - `onAvailable()`
  - `onCapabilitiesChanged()`
  - `onLinkPropertiesChanged()`
- 不再默认强制关闭 Private DNS，而是优先透传宿主状态
- DNS 服务器列表优先返回宿主网络的 `LinkProperties.getDnsServers()`

### 2. `HookManager.java`
默认移除了 `IDnsResolverProxy` 的注入。

原因：之前 `IDnsResolverProxy` 在解析失败时会返回 `8.8.8.8 / 8.8.4.4` 这样的 DNS 服务器地址列表，这和“域名解析结果”的语义不一致，容易让 WebView 的 JS/CSS/CDN 子资源请求出现异常。

## 为什么这样改

很多 WebView 壳或 SDK 不只看 `getActiveNetworkInfo()`，还会看：
- `getActiveNetwork()`
- `getNetworkCapabilities()`
- `getLinkProperties()`
- `registerDefaultNetworkCallback()` 是否真正收到在线回调

旧实现里虽然伪造了“看起来有网”的对象，但回调链不完整，而且 DNS 伪造太激进，容易出现：
- 主页面能打开
- 某个 JS/CSS 子资源失败
- SDK 最终显示“无网络”

## 仍建议你在业务层一起处理的点

如果目标 App 里的 WebView 封装会把 **子资源失败也当整页失败**，那只修分身库还不一定 100% 够。

像你前面贴的 `KRWebView` 这种逻辑，最好同步改成：
- 只有 `request.isForMainFrame()` 失败时才 `showFailed()`
- `.js/.css` 子资源失败先记录日志，不要直接弹“无网络”

## 建议验证方式

1. 打开分身后的目标页面
2. 观察 `IConnectivityManagerProxy` 日志，确认：
   - `registerDefaultNetworkCallback` / `registerNetworkCallback` 有被调用
   - 回调补发成功
3. 观察 WebView 日志，确认失败的是主文档还是子资源
4. 如果主文档已正常、只剩子资源失败，再处理业务层 WebView 的错误判定
