# 兼容性报告

## 已执行组合

| 组合 | 精确版本/制品 | 配置与种子 | 结论 | 证据 |
|---|---|---|---|---|
| Forge + Lithostitched + Tectonic | Forge 47.4.10；`lithostitched-1.4.11-forge-1.20.jar`，SHA-256 `c19a5a36...bccf75`；社区测试 JAR SHA-256 `187e6284...8bb23` | Java 21，默认高度；长测 seed `0` | TESTED：启动、生成、保存、重启和 20,736 区块扫描通过 | `TEST_REPORT.md` 的 P2-SERVER/P2-ISSUE-473 |
| Forge + Lithostitched + Tectonic | Forge 47.4.22；同一 Lithostitched 与社区测试 JAR | Java 21，默认配置 | TESTED：225 新区块、strict Mixin、保存停服通过 | `benchmarks/forge_server/2026-09-03/README.md` |
| 官方 3.0.17 世界 -> 社区版 | 官方源世界 256 full chunks、seed `0`、DataVersion 3465 | 默认高度，Forge 47.4.22，Java 21 | PARTIAL：副本连续启动两次；旧区块读取、相邻新区块生成、配置迁移、保存停服与 BlendingDataMixin 触发通过 | P2-WORLD-UPGRADE-DEFAULT-001；有玩家/实体/POI 与扩展高度仍待测 |
| Tectonic Tweak | `tectonic_tweak-1.1.0.jar` + Forge 47.4.22 | 默认 | BLOCKED：必现五个未绑定 v2 density function；移除后恢复 | `KNOWN_ISSUES.md` |
| JourneyMap / Xaero | `journeymap-forge-1.20.1-6.0.4.jar`，SHA-256 `ce7b60f3...feb156`；`xaerominimap-forge-1.20.1-26.4.2.jar`，SHA-256 `8143f332...eb59c`；社区测试 JAR SHA-256 `6727d0f4...3a915` | Forge 47.4.22 生产客户端，Java 21；两个地图模组分别与 Lithostitched 1.4.11、社区候选包加载 | TESTED：两个组合均经 Quick Play 进入同一单人世界；JourneyMap 完成客户端初始化并实际写出 day/night/topo/biome 瓦片，Xaero 完成两阶段加载、创建 HUD 会话并绑定 overworld；无 Tectonic Mixin 错误、区块反序列化异常或 FATAL | P3B-MAP-STRUCTURE-001、P3B-MAP-CLIENT-001；离线账号的 profile key 获取 ERROR 与兼容性无关 |
| Tectonic + Terralith | Terralith 2.5.4，`Terralith_1.20.x_v2.5.4.jar`，SHA-256 `8f65f309...1b95f`；社区测试 JAR SHA-256 `6727d0f4...3a915` | Forge 47.4.22，Java 21，默认高度，seed `0` | PARTIAL：256 新区块、内置 `overlay.terratonic`、Terralith cave biome、保存停服通过；官方 3.0.17 + Terralith 世界副本的旧/新区块边界和二次重启通过；`rocky_mountains` 736 个地表列未出现大面积草方块覆盖 | P3C-TERRALITH-001、P2-WORLD-UPGRADE-TERRALITH-001；`volcanic_crater` 在本次搜索半径内未定位，仍无定点断言 |
| Tectonic + Terralith + Terratonic | `terratonic-datapack-v3.1.2.zip`，SHA-256 `ad93979b...fe217`；其余制品同上 | Forge 47.4.22，Java 21，默认高度，seed `0` | PARTIAL：真实数据包与内置兼容包自动启用，256 新区块、保存、二次启动和全量扫描通过 | P3C-TERRATONIC-001；特殊地表定点断言和官方旧世界升级边界仍待测 |

`lithostitched-1.4.11-forge-1.20.jar` 的完整 SHA-256 为：

`c19a5a36c0e6cb3782cf7ca5b9648fb1bce5fc41fd737bed423a1f4971bccf75`

## 兼容制品来源

以下制品于 2026-09-03 从 Modrinth 对应版本页取得；报告固定版本 ID、文件名和 SHA-256，不用“最新版”三个字糊弄未来维护者：

| 项目 | Modrinth 版本 | 文件 | SHA-256 |
|---|---|---|---|
| 官方 Tectonic | `KLmvRxwh` | `tectonic-3.0.17-forge-1.20.1.jar` | `c6de479b27cf090510de3f029918afc9fe17226981bd703e0002f7a25f4d8969` |
| Terralith | `WeYhEb5d` | `Terralith_1.20.x_v2.5.4.jar` | `8f65f309d8f2723754bf4b60c7b5763d3ab6ed04b01c172109ba6564e981b95f` |
| Terratonic | `vmMShagY` | `terratonic-datapack-v3.1.2.zip` | `ad93979bb2d20c142dc2c31250dd2f4d09aeaf22260569dc89130540422fe217` |
| JourneyMap | `8Le0UypF` | `journeymap-forge-1.20.1-6.0.4.jar` | `ce7b60f37a8d0ec7a67cd1e927f394c1141c2ac3b2ed697583cc5d0270feb156` |
| Xaero's Minimap | `A1JacFsh` | `xaerominimap-forge-1.20.1-26.4.2.jar` | `8143f332ed20a61a518692b42c793e021b6bd2d0d9d61cf3938fb9cfa07eb59c` |

## Mixin 与客户端隔离

- Forge 47.4.10/47.4.22 正式专服都加载 `tectonic.mixins.json` 与 `tectonic_1.20.1.mixins.json`。
- Forge 47.4.22 生产客户端分别加载 JourneyMap 6.0.4 和 Xaero's Minimap 26.4.2 后均进入世界；开发环境运行因两个第三方模组各自面向生产命名的 refmap 失配而失败，因此最终结论只采用生产客户端证据。
- 普通新世界实际覆盖 14 个服务端 Tectonic mixin；strict/verify 无注入失败，专服未加载 Tectonic 客户端类。
- `BlendingDataMixin` 已在官方 3.0.17 默认高度世界，以及官方 3.0.17 + Terralith 2.5.4 世界副本的旧/新区块交界真实触发；扩展高度仍未覆盖，不能写成完整升级矩阵通过。
- Forge 1.20.1 自带 Mixin 0.8.5 会对 Java 21/major 65 mixin 输出启动期兼容级别警告；ASM 9.8/9.9.1 实测仍能变换。这里不会为了消警告偷偷降回 Java 17。
- 3.0.22 高版本 `SerializableChunkDataMixin` 修复不适用于 1.20.1：该版本目标方法首参和本分支 mixin 均为 `ServerLevel`，最终 JAR 也不包含高版本类。

## 未执行矩阵

以下计划组合尚无带文件名、版本、来源、SHA-256、配置和种子的真实证据，因此一律按未测试处理：

- Distant Horizons；
- Alex's Caves；
- Biomes O' Plenty；
- Regions Unexplored；
- Oh The Biomes We've Gone；
- Nature's Spirit；
- CTOV；
- YUNG's Better 系列；
- 常用性能模组组合；
- Clifftree（同时需要先证实存在适用于 Forge 1.20.1 的目标版本）。

缺测试不是“默认兼容”。这种表格最怕一片绿但全靠想象，本报告宁可难看，也不拿猜测冒充运行结果。
