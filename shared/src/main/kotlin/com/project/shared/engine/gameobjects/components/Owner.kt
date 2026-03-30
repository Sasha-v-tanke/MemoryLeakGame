package com.project.shared.engine.gameobjects.components

import com.project.shared.engine.OwnerType
import kotlinx.serialization.Serializable

@Serializable
data class Owner(val ownerType: OwnerType) : Component
