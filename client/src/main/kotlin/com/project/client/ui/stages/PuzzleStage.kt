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
            request = "GET /state with valid player token",
            expectedAction = "firewall_allow",
            reason = "Read-only authenticated traffic should pass."
        ),
        TrafficCase(
            request = "POST /inject from unknown token",
            expectedAction = "firewall_block",
            reason = "Injector-like command from an unknown identity must be blocked."
        ),
        TrafficCase(
            request = "PATCH /core/hotfix signed by owner",
            expectedAction = "firewall_patch",
            reason = "A trusted patch should be applied, not treated as hostile injection."
        ),
        TrafficCase(
            request = "DELETE /match from replay bot",
            expectedAction = "firewall_block",
            reason = "Replay bots may read data, but must not mutate live match state."
        )
    )

    private val allocatorActions = listOf(
        LabAction(
            id = "allocator_memory",
            title = "Allocator -> Memory Node",
            operation = "Send Allocator to reserve working memory.",
            itMeaning = "Allocator is the beginner concept: memory is a limited workspace that must be explicitly obtained."
        ),
        LabAction(
            id = "cache_cpu",
            title = "Cache Runner -> CPU Node",
            operation = "Run fast toward CPU throughput.",
            itMeaning = "Cache Runner is about tempo, not creating the memory workspace required by this task."
        ),
        LabAction(
            id = "inject_core",
            title = "Injector -> Core",
            operation = "Attack the central process.",
            itMeaning = "Injector is a risky intervention tool; it does not solve missing memory."
        ),
        LabAction(
            id = "gc_empty_heap",
            title = "GC -> Empty Heap",
            operation = "Try to collect before anything is allocated.",
            itMeaning = "Garbage Collector frees unreachable objects; it cannot create useful workspace from nothing."
        )
    )

    private val heapActions = listOf(
        LabAction(
            id = "mark_from_roots",
            title = "Mark from GC roots",
            operation = "Trace AppRoot, MatchRegistry and EventBus references.",
            itMeaning = "Garbage Collector starts from roots and only frees unreachable objects."
        ),
        LabAction(
            id = "detach_listener",
            title = "Detach stale listener",
            operation = "Remove EventBus -> ClosedRoomListener reference.",
            itMeaning = "Many leaks survive GC because an observer/listener keeps old state reachable."
        ),
        LabAction(
            id = "sweep_heap",
            title = "Sweep unreachable",
            operation = "Collect ClosedRoom and ReplayBuffer after references are fixed.",
            itMeaning = "Sweep is safe only after reachability says the objects are really dead."
        ),
        LabAction(
            id = "force_gc",
            title = "Force GC now",
            operation = "Request immediate garbage collection.",
            itMeaning = "Forcing GC is not a fix while leaked objects are still reachable."
        ),
        LabAction(
            id = "null_socket",
            title = "Null active socket",
            operation = "Clear MatchRegistry -> ActiveRoom -> Socket.",
            itMeaning = "Freeing live objects is not cleanup; it breaks the running session."
        )
    )

    private val firewallActions = listOf(
        LabAction(
            id = "firewall_allow",
            title = "Allow",
            operation = "Let the current request pass.",
            itMeaning = "Firewall should not block legitimate authenticated traffic."
        ),
        LabAction(
            id = "firewall_block",
            title = "Block",
            operation = "Reject the current request.",
            itMeaning = "Firewall protects Core and factories from hostile mutations."
        ),
        LabAction(
            id = "firewall_patch",
            title = "Apply Patch",
            operation = "Accept trusted hotfix and update the vulnerable component.",
            itMeaning = "Patch Healer represents maintenance that restores safe operation."
        )
    )

    private val deadlockActions = listOf(
        LabAction(
            id = "inspect_wait_graph",
            title = "Trace wait-for graph",
            operation = "RenderThread -> SocketLock -> NetworkThread -> FrameLock -> RenderThread",
            itMeaning = "Deadlock is a cycle in a resource wait graph."
        ),
        LabAction(
            id = "rollback_network",
            title = "Rollback NetworkThread",
            operation = "Release SocketLock and cancel the partial network section.",
            itMeaning = "Thread Guard should break hold-and-wait without destroying the critical thread."
        ),
        LabAction(
            id = "install_lock_order",
            title = "Install lock order",
            operation = "All threads acquire FrameLock before SocketLock.",
            itMeaning = "A global lock order prevents circular wait from returning."
        ),
        LabAction(
            id = "resume_network",
            title = "Resume NetworkThread",
            operation = "Retry the network section after the lock order is active.",
            itMeaning = "Recovery is complete when useful work continues under the corrected rule."
        ),
        LabAction(
            id = "kill_render",
            title = "Kill RenderThread",
            operation = "Terminate the thread that owns FrameLock.",
            itMeaning = "This breaks the cycle, but destroys a critical component."
        )
    )

    private val pipelineActions = listOf(
        LabAction(
            id = "auth",
            title = "Auth",
            operation = "Validate token and bind request to player id.",
            itMeaning = "A request pipeline must establish identity before mutation."
        ),
        LabAction(
            id = "idempotency",
            title = "Idempotency Key",
            operation = "Attach retry key to collapse repeated POST /buy commands.",
            itMeaning = "Cache Runner's speed needs safety: retries must not duplicate side effects."
        ),
        LabAction(
            id = "commit",
            title = "DB Commit",
            operation = "Persist purchase and resource delta atomically.",
            itMeaning = "The committed transaction is the source of truth."
        ),
        LabAction(
            id = "event",
            title = "Coroutine Event",
            operation = "Queue PurchaseCommitted after DB commit.",
            itMeaning = "Coroutine Archer represents async work that must start after the authoritative write."
        ),
        LabAction(
            id = "invalidate",
            title = "Invalidate Cache",
            operation = "Drop stale player resources cache.",
            itMeaning = "Cache speed is useful only when stale reads are controlled."
        ),
        LabAction(
            id = "response",
            title = "Response",
            operation = "Return success to the client.",
            itMeaning = "The response must be sent after the system can defend the result."
        ),
        LabAction(
            id = "reset_pipeline",
            title = "Reset Sequence",
            operation = "Clear the attempted pipeline order.",
            itMeaning = "Final levels allow experimentation without restarting the whole lab."
        )
    )

    private val levels = listOf(
        LabLevel(
            name = "1. Allocator Basics",
            unitBasis = "Allocator",
            kind = LabKind.ALLOCATOR_ROUTE,
            incident = "A new Instance has CPU, but no working memory reserved for units.",
            objective = "Pick the unit-target pair that creates usable workspace.",
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
            incident = "A closed match is still retained: EventBus -> ClosedRoomListener -> ClosedRoom -> ReplayBuffer.",
            objective = "Use mark/sweep thinking: trace roots, detach the stale listener, then sweep unreachable objects.",
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
            incident = "Requests are arriving at Core. Some are valid operations, one is Injector-like, one is a signed patch.",
            objective = "Classify each request as Allow, Block, or Apply Patch. One mistake means the filter policy is unsafe.",
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
            incident = "RenderThread owns FrameLock and waits for SocketLock. NetworkThread owns SocketLock and waits for FrameLock.",
            objective = "Break the wait-for cycle, keep RenderThread alive, and install a rule that prevents the same deadlock.",
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
            incident = "Client retries POST /buy after timeout. The server needs speed, async events and cache, but cannot duplicate the purchase.",
            objective = "Build the safe order: Auth -> Idempotency -> DB Commit -> Coroutine Event -> Invalidate Cache -> Response.",
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

        val title = titleLabel("Puzzle Lab", 1.28f).apply {
            setAlignment(Align.left)
        }
        val backButton = TextButton("Back", skin)
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
            "Levels unlock one by one. Each level uses a different mechanic and starts from the game units' IT metaphor.",
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
                        log("Locked: solve level ${index} first.")
                        refresh()
                    }
                    true
                } else {
                    false
                }
            }
        }

        val resetButton = TextButton("Reset Level", skin)
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
            "Goal: learn the IT rule behind the unit, then apply it in a small system.",
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
        log("Loaded ${level.name}. Unit basis: ${level.unitBasis}.")
        refresh()
    }

    private fun applyAction(actionId: String) {
        val level = levels[currentLevelIndex]
        if (isFinished()) return

        val message = when (actionId) {
            "allocator_memory" -> allocatorMemory()
            "cache_cpu" -> wrong("Cache Runner is fast, but this level needs memory workspace first.")
            "inject_core" -> wrong("Injector attacks or modifies; it does not reserve safe memory.")
            "gc_empty_heap" -> wrong("Garbage Collector needs unreachable allocations to clean.")

            "mark_from_roots" -> markHeapFromRoots()
            "detach_listener" -> detachStaleListener()
            "sweep_heap" -> sweepHeap()
            "force_gc" -> "Forced GC did not help because reachability is still wrong."
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

            else -> "Unknown operation."
        }

        state.steps += 1
        log("${state.steps}/${level.maxSteps}: $message")
        refresh()
    }

    private fun allocatorMemory(): String {
        state.allocatorSolved = true
        return "Allocator captured Memory Node. The Instance now has workspace for future processes."
    }

    private fun wrong(message: String): String {
        return "Not yet: $message"
    }

    private fun markHeapFromRoots(): String {
        state.heapMarked = true
        return if (state.staleListenerDetached) {
            "Roots traced again: ClosedRoom is now unreachable."
        } else {
            "Roots traced: EventBus still keeps ClosedRoomListener alive, so the closed match is still reachable."
        }
    }

    private fun detachStaleListener(): String {
        state.staleListenerDetached = true
        return "Detached stale listener from EventBus. The old match can become unreachable after marking."
    }

    private fun sweepHeap(): String {
        if (!state.heapMarked) return "Sweep blocked: mark phase must run first."
        if (!state.staleListenerDetached) return "Sweep found no collectible match because EventBus still references the listener."
        state.heapSwept = true
        return "Swept ClosedRoom and ReplayBuffer while keeping the active socket reachable."
    }

    private fun nullActiveSocket(): String {
        state.activeSocketLost = true
        return "Active socket was removed. That frees memory by breaking the live session, so the fix is invalid."
    }

    private fun handleTraffic(actionId: String): String {
        if (state.trafficIndex >= trafficCases.size) return "All requests are already classified."

        val traffic = trafficCases[state.trafficIndex]
        val correct = actionId == traffic.expectedAction
        state.trafficIndex += 1

        if (!correct) {
            state.firewallErrors += 1
            return "Wrong filter decision for '${traffic.request}'. ${traffic.reason}"
        }

        if (actionId == "firewall_patch") state.patchApplied = true
        return "Correct: ${traffic.reason}"
    }

    private fun inspectWaitGraph(): String {
        return if (hasDeadlockCycle(state)) {
            "Cycle found: RenderThread -> SocketLock -> NetworkThread -> FrameLock -> RenderThread."
        } else {
            "No cycle remains in the wait-for graph."
        }
    }

    private fun rollbackNetworkThread(): String {
        state.networkRolledBack = true
        state.networkHoldsSocketLock = false
        state.networkWaitsFrameLock = false
        state.renderWaitsSocketLock = false
        return "NetworkThread rolled back and released SocketLock. RenderThread can finish."
    }

    private fun installLockOrder(): String {
        if (!state.networkRolledBack) {
            return "Rule noted, but the active cycle still needs one participant to release its lock."
        }

        state.lockOrderInstalled = true
        return "Lock order installed: FrameLock before SocketLock."
    }

    private fun resumeNetworkThread(): String {
        if (!state.lockOrderInstalled) {
            state.networkWaitsFrameLock = true
            return "NetworkThread resumed without a rule and can recreate the same deadlock."
        }

        state.networkWaitsFrameLock = false
        state.networkHoldsSocketLock = false
        return "NetworkThread resumed under the new lock order."
    }

    private fun killRenderThread(): String {
        state.criticalThreadKilled = true
        state.renderHoldsFrameLock = false
        state.renderWaitsSocketLock = false
        return "RenderThread was killed. The deadlock is gone, but a critical component is lost."
    }

    private fun addPipelineStep(step: String): String {
        if (state.pipeline.size >= expectedPipeline.size) return "Pipeline is already full."

        state.pipeline += step
        val expectedStep = expectedPipeline[state.pipeline.lastIndex]
        if (step != expectedStep) {
            state.pipelineBroken = true
            return "Pipeline order is unsafe: expected ${pipelineName(expectedStep)} before ${pipelineName(step)}."
        }

        return "Added ${pipelineName(step)} in the safe position."
    }

    private fun resetPipeline(): String {
        state.pipeline.clear()
        state.pipelineBroken = false
        return "Pipeline sequence cleared."
    }

    private fun refresh() {
        val level = levels[currentLevelIndex]

        maybeUnlockNextLevel(level)

        titleLabel.setText(level.name)
        incidentLabel.setText(level.incident)
        objectiveLabel.setText("${level.objective}\nSteps: ${state.steps}/${level.maxSteps}")
        systemLabel.setText(systemSnapshot(level.kind))

        val solved = level.isSolved(state)
        val failed = !solved && level.isFailed(state)
        when {
            solved -> {
                resultLabel.setText(
                    if (currentLevelIndex < levels.lastIndex) "Solved. Next level unlocked."
                    else "Solved. Puzzle Lab complete."
                )
                resultLabel.color = UiTheme.statusOk
            }

            failed -> {
                resultLabel.setText("Failed. Reset this level and try a safer engineering decision.")
                resultLabel.color = UiTheme.statusWarn
            }

            else -> {
                resultLabel.setText("Choose the next operation.")
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
                index > highestUnlockedLevel -> "Locked"
                index < highestUnlockedLevel -> "Done"
                else -> "Open"
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
                appendLine("SYSTEM STATE")
                appendLine("Instance memory workspace: ${enabled(state.allocatorSolved)}")
                appendLine("Available unit metaphor: Allocator reserves memory.")
                appendLine("Wrong tools teach contrast: cache is speed, injector is intervention, GC is cleanup.")
            }

            LabKind.MARK_SWEEP -> buildString {
                appendLine("HEAP GRAPH")
                appendLine("EventBus -> ClosedRoomListener: ${enabled(!state.staleListenerDetached)}")
                appendLine("ClosedRoom -> ReplayBuffer reachable: ${enabled(!state.staleListenerDetached && !state.heapSwept)}")
                appendLine("Mark phase completed: ${enabled(state.heapMarked)}")
                appendLine("Unreachable objects swept: ${enabled(state.heapSwept)}")
                appendLine("Active socket alive: ${enabled(!state.activeSocketLost)}")
            }

            LabKind.FIREWALL_FILTER -> buildString {
                appendLine("REQUEST FILTER")
                appendLine("Current request:")
                appendLine(currentTrafficText())
                appendLine("Handled: ${state.trafficIndex}/${trafficCases.size}")
                appendLine("Errors: ${state.firewallErrors}")
                appendLine("Trusted patch applied: ${enabled(state.patchApplied)}")
            }

            LabKind.DEADLOCK_GRAPH -> buildString {
                appendLine("WAIT-FOR GRAPH")
                appendLine("RenderThread holds: ${held(state.renderHoldsFrameLock, "FrameLock")}")
                appendLine("RenderThread waits: ${waits(state.renderWaitsSocketLock, "SocketLock")}")
                appendLine("NetworkThread holds: ${held(state.networkHoldsSocketLock, "SocketLock")}")
                appendLine("NetworkThread waits: ${waits(state.networkWaitsFrameLock, "FrameLock")}")
                appendLine("Lock order rule: ${enabled(state.lockOrderInstalled)}")
                appendLine("Cycle detected: ${enabled(hasDeadlockCycle(state))}")
                appendLine("Critical thread alive: ${enabled(!state.criticalThreadKilled)}")
            }

            LabKind.PIPELINE_ORDER -> buildString {
                appendLine("PIPELINE ORDER")
                appendLine("Expected:")
                appendLine(expectedPipeline.joinToString(" -> ") { pipelineName(it) })
                appendLine("Current:")
                appendLine(state.pipeline.joinToString(" -> ") { pipelineName(it) }.ifBlank { "empty" })
                appendLine("Order safe: ${enabled(!state.pipelineBroken)}")
            }
        }
    }

    private fun currentTrafficText(): String {
        return trafficCases.getOrNull(state.trafficIndex)?.request ?: "all requests handled"
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
            "auth" -> "Auth"
            "idempotency" -> "Idempotency"
            "commit" -> "DB Commit"
            "event" -> "Coroutine Event"
            "invalidate" -> "Cache Invalidate"
            "response" -> "Response"
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
