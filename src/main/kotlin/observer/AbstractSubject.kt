package observer

/**
 * Reusable base implementation of [Subject] — this is the heart of the Observer pattern, and it is
 * yours to implement. Every sensor extends this class, so once it works, every sensor can be
 * subscribed to.
 *
 * Until you implement these, sensors still compute their readings but nothing is ever delivered —
 * the telemetry panel stays blank and programs receive no callbacks.
 */
abstract class AbstractSubject<T> : Subject<T> {
    private val observers: MutableSet<Observer<T>> = LinkedHashSet()

    override fun subscribe(observer: Observer<T>) {
        observers.add(observer)
    }

    override fun unsubscribe(observer: Observer<T>) {
        observers.remove(observer)
    }

    override fun notifyObservers(value: T) {
        for (observer in observers.toList()) {
            observer.onUpdate(value)
        }
    }
}
