# Forge 1.20.1 社区版发布检查表

当前目标是 `3.0.17-backport.1` 候选包，不是 Stable。未勾选的阻断项禁止靠改措辞、跳测试或手工上传绕过。

## 身份与构建

- [x] 分支基于 `5373b2084e461f83bd6e0b5f2fe943e81bd59700`，并记录审计终点 `34241bdb35acda67b5367d49f354c66c05e098e2`。
- [x] 名称、描述、URL、版本与文件名均标明非官方社区回移。
- [x] `LICENSE`、`NOTICE.md` 随源码和最终 JAR 提供，原作者与贡献者保留。
- [x] Minecraft 元数据限定 `[1.20.1,1.20.2)`，Lithostitched 为 required `[1.4.11,)`。
- [x] 指定 JDK 21 clean candidate build 成功。
- [x] 自动扫描最终 JAR 中全部 `dev/worldgen/tectonic/**/*.class` 为 major 65。
- [x] 最终候选包旁生成 `.jar.sha256`。
- [x] Wrapper distribution SHA-256 固定；ZIP 时间戳与条目顺序规范化；仓库文本换行固定。
- [ ] 相同提交、相同平台重复构建 SHA-256 一致。当前被 TinyRemapper 局部变量重命名非确定性阻断。
- [ ] Linux/Windows 两端制品 SHA-256 一致。当前不设伪通过门禁。
- [ ] 生成根目录 Stable `checksums.sha256`。候选阶段仅保留 build 输出 sidecar。

## 功能与正确性

- [x] Forge 47.4.10 与 47.4.22 正式专服启动、保存、停服。
- [x] Java 21-only 客户端基础启动到资源完成态。
- [x] River Ice、River Lanterns、Frozen Wasteland 与 Ocean Offset/高度资源契约通过。
- [x] issue #473：20,736 新区块、11 次会话/重启、完整离线扫描，无阻断错误。
- [x] issue #520：根因定位为 Tectonic Tweak 1.1.0 的 v2 数据包；移除回归通过。
- [x] `ore_fix` 默认高度 no-op 与扩展深度零矿带覆盖通过。
- [ ] `ore_fix` 极深世界密度校准通过。当前 diamond/redstone 富集。
- [ ] 相同输入的规范化地形哈希一致。当前 16 区块样本中 15 个存在真实方块差异。
- [ ] 5 个固定种子、每种配置至少 4096 区块的候选矩阵通过。

## 兼容与升级

- [x] 专服未加载 Tectonic 客户端类；14 个已触发服务端 mixin 的 strict/verify 通过。
- [x] 1.20.1 `ChunkSerializer` 签名与最终 JAR mixin 集合结构校验通过。
- [x] `BlendingDataMixin` 在官方 3.0.17 默认高度存档副本的旧/新区块边界触发并验证。
- [x] 官方 3.0.17 默认高度、256 个既有 full chunk 的副本连续两次启动、保存和相邻新区块生成通过。
- [ ] 带真实玩家、实体、POI、地图数据和大量区块的存档副本升级通过。
- [ ] Increased Height 与自定义 minY/maxY 存档副本升级通过。
- [x] Terralith 2.5.4 默认高度新世界、内置兼容包、`rocky_mountains` 地表抽样和官方 3.0.17 世界旧/新区块边界通过。
- [x] Terratonic 3.1.2 数据包新世界、256 新区块、保存与二次启动通过。
- [ ] Terralith `volcanic_crater` 定点方块断言与 Terratonic 旧世界升级边界通过。
- [x] JourneyMap 6.0.4 与 Xaero's Minimap 26.4.2 分别通过 Forge 47.4.22 / Java 21 生产客户端黑盒。
- [ ] `COMPATIBILITY_REPORT.md` 中 P0/P1 目标全部具有精确版本、文件 SHA 和结论。

## 性能与生命周期

- [ ] 默认配置对官方 3.0.17 的 matched A/B 达到计划阈值。
- [ ] 记录 chunks/s 中位数、P95、P99、CPU、峰值 heap/RSS 与 GC 暂停。
- [ ] Overkill 五种子压力、内存增长和出生点安全通过。
- [ ] 客户端 10 次进出世界无泄漏；专服至少 5 次生命周期无残留线程。
- [ ] 卡顿样本包含线程转储或 JFR/Spark 证据。

## 发布操作

- [x] `uploader.py` 默认且无条件 fail-closed。
- [x] CI 只构建并校验候选包，不含 Modrinth/CurseForge 发布凭据或步骤。
- [ ] 工作区 clean，最终 Manifest `Git-Dirty=false` 且 `Git-Commit` 等于待发布提交。
- [ ] 所有阻断项关闭后，另行设计显式人工审批的社区项目发布流程。

结论：当前只允许源码分支和候选 CI 制品，不允许 Stable 或外部模组平台发布。
