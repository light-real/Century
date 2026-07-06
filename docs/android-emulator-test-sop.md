# Android 模拟器测试 SOP

本文档记录「百年」App 在本机 Android 模拟器上的标准测试流程。

## 适用环境

- 项目目录：`D:\Data\CodingData\App\Memory`
- Android SDK：`C:\Users\233\AppData\Local\Android\Sdk`
- 模拟器名称：`Pixel_10_Pro_XL`
- App 包名：`com.bainian.memory`
- 启动 Activity：`com.bainian.memory.MainActivity`

## 1. 启动模拟器

如果 Android Studio 已打开，可以在设备列表中选择 `Pixel_10_Pro_XL` 并启动。

也可以在项目目录执行：

```bat
C:\Users\233\AppData\Local\Android\Sdk\emulator\emulator.exe -avd Pixel_10_Pro_XL
```

确认设备在线：

```bat
C:\Users\233\AppData\Local\Android\Sdk\platform-tools\adb.exe devices
```

看到类似下面内容表示模拟器已连接：

```text
List of devices attached
emulator-5554    device
```

## 2. 构建调试包

在项目根目录执行：

```bat
.\gradlew.bat assembleDebug
```

构建成功后，APK 位于：

```text
app\build\outputs\apk\debug\app-debug.apk
```

## 3. 安装 App

```bat
C:\Users\233\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
```

看到 `Success` 表示安装成功。

## 4. 启动 App

```bat
C:\Users\233\AppData\Local\Android\Sdk\platform-tools\adb.exe shell am start -n com.bainian.memory/.MainActivity
```

## 5. 截图留档

推荐每次 UI 调整后截一张图，方便对比。

```bat
C:\Users\233\AppData\Local\Android\Sdk\platform-tools\adb.exe shell screencap -p /sdcard/bainian-home.png
C:\Users\233\AppData\Local\Android\Sdk\platform-tools\adb.exe pull /sdcard/bainian-home.png D:\Data\CodingData\App\Memory\bainian-emulator-home.png
```

截图输出：

```text
D:\Data\CodingData\App\Memory\bainian-emulator-home.png
```

## 6. 常用检查项

- App 是否能正常启动，没有闪退。
- 首屏是否默认进入「日历」。
- 底部导航是否能切换「日历」「时间线」「百年」「我的」。
- 点击某一天是否能选中日期。
- 点击新增按钮是否能打开记录窗口。
- 保存文字记录后，日历日期是否被点亮。
- 时间线是否显示刚保存的记录。
- 百年视图是否统计记录数量。
- 从相册选择图片后，记录卡片是否显示图片。

## 7. 常见问题

### `adb devices` 没有设备

先确认模拟器已经启动。若仍未显示，可重启 adb：

```bat
C:\Users\233\AppData\Local\Android\Sdk\platform-tools\adb.exe kill-server
C:\Users\233\AppData\Local\Android\Sdk\platform-tools\adb.exe start-server
C:\Users\233\AppData\Local\Android\Sdk\platform-tools\adb.exe devices
```

### 安装失败

重新构建后再安装：

```bat
.\gradlew.bat assembleDebug
C:\Users\233\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
```

### App 闪退

查看崩溃日志：

```bat
C:\Users\233\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat -b crash -d
```

## 8. 一键测试命令

模拟器已经启动后，可以在项目根目录直接执行：

```bat
.\gradlew.bat assembleDebug
C:\Users\233\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
C:\Users\233\AppData\Local\Android\Sdk\platform-tools\adb.exe shell am start -n com.bainian.memory/.MainActivity
```
