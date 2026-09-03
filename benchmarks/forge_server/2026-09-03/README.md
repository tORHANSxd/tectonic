# Forge 1.20.1 生产专服与 issue #520 证据

## 范围

本轮只验证最终重映射 JAR 在真实 Forge dedicated-server classpath 上的 Java 21 启动、Mixin 严格检查、服务端客户端类隔离，以及 [上游 issue #520](https://github.com/Apollounknowndev/tectonic/issues/520) 的最小复现。`BlendingDataMixin` 的旧/新区块交界覆盖和 [issue #473](https://github.com/Apollounknowndev/tectonic/issues/473) 的 20,000 区块压力测试不在本轮冒充完成。

## 固定输入

| 输入 | 校验值 |
|---|---|
| Forge 47.4.10 installer | SHA-1 `66bfea9963bfa60d88bab6b2750e74a958392715`，与 Forge Maven 同名 `.sha1` 一致 |
| Forge 47.4.22 installer | SHA-1 `afef69499ecb9c712e738d38aa679bd9ed1f3468`，与 Forge Maven 同名 `.sha1` 一致 |
| Tectonic Java 21 JAR | SHA-256 `187e6284b3845b0869f0c9facfd43f845abfcacdaa7cd3ca0a998d67d188bb23` |
| Lithostitched 1.4.11 | SHA-256 `c19a5a36c0e6cb3782cf7ca5b9648fb1bce5fc41fd737bed423a1f4971bccf75` |
| Tectonic Tweak 1.1.0 | SHA-256 `e9ab8e7ba0bb71ad1236cc694f405b899dfcaff442f276c3b1a6f4d233fbf76e` |
| Java | Eclipse Temurin `21.0.12.1+1-LTS`，`C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` |

Tectonic 构建前执行：

```powershell
gradlew.bat --no-daemon clean forge1201UnitTest forge1201RemapJar --stacktrace --warning-mode all --console=plain
```

结果为 272 个测试全部通过，`javap -verbose` 确认项目类 major 65。

两版专服使用相同审计属性：

```text
-Dmixin.debug.verbose=true
-Dmixin.debug.countInjections=true
-Dmixin.debug.verify=true
-Dmixin.debug.strict=true
-Dmixin.checks.interfaces=true
-Dmixin.checks.interfaces.strict=true
-Dmixin.dumpTargetOnFailure=true
-Dmixin.debug.export=true
-Dmixin.debug.export.filter=net.minecraft.**
```

`-Dmixin.audit=true` 不存在于 Mixin 0.8.5，因此没有伪造这个开关。

## 生产专服结果

| Forge | `Done` | FULL 后解除强加载 | `save-all flush` | 正常退出 | 阻断模式 | major 65 启动警告 | 已加载 Tectonic mixin | 客户端引用 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 47.4.10 | 1 | 1 个区块 | 1 | 1 | 0 | 14 | 14 | 0 |
| 47.4.22 | 1 | 225 个区块 | 1 | 1 | 0 | 14 | 14 | 0 |

阻断模式包括 `ERROR`、`FATAL`、`InvalidInjection`、`InjectionError`、`Mixin apply failed` 和未绑定注册表值。客户端引用扫描包括 `WorldCreationUiStateMixin`、`TectonicLexforge$ModBusEvents` 和 `dev.worldgen.tectonic.client`。

`BlendingDataMixin` 在两次普通世界运行中均未加载，故当前结论是 14/15 个服务端 mixin 获得运行覆盖，而不是 15/15。

原始日志保留在本地忽略目录，散列如下：

| 场景 | latest.log SHA-256 | debug.log SHA-256 |
|---|---|---|
| Forge 47.4.10 clean | `50fd54c69e2aac80bede7e47d14dd5c7cc8ffc5c2ea0e42d0d29d95503777b8c` | `92709e4c60310d15ef2a581b61f0da574409644338f67783c49d085fa15b283f` |
| Forge 47.4.22 clean | `7c62ad1df665fd4dfea1350a40884afd815c52c7c94e6d9b4399a0ffe73656f9` | `a071dfd662faab7c772ff25d547ffd5e45570f0e3e255de09d75e7c8b94c2d9a` |
| Forge 47.4.22 + Tectonic Tweak | `6be60c5462dcc290e7859a18bc88d2f9bddcc029949dceb481adb4a16987ada5` | `025199aa2553d8d79d9733c28f75252019d4982da53e1a18e9b7f126db266423` |
| 移除 Tectonic Tweak 后 | `5d018594e91b56a7da83c807c776dded999858a4f732cb215feafe86be1cb4fc` | `25e931e3d3b8b13afae2041903a69e5eaab6f8b46e8df6fcc27f2e53099b1e66` |

## issue #520 最小复现与因果对照

唯一变量是是否安装来自 CurseForge 文件 5267676 的 `tectonic_tweak-1.1.0.jar`：

1. Tectonic + Lithostitched：Forge 47.4.22 到达 `Done`。
2. 加入 Tectonic Tweak 1.1.0：加载其内置数据包后出现以下错误，未到达 `Done`。
3. 移除 Tectonic Tweak：同一服务端、同一世界再次到达 `Done` 并正常停服。

```text
Unbound values in registry ResourceKey[minecraft:root / minecraft:worldgen/density_function]:
[tectonic:overworld/caves, tectonic:overworld/depth,
 tectonic:overworld/legacy/cliffs, tectonic:overworld/sloped_cheese,
 tectonic:overworld/underground_river/total]
```

附加模组的 `META-INF/mods.toml` 声明 `tectonic = "[2.1,)"`，而它覆盖的 `final_density.json` 直接引用上述五个 Tectonic v2 路径。该范围错误地把 3.x 也判成兼容。Forge 在这次注册表失败后仍返回进程退出码 0，因此自动化必须以 `Done`/明确错误状态判断成功，不能只看退出码。

Ponytail 结论：不写五个语义未知的兼容占位资源。最小且正确的处理是记录已知不兼容组合，并要求移除 Tectonic Tweak 1.1.0。
