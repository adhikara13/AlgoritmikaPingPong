import kotlinx.atomicfu.*

class MSQueue<E> : Queue<E> {
    private val head: AtomicRef<Node<E>>
    private val tail: AtomicRef<Node<E>>

    init {
        val dummy = Node<E>(null)
        head = atomic(dummy)
        tail = atomic(dummy)
    }

    override fun enqueue(element: E) {
        while (true) {
            val node = Node(element)
            val currentTail = tail.value
            val next = currentTail.next.value

            if (next == null) {
                if (currentTail.next.compareAndSet(null, node)) {
                    tail.compareAndSet(currentTail, node)
                    return
                }
            } else {
                tail.compareAndSet(currentTail, next)
            }
        }
    }

    override fun dequeue(): E? {
        while (true) {
            val currentHead = head.value
            val currentHeadNext = currentHead.next.value

            if (currentHeadNext != null) {
                if (head.compareAndSet(currentHead, currentHeadNext)) {
                    val element = currentHeadNext.element
                    currentHeadNext.element = null // Clear the element in the removed node
                    return element
                }
            } else {
                return null
            }
        }
    }


    // FOR TEST PURPOSE, DO NOT CHANGE IT.
    override fun validate() {
        check(tail.value.next.value == null) {
            "At the end of the execution, `tail.next` must be `null`"
        }
        check(head.value.element == null) {
            "At the end of the execution, the dummy node shouldn't store an element"
        }
    }

    private class Node<E>(
        var element: E?
    ) {
        val next = atomic<Node<E>?>(null)
    }
}
