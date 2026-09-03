# Tectonic 1.20.1 Forge 非官方社区回移版

> **非官方构建。** 本仓库不是 Apollo 或 Tectonic 上游维护者发布、认可或支持的版本。

本分支以官方 Tectonic 3.0.17 源码为基线，选择性吸收后续版本中可安全适配到
Minecraft 1.20.1 Forge 的变化。它保留 `modId = tectonic` 以维持世界与资源兼容，
但版本号、显示名、源码地址和制品名均明确标注为社区回移。

## 当前状态

- 目标：Minecraft 1.20.1、Forge 47.4.x、Lithostitched 1.4.11 及以上兼容版本。
- 运行与发布字节码：Java 21，项目类必须为 class major 65；不提供 Java 17 字节码。
- 当前版本：`3.0.17-backport.1`。
- 发布状态：候选构建；固定输入的严格区块确定性仍未通过，禁止标记 Stable。
- 存档建议：只在副本上测试，切勿拿唯一存档直接开冲。

构建候选包：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat --no-daemon clean assembleForge1201CommunityCandidate --console=plain
```

候选制品写入
`build/libs/tectonic-community-backport-1.20.1-forge-3.0.17-backport.1.jar`，
旁边生成 SHA-256 sidecar。该任务只生成本地候选包，不执行网络发布。

开始使用前请阅读：

- `BACKPORT_STATUS.md`
- `KNOWN_ISSUES.md`
- `docs/WORLD_UPGRADE_GUIDE.md`
- `docs/CONFIG_MIGRATION.md`
- `COMPATIBILITY_REPORT.md`

上游官方项目：https://github.com/Apollounknowndev/tectonic

社区源码与问题反馈：https://github.com/tORHANSxd/tectonic
