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
- 当前结果：Forge 47.4.10/47.4.22 的短程新区块运行没有复现 `Stream.toList` / `CubicSpline$Multipoint.mapAll` 崩溃。
- 缺口：仍需确定顺序、Java 21、不少于 20,000 个新区块的长期压力测试。短程 smoke 不能拿来顶替这个门槛。

## `ore_fix` 在极深世界会明显富集

- 状态：默认关闭，阻断 Stable。
- 当前结果：`minY=-320` 时，开启/关闭总量倍率为 diamond `9.485×`、gold `3.383×`、lapis `3.888×`、redstone `11.815×`。
- 处理：在密度校准完成前保持默认关闭；完整统计见 `benchmarks/ore_fix/2026-09-03/README.md`。
