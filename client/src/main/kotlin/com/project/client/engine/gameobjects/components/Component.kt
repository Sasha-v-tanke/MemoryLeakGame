package com.project.client.engine.gameobjects.components

abstract class Component {
    var enabled: Boolean = true

    open fun start() {}
    open fun update(delta: Float) {}

    open fun show() {}

    open fun stop() {}
}