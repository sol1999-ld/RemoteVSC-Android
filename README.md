# RemoteVSC - 移动端 SSH VS Code

## 项目描述
RemoteVSC 是一个实验性的 Android 应用程序，旨在通过利用 SSH 和远程 `vscode-server` 实例，将类似 Visual Studio Code 的开发体验带到移动设备上。此应用程序允许用户连接到远程服务器，管理 `vscode-server` 的安装和启动，设置 SSH 端口转发，然后直接在应用程序的 WebView 中与完整的 VS Code 界面进行交互。

## 功能特性
*   **SSH 连接管理**：使用 SSH 连接到远程服务器。
*   **远程 `vscode-server` 部署**：在远程主机上自动下载并解压 VS Code Server。
*   **VS Code Server 启动**：在远程机器上启动并管理 `vscode-server` 进程。
*   **SSH 端口转发**：设置本地端口转发以访问远程 `vscode-server` 实例。
*   **集成 WebView**：在 Android 应用程序中显示完整的 VS Code 界面。

## 如何使用
1.  **构建并安装**：使用 Android Studio 构建 Android 应用程序并将其安装到您的 Android 设备上。
2.  **输入 SSH 详细信息**：在应用程序的主屏幕上，输入您的远程服务器的主机、端口、用户名和密码。
3.  **执行命令（可选）**：您可以使用“执行 SSH 命令”按钮来测试基本的 SSH 连接并运行任意命令。
4.  **设置 VS Code Server 并连接**：点击“设置 VS Code Server 并连接”按钮。应用程序将执行以下步骤：
    *   通过 SSH 连接到您的远程服务器。
    *   将 `vscode-server`（目前使用硬编码的提交 ID）下载并解压到远程服务器上的临时目录（`/tmp/vscode-server-mobile`）。
    *   在随机可用端口上启动 `vscode-server`。
    *   设置从本地端口（例如 8080）到远程 `vscode-server` 端口的本地端口转发。
    *   一旦端口转发建立，将出现一个 WebView，加载 `http://localhost:8080`，该页面应显示 VS Code 界面。

## 设置
### 远程服务器要求
*   可通过 SSH 访问的远程服务器。
*   已安装 `wget` 或 `curl` 用于下载文件。
*   已安装 `tar` 用于解压压缩文件。
*   基本的 Linux 环境。

### `vscode-server` 提交 ID
目前，应用程序使用硬编码的 `commit_id` 进行 `vscode-server` 下载。此 ID 需要定期更新，以确保与最新 VS Code 版本的兼容性。您可以通过在桌面 VS Code 实例连接到远程主机时检查 VS Code Remote - SSH 扩展日志来查找最新的稳定版 `commit_id`。

## 贡献
欢迎贡献！如果您想贡献，请 Fork 仓库并提交 Pull Request。对于重大更改，请先提出 Issue 进行讨论。

## 许可证
本项目采用 MIT 许可证授权 - 有关详细信息，请参阅 [LICENSE](LICENSE) 文件。
