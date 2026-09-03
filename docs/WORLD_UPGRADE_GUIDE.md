# 世界升级指南

支持目标是：

```text
官方 Tectonic 3.0.17 + Lithostitched 1.4.11
→ Tectonic 3.0.17-backport.1 社区候选版 + Lithostitched 1.4.11
```

当前升级矩阵尚未完成，因此这是保守测试流程，不是“无缝升级”承诺。

## 开始前

1. 完整关闭客户端或服务端，确认保存结束。
2. 复制整个世界目录到另一个明确路径；不要只备份 `region/`。
3. 另存 `config/tectonic.json`、`serverconfig/`、完整模组清单、Forge 日志和原 JAR SHA-256。
4. 只在副本上替换 Tectonic JAR；保持 Minecraft 1.20.1、Forge 与 Lithostitched 版本不变。
5. 使用 Java 21。这个社区版是 class major 65，Java 17 无法加载。

## 首次启动检查

- 不要同时安装 Tectonic Tweak 1.1.0；它的 v2 density function 会让 Tectonic 3.x 世界在注册表加载阶段失败。
- 首次只加载存档，不改 `min_y`、`max_y`、`ore_fix`、`ultrasmooth` 或数据包组合。
- 检查玩家位置、实体、容器、POI/村民、结构、地图数据、出生点和强加载区块。
- 正常保存并停服，再启动一次确认存档可重复读取。
- 仅在副本中生成旧边界外的新区块，检查高度、河流、海岸、地表、生物群系和结构接缝。

## 配置变化边界

- 修改世界生成设置只影响随后生成的区块，不会改造旧区块。
- 改变 `min_y`/`max_y` 风险最高，必须创建新的备份副本；当前真实扩展高度边界测试尚未完成。
- `ore_fix` 默认关闭。在极深世界中开启会显著富集矿物，不建议现有存档启用。
- 会改变内置数据包拓扑的设置需要完整重启，不是热更新。
- 未知配置字段会在规范化写回时被删除，先阅读 `CONFIG_MIGRATION.md`。

## 回退

不保证社区版保存后的世界可以安全降级回官方 3.0.17。若要回退，只恢复升级前的完整世界副本和配置；不要把已由社区版打开、保存或生成新区块的目录直接交给旧版本。

## 当前验证状态

官方 3.0.17 默认高度基线世界包含 seed `0`、256 个 full chunk、DataVersion 3465，原件始终只读。它的副本已在 Forge 47.4.22 和 Java 21 下用社区候选包连续启动两次，旧区块读取、相邻 `[80,64]` 新区块生成、`BlendingDataMixin` 真实触发、配置新字段补齐与 `.bak`、保存及正常停服均通过；旧 `[79,64]` 和新 `[80,64]` 区块都通过 full/DataVersion/高度扫描，并写入 blending version 1。

第一次黑盒尝试还抓到测试辅助方法不是 `private` 会触发 Mixin 0.8.5 `InvalidMixinException`；生产代码已修复并增加修饰符回归测试，随后从全新基线副本重跑通过。

另一个官方 3.0.17 + Terralith 2.5.4 基线同样包含 seed `0`、256 个 full chunk。社区候选版在独立副本中第一次于 `Done (8.716s)` 可用，生成相邻 `[80,64]` 时真实触发 `BlendingDataMixin`；旧 `[79,64]` 与新 `[80,64]` 均为 full、DataVersion 3465、完整 `-64..320`、`tectonic:blending_version=1`，并都保留 `terralith:cave/mantle_caves` biome。第二次启动于 `Done (6.926s)` 可用，保存停服后两区块 terrain SHA-256 仍分别为 `5a1e2135...623f` 与 `f22c3fca...50ca`。两次运行都未出现 Mixin 注入、Codec、注册表或 ERROR/FATAL 阻断。

扩展高度、有真实玩家/实体/POI/地图数据、大量既有区块、Terratonic 旧世界边界以及尚未定位到的 `volcanic_crater` 特殊地表仍未完成，所以完整 Stable 升级门槛仍为 BLOCKED。
