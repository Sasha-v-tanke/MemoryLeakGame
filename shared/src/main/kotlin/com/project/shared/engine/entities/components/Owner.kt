package com.project.shared.engine.entities.components

import com.project.shared.engine.entities.OwnerType
import kotlinx.serialization.Serializable

@Serializable
data class Owner(val ownerType: OwnerType) : Component
