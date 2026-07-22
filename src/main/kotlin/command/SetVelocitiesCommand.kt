package command

class SetVelocitiesCommand(
    private val actuator: RobotActuator,
    private val targetLeftVel: Double,
    private val targetRightVel: Double,
) : Command {

    private var previousLeftVel: Double = 0.0
    private var previousRightVel: Double = 0.0

    override fun execute() {
        // Capture state before applying changes to support undoing
        previousLeftVel = actuator.leftTrackVelocity
        previousRightVel = actuator.rightTrackVelocity

        actuator.setTrackVelocities(targetLeftVel, targetRightVel)
    }

    override fun undo() {
        // Restore previous track velocities
        actuator.setTrackVelocities(previousLeftVel, previousRightVel)
    }
}