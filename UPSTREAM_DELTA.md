# 上游差异与回移决策

## 锚点

| 角色 | 提交/制品 |
|---|---|
| Forge 1.20.1 源码基线 | `5373b2084e461f83bd6e0b5f2fe943e81bd59700`（3.0.17） |
| 3.0.19 首次实现 | `cebbe095209a46051190deff7b247a0dd7e80b9e` |
| 3.0.20 主要实现 | `5c02e8d042a4e0183389e0599ac6a0e4cb3cb4f6` |
| 3.0.21 lantern 修复 | `d023a88d38b443de24979803f273f3cb739fcb7d`；仅摘干净 hunk，不 cherry-pick 整提交 |
| 3.0.22 汇合参考 | `46cc1eb2d5a8764722ed8e91224ab87564953ec7` |
| 当前可审计源码终点 | `34241bdb35acda67b5367d49f354c66c05e098e2`（3.0.25） |

每项回移同时比较 3.0.17、首次引入提交和当前干净形态。高版本 Minecraft、NeoForge、Apollib 与 Lithostitched 1.6 API 不进入 Forge 1.20.1 壳体。

## 决策矩阵

| 上游变化 | 决策 | 1.20.1 实现原则 |
|---|---|---|
| 3.0.18：1.21.11/Clifftree 新 API | `NOT_APPLICABLE` | 基线没有对应 1.20.1 缺陷，不复制高版本注册表与 surface rule |
| 3.0.19：Frozen Wasteland | `REQUIRED` | 扩展旧 `ConfigState`/预设/翻译，选择时深复制 |
| 3.0.19：River Ice | `REQUIRED` | 使用 1.20.1 configured/placed feature、Lithostitched 1.4 modifier 和 `#forge:is_snowy` |
| 3.0.19：River Lanterns | `REQUIRED` | amplitudes 改为 `[2]`，并吸收 3.0.21 删除重复 loader condition 的修复 |
| 3.0.19：Ocean Offset 洪泛 | `REQUIRED` | 给 raw continents 加 `[-1, 2]` clamp，不限制用户输入范围 |
| 3.0.19：默认高度解耦 | `REQUIRED` | 默认 `-64..320` 时 height modifier no-op，保留外部数据包高度 |
| 3.0.20：region cache | `REQUIRED` | 给五个未缓存 region spline 增加一层 `flat_cache`/`cache_2d`；基线 `diamond` 已有同等缓存，不复制上游的重复套娃，也不改数值表达式 |
| 3.0.20：Overkill | `REQUIRED` | 用旧 Schema 可表达字段实现；先修正上游 alternate continents 接线错误 |
| 3.0.20：alternate noise scaling | `DEFERRED` | 上游字段接线有误且首版无必要，不为实验功能扩大配置模型 |
| 3.0.21：lantern 开启崩溃 | `REQUIRED` | 与 lantern 频率作为同一原子变更 |
| 3.0.22：ore_fix | `REQUIRED` | 将 `HeightStabilizedCount` 适配到 1.20.1 Codec/Forge 注册，限定原版矿石资源 |
| 3.0.22：地图模组区块查询 | `NOT_APPLICABLE`（源码 hunk） | 上游修复位于 26.x `SerializableChunkDataMixin`；基线 1.20.1 参数本来就是 `ServerLevel`，继续做黑盒兼容测试 |
| 3.0.22：Lithostitched 依赖 | `REQUIRED` | 加载阶段声明最低 1.4.11，禁止误取 1.21/26.x 制品 |
| 3.0.23：配置清理/JSON5 | `DEFERRED` | 保留稳定 JSON；不引入没有必要的 Apollib/JSON5 依赖 |
| 3.0.23/3.0.25：Back 与深复制 | `REQUIRED` | 在旧 GUI 中实现取消不保存，并吸收 3.0.25 深复制/空值保护 |
| 3.0.24：Sulfur Caves | `NOT_APPLICABLE` | 提交历史无独立 3.0.24，1.20.1 基线也无对应注册内容 |
| 3.0.26：jaggedness | `REQUIRED_IF_APPLICABLE` | 使用资源级差异；通用 spline 控制点从 `0.65` 恢复为 `0.2` |
| 3.0.27：NeoForge 26.x 修复 | `NOT_APPLICABLE` | 加载器和 Minecraft API 均不属于 Forge 1.20.1 |

## 已确认不能整提交摘取的上游点

- `d023a88...` 含已提交的 Git 冲突标记；只使用 lantern JSON 的最终干净语义。
- 3.0.23 起的 Apollib/JSON5 与新 GUI 涉及高版本依赖，不能为一个取消按钮把整套配置栈拖回 1.20.1。
- 3.0.25 当前树仍有损坏的高版本源码和已删除 `ConfigHandler` 的遗留引用，只摘明确的小修复。
- 新版 `WorldgenModifier`、`MapCodec`、`Identifier`、NBT `get*Or` 等 API 均需转换或标记不适用，禁止原样复制。
