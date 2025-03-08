package com.elena.autoplanner


object FeatureFlags {
    val ENABLE_TASK_SEEDING = true
}


inline fun <T> withFeature(featureEnabled: Boolean, block: () -> T): T? {
    return if (featureEnabled) block() else null
}