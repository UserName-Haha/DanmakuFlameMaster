# DanmakuFlameMaster

Android 上开源弹幕解析绘制引擎项目。

[![](https://jitpack.io/v/UserName-Haha/DanmakuFlameMaster.svg)](https://jitpack.io/#UserName-Haha/DanmakuFlameMaster)

> 本项目基于 [Bilibili/DanmakuFlameMaster](https://github.com/Bilibili/DanmakuFlameMaster) 进行二次开发，感谢原作者的开源贡献。

## 二次开发新增特性

- **支持 arm64-v8a 架构** - 原生支持 64 位 ARM 处理器
- **支持 Android 15 16KB 页面大小** - 适配 Android 15 新特性，兼容 16KB 内存页面对齐要求
- **弹幕子控件动画支持** - 支持给弹幕内部的子控件单独设置动画效果

## 原版特性

- 使用多种方式 (View / SurfaceView / TextureView) 实现高效绘制
- B站 XML 弹幕格式解析
- 基础弹幕精确还原绘制
- 支持 mode7 特殊弹幕
- 多核机型优化，高效的预缓存机制
- 支持多种显示效果选项实时切换
- 实时弹幕显示支持
- 换行弹幕支持 / 运动弹幕支持
- 支持自定义字体
- 支持多种弹幕参数设置
- 支持多种方式的弹幕屏蔽

## 相关链接

- NDK Bitmap 源码：[Bilibili/NativeBitmapFactory](https://github.com/Bilibili/NativeBitmapFactory)
- 原版项目：[Bilibili/DanmakuFlameMaster](https://github.com/Bilibili/DanmakuFlameMaster)

## 快速开始

### 1. 添加 JitPack 仓库

```groovy
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}

// 或 settings.gradle
dependencyResolutionManagement {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

### 2. 添加依赖

```groovy
dependencies {
    // 核心库
    implementation 'com.github.UserName-Haha.DanmakuFlameMaster:DanmakuFlameMaster:v1.0.0'

    // NDK Bitmap 库 (根据需要选择对应的 ABI)
    implementation 'com.github.UserName-Haha.DanmakuFlameMaster:ndkbitmap-arm64-v8a:v1.0.0'  // 64位
    implementation 'com.github.UserName-Haha.DanmakuFlameMaster:ndkbitmap-armv7a:v1.0.0'    // 32位
}
```

> **提示**：如果只需要支持 64 位设备，可以只引入 `ndkbitmap-arm64-v8a`，减少包体积。

## 关于 16KB 页面大小支持

从 Android 15 开始，系统支持 16KB 内存页面大小配置。本项目的 native 库已针对此特性进行适配，确保在启用 16KB 页面大小的设备上正常运行。

## License

```
Copyright (C) 2013-2015 Chen Hui <calmer91@gmail.com>
Licensed under the Apache License, Version 2.0 (the "License").
```
