# Forge 1.20.1 回移架构

## 源集边界

仓库是一个 Gradle 根项目，Cloche 提供虚拟源集：

```text
common
  └─ shared:1.20.1
       └─ forge:1.20.1
```

- `src/common/main`：配置模型、密度函数、通用资源与跨版本 Mixin。
- `src/shared/1.20.1/main`：Minecraft 1.20.1 方法签名、NBT/GUI 适配与专用 Mixin。
- `src/forge/1.20.1/main`：Forge 入口、注册表、配置界面扩展与内置数据包选择。
- `src/common/main/resources/resourcepacks/tectonic`：保留 1.20.1 schema 与注册表 ID 的数据驱动世界生成。

高版本 Fabric/NeoForge 目标仍存在于上游构建文件，但不是该分支的验收目标。Forge 1.20.1 使用独立 `forge1201UnitTest`，避免根 `build/test` 把无关目标的 Cloche 中间产物拖进来。

## 启动与世界生成链

```text
TectonicLexforge
  -> ConfigHandler.load
  -> 注册 Codec / Placement / Lithostitched modifier
  -> 按启动快照选择内置 SERVER_DATA packs
  -> TectonicRepositorySource 按 ID first-wins 发布不可变列表
  -> PackRepositoryMixin 注入仓库源并保留顺序
  -> 数据包解码 minecraft:overworld 覆盖项
  -> density graph / Lithostitched modifiers
  -> 运行期 Mixin 修补边界行为
```

基础包与 `overlay.mod` 在启用 Tectonic 时固定加入；Terralith、Ultrasmooth、No Carvers、Ore Fix 由启动配置或模组存在性决定。它们是 `SERVER_DATA`、`Position.TOP`、`BUILT_IN` 包。

## 配置并发模型

配置有两层：受 JVM 内锁保护的磁盘草稿 `fileState`，以及通过 `volatile` 发布的不可变 `ConfigSnapshot`。世界生成解码时会把许多配置值捕获到不可变 record，热采样不重复解析 JSON；少数 Mixin 仍按调用读取当前快照。

世界内保存使用 deferred 模式，只落盘而不替换当前世界快照。这个设计避免一半已生成任务看到旧配置、一半看到新配置，但不构成跨进程写入锁。

## 数据与缓存

Tectonic 覆盖 `minecraft:overworld` noise settings，并组合自身 terrain/cave/river density graph。默认高度时 `SetHeightLimitsModifier` no-op；非默认高度才改写维度与噪声上限。

Region spline 使用一层 `flat_cache -> cache_2d -> spline`。`ore_fix` 是默认关闭的独立 overlay，只在扩展深度时启用；它按垂直 section 数线性增加 placed-feature 尝试数，因而深世界的成本和密度都必须单独审计。

## 风险边界

- 包位于 `minecraft` 命名空间并置顶；与其他世界生成数据包发生同 ID 冲突时，结果依赖包顺序。
- 数据包拓扑开关在 Forge 入口构造期间决定，不是热更新功能。
- `HeightmapMixin` 全局替换 Heightmap logger 为 NOP，可能隐藏其他模组的诊断信息。
- `WorldCarverMixin` 虽在 required mixin 配置中，当前注入逻辑已被注释；“加载到类”不等于行为覆盖。
- Forge 1.20.1 的 Mixin 0.8.5 对 major 65 发出启动警告，ASM 实测能处理，但这是已知工具链边界。
- `BlendingDataMixin` 只有旧/新区块交界才触发。section 范围与 Mixin 私有方法约束已修复，官方默认高度及官方 + Terralith 2.5.4 副本黑盒通过；扩展高度和 Terratonic 旧世界升级仍未完成。
- `max.bg.threads=1` 是目标 parallelism，不保证 Vanilla 绝对单线程；严格快照已发现真实装饰差异。
