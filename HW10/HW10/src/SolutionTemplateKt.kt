import java.util.concurrent.atomic.AtomicReference

class Solution(val env: Environment) : Lock<Solution.Node> {
    private val tail = AtomicReference<Node?>(null)

    override fun lock(): Node {
        val my = Node()
        val pred = tail.getAndSet(my)
        pred?.let {
            my.locked.set(true)
            it.next.set(my)
            while (my.locked.get()) {
                env.park()
            }
        }
        return my
    }

    override fun unlock(node: Node) {
        if (node.next.get() == null) {
            if (tail.compareAndSet(node, null)) {
                return
            }
            while (node.next.get() == null) {
                // busy wait
            }
        }
        node.next.get()?.locked?.set(false)
        env.unpark(node.next.get()?.thread ?: return)
    }

    class Node {
        val thread = Thread.currentThread()
        val locked = AtomicReference(false)
        val next = AtomicReference<Node?>(null)
    }
}
