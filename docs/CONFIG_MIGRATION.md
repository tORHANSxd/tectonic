# 配置迁移说明

配置文件为 `<config-dir>/tectonic.json`。当前实现继续使用上游 `minor_version`，规范化写回值为 `1`；没有虚构额外 schema 字段。

## 新增字段

- `continents.river_ice`：默认 `false`。
- `caves.ore_fix`：默认 `false`；只在 `min_y < -64` 时挂载资源包。
- `global_terrain.min_y` / `max_y`：取代旧 `increased_height` 布尔表达。

Overkill 需要的既有范围调整为：`snow_start_offset` 最大 512，`elevation_boost` 最大 1.6；高度仍必须以 16 对齐，`min_y` 为 `-2032..-64`，`max_y` 为 `256..2032`，且上界大于下界。

## 旧格式映射

V2 主要映射：

- `enabled` -> `general.mod_enabled`
- 根 `snow_start_offset` -> `general.snow_start_offset`
- `terrain_scales.vertical_multiplier` -> `global_terrain.vertical_scale`
- `feature_toggles.increased_height` -> `min_y/max_y`
- `feature_toggles.lava_rivers` -> `global_terrain.lava_tunnels`
- `feature_toggles.underground_rivers` -> `continents.underground_rivers`
- `feature_toggles.monument_offset` -> `oceans.monument_offset`

V1 会先升级到 V2，再进入当前结构。旧 `features.desert_dunes` 没有当前语义承接，会被丢弃。`minor_version < 1` 且 `ultrasmooth=true` 的旧配置会迁移到 `-64..640`。

## 文件行为

| 输入 | 行为 |
|---|---|
| 文件不存在 | 写入当前默认配置 |
| 真正无法解析的 JSON 或非法数值 | 原件复制到单槽 `tectonic.json.invalid`，然后写入修复后的配置 |
| 可解析的旧格式或非规范格式 | 原件复制到单槽 `tectonic.json.bak`，再规范化写回 |
| 已与规范化文本逐字节相同 | 不重写 |
| 正常保存 | 写同目录 `.tmp`，优先原子替换，不支持时回退为普通替换移动 |

`.bak` 和 `.invalid` 都会被后一次操作覆盖，不是历史归档。实现没有文件/目录 `fsync` 或跨进程锁，因此只能称为“尽力原子替换”，不能称为断电事务。

## 必须知道的限制

- 未知字段会被解码器忽略，并在规范化写回时静默删除。需要自定义扩展时先保存原件。
- 很多带默认值的字段在类型错误时会直接回退默认；它们未必生成 `.invalid`。
- 当前格式的 `general` 解码失败时可能落入 V2/V1 兼容分支，导致其他当前字段被按未知键丢弃。
- `minor_version > 1` 目前不会被拒绝，且可能被写回为 `1`；不保证未来 schema 安全。
- 读取或保存权限错误会抛出异常；GUI 目前没有专门的错误页或重试。
- 在已打开世界中确认保存只更新磁盘草稿，下次启动才成为世界生成活跃快照；Cancel/Back 不保存。
- 会改变数据包拓扑的开关必须重启，不能依赖资源 reload 热切换。

自动测试覆盖 malformed JSON、NaN、非法高度、缺失新字段、深复制、延迟激活、Overkill 边界和内容幂等。未知字段、错类型、权限失败、原子回退、跨进程竞争及完整 GUI 生命周期仍未自动化，报告中不冒充已通过。
