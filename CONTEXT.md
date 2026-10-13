# wstunnel-android

wstunnel 的 Android 客户端，两个 APK 形态：完整 VPN 版（app，基于 sockstun）与纯 SOCKS5 服务版（client）。

## Language

### 产品形态

**完整 VPN 版（app）**：
应用 `cn.bujian.wstunnel`，以 sockstun 为基底的全设备 VPN 客户端，显示名 "wstunnel VPN"。
_Avoid_: 主版本、旧版

**client 版**：
应用 `cn.bujian.wstunnel.client`，只跑 wstunnel 子进程并暴露本地 SOCKS5 端口、不做 VPN 的轻量 APK，显示名 "wstunnel Client"。VPN 层交给独立的 sockstun 类应用。
_Avoid_: 精简版、无 VPN 版

### 隧道链路

**Profile**：
sockstun 的一组代理配置单元（最多 13 个），决定 VPN 下的 SOCKS5 出口目标、DNS、按 App 分流等；配置键以 `P<index>.` 为作用域前缀。
_Avoid_: 节点、服务器配置（Profile 不等于 wstunnel 服务器）

**全局 wstunnel 配置**：
wstunnel 子进程的唯一一份参数（服务器地址/SNI/本地端口/连接池/认证），所有 Profile 共用；服务器地址留空表示禁用。
_Avoid_: 全局 Profile、wstunnel Profile

**wstunnel（子进程）**：
内嵌的 wstunnel v9.7.4 client 可执行文件，作为全局单实例运行，监听本地 SOCKS5 端口供 tun2socks 连接。
_Avoid_: 隧道 A、wstunnel 实例（不区分实例）

**上游（upstream）**：
tun2socks（TProxy）配置中的 SOCKS5 出口目标。启用 wstunnel 时上游指向 `127.0.0.1:<全局 wstunnel 端口>`；禁用时即 Profile 自身的 SOCKS5 地址/端口。
_Avoid_: 代理服务器（含糊：可能指 wstunnel server 或 Profile socks）

**wstunnel server**：
远端 wstunnel 服务端（如 `https://host:29889`），是 wstunnel 子进程的连接目标，不参与 Profile 选择。
_Avoid_: 服务器（不加限定词的"服务器"一律指代不清）

### 进程与日志

**VPN 服务（TProxyService）**：
运行在 `:native` 独立进程的 VpnService，负责建立 tun、拼装 tproxy 配置、拉起/停止 wstunnel 子进程。
_Avoid_: 主服务

**文件日志**：
`filesDir/wstunnel.log`，app 主进程与 `:native` 进程共享的唯一日志通道。
_Avoid_: LogBuffer 内存缓冲（已废弃的实现说法）
