# HoshinoScene 功能移植评估

参考快照：HoshinoScene-oss `de840c7`；对照本项目 `faeab04` 加本地功率显示改动。
本次是源码与资源评估，未运行参考应用，也未验证其守护进程和内核功能。

## 优先顺序

| 优先级 | 功能 | 当前差距与最小实现 | 成本 |
| --- | --- | --- | --- |
| 1 | 充电会话与功率曲线 | 参考项目 ChargeStatRecord 保存时间、电流、功率、温度，ChargeStatSession 带 Wh。当前 ChargeSpeedStore 主要记录电流。建议逐次保存采样电压、功率和倍率，新增功率曲线、平均功率与积分 Wh；旧记录不能用当前电压补算。 | 中 |
| 2 | FPS 与功率联合记录 | 参考 ActivityFpsSession 有功率、帧时间、Jank 等维度。当前 FpsWatchStore 已有会话、FPS、CPU/GPU 负载与温度，可先追加功率字段和同一时间轴，给出平均功率和能量。数据库应增量迁移，保留旧会话。 | 中 |
| 3 | 监视器项目与刷新间隔设置 | 参考 ActivityMonitorDesigner 支持显示项目、缩放、刷新间隔；当前 DialogMonitor 主要切换整种监视器。先做项目勾选、字号/缩放、刷新间隔，复用现有监视器，不引入完整设计器。 | 低至中 |
| 4 | 循环次数与容量信息 | 参考 layout_battery_status 展示 cycle、size。当前 BatteryUtils 已能读取 cycle_count、charge_full、charge_full_design，可直接把已有信息呈现在充电页。双电芯容量必须先核对口径，不能直接当成电池健康度百分比。 | 低 |
| 5 | 逐帧时长、Jank、会话比较/导出 | 参考 FpsData 有 FrameTimeMS、Jank、BigJank，ActivityFpsSession 有比较/导出入口。当前 GfxInfoFpsUtils 读取完成时间但只向外提供 FPS；需保留逐帧数据并验证游戏 SurfaceView/Vulkan 覆盖与高刷新率阈值。不能由每秒 FPS 反推逐帧卡顿。比较/导出也需要单独实现和验证。 | 高 |

第一阶段建议做 1 和 2，直接服务充电与游戏功耗分析；3、4 可以独立追加。

## 暂不移植

- 充电器输入功率估算：参考 `a/fr.java` 有固定 0.92 效率、1.2 补偿及 `W?` 输出。反编译表达式还有异常，不适合作为可验证的输入功率读数。
- 完整反编译 Java 包：参考 `ActivityFpsSession.onCreate()` 仍直接抛出 `UnsupportedOperationException`。界面或字段存在不等于功能可用，应借鉴交互和数据结构后用本项目结构重写。
- 调度守护进程、内核探针、厂商温控控制：本轮未验证其执行链、机型适配和运行开销，不作为近期可直接移植功能。
- CPU/GPU 调频和温控整套页面：本项目已有相应控制能力，先针对具体缺失补齐，整套搬入收益有限。

## 功率方向核对

计算保持校准电流的符号：`rawCurrent / divisor × voltage × positiveFactor / 1000`。
约定为净充电正、净放电负；×1、×2、×0.5 都不会翻转符号。
不依据 USB 是否插入强制改符号，因为接电时电池仍可能净放电。
本项目继承的默认电流校准依赖厂商、采样大小和当时充放电状态，不能保证所有机型自动正确。

只读检查连接的 CPH2655：电池状态为 Charging，原始 battery/uevent 中 CURRENT_NOW=-109，VOLTAGE_NOW=4411000。
这说明该底层节点的充电电流方向是负，但节点不等于应用读取的 BatteryManager API，不能据此确认应用当前的单位除数或功率倍率。
原始输出保存在 build/battery-evidence/。未切换充电状态、未安装 APK、未读取或修改已安装应用配置。

## 对照源码

- `.reference/HoshinoScene/app/src/main/java/com/omarea/model/ChargeStatRecord.java`
- `.reference/HoshinoScene/app/src/main/java/com/omarea/model/ChargeStatSession.java`
- `.reference/HoshinoScene/app/src/main/java/com/omarea/model/FpsData.java`
- `.reference/HoshinoScene/app/src/main/java/com/omarea/vtools/activities/ActivityFpsSession.java`
- `.reference/HoshinoScene/app/src/main/java/com/omarea/vtools/activities/ActivityMonitorDesigner.java`
- `.reference/HoshinoScene/app/src/main/res/layout/layout_battery_status.xml`
- `.reference/HoshinoScene/app/src/main/java/a/fr.java`
