package com.project.shared.engine.entities.components

import kotlinx.serialization.Serializable

@Serializable
data class Velocity(var dx: Float, var dy: Float) : Component
