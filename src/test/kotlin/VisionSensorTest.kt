package sensor

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

class VisionSensorTest {

    @Test
    fun `vision sensor detects red ball when line of sight is clear`() {
        val redBall = Ball(center = Vector2(100.0, 0.0), radius = 10.0, color = Color.RED)

        val testEnv = object : AbstractEnvironment() {
            override val name: String = "Test Environment"
            override val bounds: Rectangle = Rectangle(-200.0, -200.0, 400.0, 400.0)
            override val obstacles: List<Obstacle> = emptyList()
            override val ball: Ball = redBall
        }

        // Initialize Robot at (0, 0) facing heading 0.0 (pointing directly right at the ball at (100,0))
        val robot = Robot(startPose = Pose(0.0, 0.0, 0.0))

        // Update robot's sensors within the test environment
        robot.updateSensors(testEnv)

        // Verify robot's vision sensor reading
        assertEquals(Color.RED, robot.vision.reading)
    }

    @Test
    fun `vision sensor detects obstacle when ball is hidden or turned away`() {
        val redBall = Ball(center = Vector2(100.0, 0.0), radius = 10.0, color = Color.RED)

        val testEnv = object : AbstractEnvironment() {
            override val name: String = "Test Environment"
            override val bounds: Rectangle = Rectangle(-200.0, -200.0, 400.0, 400.0)
            override val obstacles: List<Obstacle> = emptyList()
            override val ball: Ball = redBall
        }

        // Robot at (0, 0) facing UP (heading = PI/2), pointing away from the ball at (100,0)
        val robot = Robot(startPose = Pose(0.0, 0.0, Math.PI / 2))

        robot.updateSensors(testEnv)

        // Should see the floor color, not RED
        assertEquals(VisionSensor.WALL_COLOR, robot.vision.reading)
    }
}