# Liquid Glass Weather

基于 [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) 液态玻璃效果库的 Android 天气应用。

## 特性

- 🌤 实时天气数据（和风天气 API）
- 🏙 城市搜索 + GPS 定位
- 🎨 动态天气背景（晴天/阴天/雾天/雪天/雷雨天）
- 🔤 OPPO Sans 4.0 全局字体 + 字体颜色自适应（根据天气类型自动切换）
- 📱 液态玻璃桌面小组件（Canvas 绘制毛玻璃效果）
- ⚙ 高度可定制设置（卡片排序/开关/玻璃参数/逐项颜色自定义）
- 💾 本地同日缓存

## 技术栈

- Kotlin + Jetpack Compose
- [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) — 液态玻璃效果库
- 和风天气 API（天气 + GeoAPI 反向地理编码）
- 零第三方网络库（纯 HttpURLConnection + org.json）
- Android Canvas — 小组件毛玻璃渲染

## 截图

> 待补充

## 安装

从 [Releases](https://github.com/xfl1996/LiquidGlassWeather/releases) 下载最新 APK 安装即可。

## 致谢

- **kyant** — 提供了惊艳的 Compose 液态玻璃 backdrop 效果库
- **OPPO Sans 4.0** — 优质中文字体
- 天气数据来源：[和风天气](https://dev.qweather.com/)

## License

MIT
