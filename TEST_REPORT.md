# Tectonic 1.20.1 Forge 社区回移测试报告

本报告是随实现更新的证据账本。基线为上游提交 `5373b2084e461f83bd6e0b5f2fe943e81bd59700`，工作分支为 `backport/forge-1.20.1-v3-current`。

## 计划偏差：Java 21

用户明确覆盖原计划的 Java 17 字节码要求。当前验收标准改为：

- Gradle 主进程、Tectonic Java/Kotlin 编译器、开发客户端和开发专服使用指定的 Temurin 21；
- Tectonic 发布 JAR 内全部项目类必须为 class major 65；
- Minecraft-Codev 内部补丁任务可以使用其按 Minecraft 元数据申请的 Java 17 工具链，因为该步骤不编译 Tectonic 源码；
- Java 17 玩家将无法加载本社区版，这属于有意兼容边界，不能标记为原计划定义的 Stable。

## 环境

| 项目 | 值 |
|---|---|
| 日期/时区 | 2026-09-03，Asia/Shanghai |
| 操作系统 | Microsoft Windows 11 专业工作站版，10.0.26200（build 26200），amd64 |
| CPU | Intel Core i5-13600KF，环境可见 14 核/14 逻辑处理器 |
| 可见物理内存 | 68,507,058,176 bytes |
| JDK | Eclipse Temurin 21.0.12.1+1-LTS |
| JDK 路径 | `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` |
| Gradle | 8.14 Wrapper |
| Git | 2.55.0.windows.4 |
| Cloche | 0.13.6 |
| Forge 基线 | 47.4.0 |
| Lithostitched | `lithostitched-1.4.11-forge-1.20.jar` |
| Lithostitched SHA-256 | `c19a5a36c0e6cb3782cf7ca5b9648fb1bce5fc41fd737bed423a1f4971bccf75` |
| Gradle JVM 参数 | `-Xmx4G`；`org.gradle.parallel=true` |
| 测试模组 | Forge、Lithostitched、Tectonic；未启用 Chunky、Distant Horizons 或并行区块生成扩展 |

Gradle 还自动配置了 Eclipse Temurin 17.0.20.1+1，位置为
`C:\Users\tORHANS\.gradle\jdks\eclipse_adoptium-17-amd64-windows.2`，仅供 Minecraft-Codev 的 1.20.1 补丁步骤使用。

## Phase 0：取证与基线

| 编号 | 检查 | 结果 |
|---|---|---|
| P0-GIT-001 | 基线提交、分支与 tag | 通过：基线 tag `upstream-3.0.17-forge-1.20.1` 指向 `5373b208...` |
| P0-GIT-002 | GitHub 分支 | 通过：`origin/backport/forge-1.20.1-v3-current` |
| P0-GRADLE-001 | `gradlew projects` | 通过：单根项目 `tectonic` |
| P0-GRADLE-002 | `gradlew tasks --all` | 通过：真实任务记录于 `docs/BUILD_TASKS.md` |
| P0-FILES-001 | 基线文件清单 | 通过：`UPSTREAM_BASELINE_FILES.txt`，由 tag 内容生成，不受工作树修改影响 |

## Phase 1：官方 3.0.17 复现

### P1-BUILD-001：干净构建

命令：

```powershell
.\gradlew.bat --stop
.\gradlew.bat --no-daemon clean forge1201RemapJar --stacktrace --warning-mode all --console=plain
```

结果：通过，13 个任务执行，`BUILD SUCCESSFUL`。编译报告 4 个上游既有 removal 警告，没有编译错误、Mixin 应用错误或资源解析错误。完整日志保存在未提交的 `build/reports/backport/phase1-clean-build.log`。

### P1-JAR-001：官方制品对照

官方制品来自 Modrinth 版本 `KLmvRxwh`：

- 文件：`tectonic-3.0.17-forge-1.20.1.jar`
- 大小：322,575 bytes
- SHA-1：`07bfe13bc1bd75a6277f3d62bf92a549b33067a9`
- SHA-256：`c6de479b27cf090510de3f029918afc9fe17226981bd703e0002f7a25f4d8969`

本地 Java 21 基线构建样本：

- 文件：`build/libs/intermediates/tectonic-3.0.17-forge-1.20.1.jar`
- 大小：358,930 bytes
- SHA-256：`5b9a4bd89af67a0fe59179c392aceb92151c05ab17baa09cc44e715019802355`

对照结果：

- 双方均有 293 个文件条目，路径集合完全相同；
- 56 个类名完全相同；官方 56 个类均为 major 61，本地 56 个类均为 major 65；
- 44 个非 class 文件的字节差异全部由官方 LF 与 Windows 构建 CRLF 换行造成，统一换行后为 0 个差异；
- `mods.toml`、Mixin 列表、内置数据包路径和配置默认资源语义一致；
- ZIP 时间戳/顺序尚未规范化，因此此阶段不要求本地重复构建哈希相同。

### P1-SERVER-001：首次启动与 EULA

结果：通过。第一次启动正确生成 `run/eula.txt` 并在未接受 EULA 时退出；设置 `eula=true` 后再次启动，Forge 47.4.0 在 Temurin 21.0.12.1 上完成默认世界创建。

关键日志证据：

- 自动发现并启用 `tectonic/tectonic`；
- 自动发现并启用 `tectonic/tectonic/overlay.mod`；
- 自动发现并启用 `lithostitched/breaks_seed_parity`；
- 首次启动 `Done (20.920s)`；
- 未出现 Tectonic/Lithostitched 异常或服务端客户端类加载错误。

### P1-SERVER-002：重启与保存

结果：通过。第二次启动加载同一 `run/world`，在 Temurin 21 上于 `Done (16.846s)` 后接受控制台 `stop`，Overworld、Nether 与 End 均报告保存完成。

### P1-CLIENT-001：启动到主菜单资源完成态

结果：通过。Forge 客户端在 Temurin 21.0.12.1 上初始化 OpenGL 4.6、OpenAL、资源管理器与全部主要 texture atlas，未出现 Tectonic/Lithostitched 异常。测试随后主动终止客户端。

## 尚未完成的 Phase 1 项

- 客户端 GUI 自动创建、进入并重进世界；当前只有专服路径完成世界创建与重载；
- 五个固定种子的规范化区块快照；
- 客户端连接本地专服；
- 官方 JAR 与本地 Java 21 JAR 的固定种子世界生成行为对照。

这些项目不会被伪装成通过，将在世界生成测试工具完成后补齐。

## Phase 2：配置与线程安全硬化

### P2-CONFIG-001：Forge 1.20.1 单元测试通道

新增 `forge1201UnitTest`，仅使用 Forge 1.20.1 的编译/运行 classpath，并由 `check` 调用。Cloche 0.13.6 的根 `test` 任务会尝试解析所有 loader 和 Minecraft 版本，不适合作为此单目标社区分支的验证入口。

命令：

```powershell
.\gradlew.bat --no-daemon clean forge1201UnitTest forge1201RemapJar --stacktrace --warning-mode all --console=plain
.\gradlew.bat --no-daemon check --console=plain
```

结果：通过。共 13 个 JUnit 5 测试，0 failure、0 error、0 skipped；干净构建及 Forge 1.20.1 重映射 JAR 同时通过。报告位于未提交的 `build/test-results/forge1201UnitTest` 与 `build/reports/tests/forge1201UnitTest`。

覆盖行为：

- 配置及所有嵌套可变对象执行深复制，预设调用方每次拿到独立副本；
- 世界生成读取不可变、`volatile` 发布的 `ConfigSnapshot`；
- GUI 使用独立工作副本，Cancel/Back 不保存，选择预设也不再立即写盘；
- 世界已打开时，保存前显示确认页，并仅写入下次启动配置，不替换当前世界的活跃快照；
- 配置以 UTF-8 写入同目录临时文件，再原子替换；不支持原子移动的文件系统回退为替换移动；
- 覆盖有效文件前保留 `.bak`，语法或数值非法的输入保留为 `.invalid`；
- NaN、Infinity、越界数值与不符合 1.20.1 区块节约束的高度回退到字段默认值；
- 已规范化配置连续加载不会改变文件内容。

### P2-PACK-001：内置数据包注册幂等化

`TectonicRepositorySource` 统一按 Minecraft 1.20.1 `Pack#getId()` 注册内置数据包，重复 ID 采用 first-wins；注册与仓库发现通过同步区和不可变快照隔离，保留首次注册顺序。Forge/Fabric 1.20.1 入口不再直接修改公开静态列表。

回归测试覆盖同一 ID 重复注册、并行重复注册和不同 ID 的稳定顺序。命令：

```powershell
.\gradlew.bat --no-daemon forge1201UnitTest --console=plain
```

结果：通过。该修复消除了初始化入口重复执行时静态数据包集合持续累加的路径。

### P2-JAR-001：Java 21 字节码复核

新类 `dev.worldgen.tectonic.config.ConfigSnapshot` 在重映射发布 JAR 中为 class major 65；本阶段没有恢复 Java 17 `--release`。发布制品仍位于 `build/libs/intermediates/tectonic-3.0.17-forge-1.20.1.jar`，最终社区版命名将在发布阶段统一调整。

### 尚未完成的 Phase 2 项

- #473、#520 的长时间新区块压力复现；
- Mixin audit、纯专服客户端类隔离复测及同类警告刷屏统计；
- 未知 JSON 字段当前由 `.bak` 保留原件，但规范化主文件不会原样保留未知字段；最终迁移说明必须继续明确这一限制。
