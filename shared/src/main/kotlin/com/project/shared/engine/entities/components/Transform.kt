package com.project.shared.engine.entities.components

import kotlinx.serialization.Serializable

@Serializable
data class Transform(var x: Float, var y: Float) : Component
