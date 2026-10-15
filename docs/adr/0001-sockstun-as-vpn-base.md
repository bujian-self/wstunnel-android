# 以 sockstun 为完整 VPN 版的代码基底

原有自研 Kotlin VPN 实现（单 Profile、自写 UI）功能面窄，决定用 heiher/sockstun（hev-socks5-tunnel 官方客户端）的源码整体替换 `app/`，把 wstunnel 作为其上游组件集成进去，而不是在现有实现上逐个补齐多 Profile、按 App 分流、QS 磁贴等能力。

为什么是这个方向：

- **成熟度**：sockstun 的 Profile 管理、按 App 分流、开机自启、QS 磁贴是经过多年 F-Droid 发布验证的；自研代码这些都是零。
- **构建一致性**：连同它的构建链一起采用（Gradle 8.8 wrapper + AGP 8.4.1 + hev submodule 的 ndk-build），代价是全项目从 KTS/AGP 9 降级并显式引入 kotlin-android 插件（`client/` 模块的构建脚本随之降级改写）。
- **JNI 约束**：hev 的 native 方法签名由 `PKGNAME=hev/sockstun` 宏生成，**Java 包名不可改**；因此 `namespace` 保留 `hev.sockstun`，应用身份只体现在 `applicationId = cn.bujian.wstunnel`。
- **wstunnel 形态不变**：继续是 jniLibs 里的可执行 so + 子进程（绕三星 exec 位问题）。进程同一时刻只有一个，但**参数挂 Profile**（原 Q8b"挂全局"决策已被撤回）：切到参数不同的 Profile 时重启 wstunnel；Profile 未配置 wstunnel 时该实例不运行，上游回落 Profile 自身的 SOCKS5。

被否的替代方案：在原 Kotlin 实现上逐项移植 sockstun 能力（工作量大且最终仍是二等实现）；把 wstunnel JNI 深度融合进 hev 进程（Rust 静态库不可行）。
