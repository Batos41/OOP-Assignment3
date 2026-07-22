import command.CommandInvoker
import command.RobotActuator
import command.SetVelocitiesCommand
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CommandInvokerTest {

    private lateinit var invoker: CommandInvoker
    private lateinit var mockActuator: FakeActuator

    // Minimal fake receiver to track velocity state changes
    private class FakeActuator : RobotActuator {
        override var leftTrackVelocity: Double = 0.0
            private set
        override var rightTrackVelocity: Double = 0.0
            private set

        override fun setTrackVelocities(left: Double, right: Double) {
            leftTrackVelocity = left
            rightTrackVelocity = right
        }

        fun stop() = setTrackVelocities(0.0, 0.0)
    }

    @BeforeEach
    fun setUp() {
        invoker = CommandInvoker()
        mockActuator = FakeActuator()
    }

    @Test
    fun `run executes command and updates actuator state`() {
        val cmd = SetVelocitiesCommand(mockActuator, 50.0, 50.0)
        invoker.run(cmd)

        assertEquals(50.0, mockActuator.leftTrackVelocity)
        assertEquals(50.0, mockActuator.rightTrackVelocity)
    }

    @Test
    fun `undo restores previous state`() {
        mockActuator.setTrackVelocities(10.0, 10.0)

        val cmd = SetVelocitiesCommand(mockActuator, 80.0, 80.0)
        invoker.run(cmd)
        assertEquals(80.0, mockActuator.leftTrackVelocity)

        invoker.undo()
        assertEquals(10.0, mockActuator.leftTrackVelocity, "Undo should revert to velocities prior to execution")
    }

    @Test
    fun `redo reapplies command after undo`() {
        val cmd = SetVelocitiesCommand(mockActuator, 100.0, 50.0)
        invoker.run(cmd)
        invoker.undo()
        assertEquals(0.0, mockActuator.leftTrackVelocity)

        invoker.redo()
        assertEquals(100.0, mockActuator.leftTrackVelocity)
        assertEquals(50.0, mockActuator.rightTrackVelocity)
    }

    @Test
    fun `run clears redo stack`() {
        val cmd1 = SetVelocitiesCommand(mockActuator, 10.0, 10.0)
        val cmd2 = SetVelocitiesCommand(mockActuator, 20.0, 20.0)

        invoker.run(cmd1)
        invoker.undo() // cmd1 is now in redoStack
        invoker.run(cmd2) // Running new command should clear redoStack

        invoker.redo() // Should do nothing because redoStack was cleared
        assertEquals(20.0, mockActuator.leftTrackVelocity, "Redo stack should have been cleared by new run()")
    }

    @Test
    fun `undo on empty stack does not crash`() {
        assertDoesNotThrow { invoker.undo() }
    }

    @Test
    fun `redo on empty stack does not crash`() {
        assertDoesNotThrow { invoker.redo() }
    }
}