# Forge 1.20.1 构建任务

本文件记录 2026-09-03 在根项目 `tectonic` 上由 `gradlew projects` 与
`gradlew tasks --all` 实际枚举出的任务。仓库没有 Gradle 子项目；Cloche 通过虚拟源集生成各版本任务。

## Java 约束

所有命令均以以下环境启动 Gradle：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

项目的 Java toolchain、`JavaCompile.options.release`、Kotlin `jvmTarget` 以及客户端/服务端运行任务均固定为 Java 21。Minecraft-Codev 根据 Minecraft 1.20.1 官方元数据运行内部补丁工具时仍会申请 Java 17；该工具链由 Foojay 自动解析，只处理 Minecraft/Forge 中间产物，不参与 Tectonic 源码编译。最终 JAR 必须另行扫描，确保 Tectonic 类全部为 class major 65。

## 已确认任务

| 任务 | 用途 | 主要输出 |
|---|---|---|
| `forge1201IncludeJar` | 生成 Cloche 最终 Forge 1.20.1 JarJar 制品；测试读取这一层 | `build/libs/tectonic-3.0.17-backport.1-forge-1.20.1.jar` |
| `forge1201RemapJar` | 生成重映射中间 JAR，不作为最终发布输入 | `build/libs/intermediates/tectonic-3.0.17-backport.1-forge-1.20.1.jar` |
| `forge1201Jar` | 生成重映射前的开发 JAR | `build/libs/intermediates/tectonic-3.0.17-backport.1-forge-1.20.1-dev.jar` |
| `forge1201Classes` | 编译 Forge 1.20.1 的 common、shared 1.20.1 与 forge 源集 | `build/classes`、`build/resources` |
| `compileForge1201Java` | 仅执行 Forge 1.20.1 Java 编译链 | `build/classes/java/forge1201` |
| `forge1201UnitTest` | 运行仅绑定 Forge 1.20.1 classpath 的 JUnit 5 回归测试 | `build/test-results/forge1201UnitTest`、`build/reports/tests/forge1201UnitTest` |
| `check` | 运行本分支支持目标的验证入口；当前委托给 `forge1201UnitTest` | 同上 |
| `runForge1201Client` | 启动 Forge 1.20.1 开发客户端 | `run/` |
| `runForge1201Server` | 启动 Forge 1.20.1 开发专服 | `run/` |
| `javaToolchains` | 审计 Gradle 实际发现/下载的 JDK | 控制台报告 |
| `outgoingVariants` | 审计发布 variant 的 JVM 版本属性 | 控制台报告 |
| `copyForge1201CommunityJar` | 将最终 IncludeJar 复制成明确的社区候选名 | `build/libs/tectonic-community-backport-1.20.1-forge-3.0.17-backport.1.jar` |
| `checksumForge1201CommunityJar` | 为社区候选包生成 SHA-256 sidecar | 同目录 `.jar.sha256` |
| `assembleForge1201CommunityCandidate` | 聚合最终 JAR、Forge 1.20.1 测试、社区命名与校验和 | 上述候选 JAR 与 sidecar |

## 标准命令

```powershell
.\gradlew.bat --stop
.\gradlew.bat --no-daemon --no-build-cache clean assembleForge1201CommunityCandidate --stacktrace --warning-mode all --console=plain
.\gradlew.bat --no-daemon check --console=plain
.\gradlew.bat --no-daemon runForge1201Server --console=plain
.\gradlew.bat --no-daemon runForge1201Client --console=plain
```

不要用不带目标的 `build` 代替 Forge 1.20.1 验收；该任务可能同时构建 Fabric、NeoForge 和其他 Minecraft 版本，既放大噪声，也不能证明 Forge 1.20.1 的独立发布链正确。

Cloche 0.13.6 的根 `test` source set 会把全部 loader/版本的运行时 classpath 合并解析。社区回移分支因此使用独立的 `forge1201UnitTest` source set，并让 `check` 只执行这一受支持目标，避免测试配置反过来要求生成无关 Fabric/NeoForge 游戏文件。

最终 JAR 嵌入社区身份、Git 提交、dirty 状态、上游基线、`LICENSE` 与 `NOTICE.md`。自动测试会扫描全部 Tectonic class，要求 class major 65。相同源码重复构建的整包哈希目前仍可能因 Minecraft-Codev/TinyRemapper 对 SRG 局部变量名的非确定选择而变化；归档时间戳、文件顺序和仓库换行已经固定，但这里不把“尽可能可复现”冒充“已做到位级可复现”。
