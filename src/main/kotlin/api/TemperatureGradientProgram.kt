package api

import command.SetVelocitiesCommand
import observer.Observer
import kotlin.random.Random

class TemperatureGradientProgram : RobotProgram {

    private val forwardSpeed = 80.0
    private val turnSpeed = 60.0
    private val safeDistance = 45.0

    private var previousTemp: Double? = null
    private var reverseTicksRemaining = 0
    private var avoidanceTicksRemaining = 0
    private var turnLeft = true

    private var tempObserver: Observer<Double>? = null
    private var sonarObserver: Observer<Double>? = null
    private var collisionObserver: Observer<Boolean>? = null

    override val name: String
        get() = "Temperature Gradient Program"

    override fun startProgram(robot: RobotApi) {
        val sensors = robot.sensors

        fun evaluate() {
            val currentTemp = sensors.temperature.reading ?: 0.0
            val distance = sensors.sonar.reading ?: Double.MAX_VALUE
            val isColliding = sensors.collision.reading == true

            // 1. Goal Reached (>= 93 degrees per environment objective)
            if (currentTemp >= 93.0) {
                robot.perform(SetVelocitiesCommand(robot.actuator, 0.0, 0.0))
                return
            }

            // 2. Obstacle Avoidance Handling
            if (isColliding || distance < safeDistance) {
                if (reverseTicksRemaining == 0 && avoidanceTicksRemaining == 0) {
                    reverseTicksRemaining = 4
                    avoidanceTicksRemaining = Random.nextInt(12, 20)
                    turnLeft = Random.nextBoolean()
                }
            }

            if (reverseTicksRemaining > 0) {
                reverseTicksRemaining--
                robot.perform(SetVelocitiesCommand(robot.actuator, -forwardSpeed, -forwardSpeed))
                return
            }

            if (avoidanceTicksRemaining > 0) {
                avoidanceTicksRemaining--
                val leftVel = if (turnLeft) -turnSpeed else turnSpeed
                val rightVel = if (turnLeft) turnSpeed else -turnSpeed
                robot.perform(SetVelocitiesCommand(robot.actuator, leftVel, rightVel))
                return
            }

            // 3. Gradient Ascent Logic
            val prev = previousTemp
            previousTemp = currentTemp

            if (prev != null) {
                val deltaTemp = currentTemp - prev

                // If getting colder, turn to search for a warmer heading
                if (deltaTemp < -0.05) {
                    val turnDir = if (turnLeft) 1.0 else -1.0
                    robot.perform(
                        SetVelocitiesCommand(
                            robot.actuator,
                            forwardSpeed * 0.4 - (turnSpeed * turnDir),
                            forwardSpeed * 0.4 + (turnSpeed * turnDir)
                        )
                    )
                    return
                }
            }

            // Default: Keep driving straight towards warmer gradient
            robot.perform(SetVelocitiesCommand(robot.actuator, forwardSpeed, forwardSpeed))
        }

        tempObserver = Observer { evaluate() }
        sonarObserver = Observer { evaluate() }
        collisionObserver = Observer { evaluate() }

        sensors.temperature.subscribe(tempObserver!!)
        sensors.sonar.subscribe(sonarObserver!!)
        sensors.collision.subscribe(collisionObserver!!)

        evaluate()
    }

    override fun stopProgram(robot: RobotApi) {
        tempObserver?.let { robot.sensors.temperature.unsubscribe(it) }
        sonarObserver?.let { robot.sensors.sonar.unsubscribe(it) }
        collisionObserver?.let { robot.sensors.collision.unsubscribe(it) }

        reverseTicksRemaining = 0
        avoidanceTicksRemaining = 0
        previousTemp = null

        robot.perform(SetVelocitiesCommand(robot.actuator, 0.0, 0.0))
    }
}