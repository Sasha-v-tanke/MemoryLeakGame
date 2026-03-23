package com.project.client.engine.gameobjects.components

class Transform : Component() {
    var x: Float = 0f
    var y: Float = 0f

    var rotation: Float = 0f

    var scaleX: Float = 1f
    var scaleY: Float = 1f

    var speedX: Float = 0f
    var speedY: Float = 0f
    var speedW: Float = 0f

    override fun update(delta: Float) {
        x += speedX * delta
        y += speedY * delta
        rotation += speedW * delta
    }
}