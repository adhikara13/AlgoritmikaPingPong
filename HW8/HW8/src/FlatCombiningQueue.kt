import kotlinx.atomicfu.*
import java.util.concurrent.*

class FlatCombiningQueue<E> : Queue<E> {
    private val sequentialQueue = ArrayDeque<E>() // Renamed to clarify it's the sequential queue
    private val combinerLock = atomic(false) // Renamed for clarity, it represents the lock status
    private val tasksForCombiner = atomicArrayOfNulls<Any?>(TASKS_FOR_COMBINER_SIZE)
    private val valsForCombiner = atomicArrayOfNulls<Any?>(TASKS_FOR_COMBINER_SIZE)

    private fun tryLock() = combinerLock.compareAndSet(false, update = true)
    
    private fun unlock() {
        combinerLock.value = false
    }

    @Suppress("UNCHECKED_CAST")
    private fun help() {
        for (i in 0 until TASKS_FOR_COMBINER_SIZE) {
            val currentTask = tasksForCombiner[i].value

            if (currentTask == null || currentTask == PROCESSED) {
                continue
            }

            if (currentTask == DEQUE_TASK) {
                valsForCombiner[i].value = sequentialQueue.removeFirstOrNull()
            }

            if (tasksForCombiner[i].compareAndSet(DEQUE_TASK, PROCESSED)) {
                continue
            }

            if (tasksForCombiner[i].compareAndSet(currentTask, PROCESSED)) {
                sequentialQueue.addLast(currentTask as E)
            }
        }
        unlock()
    }

    override fun enqueue(element: E) {
        val randomCellIndex = randomCellIndex()

        while (true) {
            if (tasksForCombiner[randomCellIndex].compareAndSet(null, element)) {
                while (!tryLock()) {
                    if (tasksForCombiner[randomCellIndex].compareAndSet(PROCESSED, null)) {
                        return
                    }
                }

                if (tasksForCombiner[randomCellIndex].compareAndSet(element, null)) {
                    sequentialQueue.addLast(element)
                    help()
                    return
                }

                tasksForCombiner[randomCellIndex].compareAndSet(PROCESSED, null)
                help()
                return
            }
        }
    }

    override fun dequeue(): E? {
        val randomCellIndex = randomCellIndex()

        @Suppress("UNCHECKED_CAST")
        while (true) {
            if (tasksForCombiner[randomCellIndex].compareAndSet(null, DEQUE_TASK)) {
                while (!tryLock()) {
                    if (tasksForCombiner[randomCellIndex].value == PROCESSED) {
                        return (valsForCombiner[randomCellIndex].value as E).also {
                            tasksForCombiner[randomCellIndex].compareAndSet(PROCESSED, null)
                        }
                    }
                }

                if (tasksForCombiner[randomCellIndex].compareAndSet(DEQUE_TASK, null)) {
                    return sequentialQueue.removeFirstOrNull().also {
                        help()
                    }
                }

                return (valsForCombiner[randomCellIndex].value as E).also {
                    tasksForCombiner[randomCellIndex].compareAndSet(PROCESSED, null)
                    help()
                }
            }
        }
    }

    private fun randomCellIndex(): Int =
        ThreadLocalRandom.current().nextInt(tasksForCombiner.size)
}

private const val TASKS_FOR_COMBINER_SIZE = 3 // Do not change this constant!
private val DEQUE_TASK = Any()
private val PROCESSED = Any()

// Renamed to clarify its purpose.
private object DequeueTask

// Result class is clarified with a more descriptive name.
private class ResultWrapper<V>(
    val value: V
)
