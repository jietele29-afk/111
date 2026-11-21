# NewCustomCuiLianPro v1.2.0 (Paper/Purpur 1.20.4+ 修复版 + 宝石镶嵌系统)

本项目基于原版 1.12.2 淬炼插件 `NewCustomCuiLianPro` 及 Mcayear 的 1.20.4 移植版进行深度修复与重构，**解决了高版本无法正常运行的核心 Bug**，并**完整补回了原版缺失的「宝石镶嵌（开孔/镶嵌/拆卸）」系统**。

---

## 一、已修复的核心 Bug 清单

1. **Paper 1.20.4 异步线程崩溃 (`SyncEffectRunnable` / `ScriptRunnable`)**
   - 原版在 `onEnable()` 中使用 `runTaskTimerAsynchronously` 异步访问 `world.getLivingEntities()`、`spawnParticle()` 以及属性插件 API，在 Paper 1.20.4+ 直接抛出异步实体访问异常。
   - **已修复**：统一改为安全的主线程定时任务；脚本异常按单个脚本隔离，不再导致全服脚本永久禁用；新增玩家退出清理防止内存泄漏。
2. **熔炉配方注册失效与卡顿 (`NewCustomCuiLianPro.setRecipe`)**
   - 原版使用已废弃的 `FurnaceRecipe(ItemStack, MaterialData)` 并循环 `0..maxDurability`（每件装备注册 1500~2000+ 次无 Key 配方），在 1.20.4 触发重复异常且严重拖慢启动。
   - **已修复**：改用标准 `NamespacedKey` 注册，每种材质仅需 1 条配方即可匹配全耐久装备，并在 `/cuilian reload` 时自动清理重建。
3. **保护符监听器直接报空指针 (`ProtectRuneListener`)**
   - 原版在判断物品是否为空之前就调用了 `item.getItemMeta()`，导致玩家在背包右键任何空位（包括取消附加保护符）都会直接抛出 `NullPointerException`；且背包类型判断条件 `!= CRAFTING || != PLAYER` 恒为真。
   - **已修复**：修复空位判断与容器类型判断，克隆物品避免篡改单例数量，并在玩家退出时清理状态。
4. **淬炼核心接口多处 NPE 与逻辑错误 (`CuiLianAPI`)**
   - 修复玩家离线或漏斗熔炼时 `p == null` 调用 `sendTitlePart()` / `getDisplayName()` 导致 NPE。
   - 修复保护符未生效或降星至 0 星时 `toLevel == null` 引发 NPE。
   - 修复 `stone.yml` 未配置目标星级概率时 `.doubleValue()` 引发 NPE。
   - 修复 `getRandom(0, 100) <= chance` 导致概率为 0% 时仍可能成功的 Bug。
   - 修复 `replaceLore` 嵌套循环未 `break` 导致 `IllegalStateException`。
   - 修复降回 0 星后物品名称仍残留 `+0` 的显示问题，并补充下界合金（Netherite）武器/防具中文名与 `items.yml` 默认配置。
   - 修复 `JudgeOffHand` 静态初始化导致修改配置后不生效的问题。
5. **属性插件兼容修复 (`AttributeHook`)**
   - 原版 `pom.xml` 依赖 `SX-Attribute 4.0.0` 与 `AttributePlus 3.x`，但代码仅检测 SX v2/v3 且仅调用 AP v2 接口，导致高版本属性同步完全失效。
   - **已修复**：通过安全反射同时兼容 **AttributePlus v2 / v3** 与 **SX-Attribute v2 / v3 / v4**，且无本地 `lib/*.jar` 硬依赖。
6. **熔炉交互、移星界面与指令修复 (`FurnaceListener` / `MoveLevelHandle` / `Commander`)**
   - 移星 GUI 改用原生 Paper 1.20.4 `InventoryHolder`，彻底摆脱老旧 `LLib` GUI 与 1.12 `STAINED_GLASS_PANE` 兼容问题，并防止堆叠装备刷星及关闭界面吞物品。
   - 修复 `/cuilian stone`、`/cuilian protect` 直接修改模板 `ItemStack` 数量单例的 Bug；新增整数校验、控制台检查、普通玩家 `/cuilian` 帮助查看与完整 Tab 补全。

---

## 二、新增：宝石镶嵌系统 (Gem Socketing)

- **数据持久化**：使用 Paper 1.20.4 原生 `PersistentDataContainer (PDC)` 存储装备槽位数与已镶嵌宝石 ID，同步渲染兼容 AttributePlus / SX-Attribute 的 Lore，**装备淬炼升星/降星或移星时自动保留全部宝石与槽位**。
- **双模式操作体验**：
  1. **可视化 GUI 界面**：`/cuilian gem` 打开 27 格镶嵌/打孔界面（左放装备、中放宝石或打孔符、右实时显示成功率与校验状态，点击执行；关闭界面自动退还物品不吞装备）。
  2. **背包右键快捷操作**：与保护符一致，在背包内右键宝石或打孔符后再右键装备即可快速镶嵌/开孔，右键空位取消。
  3. **宝石拆卸界面**：手持已镶嵌宝石的装备输入 `/cuilian gem remove`，点击对应宝石消耗「宝石拆卸符」将其完好取回。
- **完整自定义配置 (`gem.yml` + `config.yml` + `message.yml`)**：
  - 支持自定义最大槽位数 (`MaxSlots`)、默认槽位数 (`DefaultSlots`)、打孔符成功率、拆卸符成功率。
  - 每颗宝石支持配置：材质、`CustomModelData`、成功率、失败是否碎宝石/碎装备、同类宝石上限 (`MaxSameGem`)、最低淬炼星级要求 (`MinCuiLianLevel`)、适用部位 (`AllowTypes`)、属性词条 (`Attribute`) 与被动药水效果 (`PotionEffect`)。

---

## 三、指令一览

| 指令 | 说明 | 权限 |
| --- | --- | --- |
| `/cuilian` 或 `/cuilian help` | 查看指令帮助 | 全部玩家 |
| `/cuilian moveLevel` | 打开淬炼移星界面 | 全部玩家 |
| `/cuilian gem` | 打开装备宝石镶嵌 / 打孔界面 | 全部玩家 |
| `/cuilian gem remove` | 打开手持装备宝石拆卸界面 | 全部玩家 |
| `/cuilian set <等级>` | 设置手持装备淬炼等级（0 为清除） | OP / `cuilian.admin` |
| `/cuilian stone <ID> <数量> <玩家>` | 给予淬炼石（`/cuilian stones` 查看列表） | OP / `cuilian.admin` |
| `/cuilian protect <等级> <数量> <玩家>` | 给予保护符（`/cuilian protects` 查看列表） | OP / `cuilian.admin` |
| `/cuilian gem list` | 查看所有宝石（点击可直接获取） | OP / `cuilian.admin` |
| `/cuilian gem give <宝石ID> <数量> <玩家>` | 给予指定宝石 | OP / `cuilian.admin` |
| `/cuilian gem punch <数量> <玩家>` | 给予装备打孔符 | OP / `cuilian.admin` |
| `/cuilian gem extractor <数量> <玩家>` | 给予宝石拆卸符 | OP / `cuilian.admin` |
| `/cuilian gem addslot [数量]` | 直接为手持装备增加宝石槽位 | OP / `cuilian.admin` |
| `/cuilian reload` | 重载所有配置文件、配方与宝石 | OP / `cuilian.admin` |

---

## 四、如何编译打包为 JAR

本项目已移除所有本地 `lib/*.jar` 依赖，仅依赖公开的 Paper 1.20.4 API，**只需 JDK 17 + Maven 即可一键打包**：

```bash
mvn clean package
```

编译完成后，插件文件位于 `target/NewCustomCuiLianPro-1.2.0.jar`，直接放入 Paper/Purpur 1.20.4+ 服务器的 `plugins/` 目录即可使用。

> 提示：项目内还附带了 `.github/workflows/build.yml`，若本地未安装 Maven，直接将源码上传至 GitHub 仓库即可在 Actions 页面自动编译并下载成品 JAR。
