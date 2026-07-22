package api

import command.SetVelocitiesCommand
import javafx.scene.paint.Color
import observer.Observer
import kotlin.math.sin

class ObstacleCourseProgram : RobotProgram {

    private val forwardSpeed = 70.0
    private val turnSpeed = 75.0

    // REDUCED: Only avoid when actually close to a wall/obstacle
    private val safeDistance = 35.0

    // Avoidance state
    private var reverseTicksRemaining = 0
    private var avoidanceTicksRemaining = 0

    // Sweep timer for driving with a subtle sinusoidal weave
    private var driveTick = 0

    private var hasSeenBall = false

    private var sonarObserver: Observer<Double>? = null
    private var visionObserver: Observer<Color>? = null
    private var collisionObserver: Observer<Boolean>? = null

    override val name: String
        get() = "Obstacle Course Program"

    override fun startProgram(robot: RobotApi) {
        val sensors = robot.sensors

        fun evaluate() {
            val distance = sensors.sonar.reading ?: Double.MAX_VALUE
            val currentColor = sensors.vision.reading
            val isColliding = sensors.collision.reading == true

            // 0. Secret actual highest priority. If we no longer see red, we're on it. Stop.
            if (hasSeenBall && currentColor != Color.RED) {
                // Lost sight of the ball after tracking it -> STOP immediately!
                robot.perform(SetVelocitiesCommand(robot.actuator, 0.0, 0.0))
                return
            }

            // 1. HIGHEST PRIORITY: Target Acquired!
            if (currentColor == Color.RED) {
                hasSeenBall = true
                reverseTicksRemaining = 0
                avoidanceTicksRemaining = 0
                robot.perform(SetVelocitiesCommand(robot.actuator, forwardSpeed, forwardSpeed))
                return
            }

            // 2. Obstacle / Collision Reaction (Only when VERY close or colliding)
            if (isColliding || distance < safeDistance) {
                if (reverseTicksRemaining == 0 && avoidanceTicksRemaining == 0) {
                    reverseTicksRemaining = 5
                    avoidanceTicksRemaining = 16 // Significant pivot to face deep into room
                }
            }

            // Phase A: Reverse away from obstacle
            if (reverseTicksRemaining > 0) {
                reverseTicksRemaining--
                robot.perform(SetVelocitiesCommand(robot.actuator, -forwardSpeed, -forwardSpeed))
                return
            }

            // Phase B: Pivot to head toward open space
            if (avoidanceTicksRemaining > 0) {
                avoidanceTicksRemaining--
                robot.perform(SetVelocitiesCommand(robot.actuator, turnSpeed, -turnSpeed))
                return
            }

            // 3. Open-Space Traversal with a Weaving Scan
            // Drive forward while gently oscillating track speeds to weave the camera left and right
            driveTick++
            val weave = sin(driveTick * 0.2) * 20.0 // Subtle speed variance (+/- 20)
            val leftVel = forwardSpeed + weave
            val rightVel = forwardSpeed - weave

            robot.perform(SetVelocitiesCommand(robot.actuator, leftVel, rightVel))
        }

        sonarObserver = Observer { evaluate() }
        visionObserver = Observer { evaluate() }
        collisionObserver = Observer { evaluate() }

        sensors.sonar.subscribe(sonarObserver!!)
        sensors.vision.subscribe(visionObserver!!)
        sensors.collision.subscribe(collisionObserver!!)

        evaluate()
    }

    override fun stopProgram(robot: RobotApi) {
        sonarObserver?.let { robot.sensors.sonar.unsubscribe(it) }
        visionObserver?.let { robot.sensors.vision.unsubscribe(it) }
        collisionObserver?.let { robot.sensors.collision.unsubscribe(it) }

        reverseTicksRemaining = 0
        avoidanceTicksRemaining = 0
        driveTick = 0

        robot.perform(SetVelocitiesCommand(robot.actuator, 0.0, 0.0))
    }
}