package com.hapticrace.app.haptic

import kotlinx.serialization.Serializable

@Serializable
enum class HapticEventType(
    val title: String,
    val description: String,
    val defaultDurationMs: Long,
    val defaultIntensity: HapticIntensity
) {
    ENGINE(
        title = "ENGINE",
        description = "Subtle low-frequency repeating rumble",
        defaultDurationMs = 220L,
        defaultIntensity = HapticIntensity.LOW
    ),
    GEAR_SHIFT(
        title = "GEAR SHIFT",
        description = "Short sharp snappy kick",
        defaultDurationMs = 45L,
        defaultIntensity = HapticIntensity.HIGH
    ),
    BRAKE(
        title = "BRAKE",
        description = "Firm decelerating vibration",
        defaultDurationMs = 180L,
        defaultIntensity = HapticIntensity.MEDIUM
    ),
    KERB(
        title = "KERB",
        description = "Rapid stutter pulses over rumble strip",
        defaultDurationMs = 240L,
        defaultIntensity = HapticIntensity.MEDIUM
    ),
    IMPACT(
        title = "IMPACT",
        description = "Heavy multi-stage shock collision",
        defaultDurationMs = 320L,
        defaultIntensity = HapticIntensity.HIGH
    ),
    CUSTOM(
        title = "CUSTOM",
        description = "User-customized pulse duration and intensity",
        defaultDurationMs = 150L,
        defaultIntensity = HapticIntensity.MEDIUM
    )
}
