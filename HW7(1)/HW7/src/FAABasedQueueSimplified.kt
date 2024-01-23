import kotlinx.atomicfu.*
import kotlin.math.*

class FAABasedQueueSimplified<E> : Queue<E> {
    private val infiniteArray = atomicArrayOfNulls<Any?>(1024) // conceptually infinite array
    private val enqIdx = atomic(0)
    private val deqIdx = atomic(0)

    override fun enqueue(element: E) {
        while (true) {
            val i = enqIdx.getAndIncrement()
            if (infiniteArray[i].compareAndSet(null, element)) {
                return
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun dequeue(): E? {
        while (true) {
            if (deqIdx.value >= enqIdx.value) return null
            val i = deqIdx.getAndIncrement()
            if (!infiniteArray[i].compareAndSet(null, POISONED)) {
                return infiniteArray[i].value as E
            }
        }
    }
}

private val POISONED = Any()