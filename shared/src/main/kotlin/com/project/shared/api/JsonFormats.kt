package com.project.shared.api

import kotlinx.serialization.json.Json

object JsonFormats {
    val default = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }
}
