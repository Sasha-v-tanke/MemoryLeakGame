package com.project.client.engine.gameobjects

import com.project.client.engine.gameobjects.components.Component

class GameObject {
    private var name: String = "GameObject"
    private var tag: Tag = Tag.None

    val components = ArrayList<Component>()
}