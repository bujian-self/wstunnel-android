# 第三方开源组件声明

本项目（wstunnel-android）包含以下第三方开源软件。各组件的版权归其原作者所有，
并分别遵循下列许可证。本项目自身代码的许可证见 [LICENSE](LICENSE)。

App 内的「开源许可」页面展示同一份声明，随各模块实际包含的组件裁剪：

- `app`（完整 VPN 版）：`app/src/main/res/raw/third_party_licenses.txt`
- `client`（纯 SOCKS5 服务版）：`client/src/main/res/raw/third_party_licenses.txt`，
  仅包含 wstunnel 与 Rust 生态组件，不含 hev-socks5-tunnel、lwip。

## 组件清单

| 组件 | 版本/来源 | 许可证 | 使用方式 |
| --- | --- | --- | --- |
| wstunnel | v9.7.4，https://github.com/erebe/wstunnel | BSD-3-Clause | 预编译可执行文件，打包为 `jniLibs/*/libwstunnel.so` |
| hev-socks5-tunnel | https://github.com/heiher/hev-socks5-tunnel | MIT | 源码，编译为 `libhev-socks5-tunnel.so` |
| hev-socks5-core | https://github.com/heiher/hev-socks5-core | MIT | 源码（`src/core`） |
| hev-task-system | https://github.com/heiher/hev-task-system | MIT | 源码（`third-part/hev-task-system`） |
| yaml (libyaml fork) | https://github.com/heiher/yaml | MIT | 源码（`third-part/yaml`） |
| lwip | https://github.com/heiher/lwip | BSD-3-Clause | 源码（`third-part/lwip`） |
| Rust 生态组件 | tokio、hyper、rustls 等 | MIT / Apache-2.0 / ISC | 静态链接进 wstunnel 可执行文件 |

说明：

- `wintun` 仅用于 wstunnel 的 Windows 版本，本项目的 Android 构建未包含该组件。
- wstunnel 可执行文件内的 Rust 依赖清单以其对应版本源码的 `cargo metadata` 输出为准；
  上表列出的是已在二进制中识别出的主要组件。

---

## wstunnel — BSD 3-Clause License

Copyright (c) 2016-2024, Erèbe - Romain Gerard

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

## hev-socks5-tunnel、hev-socks5-core、hev-task-system、yaml — MIT License

Copyright (c) 2022 hev

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## lwip — BSD 3-Clause License

Copyright (c) 2001, 2002 Swedish Institute of Computer Science.
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.
3. The name of the author may not be used to endorse or promote products
   derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
OF SUCH DAMAGE.

## Rust 生态组件 — MIT / Apache-2.0 / ISC

wstunnel 可执行文件由 Rust 编写，并以静态链接的方式包含 tokio、hyper、rustls
等开源组件。这些组件分别以 MIT、Apache-2.0 或 ISC 许可证发布，上述 MIT 许可证
文本同样适用于其中的 MIT 组件。如需完整的依赖树及其版权行，请在 wstunnel
对应版本（v9.7.4）源码目录执行：

```
cargo metadata --format-version 1
```

---

## 修改说明

本项目对第三方组件的使用方式：

- wstunnel 以预编译二进制形式原样打包，未修改其代码，仅重命名为
  `libwstunnel.so` 以便 Android 在安装时解压并赋予可执行权限。
- hev-socks5-tunnel 及其子模块以源码形式编译，并按本项目需要调整了
  Android 构建脚本（`APP_CFLAGS`、包名等），未修改其版权声明。
