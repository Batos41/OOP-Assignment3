package api

import command.CommandInvoker
import environment.AbstractEnvironment
import environment.Ball
import environment.Obstacle
import geometry.Pose
import geometry.Rectangle
import geometry.Vector2
import javafx.scene.paint.Color
import model.Robot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ObstacleCourseProgramTest {

    private fun createTestApi(robot: Robot): DefaultRobotApi {
        return DefaultRobotApi(
            invoker = CommandInvoker(),
            actuatorProvider = { robot },
            sensorsProvider = { robot }
        )
    }

    @Test
    fun `charges forward when vision sensor detects red ball`() {
        val redBall = Ball(center = Vector2(100.0, 0.0), radius = 15.0, color = Color.RED)
        val env = object : AbstractEnvironment() {
            override val name = "Ball Env"
            override val bounds = Rectangle(-200.0, -200.0, 400.0, 400.0)
            override val obstacles: List<Obstacle> = emptyList()
            override val ball = redBall
        }

        val robot = Robot(startPose = Pose(0.0, 0.0, 0.0))
        val api = createTestApi(robot)
        val program = ObstacleCourseProgram()

        program.startProgram(api)
        robot.updateSensors(env)

        // Vision sees RED -> drive full speed ahead (70.0, 70.0)
        assertEquals(70.0, robot.leftTrackVelocity)
        assertEquals(70.0, robot.rightTrackVelocity)
    }

    @Test
    fun `stops immediately when target sight is lost after being spotted`() {
        val redBall = Ball(center = Vector2(100.0, 0.0), radius = 15.0, color = Color.RED)
        val envWithBall = object : AbstractEnvironment() {
            override val name = "Ball Env"
            override val bounds = Rectangle(-200.0, -200.0, 400.0, 400.0)
            override val obstacles: List<Obstacle> = emptyList()
            override val ball = redBall
        }
        val envEmpty = object : AbstractEnvironment() {
            override val name = "Empty Env"
            override val bounds = Rectangle(-200.0, -200.0, 400.0, 400.0)
            override val obstacles: List<Obstacle> = emptyList()
        }

        val robot = Robot(startPose = Pose(0.0, 0.0, 0.0))
        val api = createTestApi(robot)
        val program = ObstacleCourseProgram()

        program.startProgram(api)

        // Tick 1: Spot the ball -> charges forward
        robot.updateSensors(envWithBall)
        assertEquals(70.0, robot.leftTrackVelocity)

        // Tick 2: Lose sight of ball -> must stop track velocities immediately (0.0, 0.0)
        robot.updateSensors(envEmpty)
        assertEquals(0.0, robot.leftTrackVelocity)
        assertEquals(0.0, robot.rightTrackVelocity)
    }

    @Test
    fun `reverses when obstacle is within safe distance`() {
        // Place an obstacle right in front of the robot (sonar distance < 35.0)
        val env = object : AbstractEnvironment() {
            override val name = "Obstacle Env"
            override val bounds = Rectangle(-200.0, -200.0, 400.0, 400.0)
            override val obstacles = listOf(Obstacle(Rectangle(20.0, -20.0, 40.0, 40.0)))
        }

        val robot = Robot(startPose = Pose(0.0, 0.0, 0.0))
        val api = createTestApi(robot)
        val program = ObstacleCourseProgram()

        program.startProgram(api)
        robot.updateSensors(env)

        // Close obstacle -> Reverse (-70.0, -70.0)
        assertEquals(-70.0, robot.leftTrackVelocity)
        assertEquals(-70.0, robot.rightTrackVelocity)
    }

    @Test
    fun `resets state and halts robot on stopProgram`() {
        val robot = Robot(startPose = Pose(0.0, 0.0, 0.0))
        val api = createTestApi(robot)
        val program = ObstacleCourseProgram()

        program.startProgram(api)
        program.stopProgram(api)

        assertEquals(0.0, robot.leftTrackVelocity)
        assertEquals(0.0, robot.rightTrackVelocity)
    }
}