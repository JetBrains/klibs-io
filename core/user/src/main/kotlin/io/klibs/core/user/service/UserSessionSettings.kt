package io.klibs.core.user.service

import java.time.Duration

data class UserSessionSettings(
    val idleTtl: Duration,
    val refreshInterval: Duration,
    val absoluteTtl: Duration,
) {
    init {
        require(idleTtl.isPositive) {
            "Session idle TTL must be positive"
        }
        require(refreshInterval.isPositive && refreshInterval < idleTtl) {
            "Session refresh interval must be positive and shorter than the idle TTL"
        }
        require(absoluteTtl >= idleTtl) {
            "Session absolute TTL must be greater than or equal to the idle TTL"
        }
    }
}
