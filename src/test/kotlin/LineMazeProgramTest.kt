package api

import command.CommandInvoker
import environment.AbstractEnvironment
import environment.LineSegment
import environment.Obstacle
import geometry.Pose
import geometry.Rectangle
import geometry.Vector2
import model.Robot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class LineMazeProgramTest {

    private fun createTestApi(robot: Robot): DefaultRobotApi {
        return DefaultRobotApi(
            invoker = CommandInvoker(),
            actuatorProvider = { robot },
            sensorsProvider = { robot }
        )
    }

    @Test
    fun `drives straight when robot center line sensor is on line`() {
        // Line segment running directly down the center from (90,0) to (90,200)
        val env = object : AbstractEnvironment() {
            override val name = "Test Line Env"
            override val bounds = Rectangle(0.0, 0.0, 300.0, 300.0)
            override val obstacles: List<Obstacle> = emptyList()
            override val lines = listOf(LineSegment(Vector2(90.0, 0.0), Vector2(90.0, 200.0)))
        }

        // Place robot at (90, 50) facing down (90 deg / PI/2) right along the line
        val robot = Robot(startPose = Pose(90.0, 50.0, Math.PI / 2))
        val api = createTestApi(robot)
        val program = LineMazeProgram()

        program.startProgram(api)
        robot.updateSensors(env)

        // Center line sensor active -> Equal forward velocities (80.0, 80.0)
        assertEquals(80.0, robot.leftTrackVelocity)
        assertEquals(80.0, robot.rightTrackVelocity)
    }

    @Test
    fun `turns in place when only left sensor detects line`() {
        // Offset line slightly to the left of the robot
        val env = object : AbstractEnvironment() {
            override val name = "Test Line Env"
            override val bounds = Rectangle(0.0, 0.0, 300.0, 300.0)
            override val obstacles: List<Obstacle> = emptyList()
            override val lines = listOf(LineSegment(Vector2(82.0, 0.0), Vector2(82.0, 200.0)))
        }

        val robot = Robot(startPose = Pose(90.0, 50.0, Math.PI / 2))
        val api = createTestApi(robot)
        val program = LineMazeProgram()

        program.startProgram(api)
        robot.updateSensors(env)

        // Left sensor active -> turn left (left=60.0, right=-60.0)
        assertEquals(-60.0, robot.leftTrackVelocity)
        assertEquals(60.0, robot.rightTrackVelocity)
    }

    @Test
    fun `stops robot and ignores updates after stopProgram`() {
        val env = object : AbstractEnvironment() {
            override val name = "Test Line Env"
            override val bounds = Rectangle(0.0, 0.0, 300.0, 300.0)
            override val obstacles: List<Obstacle> = emptyList()
            override val lines = listOf(LineSegment(Vector2(90.0, 0.0), Vector2(90.0, 200.0)))
        }

        val robot = Robot(startPose = Pose(90.0, 50.0, Math.PI / 2))
        val api = createTestApi(robot)
        val program = LineMazeProgram()

        program.startProgram(api)
        robot.updateSensors(env)
        assertNotEquals(0.0, robot.leftTrackVelocity)

        // Stopping the program must halt track speeds and detach listeners
        program.stopProgram(api)
        assertEquals(0.0, robot.leftTrackVelocity)
        assertEquals(0.0, robot.rightTrackVelocity)

        // Additional sensor updates post-stop should leave track speeds at 0.0
        robot.updateSensors(env)
        assertEquals(0.0, robot.leftTrackVelocity)
        assertEquals(0.0, robot.rightTrackVelocity)
    }
}