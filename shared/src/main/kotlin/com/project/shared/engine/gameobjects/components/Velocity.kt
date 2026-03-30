package com.project.shared.engine.gameobjects.components

import kotlinx.serialization.Serializable

@Serializable
data class Velocity(var dx: Float, var dy: Float) : Component
