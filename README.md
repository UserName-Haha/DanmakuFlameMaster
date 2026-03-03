DanmakuFlameMaster
==================

android上开源弹幕解析绘制引擎项目。

[![](https://jitpack.io/v/UserName-Haha/DanmakuFlameMaster.svg)](https://jitpack.io/#UserName-Haha/DanmakuFlameMaster)

### DFM Inside: 
[![bili](https://raw.github.com/ctiao/ctiao.github.io/master/images/apps/bili.png?raw=true)](https://play.google.com/store/apps/details?id=tv.danmaku.bili)

- libndkbitmap.so(ndk)源码：https://github.com/Bilibili/NativeBitmapFactory
- 开发交流群：314468823 (加入请注明DFM开发交流)

### Features

- 使用多种方式(View/SurfaceView/TextureView)实现高效绘制

- B站xml弹幕格式解析

- 基础弹幕精确还原绘制

- 支持mode7特殊弹幕

- 多核机型优化，高效的预缓存机制

- 支持多种显示效果选项实时切换

- 实时弹幕显示支持

- 换行弹幕支持/运动弹幕支持

- 支持自定义字体

- 支持多种弹幕参数设置

- 支持多种方式的弹幕屏蔽

### TODO:

- 增加OpenGL ES绘制方式


### Download

**Step 1.** Add JitPack repository to your build file

```groovy
// settings.gradle 或 root build.gradle
repositories {
    maven { url 'https://jitpack.io' }
}
```

**Step 2.** Add the dependency

```groovy
dependencies {
    implementation 'com.github.UserName-Haha.DanmakuFlameMaster:DanmakuFlameMaster:v1.0.0'

    // 选择对应的 ABI
    implementation 'com.github.UserName-Haha.DanmakuFlameMaster:ndkbitmap-arm64-v8a:v1.0.0'
    implementation 'com.github.UserName-Haha.DanmakuFlameMaster:ndkbitmap-armv7a:v1.0.0'
}
```

### License
    Copyright (C) 2013-2015 Chen Hui <calmer91@gmail.com>
    Licensed under the Apache License, Version 2.0 (the "License");
