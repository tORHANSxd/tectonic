# 规范化地形快照证据（2026-09-03）

本目录记录 Forge 1.20.1 社区回移版的首轮严格重复性验收。结论很明确：规范化哈希工具已经可用，但相同种子和固定请求顺序生成的两个独立世界仍不相等，因此严格确定性门槛未通过。

## 快照协议

`scripts/hash_test_world.py` 实现 `tectonic-terrain-snapshot-v1`：

- 包含每个完整区块、每个有效 section 的全部 `block_states` 与 `biomes`，保留方块坐标、方块属性和生物群系位置；
- 将 palette 顺序、重复/未引用 palette 项、compound 键顺序和 long 尾部 padding 规范化；
- 排除 `LastUpdate`、`InhabitedTime`、scheduled ticks、光照、Heightmaps、PostProcessing、结构元数据、方块实体、实体以及其他运行期区块字段；
- 严格校验闭区间覆盖、坐标、`minecraft:full`、DataVersion、Y 范围和快照自身总哈希；
- 区块按 `Z → X` 排序，总哈希同时包含坐标和逐区块 SHA-256；扫描时只保留逐区块摘要，不在内存中堆积完整规范化区块。

比较命令退出码为：相同 `0`、有效但内容不同 `1`、输入损坏或不符合 schema `2`。18 个标准库单元测试覆盖 golden hash、palette/section/区块顺序无关性、有效内容变化、运行期字段排除、范围外照明 section、缺区块、非 FULL 区块、快照完整性、独立 biome 位序向量、CSV/summary 采样窗绑定和损坏 gzip 的坐标化错误。

## 固定顺序生产专服复验

两次运行使用相同输入：

- Forge `47.4.22` 正式服务端；
- `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`，实际报告 Temurin `21.0.12.1`；
- Tectonic JAR SHA-256 `187e6284b3845b0869f0c9facfd43f845abfcacdaa7cd3ca0a998d67d188bb23`；
- Lithostitched JAR SHA-256 `c19a5a36c0e6cb3782cf7ca5b9648fb1bce5fc41fd737bed423a1f4971bccf75`；
- seed `0`，默认高度 `-64..320`，`ore_fix=false`；
- 闭区间 `[700,700]..[703,703]`，16 个区块，一个 `4×4` tile；
- tile 与区块均按 `Z → X` 请求，每个区块等待单独 FULL 回执；每 tile 保存，不重启；
- 唯一 `-Dmax.bg.threads=1`。

两次 Java 21 会话都正常启动、生成、保存和停服，退出码为 `0`。快照结果却是：

| run | terrain SHA-256 | 快照文件 SHA-256 | 字节 |
|---|---|---|---:|
| `20260903-determinism-fixed-a` | `fc8dfbc9cedb72f2f08f5d1c5a451a021b3115570ae4c1a6a821285309108ba2` | `f23bafdfca093ea625c1c221ea58482bb9b396d3f809f857a53d58dd632f827a` | 3,110 |
| `20260903-determinism-fixed-b` | `2ec87c3d440cfda5fd27c778fa7180fbe5fcb33cae5d633f01acf10198cb0468` | `54eb7f3b308add566e90d94fd6f7865cf9710a0f32cfea4903b24f1b7eff39ae` | 3,111 |

严格比较返回 `1`，16 个目标区块中 15 个不同。补充解码显示两个世界的 biome 位置完全一致，方块状态共有 5,544 个位置不同：

| chunk Z \ X | 700 | 701 | 702 | 703 |
|---:|---:|---:|---:|---:|
| 700 | 0 | 8 | 249 | 955 |
| 701 | 17 | 59 | 720 | 934 |
| 702 | 212 | 86 | 428 | 369 |
| 703 | 146 | 172 | 436 | 753 |

差异主要是 Ancient City 周边的 `sculk`、`sculk_vein`、`deepslate`、`air`，另有树叶 `distance` 属性差异。这些是保存下来的真实世界内容，不能作为运行期噪声从 schema 中删掉。

服务日志还显示，虽然目标 parallelism 设置为 `1`，A 运行出现 `Worker-Main-1/6`，B 运行出现 `Worker-Main-1/6/7`。Minecraft 1.20.1 的该 executor 是 async `ForkJoinPool`；`max.bg.threads=1` 约束目标 parallelism，并不构成“进程中永远只有一个 worker”的硬保证。该调度差异是当前最可疑路径，但尚不足以证明根因属于 Tectonic、Vanilla 或某一 decoration feature。

## 旧批量流程交叉检查

旧版一次性强加载 `[64,64]..[79,79]` 的两个 seed `0` 世界也用同一 schema 扫描：

| run | terrain SHA-256 | 快照文件 SHA-256 | 不同区块 |
|---|---|---|---:|
| `20260903-default-noop-check` | `df7556646e6ffffb0cf5f29eb4367f9e664e398aa76f16b06da11eaccf38c976` | `c38d424dc0b573952a1933a66ff940f3f77094b95ac79b0c52e016cf9ad13996` | 178 / 256 |
| `20260903-default-noop-repeat` | `932a80fb43356b8b92dc1df83a3936361d8070eb374b21615e7547131b2022ed` | `81749c3cfcd6e9250d1f6367d89d56c0fce6a56ad87b52da59f2e4c449aa5b80` | 178 / 256 |

这组结果只作为旧流程交叉检查；严格门槛以固定顺序生产专服复验为准。

## 复现

原始世界和快照位于被 Git 忽略的 `run/`，本页保存其内容哈希。仓库根目录执行：

```powershell
python scripts/hash_test_world.py scan `
  run/production-forge-47.4.22/batch-20260903-determinism-fixed-a-determinism-fixed-miny-m64-seed-0 `
  --chunks 700 700 703 703 --expected-min-y -64 `
  --output run/production-forge-47.4.22/batch-results/20260903-determinism-fixed-a/terrain-snapshot-v1.json

python scripts/hash_test_world.py scan `
  run/production-forge-47.4.22/batch-20260903-determinism-fixed-b-determinism-fixed-miny-m64-seed-0 `
  --chunks 700 700 703 703 --expected-min-y -64 `
  --output run/production-forge-47.4.22/batch-results/20260903-determinism-fixed-b/terrain-snapshot-v1.json

python scripts/hash_test_world.py compare `
  run/production-forge-47.4.22/batch-results/20260903-determinism-fixed-a/terrain-snapshot-v1.json `
  run/production-forge-47.4.22/batch-results/20260903-determinism-fixed-b/terrain-snapshot-v1.json
```

结论：工具门槛已落地，结果门槛失败。继续扩大到 20,000 区块不会把 15/16 的真实差异“平均没”；必须先隔离 decoration/halo 调度路径，或明确把严格逐方块相等从发布标准中移除，不能装作已经确定。
