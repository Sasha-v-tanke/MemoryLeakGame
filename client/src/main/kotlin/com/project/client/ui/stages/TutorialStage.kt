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
import com.project.client.ui.theme.UiTheme

class TutorialStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {
    override fun buildUI() {
        val root = screenRoot(20f)
        addActor(root)

        val box = panel()
        root.add(box).grow()

        val title = titleLabel("Обучение", 1.10f).apply {
            setAlignment(Align.center)
        }

        val content = Table(skin)
        content.defaults().left().padBottom(8f).padRight(12f)

        val sections = listOf(
            "ОСНОВНАЯ ТЕОРИЯ ИГРЫ" to listOf(
                "Memory Leak Arena - это карточная стратегическая игра про архитектуру систем.",
                "Цель: захватить ресурсные узлы, управлять экономикой и разломать Core противника.",
                "Игра сфокусирована на трёх ресурсах: CPU, Memory и различные узлы на карте."
            ),
            "РЕСУРСЫ И ЭКОНОМИКА" to listOf(
                "CPU - вычислительная мощность, которая питает ваши войска.",
                "Memory - рабочее пространство для процессов; каждый юнит требует выделения.",
                "Узлы (Nodes) - стратегические точки на карте для захвата и контроля.",
                "Вы получаете ресурсы разными юнитами, которые специализируются на захвате CPU или Memory."
            ),
            "РОЛИ ЮНИТОВ" to listOf(
                "Capture - специалисты по захвату ресурсов (Allocator, Buffer, CPU_Scheduler).",
                "Attack - боевые юниты, наносящие урон врагам и структурам (Injector, Coroutine Archer).",
                "Defense - защитники территории, блокирующие вторжения (Thread Guard, Firewall).",
                "Support - вспомогательные юниты усиливают или исцеляют (Patch Healer, Pointer).",
                "Spell - мгновенные эффекты с большой силой и высокой стоимостью (Deadlock, Overclock)."
            ),
            "БАЗОВЫЕ КОНЦЕПЦИИ ПРОГРАММИРОВАНИЯ В КОНТЕКСТЕ" to listOf(
                "Allocator - выделение памяти. Быстрая операция выделения малого блока памяти.",
                "Garbage Collector - сборка мусора. Освобождает недостижимые объекты в памяти.",
                "Thread Guard - синхронизация потоков. Защищает критические секции от одновременного доступа.",
                "Deadlock - взаимная блокировка. Процессы ждут друг друга и система зависает.",
                "Cache Runner - кэширование. Быстрый доступ к часто используемым данным.",
                "Firewall - безопасность системы. Фильтрует вредоносный или неавторизованный трафик.",
                "Exception Handler - обработка ошибок. Предотвращает крах системы из-за исключений.",
                "Coroutine Archer - асинхронность. Работа без блокирования основного потока."
            ),
            "ТИПИЧНАЯ ИГРОВАЯ СТРАТЕГИЯ" to listOf(
                "Ранний этап: захватите несколько Memory и CPU узлов для раннего преимущества в ресурсах.",
                "Середина игры: постройте армию защитников и начните небольшие стычки.",
                "Поздний этап: мобилизуйте большую армию для атаки на Core противника.",
                "Рассмотрите колоду: баланс между захватом, защитой и атакой критичен для успеха."
            ),
            "РЕКОМЕНДАЦИИ ДЛЯ НОВИЧКОВ" to listOf(
                "Сначала сыграйте в Puzzle Lab для понимания механик каждого юнита.",
                "Начните с простой колоды - несколько захватчиков, один защитник и одна атакующая карта.",
                "Наблюдайте за метриками: не давайте противнику слишком большое преимущество в ресурсах.",
                "Коммуникация между картами важна: не разбрасывайте юниты случайно.",
                "Экспериментируйте с разными колодами и находите свой стиль игры."
            )
        )

        sections.forEach { (sectionTitle, sectionContent) ->
            val sectionLabel = titleLabel(sectionTitle, 1.00f).apply {
                color = UiTheme.statusInfo
            }
            content.add(sectionLabel).growX().row()

            sectionContent.forEach { line ->
                val lineLabel = subtitleLabel("• $line", 0.85f).apply {
                    wrap = true
                }
                content.add(lineLabel).width(900f).padLeft(16f).row()
            }

            content.add(Label("", skin)).height(8f).row()
        }

        val scroll = ScrollPane(content, skin)
        scroll.setFadeScrollBars(false)

        val backButton = TextButton("Главное меню", skin)
        UiTheme.stylePrimaryButton(backButton)

        box.add(title).growX().padBottom(12f).row()
        box.add(scroll).grow().row()
        box.add(backButton).height(44f).growX().padTop(8f).row()

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

