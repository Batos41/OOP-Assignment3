package api

import command.SetVelocitiesCommand
import observer.Observer

class LineMazeProgram : RobotProgram {

    private val speed = 80.0
    private val turn = 60.0

    // Keep references to observers so you can unsubscribe cleanly in stop()
    private var leftObserver: Observer<Boolean>? = null
    private var centerObserver: Observer<Boolean>? = null
    private var rightObserver: Observer<Boolean>? = null

    // Memory state to remember which side lost the line last
    private enum class Side { LEFT, RIGHT, NONE }
    private var lastSeenSide = Side.NONE

    override val name: String
        get() = "Line Maze Program"

    override fun startProgram(robot: RobotApi) {
        val sensors = robot.sensors

        // Helper to evaluate sensor states and issue the right driving command
        fun evaluateLine() {
            val l = sensors.lineLeft.reading == true
            val c = sensors.lineCenter.reading == true
            val r = sensors.lineRight.reading == true

            // Track memory for corner recovery
            if (l) lastSeenSide = Side.LEFT
            if (r) lastSeenSide = Side.RIGHT

            when {
                // 1. Fully centered on wide line OR center-only -> Drive Straight
                (l && c && r) || (c && !l && !r) -> {
                    robot.perform(SetVelocitiesCommand(robot.actuator, speed, speed))
                }

                // 2. Drifting right (left & center active) -> Gentle nudge left
                l && c -> {
                    robot.perform(SetVelocitiesCommand(robot.actuator, speed, speed * 0.9))
                }

                // 3. Drifting left (right & center active) -> Gentle nudge right
                r && c -> {
                    robot.perform(SetVelocitiesCommand(robot.actuator, speed * 0.9, speed))
                }

                // 4. Center is LOST, but only Left or Right is active -> Rotate on spot to get Center back
                l -> {
                    robot.perform(SetVelocitiesCommand(robot.actuator, turn, -turn))
                }

                r -> {
                    robot.perform(SetVelocitiesCommand(robot.actuator, -turn, turn))
                }

                // 5. Completely lost line -> Recovery spin based on last seen side
                else -> {
                    when (lastSeenSide) {
                        Side.LEFT -> robot.perform(SetVelocitiesCommand(robot.actuator, turn, -turn))
                        Side.RIGHT -> robot.perform(SetVelocitiesCommand(robot.actuator, -turn, turn))
                        Side.NONE -> robot.perform(SetVelocitiesCommand(robot.actuator, turn, -turn))
                    }
                }
            }
        }

        leftObserver = Observer { evaluateLine() }
        centerObserver = Observer { evaluateLine() }
        rightObserver = Observer { evaluateLine() }

        sensors.lineLeft.subscribe(leftObserver!!)
        sensors.lineCenter.subscribe(centerObserver!!)
        sensors.lineRight.subscribe(rightObserver!!)

        // Initial trigger
        evaluateLine()
    }

    override fun stopProgram(robot: RobotApi) {
        val sensors = robot.sensors

        leftObserver?.let { sensors.lineLeft.unsubscribe(it) }
        centerObserver?.let { sensors.lineCenter.unsubscribe(it) }
        rightObserver?.let { sensors.lineRight.unsubscribe(it) }

        // Stop the robot when the program finishes/resets
        robot.perform(SetVelocitiesCommand(robot.actuator, 0.0, 0.0))
    }
}