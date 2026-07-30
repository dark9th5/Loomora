package com.loomora.core.offlineai

internal class SingleEntryResourceCache<K, V>(
    private val release: (V) -> Unit
) {
    private var entry: Pair<K, V>? = null

    @Synchronized
    fun getOrCreate(key: K, create: () -> V): V {
        entry?.takeIf { it.first == key }?.let { return it.second }
        entry?.second?.let(release)
        return create().also { entry = key to it }
    }

    @Synchronized
    fun clear() {
        entry?.second?.let(release)
        entry = null
    }
}
