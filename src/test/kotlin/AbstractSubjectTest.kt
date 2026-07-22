package observer

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AbstractSubjectTest {

    // Concrete subclass of AbstractSubject for testing
    private class TestSubject : AbstractSubject<Double>() {
        fun emit(value: Double) {
            notifyObservers(value)
        }
    }

    private lateinit var subject: TestSubject

    @BeforeEach
    fun setUp() {
        subject = TestSubject()
    }

    @Test
    fun `subscribe registers observer and receives updates`() {
        var receivedValue = 0.0
        val observer = Observer<Double> { value -> receivedValue = value }

        subject.subscribe(observer)
        subject.emit(42.5)

        assertEquals(42.5, receivedValue)
    }

    @Test
    fun `subscribe prevents duplicate observers`() {
        var callCount = 0
        val observer = Observer<Double> { callCount++ }

        subject.subscribe(observer)
        subject.subscribe(observer) // Duplicate registration
        subject.emit(10.0)

        assertEquals(1, callCount, "Observer should only be notified once per event")
    }

    @Test
    fun `unsubscribe stops receiving updates`() {
        var callCount = 0
        val observer = Observer<Double> { callCount++ }

        subject.subscribe(observer)
        subject.emit(1.0)
        assertEquals(1, callCount)

        subject.unsubscribe(observer)
        subject.emit(2.0)
        assertEquals(1, callCount, "Observer should not be notified after unsubscribing")
    }
}