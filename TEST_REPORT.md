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

结果：通过。共 250 个 JUnit 5 测试实例，0 failure、0 error、0 skipped；其中资源参数化测试逐个验证 226 个 JSON 文件的严格语法。干净构建及 Forge 1.20.1 重映射 JAR 同时通过。报告位于未提交的 `build/test-results/forge1201UnitTest` 与 `build/reports/tests/forge1201UnitTest`。

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

## Phase 3A：3.0.19 通用修复

### P3A-HEIGHT-001：默认高度与外部数据包解耦

上游来源为 `cebbe095209a46051190deff7b247a0dd7e80b9e`。当 Tectonic 配置仍为原版 Overworld 高度 `-64..320` 时，`SetHeightLimitsModifier` 不再覆盖外部数据包提供的维度和噪声高度；只有用户显式选择非原版高度时才写入。现有 modifier 资源只绑定 `minecraft:overworld`，Nether、End 与模组维度不在修改目标中。

回归测试覆盖原版、扩展上限和扩展下限的识别；Forge 1.20.1 干净构建、单元测试、重映射 JAR 与 Java 21 class major 65 复核均作为提交阻断检查。

### P3A-OCEAN-001：低 Ocean Offset 大陆密度钳制

上游来源为 `cebbe095209a46051190deff7b247a0dd7e80b9e`，并与 `34241bdb35acda67b5367d49f354c66c05e098e2` 的最终资源一致。`tectonic:noise/raw_continents` 在原有 ocean offset 与大陆 spline 相加后统一钳制到 `[-1, 2]`，避免低于 `-1` 的配置把 Mushroom Fields 选择区间异常扩散；配置滑块仍保留 `[-2, 0]`，没有限制用户表达能力。

自动测试验证 clamp 类型、边界和内部 add 表达式，并增加全部 JSON 资源的严格语法扫描。实际海洋/Mushroom Fields 占比仍需固定种子世界生成统计，当前不冒充为已完成黑盒验收。

### P3A-RIVER-ICE-001：River Ice

上游来源为 `cebbe095209a46051190deff7b247a0dd7e80b9e`。回移内容包括旧 `ConfigState`/不可变 `ConfigSnapshot`/GUI 中默认关闭的 `continents.river_ice`，以及 1.20.1 可解析的 configured feature、placed feature 和 Lithostitched 1.4 modifier。高版本通用标签 `#c:is_snowy` 已按 Forge 1.20.1 实际资源转换为 `#forge:is_snowy`；未复制 Fabric/NeoForge 高版本加载条件。

自动测试覆盖旧配置缺少新字段时默认关闭、快照隔离、modifier 配置键/资源 ID/雪地标签，以及全部 JSON 的严格解析。真实专服测试分别以 `river_ice=false` 和 `river_ice=true` 启动并载入同一世界；开启后额外强制生成 `[32,32]..[47,47]` 共 256 个新区块，再移除强加载、保存并正常停服。`run/logs/latest.log` 未命中发布阻断错误模式；批量生成期间出现一次 `Can't keep up`（落后 18.021 秒），保留为后续统一性能基准的观察项，不据此宣称性能达标。

### P3A-PRESET-001：Frozen Wasteland

上游来源为 `cebbe095209a46051190deff7b247a0dd7e80b9e`，并逐字段对照至 `34241bdb35acda67b5367d49f354c66c05e098e2`。预设使用 `vertical_scale=1.2`、`elevation_boost=0.5`、高度 `-64..448`，启用 Underground Rivers、River Lanterns 和 River Ice，禁用 Jungle Pillars，并使用上游寒冷/植被噪声参数。预设入口继续向 GUI 提供深复制，不共享可变全局对象。

回归测试逐项断言关键数值、开关与高度；翻译键 `preset.tectonic.frozen_wasteland` 已加入。GUI 仍通过统一 `acceptPresets` 路径展示和应用，无新增客户端依赖进入专服路径。

### P3A-RIVER-LANTERNS-001：频率与加载条件修复

频率调整来源为上游提交 `cebbe095209a46051190deff7b247a0dd7e80b9e`：`tectonic:river_lanterns` 噪声振幅由 `[1, 2]` 收敛为 `[2]`。加载崩溃修复来源为 `d023a88`：Lithostitched modifier 仅保留其通用 `predicate`，删除同一文件中重复的 `fabric:load_conditions` 与 `neoforge:conditions`，避免不属于当前加载器的条件类型参与解码。

本回移有意保留 Forge 1.20.1 已发布的 `tectonic:river_lanterns` 资源 ID 和路径，没有跟随高版本重命名为 `underground_river`，以免无必要地破坏现有数据包引用。自动测试断言振幅、配置键和两个加载器专用字段的缺失，并继续严格解析全部 JSON 资源。

真实专服以默认开启的 `river_lanterns=true` 启动，在 Temurin 21 上于 `Done (3.971s)` 后强制生成 `[64,64]..[79,79]` 共 256 个新区块，随后解除强加载、保存并正常停服。`run/logs/latest.log` 未命中 ERROR、FATAL、Mixin 应用失败、链接错误、注册表映射失败、资源解析失败或非重叠 MIN 输入等阻断模式；生成期间出现一次 `Can't keep up`（落后 13.078 秒），仅记录为后续统一压力测试的性能观察。

## Phase 3B：3.0.20—3.0.22 回移

### P3B-REGION-CACHE-001：水平 region spline 缓存

上游来源为 `5c02e8d042a4e0183389e0599ac6a0e4cb3cb4f6`。`club`、`club_weak`、`heart`、`spade` 和 `spade_weak` 五个 region spline 增加一层 `minecraft:flat_cache → minecraft:cache_2d`，内部 spline 数值与引用保持不变。Forge 1.20.1 已原生提供这两个 DensityFunction marker 类型，相关输入只依赖水平坐标。

上游还给 `diamond` 再套了一层相同包装，但 3.0.17 基线中的该资源本来已经是 `flat_cache → cache_2d → spline`。本分支按 Ponytail 最小化原则保留现有单层缓存，并用测试锁定六个 region 都恰好只有一层，避免无收益的缓存套娃。该偏差已同步记录在 `UPSTREAM_DELTA.md`。

自动测试通过，共 247 个实例且全部 JSON 严格解析成功。真实专服使用默认配置在 Temurin 21 上于 `Done (3.841s)` 后强制生成 `[96,96]..[111,111]` 共 256 个新区块，随后解除强加载、保存并正常停服。日志未命中阻断错误模式；生成期间出现一次 `Can't keep up`（落后 12.523 秒），只作为后续固定种子性能基准的观察值。缓存前后地形数值等价性仍需固定种子快照验证，当前不冒充为已完成。

### P3B-OVERKILL-001：Overkill 预设

预设初次来源为 `70bbe33a068caf8dea276781a66a86bf6433a7f6`，最终值来源为 `5c02e8d042a4e0183389e0599ac6a0e4cb3cb4f6`，并对照至 `34241bdb35acda67b5367d49f354c66c05e098e2`。Forge 1.20.1 版本使用旧 Schema 可表达的最终参数：`snow_start_offset=512`、`vertical_scale=2.5`、`elevation_boost=1.6`、高度 `-64..768`、Ultrasmooth 开启、Lava Tunnels 关闭，以及上游最终的 continents/biome 参数。

当前保存校验和 GUI 原先只允许 snow offset `0..256`、elevation boost `0..1`，会把预设值重置或让滑块越界；两处范围现同步放宽至 `0..512` 和 `0..1.6`。预设按钮显示为 `Overkill - Very High Worldgen Cost`，直接在 GUI 提示成本。自动测试覆盖最终参数、按钮警告，以及保存、激活、重新读盘后 `512/1.6/768` 不变。

上游 `Experimental(true, true)` 属于独立的 alternate noise scaling 功能，不在旧 Schema 中硬塞；该功能截至审计终点仍把 continents 接到错误的 erosion 标志，且与 C2ME hardware acceleration 有已知约束，继续按矩阵标记 `DEFERRED`。因此这里声称的是 1.20.1 旧 Schema 版 Overkill，不冒充高版本逐方块同构。

真实专服使用固定种子 `8675309` 创建独立 `world-overkill`，维度总高度 832。出生区准备耗时 `36.783s`，服务端于 `Done (43.693s)` 后可用；随后强制生成 `[64,64]..[79,79]` 共 256 个新区块，解除强加载、保存并正常停服。批量生成产生一次 `Can't keep up`（落后 30.723 秒）；生成结束后的 `forge tps` 为总体 `3.729ms / 20 TPS`。`jcmd GC.heap_info` 单点样本为 G1 heap `3,072,000K` total、`2,184,278K` used，Metaspace `122,577K` used。日志无 Tectonic/Lithostitched、Codec、Mixin、链接或注册表阻断错误；首次创建世界时 Forge 自动补齐自身 server config 的警告不属于 Tectonic 故障。

本项目前只证明配置可持久化、832 高度新世界可创建并可生成小批新区块。出生点安全、结构可达、山体截断、五种子 1024/4096 区块基准和内存增长曲线尚未完成，不能据此宣称 Overkill 性能达标。

### P3B-LITHOSTITCHED-METADATA-001：运行与发布依赖

Forge 1.20.1 继续使用经实际加载验证的 `maven.modrinth:lithostitched:1.4.11-forge-1.20`（JAR SHA-256 `c19a5a36c0e6cb3782cf7ca5b9648fb1bce5fc41fd737bed423a1f4971bccf75`）。最终重映射 JAR 的 `META-INF/mods.toml` 声明 `lithostitched` 为 `mandatory = true`、`type = "required"`、`versionRange = "[1.4.11,)"`；发布脚本也分别向 Modrinth（项目 `XaDC71GB`）和 CurseForge（项目 `936015`、关系 `3`）发送必需依赖关系。上游较新的 1.6.0 属于高版本 Minecraft 线，不能拿来卡死仍然正确的 Forge 1.20.1 依赖。

回归测试现在直接依赖 `forge1201RemapJar` 并读取最终制品，而不是只检查模板。Temurin 21 下执行 `clean forge1201UnitTest` 通过，共 251 个测试，0 failures、0 errors、0 skipped；`javap -verbose` 同时确认制品内 `dev.worldgen.tectonic.Tectonic` 为 class major 65（Java 21）。

### P3B-ORE-FIX-001：扩展深度矿物放置

上游来源为 `aab71be61aa11c7c5175d71ffa15e42d01b91da8`，资源与算法又对照至 `34241bdb35acda67b5367d49f354c66c05e098e2`。新增的 `HeightStabilizedCount` 使用 Forge 1.20.1 所需的 `Codec`（不是高版本 `MapCodec`），按 `(maxY - minGenY) / 16 × count_per_section` 向上取整计算每区块尝试数，并保留上游的均匀/偏底采样。`caves.ore_fix` 默认关闭，可经旧配置补默认值、深复制、不可变运行快照和 GUI 正常保存；Forge 只在开关开启时挂载独立的 `overlay.ore_fix` 数据包。

资源精确覆盖 1.20.1 存在的 11 个原版 placed feature。上游第 12 个 `ore_diamond_medium` 在 Minecraft 1.20.1 的 configured/placed feature 注册表中均不存在，因此明确排除，避免用一个不存在的 ID 把整个数据包炸掉。严格资源测试锁定文件全集、configured feature、锚点、密度、偏底标志和 placement 顺序；算法测试额外锁定 `ceil` 语义，包括 `count_per_section=0.02` 在默认跨度下仍为一次尝试。

Temurin 21 下执行 `clean forge1201UnitTest forge1201RemapJar` 通过，共 270 个测试，0 failures、0 errors、0 skipped；最终 JAR 中 `HeightStabilizedCount` 同样是 class major 65（Java 21）。根 `build` 会连带解析本分支不支持的 Fabric/NeoForge 目标，仍会触发 `docs/BUILD_TASKS.md` 已记录的 Cloche 跨目标中间文件问题，因此不作为 Forge 1.20.1 验收入口。

真实专服使用固定种子 `4382026`、高度 `-128..320` 和 `ore_fix=true` 创建独立 `world-ore-fix-smoke`。服务端自动发现 `tectonic/tectonic/overlay.ore_fix`，注册表与全部 Codec 解码成功，于 `Done (16.443s)` 后强制生成 `[64,64]..[79,79]` 共 256 个新区块，解除强加载、刷新保存并正常停服。生成期间一次 `Can't keep up` 落后 `8.321s`；结束前总体为 `2.528ms / 20 TPS`。日志未出现 ERROR、FATAL、异常、未知注册表或资源解析失败；首次世界创建时 Forge 补齐自身 server config 的警告仍属预期。完整五种子、三档 minY 矿物 CSV/图表尚未完成，本项当前只算实现与烟测通过，不能冒充统计验收完成。

### 尚未完成的 Phase 2 项

- #473、#520 的长时间新区块压力复现；
- Mixin audit、纯专服客户端类隔离复测及同类警告刷屏统计；
- 未知 JSON 字段当前由 `.bak` 保留原件，但规范化主文件不会原样保留未知字段；最终迁移说明必须继续明确这一限制。
