# Changelog

## 3.0.17-backport.1（Candidate）

基于官方 Tectonic 3.0.17 Forge 1.20.1 源码的非官方社区回移版。

### 新增与回移

- Frozen Wasteland 与 Overkill 预设。
- River Ice；River Lanterns 最终频率与加载条件修复。
- Ocean Offset 大陆密度钳制、默认高度与外部数据包高度解耦。
- Region spline 单层水平缓存。
- 可选、默认关闭的扩展深度 `ore_fix`。
- Java 21-only 构建，项目发布类强制 class major 65。

### 修复与硬化

- 配置深复制、不可变运行快照、取消不保存及尽力原子替换。
- 内置数据包幂等注册和稳定顺序。
- 旧区块序列化 section `Y` 范围读取及 exclusive 上界。
- Forge 1.20.1 专属单元测试、资源契约、生产专服、矿物统计、确定顺序压测与规范化世界快照工具。
- 最终 JAR 写入非官方身份、源码来源、Git/baseline、MIT 许可证和 NOTICE，并生成 SHA-256 sidecar。
- 原上传脚本改为 fail-closed，避免向上游官方 Modrinth/CurseForge 项目误发布。

### 已知阻断

- 固定输入仍会产生真实方块状态差异，严格地形确定性未通过。
- `ore_fix` 在极深世界显著富集 diamond/redstone，继续默认关闭。
- Terralith 2.5.4 默认高度新世界、`rocky_mountains` 地表抽样及旧/新区块升级边界已通过；Terratonic 3.1.2 新世界也已通过；JourneyMap 6.0.4 与 Xaero's Minimap 26.4.2 分别通过生产客户端进服测试。`volcanic_crater`、Terratonic 旧世界、Distant Horizons 和完整老世界升级矩阵仍未完成。
- Minecraft-Codev/TinyRemapper 对少数局部变量名的选择不确定，重复构建的整包 SHA-256 尚不稳定。

本版本不得标记 Stable，也不保证可由 Java 17 启动。
