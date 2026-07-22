package api

import command.SetVelocitiesCommand
import environment.AbstractEnvironment
import environment.Obstacle
import environment.TemperatureField
import geometry.Pose
import geometry.Rectangle
import geometry.Vector2
import model.Robot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TemperatureGradientProgramTest {

    @Test
    fun `program stops robot when peak temperature is reached`() {
        // 1. Set up a test environment with a hot spot right at (0, 0)
        val hotEnv = object : AbstractEnvironment() {
            override val name = "Test Hot Environment"
            override val bounds = Rectangle(-100.0, -100.0, 200.0, 200.0)
            override val obstacles: List<Obstacle> = emptyList()
            override val temperatureField = TemperatureField(source = Vector2(0.0, 0.0), peak = 110.0)
        }

        // 2. Initialize robot directly on the hot spot (0,0)
        val robot = Robot(startPose = Pose(0.0, 0.0, 0.0))
        val api = DefaultRobotApi(
            invoker = command.CommandInvoker(),
            actuatorProvider = { robot },
            sensorsProvider = { robot }
        )

        val program = TemperatureGradientProgram()

        // 3. Start the program and simulate a step at the peak location
        program.startProgram(api)
        robot.updateSensors(hotEnv)

        // 4. Assert tracks are stopped (0.0, 0.0)
        assertEquals(0.0, robot.leftTrackVelocity)
        assertEquals(0.0, robot.rightTrackVelocity)
    }

    @Test
    fun `program unsubscribes all observers on stop`() {
        val robot = Robot(startPose = Pose(0.0, 0.0, 0.0))
        val api = DefaultRobotApi(
            invoker = command.CommandInvoker(),
            actuatorProvider = { robot },
            sensorsProvider = { robot }
        )

        val program = TemperatureGradientProgram()

        program.startProgram(api)
        robot.setTrackVelocities(80.0, 80.0)

        // Stopping the program should execute a 0.0 velocity command
        program.stopProgram(api)

        assertEquals(0.0, robot.leftTrackVelocity)
        assertEquals(0.0, robot.rightTrackVelocity)
    }
}