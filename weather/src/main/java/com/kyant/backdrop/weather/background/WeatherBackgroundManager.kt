package com.kyant.backdrop.weather.background

import android.util.Log

/**
 * 模块配置数据类（支持配置注入）
 */
data class ModuleConfig(
    val type: WeatherBackgroundType,
    val enabled: Boolean = true,
    val priority: Int = 0,
    val params: WeatherBackgroundParams = WeatherBackgroundParams(),
    val glowParams: GlowParams = GlowParams(),
    val waterDropParams: WaterDropParams = WaterDropParams()
)

/**
 * 天气背景模块管理器
 * 负责模块的加载、切换、缓存和生命周期管理
 */
class WeatherBackgroundManager {

    companion object {
        private const val TAG = "WeatherBGManager"
        private const val DEFAULT_CACHE_SIZE = 8

        @Volatile
        private var instance: WeatherBackgroundManager? = null

        fun getInstance(): WeatherBackgroundManager {
            return instance ?: synchronized(this) {
                instance ?: WeatherBackgroundManager().also { instance = it }
            }
        }
    }

    /** 已注册的背景模块 */
    private val registeredModules = mutableMapOf<WeatherBackgroundType, WeatherBackground>()

    /** 当前激活的模块 */
    private var currentModule: WeatherBackground? = null

    /** 上一个模块（用于跨淡过渡） */
    private var previousModule: WeatherBackground? = null

    /** 跨淡过渡进度 (0f = 显示上一个, 1f = 显示当前) */
    @Volatile
    var crossfadeProgress: Float = 1f; private set

    /** 跨淡过渡时长（毫秒） */
    var transitionDurationMs: Long = 300L

    /** 模块切换监听器 */
    private val listeners = mutableListOf<OnModuleChangedListener>()

    // ═══════════════════════════════════════════
    // LRU 缓存
    // ═══════════════════════════════════════════

    /** LRU 缓存：按访问顺序排列的模块类型 */
    private val lruCache = object : LinkedHashMap<WeatherBackgroundType, WeatherBackground>(
        DEFAULT_CACHE_SIZE, 0.75f, true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<WeatherBackgroundType, WeatherBackground>?): Boolean {
            return size > DEFAULT_CACHE_SIZE
        }
    }

    /** 模块配置映射 */
    private val moduleConfigs = mutableMapOf<WeatherBackgroundType, ModuleConfig>()

    init {
        registerDefaultModules()
    }

    private fun registerDefaultModules() {
        registerModule(OvercastBackgroundModule())
        Log.d(TAG, "Default modules registered")
    }

    /**
     * 注册背景模块
     */
    fun registerModule(module: WeatherBackground) {
        val type = module.getType()
        registeredModules[type] = module
        lruCache[type] = module
        Log.d(TAG, "Module registered: ${module.getName()} for type $type")
    }

    /**
     * 注销背景模块
     */
    fun unregisterModule(type: WeatherBackgroundType) {
        registeredModules.remove(type)
        lruCache.remove(type)
        Log.d(TAG, "Module unregistered for type $type")
    }

    /**
     * 注入模块配置
     */
    fun applyConfig(config: ModuleConfig) {
        moduleConfigs[config.type] = config
        Log.d(TAG, "Config applied for type: ${config.type}, enabled=${config.enabled}")
    }

    /**
     * 批量预加载指定类型的模块
     * @param types 要预加载的模块类型列表
     */
    fun preload(types: List<WeatherBackgroundType>) {
        types.forEach { type ->
            val module = registeredModules[type]
            if (module != null) {
                module.init()
                lruCache[type] = module
                Log.d(TAG, "Preloaded module: ${module.getName()}")
            } else {
                Log.w(TAG, "Cannot preload unknown type: $type")
            }
        }
    }

    fun getCurrentModule(): WeatherBackground? = currentModule

    fun getModule(type: WeatherBackgroundType): WeatherBackground? {
        // 先查 LRU 缓存
        lruCache[type]?.let { return it }
        return registeredModules[type]
    }

    /**
     * 根据天气代码和云量自动选择并切换背景模块
     */
    fun selectModule(weatherCode: Int, cloudTotal: Int): Boolean {
        // 按 priority 排序选择匹配的模块
        val candidates = registeredModules.values
            .filter { module ->
                val config = moduleConfigs[module.getType()]
                (config?.enabled ?: true) && module.supports(weatherCode, cloudTotal)
            }
            .sortedByDescending { module ->
                moduleConfigs[module.getType()]?.priority ?: 0
            }

        val selectedModule = candidates.firstOrNull()

        if (selectedModule != null) {
            switchTo(selectedModule.getType())
            return true
        }

        Log.w(TAG, "No module found for weatherCode=$weatherCode, cloudTotal=$cloudTotal, falling back to OVERCAST")
        switchTo(WeatherBackgroundType.OVERCAST)
        return false
    }

    /**
     * 切换到指定类型的背景模块（带跨淡过渡动画）
     * @param type 目标模块类型
     * @param durationMs 过渡动画时长（毫秒），默认使用 transitionDurationMs
     * @return 是否成功切换
     */
    fun switchTo(type: WeatherBackgroundType, durationMs: Long = transitionDurationMs): Boolean {
        val targetModule = registeredModules[type]
        if (targetModule == null) {
            Log.e(TAG, "Module not found for type: $type")
            return false
        }

        if (currentModule?.getType() == type) {
            Log.d(TAG, "Already using module: ${targetModule.getName()}")
            return true
        }

        // 记录上一个模块，用于跨淡过渡
        previousModule = currentModule
        currentModule = targetModule

        // 更新 LRU 缓存
        lruCache[type] = targetModule

        // 重置跨淡进度（触发过渡）
        crossfadeProgress = 0f

        Log.d(TAG, "Switching from ${previousModule?.getName()} to ${targetModule.getName()} (transition=${durationMs}ms)")

        // 通知监听器
        listeners.forEach { listener ->
            listener.onModuleChanged(previousModule, targetModule)
        }

        return true
    }

    /**
     * 更新跨淡过渡进度
     * @param progress 0f ~ 1f
     */
    fun updateCrossfadeProgress(progress: Float) {
        crossfadeProgress = progress.coerceIn(0f, 1f)
    }

    /**
     * 获取上一个模块（用于跨淡过渡渲染）
     */
    fun getPreviousModule(): WeatherBackground? = previousModule

    fun switchToNext(): Boolean {
        val types = registeredModules.keys.toList()
        if (types.isEmpty()) return false

        val currentType = currentModule?.getType()
        val currentIndex = if (currentType != null) types.indexOf(currentType) else -1
        val nextIndex = (currentIndex + 1) % types.size

        return switchTo(types[nextIndex])
    }

    fun addOnModuleChangedListener(listener: OnModuleChangedListener) {
        listeners.add(listener)
    }

    fun removeOnModuleChangedListener(listener: OnModuleChangedListener) {
        listeners.remove(listener)
    }

    fun getRegisteredTypes(): Set<WeatherBackgroundType> = registeredModules.keys.toSet()

    fun hasModule(type: WeatherBackgroundType): Boolean = registeredModules.containsKey(type)

    fun getModuleCount(): Int = registeredModules.size

    /**
     * 获取 LRU 缓存大小
     */
    fun getCacheSize(): Int = lruCache.size

    /**
     * 获取指定模块的配置（如无则返回默认配置）
     */
    fun getConfig(type: WeatherBackgroundType): ModuleConfig {
        return moduleConfigs[type] ?: ModuleConfig(type = type)
    }

    /**
     * 清空所有模块和缓存
     */
    fun clear() {
        registeredModules.clear()
        lruCache.clear()
        moduleConfigs.clear()
        currentModule = null
        previousModule = null
        crossfadeProgress = 1f
        Log.d(TAG, "All modules and cache cleared")
    }

    /**
     * 模块切换监听器接口
     */
    interface OnModuleChangedListener {
        fun onModuleChanged(previous: WeatherBackground?, current: WeatherBackground)
    }
}
