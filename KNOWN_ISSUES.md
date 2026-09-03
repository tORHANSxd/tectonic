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
- 保留风险：普通新世界没有触发 `BlendingDataMixin`；必须在旧/新区块交界测试后才能宣称完整 Mixin audit。
- 决策：继续使用 Java 21 / major 65，按用户明确要求不产出 Java 17 字节码。

## issue #473 尚未关闭

- 状态：发布阻断。
- 当前结果：Forge 47.4.10/47.4.22 的短程新区块运行没有复现 `Stream.toList` / `CubicSpline$Multipoint.mapAll` 崩溃；确定顺序的 Java 21 压力脚本已通过 20,736 区块规划检查、两 tile/两会话生产专服冒烟和失败快速判定测试。
- 缺口：仍需用该通道实际跑完不少于 20,000 个新区块。规划检查和短程 smoke 不能拿来顶替长期门槛；严格重复性也未通过，见下一节。

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
