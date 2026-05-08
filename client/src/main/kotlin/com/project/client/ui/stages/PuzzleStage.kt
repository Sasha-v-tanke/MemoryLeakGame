package com.project.client.ui.stages

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame
import com.project.client.ui.screens.MainScreen
import com.project.client.ui.theme.UiTheme

class PuzzleStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {
    private enum class LabKind {
        ALLOCATOR_ROUTE,
        MARK_SWEEP,
        FIREWALL_FILTER,
        DEADLOCK_GRAPH,
        PIPELINE_ORDER
    }

    private data class LabState(
        var steps: Int = 0,
        var allocatorSolved: Boolean = false,

        var heapMarked: Boolean = false,
        var staleListenerDetached: Boolean = false,
        var heapSwept: Boolean = false,
        var activeSocketLost: Boolean = false,

        var trafficIndex: Int = 0,
        var firewallErrors: Int = 0,
        var patchApplied: Boolean = false,

        var lockOrderInstalled: Boolean = false,
        var renderHoldsFrameLock: Boolean = true,
        var renderWaitsSocketLock: Boolean = true,
        var networkHoldsSocketLock: Boolean = true,
        var networkWaitsFrameLock: Boolean = true,
        var networkRolledBack: Boolean = false,
        var criticalThreadKilled: Boolean = false,

        var pipeline: MutableList<String> = mutableListOf(),
        var pipelineBroken: Boolean = false
    )

    private data class LabAction(
        val id: String,
        val title: String,
        val operation: String,
        val itMeaning: String
    )

    private data class TrafficCase(
        val request: String,
        val expectedAction: String,
        val reason: String
    )

    private data class LabLevel(
        val name: String,
        val unitBasis: String,
        val kind: LabKind,
        val incident: String,
        val objective: String,
        val maxSteps: Int,
        val initial: () -> LabState,
        val actions: List<LabAction>,
        val isSolved: (LabState) -> Boolean,
        val isFailed: (LabState) -> Boolean
    )

    private val trafficCases = listOf(
        TrafficCase(
            request = "GET /state с валидным токеном",
            expectedAction = "firewall_allow",
            reason = "Только чтение с аутентификацией должно пройти."
        ),
        TrafficCase(
            request = "POST /inject с неизвестным токеном",
            expectedAction = "firewall_block",
            reason = "Команда, похожая на Injector от неизвестной идентичности, должна быть заблокирована."
        ),
        TrafficCase(
            request = "PATCH /core/hotfix подписано владельцем",
            expectedAction = "firewall_patch",
            reason = "Доверенный патч должен быть применен, а не считаться враждебной инъекцией."
        ),
        TrafficCase(
            request = "DELETE /match из replay бота",
            expectedAction = "firewall_block",
            reason = "Боты воспроизведения могут читать данные, но не должны изменять живое состояние матча."
        )
    )

    private val allocatorActions = listOf(
        LabAction(
            id = "allocator_memory",
            title = "Allocator -> Memory Node",
            operation = "Отправить Allocator для захвата рабочей памяти.",
            itMeaning = "Allocator - это базовая концепция: память - это ограниченное рабочее пространство, которое должно быть явно получено."
        ),
        LabAction(
            id = "cache_cpu",
            title = "Cache Runner -> CPU Node",
            operation = "Быстро идти к пропускной способности CPU.",
            itMeaning = "Cache Runner - это про скорость, а не про создание рабочего памяти, необходимого для этой задачи."
        ),
        LabAction(
            id = "inject_core",
            title = "Injector -> Core",
            operation = "Атаковать центральный процесс.",
            itMeaning = "Injector - это рискованный инструмент вмешательства; он не решает проблему нехватки памяти."
        ),
        LabAction(
            id = "gc_empty_heap",
            title = "GC -> Empty Heap",
            operation = "Попытаться собрать мусор перед любым выделением.",
            itMeaning = "Garbage Collector освобождает недостижимые объекты; не может создать полезное рабочее пространство из ничего."
        )
    )

    private val heapActions = listOf(
        LabAction(
            id = "mark_from_roots",
            title = "Отметить из GC roots",
            operation = "Проследить ссылки AppRoot, MatchRegistry и EventBus.",
            itMeaning = "Garbage Collector начинает с корней и освобождает только недостижимые объекты."
        ),
        LabAction(
            id = "detach_listener",
            title = "Отсоединить устаревший listener",
            operation = "Удалить ссылку EventBus -> ClosedRoomListener.",
            itMeaning = "Многие утечки остаются живы, потому что observer/listener держит старое состояние досягаемым."
        ),
        LabAction(
            id = "sweep_heap",
            title = "Очистить недостижимое",
            operation = "Собрать ClosedRoom и ReplayBuffer после исправления ссылок.",
            itMeaning = "Sweep безопасен только если досягаемость говорит, что объекты действительно мертвы."
        ),
        LabAction(
            id = "force_gc",
            title = "Принудительно запустить GC",
            operation = "Запросить немедленную сборку мусора.",
            itMeaning = "Принуждение к GC не исправляет ситуацию, пока утёкшие объекты все еще досягаемы."
        ),
        LabAction(
            id = "null_socket",
            title = "Обнулить активный сокет",
            operation = "Очистить MatchRegistry -> ActiveRoom -> Socket.",
            itMeaning = "Освобождение живых объектов - это не очистка; это нарушает живую сессию."
        )
    )

    private val firewallActions = listOf(
        LabAction(
            id = "firewall_allow",
            title = "Разрешить",
            operation = "Пропустить текущий запрос.",
            itMeaning = "Firewall не должна блокировать легитимный аутентифицированный трафик."
        ),
        LabAction(
            id = "firewall_block",
            title = "Заблокировать",
            operation = "Отклонить текущий запрос.",
            itMeaning = "Firewall защищает Core и фабрики от враждебных мутаций."
        ),
        LabAction(
            id = "firewall_patch",
            title = "Применить Patch",
            operation = "Принять доверенный hotfix и обновить уязвимый компонент.",
            itMeaning = "Patch Healer представляет техническое обслуживание, которое восстанавливает безопасную работу."
        )
    )

    private val deadlockActions = listOf(
        LabAction(
            id = "inspect_wait_graph",
            title = "Проследить граф ожидания",
            operation = "RenderThread -> SocketLock -> NetworkThread -> FrameLock -> RenderThread",
            itMeaning = "Deadlock - это цикл в графе ожидания ресурсов."
        ),
        LabAction(
            id = "rollback_network",
            title = "Откатить NetworkThread",
            operation = "Освободить SocketLock и отменить частичный сетевой раздел.",
            itMeaning = "Thread Guard должен разорвать hold-and-wait без уничтожения критического потока."
        ),
        LabAction(
            id = "install_lock_order",
            title = "Установить порядок блокировок",
            operation = "Все потоки получают FrameLock перед SocketLock.",
            itMeaning = "Глобальный порядок блокировок предотвращает циклическое ожидание."
        ),
        LabAction(
            id = "resume_network",
            title = "Возобновить NetworkThread",
            operation = "Повторить сетевой раздел после того, как новый порядок активен.",
            itMeaning = "Восстановление завершено, когда полезная работа продолжается по исправленному правилу."
        ),
        LabAction(
            id = "kill_render",
            title = "Убить RenderThread",
            operation = "Завершить поток, владеющий FrameLock.",
            itMeaning = "Это разрывает цикл, но уничтожает критический компонент."
        )
    )

    private val pipelineActions = listOf(
        LabAction(
            id = "auth",
            title = "Аутентификация",
            operation = "Проверить токен и привязать запрос к ID игрока.",
            itMeaning = "Конвейер запросов должен установить идентичность перед изменением."
        ),
        LabAction(
            id = "idempotency",
            title = "Ключ идемпотентности",
            operation = "Прикрепить ключ повтора для свертывания повторных команд POST /buy.",
            itMeaning = "Скорость Cache Runner нуждается в безопасности: повторы не должны дублировать побочные эффекты."
        ),
        LabAction(
            id = "commit",
            title = "DB Commit",
            operation = "Сохранить покупку и изменение ресурсов атомарно.",
            itMeaning = "Зафиксированная транзакция - это источник истины."
        ),
        LabAction(
            id = "event",
            title = "Coroutine Event",
            operation = "Поставить PurchaseCommitted в очередь после DB commit.",
            itMeaning = "Coroutine Archer представляет асинхронную работу, которая должна начаться после авторитетной записи."
        ),
        LabAction(
            id = "invalidate",
            title = "Инвалидировать Cache",
            operation = "Сбросить кэш устаревших ресурсов игрока.",
            itMeaning = "Скорость Cache полезна только когда контролируются устаревшие чтения."
        ),
        LabAction(
            id = "response",
            title = "Ответ",
            operation = "Вернуть успех клиенту.",
            itMeaning = "Ответ должен быть отправлен после того, как система может защитить результат."
        ),
        LabAction(
            id = "reset_pipeline",
            title = "Сбросить последовательность",
            operation = "Очистить попытанный порядок конвейера.",
            itMeaning = "Финальные уровни позволяют экспериментировать без перезагрузки всей лаборатории."
        )
    )

    private val levels = listOf(
        LabLevel(
            name = "1. Основы Allocator",
            unitBasis = "Allocator",
            kind = LabKind.ALLOCATOR_ROUTE,
            incident = "Новая инстанция имеет CPU, но зарезервированной рабочей памяти для юнитов нет.",
            objective = "Выберите пару юнит-цель, которая создает используемое рабочее пространство.",
            maxSteps = 3,
            initial = { LabState() },
            actions = allocatorActions,
            isSolved = { it.allocatorSolved },
            isFailed = { it.steps >= 3 && !it.allocatorSolved }
        ),
        LabLevel(
            name = "2. Garbage Collector",
            unitBasis = "Garbage Collector + Patch Healer",
            kind = LabKind.MARK_SWEEP,
            incident = "Закрытый матч все еще удерживается: EventBus -> ClosedRoomListener -> ClosedRoom -> ReplayBuffer.",
            objective = "Используйте mark/sweep логику: проследите корни, отсоедините устаревший listener, затем очистите недостижимые объекты.",
            maxSteps = 5,
            initial = { LabState() },
            actions = heapActions,
            isSolved = { it.heapMarked && it.staleListenerDetached && it.heapSwept && !it.activeSocketLost },
            isFailed = { it.activeSocketLost || (it.steps >= 5 && !it.heapSwept) }
        ),
        LabLevel(
            name = "3. Firewall Filter",
            unitBasis = "Firewall + Injector + Patch Healer",
            kind = LabKind.FIREWALL_FILTER,
            incident = "Запросы идут в Core. Одни - валидные операции, один - похож на Injector, один - подписанный патч.",
            objective = "Классифицируйте каждый запрос как Allow, Block или Apply Patch. Одна ошибка означает, что фильтр небезопасен.",
            maxSteps = trafficCases.size,
            initial = { LabState() },
            actions = firewallActions,
            isSolved = { it.trafficIndex >= trafficCases.size && it.firewallErrors == 0 && it.patchApplied },
            isFailed = { it.firewallErrors > 0 || (it.steps >= trafficCases.size && it.trafficIndex < trafficCases.size) }
        ),
        LabLevel(
            name = "4. Thread Guard",
            unitBasis = "Thread Guard + Deadlock",
            kind = LabKind.DEADLOCK_GRAPH,
            incident = "RenderThread владеет FrameLock и ждет SocketLock. NetworkThread владеет SocketLock и ждет FrameLock.",
            objective = "Разорвите граф ожидания, оставьте RenderThread живым и установите правило, предотвращающее один и тот же deadlock.",
            maxSteps = 5,
            initial = { LabState() },
            actions = deadlockActions,
            isSolved = {
                it.lockOrderInstalled &&
                        !it.renderWaitsSocketLock &&
                        !it.networkWaitsFrameLock &&
                        !it.criticalThreadKilled
            },
            isFailed = { it.criticalThreadKilled || (it.steps >= 5 && hasDeadlockCycle(it)) }
        ),
        LabLevel(
            name = "5. Async Pipeline",
            unitBasis = "Cache Runner + Coroutine Archer + Overclock",
            kind = LabKind.PIPELINE_ORDER,
            incident = "Клиент повторяет POST /buy после timeout. Серверу нужна скорость, асинхронные события и кэш, но без дублирования покупки.",
            objective = "Постройте безопасный порядок: Auth -> Idempotency -> DB Commit -> Coroutine Event -> Invalidate Cache -> Response.",
            maxSteps = 8,
            initial = { LabState() },
            actions = pipelineActions,
            isSolved = { it.pipeline == expectedPipeline && !it.pipelineBroken },
            isFailed = { it.steps >= 8 && it.pipeline != expectedPipeline }
        )
    )

    private var highestUnlockedLevel = 0
    private var currentLevelIndex = 0
    private var state = levels[currentLevelIndex].initial()
    private val logLines = ArrayDeque<String>()

    private lateinit var titleLabel: Label
    private lateinit var incidentLabel: Label
    private lateinit var objectiveLabel: Label
    private lateinit var systemLabel: Label
    private lateinit var resultLabel: Label
    private lateinit var logLabel: Label
    private lateinit var actionsTable: Table
    private val levelButtons = mutableListOf<TextButton>()
    private val actionButtons = mutableListOf<TextButton>()

    override fun buildUI() {
        highestUnlockedLevel = loadProgress()

        val root = screenRoot(20f)
        addActor(root)

        val box = panel()
        root.add(box).grow()

        val header = Table(skin)

        val title = titleLabel("Лаборатория задач", 1.28f).apply {
            setAlignment(Align.left)
        }
        val backButton = TextButton("Назад", skin)
        UiTheme.styleSecondaryButton(backButton, compact = true)

        header.add(title).padTop(15f).growX().left()
        header.add(backButton).padTop(15f).width(120f).height(38f).right()

        val body = Table(skin)
        body.defaults().pad(4f)

        val left = panel(tint = Color(0.045f, 0.075f, 0.115f, 0.94f), padding = 12f)
        val right = panel(tint = Color(0.035f, 0.055f, 0.085f, 0.94f), padding = 12f)

        buildLevelPanel(left)
        buildActionPanel(right)

        body.add(left).width(455f).growY()
        body.add(right).grow()

        box.add(header).growX().padBottom(8f).row()
        box.add(body).grow().row()

        backButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                game.setScreen(MainScreen(game))
                true
            } else {
                false
            }
        }

        selectLevel(currentLevelIndex.coerceAtMost(highestUnlockedLevel))
    }

    private fun buildLevelPanel(table: Table) {
        val modeLabel = subtitleLabel(
            "Уровни раскрываются по одному. Каждый уровень использует другую механику и начинается с IT метафоры игровых юнитов.",
            0.94f
        ).apply {
            wrap = true
        }

        titleLabel = titleLabel("", 1.05f)
        incidentLabel = subtitleLabel("", 0.92f).apply {
            wrap = true
        }
        objectiveLabel = mutedLabel("", 0.90f).apply {
            wrap = true
            color = UiTheme.statusInfo
        }
        systemLabel = Label("", skin, "small").apply {
            wrap = true
            color = Color(0.91f, 0.96f, 1f, 1f)
        }
        resultLabel = Label("", skin).apply {
            wrap = true
            setAlignment(Align.center)
        }

        val levelTable = Table(skin)
        levelTable.defaults().padBottom(4f).growX()
        levels.forEachIndexed { index, _ ->
            val button = TextButton("", skin)
            UiTheme.styleSecondaryButton(button, compact = true)
            levelButtons += button
            levelTable.add(button).height(30f).growX().row()

            button.addListener { event ->
                if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                    if (index <= highestUnlockedLevel) {
                        selectLevel(index)
                    } else {
                        log("Заблокирован: сначала решите уровень ${index}.")
                        refresh()
                    }
                    true
                } else {
                    false
                }
            }
        }

        val resetButton = TextButton("Сбросить уровень", skin)
        UiTheme.styleSecondaryButton(resetButton)
        resetButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                resetCurrentLevel()
                true
            } else {
                false
            }
        }

        table.defaults().left().growX()
        table.add(modeLabel).width(405f).padBottom(10f).row()
        table.add(levelTable).growX().padBottom(6f).row()
        table.add(titleLabel).growX().padBottom(4f).row()
        table.add(incidentLabel).width(405f).padBottom(6f).row()
        table.add(objectiveLabel).width(405f).padBottom(8f).row()
        table.add(systemLabel).width(405f).height(150f).padBottom(8f).row()
        table.add(resultLabel).width(405f).height(45f).padBottom(6f).row()
        table.add(resetButton).height(40f).growX().row()
    }

    private fun buildActionPanel(table: Table) {
        val formula = subtitleLabel(
            "Цель: изучить IT правило за юнитом, затем применить его в маленькой системе.",
            0.96f
        ).apply {
            setAlignment(Align.center)
            color = UiTheme.statusInfo
        }

        actionsTable = Table(skin)
        actionsTable.defaults().pad(6f).growX()

        logLabel = Label("", skin, "small").apply {
            wrap = true
            color = Color(0.84f, 0.91f, 0.98f, 1f)
        }

        table.defaults().growX()
        table.add(formula).padBottom(8f).row()
        table.add(actionsTable).grow().row()
        table.add(logLabel).height(104f).padTop(8f).row()
    }

    private fun selectLevel(index: Int) {
        if (index > highestUnlockedLevel) return
        currentLevelIndex = index
        rebuildActions()
        resetCurrentLevel()
    }

    private fun rebuildActions() {
        actionsTable.clearChildren()
        actionButtons.clear()

        levels[currentLevelIndex].actions.forEachIndexed { index, action ->
            val actionCard = panel(tint = actionTint(index), padding = 10f)
            val button = TextButton(action.title, skin)
            UiTheme.stylePrimaryButton(button, compact = true)
            actionButtons += button

            val operationLabel = mutedLabel(action.operation, 0.86f).apply {
                wrap = true
            }
            val meaningLabel = subtitleLabel(action.itMeaning, 0.86f).apply {
                wrap = true
            }

            actionCard.add(button).height(34f).growX().row()
            actionCard.add(operationLabel).width(245f).padTop(6f).row()
            actionCard.add(meaningLabel).width(245f).padTop(4f).row()

            button.addListener { event ->
                if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                    applyAction(action.id)
                    true
                } else {
                    false
                }
            }

            actionsTable.add(actionCard).width(275f).height(154f)
            if (index % 2 == 1) {
                actionsTable.row()
            }
        }
    }

    private fun resetCurrentLevel() {
        val level = levels[currentLevelIndex]
        state = level.initial()
        logLines.clear()
        log("Загружен ${level.name}. Основа юнитов: ${level.unitBasis}.")
        refresh()
    }

    private fun applyAction(actionId: String) {
        val level = levels[currentLevelIndex]
        if (isFinished()) return

        val message = when (actionId) {
            "allocator_memory" -> allocatorMemory()
            "cache_cpu" -> wrong("Cache Runner быстр, но этому уровню нужна рабочая память сначала.")
            "inject_core" -> wrong("Injector атакует или изменяет; он не резервирует безопасную память.")
            "gc_empty_heap" -> wrong("Garbage Collector нуждается в недостижимых выделениях для очистки.")

            "mark_from_roots" -> markHeapFromRoots()
            "detach_listener" -> detachStaleListener()
            "sweep_heap" -> sweepHeap()
            "force_gc" -> "Принудительный GC не помог потому что досягаемость все еще неправильна."
            "null_socket" -> nullActiveSocket()

            "firewall_allow",
            "firewall_block",
            "firewall_patch" -> handleTraffic(actionId)

            "inspect_wait_graph" -> inspectWaitGraph()
            "rollback_network" -> rollbackNetworkThread()
            "install_lock_order" -> installLockOrder()
            "resume_network" -> resumeNetworkThread()
            "kill_render" -> killRenderThread()

            "auth",
            "idempotency",
            "commit",
            "event",
            "invalidate",
            "response" -> addPipelineStep(actionId)

            "reset_pipeline" -> resetPipeline()

            else -> "Неизвестная операция."
        }

        state.steps += 1
        log("${state.steps}/${level.maxSteps}: $message")
        refresh()
    }

    private fun allocatorMemory(): String {
        state.allocatorSolved = true
        return "Allocator захватил Memory Node. Инстанция теперь имеет рабочее пространство для будущих процессов."
    }

    private fun wrong(message: String): String {
        return "Не совсем: $message"
    }

    private fun markHeapFromRoots(): String {
        state.heapMarked = true
        return if (state.staleListenerDetached) {
            "Корни переотслежены: ClosedRoom теперь недостижим."
        } else {
            "Корни отслежены: EventBus все еще держит ClosedRoomListener живым, поэтому закрытый матч все еще досягаем."
        }
    }

    private fun detachStaleListener(): String {
        state.staleListenerDetached = true
        return "Отсоединен устаревший listener от EventBus. Старый матч может стать недостижимым после маркировки."
    }

    private fun sweepHeap(): String {
        if (!state.heapMarked) return "Sweep заблокирован: фаза mark должна запуститься первой."
        if (!state.staleListenerDetached) return "Sweep не нашел ничего для сборки потому что EventBus все еще ссылается на listener."
        state.heapSwept = true
        return "Очищены ClosedRoom и ReplayBuffer сохраняя активный сокет досягаемым."
    }

    private fun nullActiveSocket(): String {
        state.activeSocketLost = true
        return "Активный сокет был удален. Это освобождает память, нарушив живую сессию, поэтому исправление неправильно."
    }

    private fun handleTraffic(actionId: String): String {
        if (state.trafficIndex >= trafficCases.size) return "Все запросы уже классифицированы."

        val traffic = trafficCases[state.trafficIndex]
        val correct = actionId == traffic.expectedAction
        state.trafficIndex += 1

        if (!correct) {
            state.firewallErrors += 1
            return "Неправильное решение фильтра для '${traffic.request}'. ${traffic.reason}"
        }

        if (actionId == "firewall_patch") state.patchApplied = true
        return "Правильно: ${traffic.reason}"
    }

    private fun inspectWaitGraph(): String {
        return if (hasDeadlockCycle(state)) {
            "Цикл найден: RenderThread -> SocketLock -> NetworkThread -> FrameLock -> RenderThread."
        } else {
            "В графе ожидания не остается циклов."
        }
    }

    private fun rollbackNetworkThread(): String {
        state.networkRolledBack = true
        state.networkHoldsSocketLock = false
        state.networkWaitsFrameLock = false
        state.renderWaitsSocketLock = false
        return "NetworkThread откачен и освободил SocketLock. RenderThread может завершиться."
    }

    private fun installLockOrder(): String {
        if (!state.networkRolledBack) {
            return "Правило отмечено, но активный цикл все еще требует, чтобы один участник отпустил свою блокировку."
        }

        state.lockOrderInstalled = true
        return "Порядок блокировок установлен: FrameLock перед SocketLock."
    }

    private fun resumeNetworkThread(): String {
        if (!state.lockOrderInstalled) {
            state.networkWaitsFrameLock = true
            return "NetworkThread возобновлен без правила и может пересоздать один и тот же deadlock."
        }

        state.networkWaitsFrameLock = false
        state.networkHoldsSocketLock = false
        return "NetworkThread возобновлен под новым порядком блокировок."
    }

    private fun killRenderThread(): String {
        state.criticalThreadKilled = true
        state.renderHoldsFrameLock = false
        state.renderWaitsSocketLock = false
        return "RenderThread был убит. Deadlock исчез, но критический компонент потерян."
    }

    private fun addPipelineStep(step: String): String {
        if (state.pipeline.size >= expectedPipeline.size) return "Конвейер уже полон."

        state.pipeline += step
        val expectedStep = expectedPipeline[state.pipeline.lastIndex]
        if (step != expectedStep) {
            state.pipelineBroken = true
            return "Порядок конвейера небезопасен: ожидается ${pipelineName(expectedStep)} перед ${pipelineName(step)}."
        }

        return "Добавлен ${pipelineName(step)} в безопасной позиции."
    }

    private fun resetPipeline(): String {
        state.pipeline.clear()
        state.pipelineBroken = false
        return "Последовательность конвейера очищена."
    }

    private fun refresh() {
        val level = levels[currentLevelIndex]

        maybeUnlockNextLevel(level)

        titleLabel.setText(level.name)
        incidentLabel.setText(level.incident)
        objectiveLabel.setText("${level.objective}\nШаги: ${state.steps}/${level.maxSteps}")
        systemLabel.setText(systemSnapshot(level.kind))

        val solved = level.isSolved(state)
        val failed = !solved && level.isFailed(state)
        when {
            solved -> {
                resultLabel.setText(
                    if (currentLevelIndex < levels.lastIndex) "Решено. Следующий уровень разблокирован."
                    else "Решено. Лаборатория завершена."
                )
                resultLabel.color = UiTheme.statusOk
            }

            failed -> {
                resultLabel.setText("Неудача. Сбросьте этот уровень и попробуйте более безопасное инженерное решение.")
                resultLabel.color = UiTheme.statusWarn
            }

            else -> {
                resultLabel.setText("Выберите следующую операцию.")
                resultLabel.color = UiTheme.statusInfo
            }
        }

        refreshLevelButtons()
        actionButtons.forEach { button ->
            button.isDisabled = solved || failed
        }
        logLabel.setText(logLines.joinToString("\n"))
    }

    private fun maybeUnlockNextLevel(level: LabLevel) {
        if (!level.isSolved(state)) return
        if (currentLevelIndex >= levels.lastIndex) return
        if (highestUnlockedLevel > currentLevelIndex) return

        highestUnlockedLevel = currentLevelIndex + 1
        saveProgress()
    }

    private fun refreshLevelButtons() {
        levelButtons.forEachIndexed { index, button ->
            val prefix = when {
                index > highestUnlockedLevel -> "Заблокирован"
                index < highestUnlockedLevel -> "Решено"
                else -> "Открыто"
            }
            button.setText("$prefix: ${levels[index].name}")
            button.color = if (index == currentLevelIndex) {
                Color(0.72f, 0.90f, 1f, 1f)
            } else {
                Color.WHITE
            }
        }
    }

    private fun systemSnapshot(kind: LabKind): String {
        return when (kind) {
            LabKind.ALLOCATOR_ROUTE -> buildString {
                appendLine("СОСТОЯНИЕ СИСТЕМЫ")
                appendLine("Рабочее пространство памяти: ${enabled(state.allocatorSolved)}")
                appendLine("Доступный юнит-метафор: Allocator резервирует память.")
                appendLine("Неправильные инструменты учат контрасту: cache это скорость, injector это вмешательство, GC это очистка.")
            }

            LabKind.MARK_SWEEP -> buildString {
                appendLine("ГРАФ HEAP")
                appendLine("EventBus -> ClosedRoomListener: ${enabled(!state.staleListenerDetached)}")
                appendLine("ClosedRoom -> ReplayBuffer досягаемо: ${enabled(!state.staleListenerDetached && !state.heapSwept)}")
                appendLine("Фаза Mark завершена: ${enabled(state.heapMarked)}")
                appendLine("Недостижимые объекты очищены: ${enabled(state.heapSwept)}")
                appendLine("Активный сокет живой: ${enabled(!state.activeSocketLost)}")
            }

            LabKind.FIREWALL_FILTER -> buildString {
                appendLine("ФИЛЬТР ЗАПРОСОВ")
                appendLine("Текущий запрос:")
                appendLine(currentTrafficText())
                appendLine("Обработано: ${state.trafficIndex}/${trafficCases.size}")
                appendLine("Ошибок: ${state.firewallErrors}")
                appendLine("Доверенный патч применен: ${enabled(state.patchApplied)}")
            }

            LabKind.DEADLOCK_GRAPH -> buildString {
                appendLine("ГРАФ ОЖИДАНИЯ")
                appendLine("RenderThread держит: ${held(state.renderHoldsFrameLock, "FrameLock")}")
                appendLine("RenderThread ждет: ${waits(state.renderWaitsSocketLock, "SocketLock")}")
                appendLine("NetworkThread держит: ${held(state.networkHoldsSocketLock, "SocketLock")}")
                appendLine("NetworkThread ждет: ${waits(state.networkWaitsFrameLock, "FrameLock")}")
                appendLine("Правило порядка блокировок: ${enabled(state.lockOrderInstalled)}")
                appendLine("Цикл обнаружен: ${enabled(hasDeadlockCycle(state))}")
                appendLine("Критический поток живой: ${enabled(!state.criticalThreadKilled)}")
            }

            LabKind.PIPELINE_ORDER -> buildString {
                appendLine("ПОРЯДОК КОНВЕЙЕРА")
                appendLine("Ожидаемый:")
                appendLine(expectedPipeline.joinToString(" -> ") { pipelineName(it) })
                appendLine("Текущий:")
                appendLine(state.pipeline.joinToString(" -> ") { pipelineName(it) }.ifBlank { "пусто" })
                appendLine("Порядок безопасен: ${enabled(!state.pipelineBroken)}")
            }
        }
    }

    private fun currentTrafficText(): String {
        return trafficCases.getOrNull(state.trafficIndex)?.request ?: "все запросы обработаны"
    }

    private fun loadProgress(): Int {
        return Gdx.app
            .getPreferences("memory-leak-arena")
            .getInteger(progressKey(), 0)
            .coerceIn(0, levels.lastIndex)
    }

    private fun saveProgress() {
        Gdx.app
            .getPreferences("memory-leak-arena")
            .putInteger(progressKey(), highestUnlockedLevel)
            .flush()
    }

    private fun progressKey(): String {
        return "puzzle.highestUnlocked.${game.getPlayerId()}"
    }

    private fun isFinished(): Boolean {
        val level = levels[currentLevelIndex]
        return level.isSolved(state) || level.isFailed(state)
    }

    private fun log(message: String) {
        logLines.addFirst(message)
        while (logLines.size > 4) {
            logLines.removeLast()
        }
    }

    private fun held(condition: Boolean, value: String): String {
        return if (condition) value else "none"
    }

    private fun waits(condition: Boolean, value: String): String {
        return if (condition) value else "none"
    }

    private fun enabled(condition: Boolean): String {
        return if (condition) "yes" else "no"
    }

    private fun pipelineName(step: String): String {
        return when (step) {
            "auth" -> "Аутентификация"
            "idempotency" -> "Идемпотентность"
            "commit" -> "DB Commit"
            "event" -> "Coroutine Event"
            "invalidate" -> "Инвалидировать Cache"
            "response" -> "Ответ"
            else -> step
        }
    }

    private fun actionTint(index: Int): Color {
        return when (index % 6) {
            0 -> Color(0.06f, 0.12f, 0.18f, 0.94f)
            1 -> Color(0.05f, 0.13f, 0.17f, 0.94f)
            2 -> Color(0.05f, 0.14f, 0.10f, 0.94f)
            3 -> Color(0.10f, 0.10f, 0.17f, 0.94f)
            4 -> Color(0.13f, 0.08f, 0.13f, 0.94f)
            else -> Color(0.16f, 0.12f, 0.05f, 0.94f)
        }
    }

    companion object {
        private val expectedPipeline = listOf("auth", "idempotency", "commit", "event", "invalidate", "response")

        private fun hasDeadlockCycle(state: LabState): Boolean {
            return state.renderHoldsFrameLock &&
                    state.renderWaitsSocketLock &&
                    state.networkHoldsSocketLock &&
                    state.networkWaitsFrameLock
        }
    }
}
