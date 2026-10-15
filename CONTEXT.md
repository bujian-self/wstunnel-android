# wstunnel-android

wstunnel 的 Android 客户端，两个 APK 形态：完整 VPN 版（app，基于 sockstun）与纯 SOCKS5 服务版（client）。

## Language

### 产品形态

**完整 VPN 版（app）**：
应用 `cn.bujian.wstunnel`，以 sockstun 为基底的全设备 VPN 客户端，显示名 "wstunnel VPN"。
_Avoid_: 主版本、旧版

**client 版**：
应用 `cn.bujian.wstunnel.client`，只跑 wstunnel 子进程并暴露本地 SOCKS5 端口、不做 VPN 的轻量 APK，显示名 "wstunnel Client"。**已冻结**：仓库保留 v0.1.0-beta 已发布内容，作为与主包互不混用的独立项目，不再参与构建发布。
_Avoid_: 精简版、无 VPN 版、双模式（主包不做纯 client 模式）

### 隧道链路

**Profile**：
sockstun 的一组配置单元（最多 13 个），决定 VPN 下的 wstunnel 上游参数、DNS、按 App 分流等；配置键以 `P<index>.` 为作用域前缀。
_Avoid_: 节点、服务器配置（Profile 不等于 wstunnel 服务器）

**wstunnel 配置**：
每个 Profile 单独一份，直接呈现在主界面输入框组中：地址（必填，wstunnel server URL）、SNI、本地 SOCKS5 端口、用户名、密码、线程池（括号内为可选，含默认值）；sockstun 与 wstunnel 子进程读取同一份内容。地址未填写时，保存与启动均被拒绝。
_Avoid_: 全局 wstunnel 配置（已废弃：不再全应用共用）、全局 Profile、wstunnel Profile、wstunnel 设置对话框（已废弃：配置不在弹窗里）

**wstunnel（子进程）**：
内嵌的 wstunnel v9.7.4 client 可执行文件，监听本地 SOCKS5 端口供 tun2socks 连接。进程同一时刻只有一个，参数取自当前激活的 Profile，参数变化时重启。
_Avoid_: 隧道 A、wstunnel 实例（不区分实例）

**上游（upstream）**：
tun2socks（TProxy）配置中的 SOCKS5 出口目标。**唯一来源是 wstunnel**：上游恒为 `127.0.0.1:<当前 Profile 的本地 SOCKS5 端口>`，不存在"回落到外部 SOCKS5"的第二来源，主界面也不再有 Profile 自填的 SOCKS5 字段族。
_Avoid_: 代理服务器（含糊：可能指 wstunnel server 或本地监听端口）、回落/直连（已废弃：无第二上游）

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
