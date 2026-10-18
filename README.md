# Scene

功率历史、FPS 联合记录、帧时间、会话比较／CSV 导出及监视器设置：[使用说明](docs/power-monitoring.md)。

## 实时电池功率

充电页、首页电池监测和两种悬浮监视器显示电池端功率（W）。
迷你监视器固定同时显示温度和功率（例如 `33.8°C +2.10W`），不显示电流，不交替切换。
功率按校准后的电流（mA）× Android 上报电压（V）÷ 1000 计算；正值为充电，负值为放电，无法读取时显示 `--W`。
充电时是电池净输入功率，不是充电器输入功率。

在充电页或首页电池卡片打开电流校准，可选择功率倍率：

| 系统上报的数据 | 倍率 |
| --- | --- |
| 单电芯，或已经匹配的整包电压/电流 | ×1（默认） |
| 串联双电芯：单芯电压 + 物理串联电流 | ×2 |
| 串联双电芯：整包电压 + 单芯电压口径的等效电流 | ×0.5 |

例如，4 V × 1500 mA 使用 ×2，与 8 V × 1500 mA 使用 ×1，均为 12 W。
只凭“双电芯”不能确定倍率，须核对设备的电压、电流上报口径。倍率只改变功率显示，不改写历史电流数据。
确认修改电流单位仍会清除旧的充电速度记录；仅修改功率倍率或取消校准不会清除记录。

计算自检（不需要 Android 设备）：

```powershell
New-Item -ItemType Directory -Force build/power-test | Out-Null
javac -d build/power-test app/src/main/java/com/omarea/utils/BatteryPowerMath.java app/src/test/java/com/omarea/utils/BatteryPowerMathTest.java
java -ea -cp build/power-test com.omarea.utils.BatteryPowerMathTest
```

参考 [HoshinoScene 的电池功率显示](https://github.com/xingyeovo/HoshinoScene-oss)，这里独立实现计算与校准，未移植其反编译代码。

构建使用 JDK 17+、Android SDK 36、NDK 21.0.6113669 和 CMake 3.22.1。
Debug 使用 Android 默认调试签名，不需要 `keystore.properties`；Release 需要自己的签名配置，否则产出未签名 APK。

> Before using this tool, please make sure you have ROOT phone and busybox installed. You'd better be a gaming expert, at least you should also be able to refresh and repair the system. Because some advanced functions in the application may affect the normal startup of the system! ! !
> It has the best experience on devices using [Snapdragon 845/835/821/820] [Exynos8890] processor, and other device functions will be limited.

---

No detailed description

---

**Some app screenshot**

<img src="https://user-images.githubusercontent.com/14274061/138872394-c27b859a-4cf0-4abf-ada8-3210de240ae9.jpg" width="320"> <img src="https://user-images.githubusercontent.com/14274061/138872398-b8475111-1005-46ea-a5fa-b9ccff685eb1.jpg" width="320">
<img src="https://user-images.githubusercontent.com/14274061/138872401-0ac5f127-0bd9-4908-8168-f470b6ad0dfb.jpg" width="320"> <img src="https://user-images.githubusercontent.com/14274061/138872402-e4256e0b-9651-4ddd-99eb-11627344147b.jpg" width="320">
<img src="https://user-images.githubusercontent.com/14274061/138872405-094a32fe-6e44-4e27-a8b4-4fd6c540898c.jpg" width="320"> <img src="https://user-images.githubusercontent.com/14274061/138872384-b98fe241-fbbf-4114-ab4f-df7832b501ce.jpg" width="320">
