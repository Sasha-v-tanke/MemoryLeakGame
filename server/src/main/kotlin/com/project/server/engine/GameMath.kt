package com.project.server.engine

import com.project.shared.engine.entities.components.Transform
import kotlin.math.sqrt

object GameMath {
    fun distance(a: Transform, b: Transform): Float {
        return distance(a.x, a.y, b.x, b.y)
    }

    fun distance(ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = ax - bx
        val dy = ay - by
        return sqrt(dx * dx + dy * dy)
    }

    fun moveTowards(
        transform: Transform,
        targetX: Float,
        targetY: Float,
        speed: Float,
        deltaSeconds: Float
    ) {
        val dx = targetX - transform.x
        val dy = targetY - transform.y
        val distance = sqrt(dx * dx + dy * dy)

        if (distance <= 0.01f) return

        val step = speed * deltaSeconds

        if (step >= distance) {
            transform.x = targetX
            transform.y = targetY
            return
        }

        transform.x += dx / distance * step
        transform.y += dy / distance * step
    }
}
