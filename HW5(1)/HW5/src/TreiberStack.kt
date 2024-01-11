import java.util.concurrent.atomic.AtomicReference

/**
 * @author TODO: Last Name, First Name
 */
class TreiberStack<E> : Stack<E> {
    private val top = AtomicReference<Node<E>?>()

    override fun push(element: E) {
        while (true) {
            val curTop = top.get()
            val newTop = Node(element, curTop)
            if (top.compareAndSet(curTop, newTop)) {
                return
            }
        }
    }

    override fun pop(): E? {
        while (true) {
            val curTop = top.get() ?: return null
            if (top.compareAndSet(curTop, curTop.next)) {
                return curTop.element
            }
        }
    }

    private class Node<E>(val element: E, val next: Node<E>?)
}

