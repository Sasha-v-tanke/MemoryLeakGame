package com.project.shared.api.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("system_message")
data class SystemMessageEvent(
    val message: String,
    val severity: Severity = Severity.INFO
) : Event {
    @Serializable
    enum class Severity {
        INFO,
        WARNING,
        ERROR
    }
}
