# Ore Fix 统计证据（2026-09-03）

本目录记录 Forge 1.20.1 社区回移版 `ore_fix` 的首轮统计验收。结论不是“全绿”：#438 的深层零矿带已经修复，但 diamond/redstone 密度和严格确定性仍需后续处理。

## 方法

- JDK：`C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`
- 固定种子：`0`、`1`、`-1`、`123456789`、`-987654321987654321`
- 高度：`minY=-64/-128/-320`，`maxY=320`
- 采样窗：chunk `[64,64]..[79,79]`，每个 case 恰好 256 个 `minecraft:full` 区块
- 数据版本：Minecraft 1.20.1 DataVersion `3465`
- 分桶：每 16 格 Y 一个桶；只读取采样窗，不计出生区和生成 halo
- 总规模：50 个世界，12,800 个受控区块

矩阵与源码来源：

| 场景 | case | 源码提交 | 配置 |
|---|---:|---|---|
| `official-3.0.17` | 15 | `5373b2084e461f83bd6e0b5f2fe943e81bd59700` | 官方 3.0.17，不支持开关 |
| `community-off` | 15 | `01cbec5986a3329da3a54e582105993be825a469` | Tectonic 开，`ore_fix=false` |
| `community-on` | 15 | `d0051325604a6014eeab115d98d4d61a0129d9c2` | Tectonic 开，`ore_fix=true`；默认高度有 no-op 保护 |
| `vanilla-reference` | 5 | `01cbec5986a3329da3a54e582105993be825a469` | `mod_enabled=false`，仅 `minY=-64` |

关闭组与原版参考的源码早于默认高度保护提交，但该保护只影响开启组的数据包挂载判断，不改变这两组的执行路径。

## 主要结果

每格为五种子“每区块总量均值”：

| minY | 场景 | diamond | gold | lapis | redstone |
|---:|---|---:|---:|---:|---:|
| -64 | 官方 3.0.17 | 14.74 | 25.52 | 21.48 | 34.74 |
| -64 | 社区关闭 | 14.75 | 25.54 | 21.48 | 34.74 |
| -64 | 社区开启 | 14.75 | 25.51 | 21.48 | 34.73 |
| -64 | 原版参考 | 14.70 | 25.46 | 24.09 | 35.31 |
| -128 | 社区关闭 | 15.12 | 26.59 | 22.90 | 35.91 |
| -128 | 社区开启 | 69.84 | 42.73 | 43.00 | 147.03 |
| -320 | 社区关闭 | 15.39 | 27.18 | 24.85 | 37.05 |
| -320 | 社区开启 | 146.02 | 91.94 | 96.60 | 437.73 |

### 已通过

- 默认 `minY=-64` 时，即使配置 `ore_fix=true`，四个目标矿物与关闭组的均值比都在 `0.999..1.000`，满足无效果要求。
- `minY=-320` 时，关闭组 diamond/gold 在深层分别有 `10/16`、`15/16` 个均值为零的桶；开启后四种目标矿物都是 `0/16` 个零桶。
- `minY=-128` 时，gold 从 `3/4` 个深层零桶变为 `0/4`。
- 50 份服务端日志合计：ERROR `0`、FATAL `0`、异常 `0`、Codec/注册表错误 `0`。

### 尚未通过

`minY=-320` 的开启/关闭总量倍率为：

| family | 倍率 |
|---|---:|
| diamond | 9.485× |
| gold | 3.383× |
| lapis | 3.888× |
| redstone | 11.815× |

红石在 16 个深层桶中的均值为每区块 `23.79`，最小 `20.54`、最大 `25.12`、CV `4.0%`。这排除了循环或偶发爆量：倍率来自上游 `HeightStabilizedCount` 按新增垂直 section 线性增加尝试次数，并叠加扩展深板岩层中更高的有效替换率。它有明确机制解释，但 diamond/redstone 的密度仍明显偏富；上游公开 issue [#498](https://github.com/Apollounknowndev/tectonic/issues/498) 也报告开启该修复后矿石过多。因此本轮不把“总量无失衡”涂成通过，配置继续默认关闭。

严格重复性检查也未通过。相同 seed/config/采样窗的两次批量强加载逐桶向量不同：

- 社区默认高度控制组：目标矿总量差异 `0..0.131%`；
- 原版控制组：目标矿总量差异 `0..0.138%`；
- 原版 coal 的最大差异为 `1.107%`。

原版控制同样变化，说明一次性强加载 256 区块引入了生成顺序或并发噪声，不能据此指控 Tectonic 非确定；反过来也不能把它算作计划要求的严格一致。需要确定顺序的生成器继续验证。

全部 50 个 case 都有一次 `Can't keep up`，所以本数据集只用于分布验证，不用于性能结论。

## 文件

- `acceptance.json`：输入 case ID、逐 CSV/summary SHA-256、精确 chunk 闭区间、矩阵维度和确定性状态
- `ore-distribution-raw.csv`：所有 case 的 family/block 逐 16 格桶原始行
- `ore-distribution-mean.csv`：按场景、高度、family 聚合的五种子均值
- `ore-totals.csv`：每区块总量均值、样本标准差和 `Y < -64` 均值
- `charts/index.md`：30 张 SVG 的索引
- `reproducibility/`：社区与原版相同 seed 重复运行的独立证据

原始世界和运行日志体积过大，保留在被 Git 忽略的 `run/batch-results/`；已提交的 `acceptance.json` 保存每份输入 CSV 的 SHA-256，使汇总来源可核验。

## 复现汇总

在仓库根目录执行：

```powershell
python scripts/analyze_ore_distribution.py aggregate `
  --input run/batch-results/20260903-community-off `
  --input run/batch-results/20260903-community-on-final `
  --input run/batch-results/20260903-vanilla-reference `
  --input E:\Games\Minecraft\Workspace\Tectonic\tectonic-official-3.0.17-benchmark\run\batch-results\20260903-official-3.0.17 `
  --output benchmarks/ore_fix/2026-09-03
```

生成新世界使用 `scripts/benchmark_worldgen.ps1`。脚本会拒绝覆盖已有世界，并在 `finally` 中逐字节恢复 `tectonic.json`、`.bak`、`.invalid` 与 `server.properties`。本页的原始数据由旧版“一次强加载整个 256 区块窗口”流程生成；当前脚本已改为固定 tile/区块顺序、逐区块 FULL 回执、后台池目标 parallelism `1` 和可选周期保存/重启。该参数不是硬单线程保证；新流程的严格重复性复验结果见 `benchmarks/determinism/2026-09-03/README.md`，不能与本页旧流程的数据混作同一生成协议。
