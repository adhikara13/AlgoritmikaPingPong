import java.util.concurrent.*
import java.util.concurrent.atomic.*

/**
 * @author TODO: Last Name, First Name
 */
open class TreiberStackWithElimination<E> : Stack<E> {
    private val stack = TreiberStack<E>()
    private val eliminationArray = AtomicReferenceArray<Any?>(ELIMINATION_ARRAY_SIZE)

    override fun push(element: E) {
        if (tryPushElimination(element)) return
        stack.push(element)
    }

    override fun pop(): E? {
        return tryPopElimination() ?: stack.pop()
    }

    protected open fun tryPushElimination(element: E): Boolean {
        val index = randomCellIndex()
        if (eliminationArray.compareAndSet(index, CELL_STATE_EMPTY, element)) {
            for (i in 0 until ELIMINATION_WAIT_CYCLES) {
                if (eliminationArray.compareAndSet(index, element, CELL_STATE_EMPTY)) {
                    return false // Element was not taken by pop, retry in main stack
                }
                if (eliminationArray.get(index) === CELL_STATE_RETRIEVED) {
                    eliminationArray.set(index, CELL_STATE_EMPTY)
                    return true // Element was successfully eliminated
                }
            }
            eliminationArray.compareAndSet(index, element, CELL_STATE_EMPTY) // Clean up if timeout occurs
        }
        return false
    }

    private fun tryPopElimination(): E? {
        val index = randomCellIndex()
        var element: Any?
        do {
            element = eliminationArray.get(index)
            if (element == CELL_STATE_EMPTY || element == CELL_STATE_RETRIEVED) {
                return null // No element to pop or already retrieved
            }
        } while (!eliminationArray.compareAndSet(index, element, CELL_STATE_RETRIEVED))
        return element as E
    }

    private fun randomCellIndex(): Int =
        ThreadLocalRandom.current().nextInt(eliminationArray.length())

    companion object {
        private const val ELIMINATION_ARRAY_SIZE = 2 // Do not change!
        private const val ELIMINATION_WAIT_CYCLES = 1 // Do not change!

        // Initially, all cells are in EMPTY state.
        private val CELL_STATE_EMPTY = null

        // `tryPopElimination()` moves the cell state to `RETRIEVED` if the cell contains element.
        private val CELL_STATE_RETRIEVED = Any()
    }
}