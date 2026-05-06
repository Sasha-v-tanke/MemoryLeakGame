package com.project.shared.engine.entities.components

import com.project.shared.engine.entities.OwnerType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("owner")
data class Owner(
    var ownerType: OwnerType
) : Component
