# 已知问题

本文件记录当前社区回移版已经确认但不能靠伪兼容代码掩盖的边界。当前验收证据以 `TEST_REPORT.md` 和回移计划为准。

## Tectonic Tweak 1.1.0 与 Tectonic 3.x 不兼容

- 状态：已完成最小复现和移除回归。
- 影响：Forge 1.20.1 在加载数据包时报告五个 `tectonic:overworld/*` density function 未绑定，世界不能启动。
- 根因：Tectonic Tweak 1.1.0 面向 Tectonic 2.3.4 的资源布局，但其 `mods.toml` 使用过宽的 `[2.1,)`，错误接受 Tectonic 3.x。
- 处理：移除 Tectonic Tweak 1.1.0。不要给本社区回移版补五个含义不确定的 v2 ID；那只会把明确崩溃变成难排查的错误地形。
- 证据：`benchmarks/forge_server/2026-09-03/README.md`。

## Java 21 mixin 的 Mixin 0.8.5 启动警告

- 状态：已知工具链边界；不是降级字节码的理由。
- 影响：Forge 1.20.1 内置 Mixin 0.8.5。项目 mixin 为 Java 21 class major 65 时，每个实际加载的 Tectonic mixin 会在启动阶段输出一次 `JAVA_17 supports class version 61` 警告。
- 已验证：Forge 47.4.10 的 ASM 9.8 与 Forge 47.4.22 的 ASM 9.9.1 均成功解析并变换 major 65；strict/verify 无注入失败，警告未按区块重复。
- 保留风险：默认高度官方 3.0.17 存档，以及官方 3.0.17 + Terralith 2.5.4 存档副本均已触发 `BlendingDataMixin` 并通过；扩展高度尚未覆盖，仍不能宣称完整 Mixin audit。
- 决策：继续使用 Java 21 / major 65，按用户明确要求不产出 Java 17 字节码。

## issue #473 长期崩溃门槛已通过

- 状态：该项已验证，不再单独阻断；严格确定性仍是另一项阻断。
- 当前结果：Forge 47.4.10、指定 Java 21、默认高度、seed `0` 下，确定顺序通道真实生成 `[64,64]..[207,207]` 共 20,736 个新区块；81 个 tile、11 次服务端会话/重启、21 次保存全部完成，退出码均为 0。
- 离线校验：20,736 个区块全部为 `minecraft:full`、DataVersion 3465、完整 `-64..320`；日志未命中 OOME、异常、ERROR、死锁、watchdog、Mixin 注入或保存失败模式。
- 地形哈希：`3931aa963f7b443a9bf74b709af503eb7c341425ad330de8155edd309445e4b2`。
- 边界：结论只覆盖 manifest 记录的 JAR、模组与配置，不代表所有配置或逐方块确定性已经通过。

## 严格地形重复性未通过

- 状态：发布阻断。
- 当前结果：Forge 47.4.22 正式服务端使用指定 Java 21、相同 seed/config/JAR、固定 `Z → X` 请求顺序、逐区块 FULL 屏障和唯一 `-Dmax.bg.threads=1`，两个独立的 16 区块世界仍有 15 个区块的规范化地形 SHA-256 不同。
- 差异性质：biome 完全一致；方块状态有 5,544 个位置不同，主要是 Ancient City 周边的 sculk 放置和少量树叶 `distance` 属性。这是真实保存内容，不能当运行期元数据排除。
- 边界：`max.bg.threads=1` 设置目标 parallelism，但日志仍观察到不同的补偿 worker 名称；目前只把它列为嫌疑路径，不武断归因给 Tectonic。
- 证据：`benchmarks/determinism/2026-09-03/README.md`。

## `ore_fix` 在极深世界会明显富集

- 状态：默认关闭，阻断 Stable。
- 当前结果：`minY=-320` 时，开启/关闭总量倍率为 diamond `9.485×`、gold `3.383×`、lapis `3.888×`、redstone `11.815×`。
- 处理：在密度校准完成前保持默认关闭；完整统计见 `benchmarks/ore_fix/2026-09-03/README.md`。

## 候选 JAR 的 SHA-256 尚不可重复

- 状态：Stable 发布阻断；不影响 Java 21 类版本检查。
- 当前结果：已固定 ZIP 条目顺序、时间戳和仓库换行，但同一工作树连续 clean build 的整包 SHA-256 仍会变化。
- 定位：变化只剩少数重映射 class；业务指令长度一致，差异集中在 Minecraft-Codev 0.6.7 / TinyRemapper `renameInvalidLocals(true)` 选择的 SRG 局部变量名，例如 `p_208228_` 与 `p_209267_`。
- 处理：候选任务仍为每次实际产物生成 sidecar；在上游重映射器可稳定输出或找到小而可靠的修复前，不发布固定哈希，也不引入高风险 classfile 后处理器。

## 老世界与主要可选模组矩阵未完成

- 状态：Stable 发布阻断。
- 当前结果：blending section 范围读取与 Mixin 私有方法约束已有回归测试；官方 3.0.17 默认高度世界，以及官方 3.0.17 + Terralith 2.5.4 世界副本均连续两次启动，旧/新区块边界生成和保存通过；JourneyMap 6.0.4 与 Xaero's Minimap 26.4.2 也已分别通过 Forge 47.4.22 生产客户端进服黑盒。
- 缺口：扩展高度、有真实玩家/实体/POI 的复杂世界，以及 Distant Horizons 等真实组合仍无完整黑盒记录。Terralith 2.5.4 已完成默认高度运行、升级边界和 `rocky_mountains` 地表抽样；Terratonic 3.1.2 已完成新世界加载与重启，但 `volcanic_crater` 定点断言和 Terratonic 旧世界升级仍未完成。
- 处理：升级只能在完整存档副本上测试；缺证据的组合不得宣称兼容。

## 配置迁移不是严格 schema

- 未知字段会在规范化写回时删除；许多错类型字段会回退默认而不生成 `.invalid`。
- `minor_version > 1` 当前不会被拒绝，并可能被写回 `1`。
- `.bak` 与 `.invalid` 是会覆盖的单槽文件；写入仅为尽力原子替换，无跨进程锁或断电事务保证。
- 权限错误会抛异常，GUI 没有专用错误提示。迁移前应保留独立配置副本。
