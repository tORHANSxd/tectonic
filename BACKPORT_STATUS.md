# 回移状态

状态只使用计划规定的 `DONE`、`TESTED`、`PARTIAL`、`NOT_APPLICABLE`、`BLOCKED` 和 `DEFERRED`。`TESTED` 表示已有对应证据，不等于整个候选版达到 Stable。

| 上游版本/提交 | 项目 | 状态 | 主要文件 | Forge 1.20.1 实现 | 测试编号 | 未完成原因 |
|---|---|---|---|---|---|---|
| 3.0.17 / `5373b208...` | 官方基线复现 | TESTED | `build.gradle.kts`、`UPSTREAM_BASELINE_FILES.txt` | 保留 1.20.1 Forge 壳体并以 Java 21 构建 | P1-BUILD-001、P1-JAR-001、P1-SERVER-001/002、P1-CLIENT-001 | 客户端 GUI 建世/重进尚无自动化 |
| 本分支 | 配置深复制、校验与尽力原子写入 | PARTIAL | `ConfigHandler.java`、`ConfigSnapshot.java`、`ConfigScreen.java` | 磁盘草稿与世界生成快照分离；临时文件替换；保留单槽 `.bak`/`.invalid` | P2-CONFIG-001 | 未知字段、错类型、权限失败和 GUI 生命周期仍缺自动化覆盖 |
| 本分支 | 内置数据包幂等注册 | TESTED | `TectonicRepositorySource.java`、`PackRepositoryMixin.java` | 按 pack ID first-wins，并发布不可变顺序快照 | P2-PACK-001 | 无 |
| 3.0.19 / `cebbe095...` | Frozen Wasteland | TESTED | `ConfigPresets.java`、翻译与配置类 | 在旧 schema 可表达范围内移植最终参数 | P3A-PRESET-001 | 大规模性能与出生点安全仍归入性能门禁 |
| 3.0.19 / `cebbe095...` | River Ice | TESTED | `ConfigState.java`、`ConfigSnapshot.java`、river ice 资源 | 使用 1.20.1 configured/placed feature、Lithostitched 1.4 modifier 与 `#forge:is_snowy` | P3A-RIVER-ICE-001 | 无 |
| 3.0.19 + 3.0.21 / `cebbe095...`、`d023a88...` | River Lanterns 频率与加载修复 | TESTED | river lantern JSON | 振幅 `[2]`；移除 Fabric/NeoForge 专属 condition；保留旧资源 ID | P3A-RIVER-LANTERNS-001 | 无 |
| 3.0.19 / `cebbe095...` | Ocean Offset 洪泛修复 | PARTIAL | `raw_continents.json` | 对组合结果钳制到 `[-1,2]`，不缩小配置输入范围 | P3A-OCEAN-001 | 还缺五种子 Mushroom Fields 面积黑盒统计 |
| 3.0.19 / `cebbe095...` | 默认高度与外部数据包解耦 | TESTED | `SetHeightLimitsModifier.java` | 默认 `-64..320` 时不覆盖外部高度 | P3A-HEIGHT-001 | Terralith 真实组合另见兼容矩阵 |
| 3.0.20 / `5c02e8d...` | Region spline 水平缓存 | PARTIAL | 六个 `terrain_spline/region/*.json` | 五个缺失项补单层 `flat_cache -> cache_2d`；基线 diamond 不套娃 | P3B-REGION-CACHE-001 | 没有 matched A/B 性能数据 |
| 3.0.20 / `70bbe33...`、`5c02e8d...` | Overkill 预设 | PARTIAL | `ConfigPresets.java`、配置校验与 GUI | 移植旧 schema 能表达的最终值并显示高成本警告 | P3B-OVERKILL-001 | 缺 5 种子 4096 区块、峰值内存和出生点安全验收 |
| 3.0.20 | Alternate noise scaling | DEFERRED | 无 | 未把仍有接线问题的实验字段硬塞进旧 schema | 无 | 上游语义未稳定，并与部分并行世界生成方案有约束 |
| 3.0.22 / `aab71be...` | `ore_fix` 扩展深度矿物 | PARTIAL | `HeightStabilizedCount.java`、`overlay.ore_fix`、配置与 GUI | 适配 1.20.1 `Codec`，只在 `minY < -64` 时激活 11 个存在的 feature | P3B-ORE-FIX-001、P3B-ORE-STATS-001 | 深层 diamond/redstone 明显富集，默认关闭并阻断 Stable |
| 3.0.22 / `46cc1eb...` | 高版本地图查询源码修复 | NOT_APPLICABLE | `ChunkSerializerMixin.java`、`ChunkSerializerClientCompatibilityTest.java` | 1.20.1 `ChunkSerializer.read` 原本就是 `ServerLevel`；禁止复制 26.x `ClientLevel` hunk | P3B-MAP-STRUCTURE-001、P3B-MAP-CLIENT-001 | JourneyMap 6.0.4 与 Xaero's Minimap 26.4.2 已分别通过 Forge 47.4.22 生产客户端黑盒；Distant Horizons 仍待测 |
| 3.0.22 | Lithostitched 依赖元数据 | TESTED | `build.gradle.kts`、最终 JAR `mods.toml` | Forge 1.20.1 固定最低 `1.4.11` 且声明 required | P3B-LITHOSTITCHED-METADATA-001 | 无 |
| 3.0.23 | JSON5、Export 与 Apollib 配置栈 | DEFERRED | 无 | 继续使用小而稳定的 JSON 实现 | 无 | 为非核心便利功能引入高版本依赖不划算 |
| 3.0.23/3.0.25 | Back/Cancel 与深复制修复 | TESTED | `ConfigScreen.java`、`PresetSelectorScreen.java`、配置模型 | 取消不落盘；预设只改工作副本；世界内保存下次启动生效 | P2-CONFIG-001 | GUI 点击路径没有自动化 UI 测试 |
| 3.0.24 | Sulfur Caves | NOT_APPLICABLE | 无 | 1.20.1 基线无对应注册内容 | 无 | 上游历史没有可独立适配的 1.20.1 功能 |
| 3.0.26 / 官方制品 `80oiGLPz` | Jaggedness 修复 | TESTED | `continents.json`、资源契约测试 | 3.0.17 基线已经是修复值 `0.65`，只加防回归测试 | P3C-JAGGEDNESS-001 | 无 |
| 3.0.27 | NeoForge 26.x 修复 | NOT_APPLICABLE | 无 | 不回移不同加载器与 Minecraft API | 无 | 不属于目标矩阵 |
| issue #473 | 长期新区块压力测试 | TESTED | `benchmark_worldgen.ps1`、`hash_test_world.py` | Forge 47.4.10、Java 21、11 次重启累计生成并扫描 20,736 个新区块 | P2-ISSUE-473-LONG-001 | 该结论只覆盖记录的制品和默认配置，不代表严格确定性通过 |
| issue #520 | Tectonic Tweak 1.1.0 崩溃 | TESTED | 测试报告与已知问题 | 证实是第三方 v2 数据包和过宽依赖范围；不伪造旧 density ID | P2-ISSUE-520-001 | 组合仍不兼容，用户必须移除该附加模组 |
| 本分支 | 旧/新区块 blending 范围 | PARTIAL | `ChunkSerializerMixin.java`、对应测试 | 正确读取 section compound 的 `Y`、使用 exclusive 上界，并保持 Mixin 辅助方法私有 | P2-BLENDING-RANGE-001、P2-WORLD-UPGRADE-DEFAULT-001、P2-WORLD-UPGRADE-TERRALITH-001 | 官方默认高度及官方 + Terralith 2.5.4 的 256 区块存档副本和相邻新区块已通过；仍缺扩展高度、Terratonic 旧世界和有玩家/实体/POI 的存档 |
| 本分支 | Java 21 社区候选构建 | TESTED | `build.gradle.kts`、`ForgeModMetadataTest.java` | 最终 `IncludeJar` 重命名、署名、依赖、许可证、Manifest 和全量 major 65 门禁 | P4-CANDIDATE-001 | 不支持 Java 17；发布包 SHA 受 TinyRemapper 局部变量重命名非确定性影响 |
| 本分支 | Stable 发布 | BLOCKED | `KNOWN_ISSUES.md`、`docs/RELEASE_CHECKLIST.md` | 发布器 fail-closed；CI 只验证候选包 | 发布门禁 | 严格地形确定性、矿物富集、主要兼容矩阵、老世界升级与完整性能/内存矩阵未通过 |
