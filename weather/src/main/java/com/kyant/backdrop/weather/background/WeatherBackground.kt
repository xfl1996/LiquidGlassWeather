package com.kyant.backdrop.weather.background

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * 天气背景模块接口
 * 所有天气背景效果（阴天、晴天、雨天等）都必须实现此接口
 */
interface WeatherBackground {

    /**
     * 获取天气背景类型
     */
    fun getType(): WeatherBackgroundType

    /**
     * 获取背景名称（用于调试和日志）
     */
    fun getName(): String

    // ═══════════════════════════════════════════
    // 标准生命周期方法
    // ═══════════════════════════════════════════

    /**
     * 初始化模块（模块首次加载时调用）
     */
    fun init() {}

    /**
     * 更新模块状态（每帧调用）
     * @param deltaTime 距离上一帧的时间（秒）
     */
    fun update(deltaTime: Float) {}

    /**
     * 销毁模块，释放资源
     */
    fun destroy() {}

    /**
     * 模块恢复时调用（从后台返回前台）
     */
    fun onResume() {}

    /**
     * 模块暂停时调用（从前台进入后台）
     */
    fun onPause() {}

    /**
     * 模块销毁时调用（与 destroy 类似，但在生命周期末尾）
     */
    fun onLifecycleDestroy() {}

    // ═══════════════════════════════════════════
    // 天气条件匹配
    // ═══════════════════════════════════════════

    /**
     * 是否支持当前天气条件
     * @param weatherCode 和风天气天气代码
     * @param cloudTotal 云量（0-100）
     * @return true表示支持此天气条件
     */
    fun supports(weatherCode: Int, cloudTotal: Int): Boolean

    // ═══════════════════════════════════════════
    // 渲染方法
    // ═══════════════════════════════════════════

    /**
     * 返回背景层渲染的 Composable
     * 此方法应在 WeatherScreen 的最底层调用
     * @param modifier 修饰符
     * @param params 背景参数（含屏幕尺寸）
     */
    @Composable
    fun BackgroundLayer(
        modifier: Modifier = Modifier,
        params: WeatherBackgroundParams = WeatherBackgroundParams()
    )

    /**
     * 返回光晕层渲染的 Composable
     * 此方法应在背景层之上、卡片层之下调用
     * @param modifier 修饰符
     * @param params 光晕参数
     */
    @Composable
    fun GlowLayer(
        modifier: Modifier = Modifier,
        params: GlowParams = GlowParams()
    )

    // ═══════════════════════════════════════════
    // 性能监控
    // ═══════════════════════════════════════════

    /**
     * 获取帧率统计数据
     * @return 帧率统计信息
     */
    fun getFrameStats(): FrameStats = FrameStats()

    /**
     * 获取内存使用数据
     * @return 内存使用信息
     */
    fun getMemoryUsage(): MemoryStats = MemoryStats()
}

/**
 * 帧率统计数据
 */
data class FrameStats(
    /** 平均帧率（FPS） */
    val avgFps: Float = 0f,
    /** 最小帧率 */
    val minFps: Float = 0f,
    /** 最大帧率 */
    val maxFps: Float = 0f,
    /** 掉帧次数 */
    val droppedFrames: Int = 0,
    /** 总帧数 */
    val totalFrames: Int = 0
)

/**
 * 内存使用统计数据
 */
data class MemoryStats(
    /** 当前内存使用（字节） */
    val currentMemoryBytes: Long = 0L,
    /** 峰值内存使用（字节） */
    val peakMemoryBytes: Long = 0L,
    /** 动画缓存大小（字节） */
    val animationCacheBytes: Long = 0L
)

/**
 * 天气背景类型枚举
 */
enum class WeatherBackgroundType {
    OVERCAST,   // 阴天
    CLOUDY,     // 多云
    SUNNY,      // 晴天
    RAINY,      // 雨天
    SNOWY,      // 雪天
    STORMY,     // 雷暴
    FOGGY,      // 雾天
    UNKNOWN     // 未知天气
}

/**
 * 天气背景参数
 */
data class WeatherBackgroundParams(
    /** 主色调 */
    val primaryColor: Color = Color(0xFF4a5f7a),
    /** 亮色调 */
    val lightColor: Color = Color(0xFF8a9bb5),
    /** 暗色调 */
    val darkColor: Color = Color(0xFF2a3548),
    /** 模糊强度（像素） */
    val blurRadius: Float = 50f,
    /** 动画周期（毫秒） */
    val animationDuration: Int = 12000,
    /** 是否启用动画 */
    val enableAnimation: Boolean = true,
    /** 屏幕宽度（像素） */
    val screenWidth: Int = 1080,
    /** 屏幕高度（像素） */
    val screenHeight: Int = 1920
)

/**
 * 光晕参数
 */
data class GlowParams(
    /** 冷色光晕颜色 */
    val coolGlowColor: Color = Color(0x14B4C8E6),  // rgba(180, 200, 230, 0.08)
    /** 暖色光晕颜色 */
    val warmGlowColor: Color = Color(0x0AB4B4A0),  // rgba(200, 180, 160, 0.04)
    /** 冷色光晕直径（dp） */
    val coolGlowSize: Float = 700f,
    /** 暖色光晕直径（dp） */
    val warmGlowSize: Float = 600f,
    /** 冷色光晕模糊（dp） */
    val coolGlowBlur: Float = 80f,
    /** 暖色光晕模糊（dp） */
    val warmGlowBlur: Float = 70f,
    /** 冷色光晕动画周期（毫秒） */
    val coolGlowDuration: Int = 25000,
    /** 暖色光晕动画周期（毫秒） */
    val warmGlowDuration: Int = 30000,
    /** 是否启用动画 */
    val enableAnimation: Boolean = true
)

/**
 * 水滴参数
 */
data class WaterDropParams(
    /** 水滴颜色 */
    val dropColor: Color = Color(0x59FFFFFF),  // rgba(255, 255, 255, 0.35)
    /** 水滴数量 */
    val dropCount: Int = 4,
    /** 水滴动画周期（毫秒） */
    val animationDuration: Int = 4000,
    /** 是否启用动画 */
    val enableAnimation: Boolean = true
)
