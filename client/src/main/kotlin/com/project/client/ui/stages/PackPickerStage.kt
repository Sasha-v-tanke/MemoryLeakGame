package com.project.client.ui.stages

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame
import com.project.client.ui.screens.MainScreen
import com.project.shared.engine.entities.units.UnitRegistry

class PackPickerStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {
    override fun buildUI() {
        val root = Table()
        root.setFillParent(true)
        root.pad(20f)
        addActor(root)

        val box = panel()
        root.add(box).grow()

        val title = Label("Deck / Cards", skin).apply {
            setAlignment(Align.center)
            fontScaleX = 1.25f
            fontScaleY = 1.25f
        }

        val cardsTable = Table(skin)
        cardsTable.defaults().pad(8f).growX()

        UnitRegistry.all().forEach { config ->
            val card = panel()

            card.add(Label(config.displayName, skin)).left().row()
            card.add(Label("Role: ${config.role}", skin)).left().row()
            card.add(Label("Cost: ${config.costMemory} Memory / ${config.costCpu} CPU", skin)).left().row()
            card.add(Label("HP: ${config.health} · DMG: ${config.damage} · Speed: ${config.speed.toInt()}", skin)).left().row()

            val gameDesc = Label(config.gameDescription, skin).apply {
                wrap = true
            }
            val techDesc = Label("IT: ${config.techDescription}", skin).apply {
                wrap = true
            }

            card.add(gameDesc).width(680f).left().padTop(6f).row()
            card.add(techDesc).width(680f).left().padTop(4f).row()

            cardsTable.add(card).growX().row()
        }

        val scroll = ScrollPane(cardsTable, skin)
        scroll.setFadeScrollBars(false)

        val backButton = TextButton("Back", skin)

        box.add(title).growX().padBottom(12f).row()
        box.add(scroll).grow().row()
        box.add(backButton).width(220f).height(42f).padTop(12f)

        backButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                game.setScreen(MainScreen(game))
                true
            } else {
                false
            }
        }
    }
}
