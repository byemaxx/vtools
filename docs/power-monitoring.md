# 功率历史与监测

## 使用入口

- **充电页主图 Charging power (W)**：直接显示最近一次充电会话的功率随时间变化，并给出时间加权平均功率、净 Wh 和有效记录时长。曲线保留正负方向，不再绘制电流图。旧电流记录不用于补算功率。
- **充电页 → Power curve / charging sessions**：选择其他充电记录，切换功率、温度曲线；查看时间加权平均功率、净 Wh 和有效功率数据覆盖率。
- **FPS 图表页 → FPS / power / frame times / compare / export**：查看新录制会话的 FPS、功率、温度和帧时间。录制仍使用现有 FPS 悬浮按钮。
- **Session A / Compare with B**：同时选择两段记录，橙色为 A、蓝色为 B，按录制开始后的相对时间对齐。指标下拉框切换曲线。
- **Export selected session(s) as CSV**：通过系统文件选择器保存一份 CSV，包含采样值与逐帧记录。帧时间图显示每 100 ms 的峰值，CSV 保留所有采集到的帧间隔。
- **监视器选择窗口 → Monitor settings**：选择 CPU、GPU、FPS、电池显示项，调整大小、500–5000 ms 刷新间隔及展开状态。保存后正在显示的普通／迷你监视器会重新应用设置。
- **充电页电池信息卡**：显示设备报告的循环次数、满充容量和设计容量。无法取得时显示 `--`；不把这些数字直接转换为健康度百分比。

迷你监视器的电池部分始终同时显示温度和功率，不显示电流。

## 数据含义

功率 = 校准电流 × 最新已知电压 × 功率倍率。正值表示净充入电池，负值表示净放电。
电压、温度来自 Android 电池状态，属于最新已知读数；并非独立仪器的同步测量。
每条新记录保留原始电流、单位除数、电压、倍率及计算功率；修改校准不会重算旧记录。

能量按相邻功率样本梯形积分，单位 Wh；平均 W = 有效积分 Wh ÷ 有效记录小时数。
无效功率以及超过 10 秒的采样间隔不参与积分，图表也不跨该缺口连线。覆盖率显示参与积分的时长比例。
净 Wh 可以为负；它不是充电器的输入电量，也不是应用自身的独占能耗。

新增记录位于独立的 `monitor_telemetry` 数据库，不修改原来的充电／FPS 数据库结构。
旧记录继续从原页面查看，没有电压和功率的旧记录不会补造功率。
充电采样沿用现有后台监听与初始 15 秒延迟，之后每秒记录一次；会话从首次采样开始。后台进程重启后新建会话，不把断档拼成完整充电周期。
FPS 功率采样每秒一次，不随普通监视器的刷新间隔改变。录制数据不自动清理。

## 帧时间与长帧

帧时间来自 `SurfaceFlinger --latency` 的应用图层实际呈现时间戳之差。
第一次读取只建立基线，不记录录制开始前的旧帧。零值、无效标记、重复时间戳均忽略。
图层消失、未找到图层、读取不支持、没有新时间戳或环形缓冲区被覆盖时，报告显示来源与缺口数量。
观察到的帧间隔仍可保存，但有缺口的计数不能视为完整会话统计。

在 Monitor settings 中于录制前设置游戏目标 FPS（默认 60）：

- 长帧：间隔 **大于 3 个目标帧周期**。
- 严重长帧：间隔 **大于 6 个目标帧周期**，包含在长帧总数中。
- 例如目标 60 FPS 时，阈值分别为 >50 ms 和 >100 ms。

这是本项目明确采用的观察口径，不冒充 Hoshino 或其他工具的标准 Jank 算法。
修改目标 FPS 只影响新会话。FPS 优先采用采集到的帧间隔计算，否则使用原有 FPS 读取路径；读取失败显示不可用，不再伪造 1 FPS。
每秒轮询可能跟不上高刷新率缓冲区，因此不能保证完整捕获所有帧，也不能保证适配所有游戏或 Vulkan 图层。
监测会有额外开销，测量精度需通过真机比较验证。

## 校验

```powershell
javac -d build/power-test app/src/main/java/com/omarea/utils/BatteryPowerMath.java app/src/main/java/com/omarea/utils/TelemetryMath.java app/src/test/java/com/omarea/utils/BatteryPowerMathTest.java app/src/test/java/com/omarea/utils/TelemetryMathTest.java
java -ea -cp build/power-test com.omarea.utils.BatteryPowerMathTest
java -ea -cp build/power-test com.omarea.utils.TelemetryMathTest
& "$env:USERPROFILE/miniconda3/envs/py14/python.exe" scripts/test_telemetry_store.py
.\gradlew.bat --gradle-user-home "$env:USERPROFILE/.gradle" :app:assembleDebug
```

自检覆盖功率符号／倍率、缺失数据、非等间隔 Wh 积分、帧时间去重／基线／丢帧，以及实际 SQLite 建表语句、报表查询、外键和写入回滚。
已在 CPH2655 上通过 ADB 覆盖安装，并核对充电页实际出现 W—时间曲线、平均功率和 Wh；同时修复了 Application 启动早期上下文为空导致后台采样写入失败的问题。
未进行系统文件选择器、游戏运行或外部功率仪精度验收。
