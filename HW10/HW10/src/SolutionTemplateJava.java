import java.util.concurrent.atomic.AtomicReference;

public class SolutionTemplateJava implements Lock<SolutionTemplateJava.Node> {
    private final Environment env;
    private final AtomicReference<Node> tail = new AtomicReference<>(null);

    public SolutionTemplateJava(Environment env) {
        this.env = env;
    }

    @Override
    public Node lock() {
        Node my = new Node();
        Node pred = tail.getAndSet(my);
        if (pred != null) {
            my.locked.set(true);
            pred.next.set(my);
            while (my.locked.get()) {
                env.park();
            }
        }
        return my;
    }

    @Override
    public void unlock(Node node) {
        if (node.next.get() == null) {
            if (tail.compareAndSet(node, null)) {
                return;
            }
            while (node.next.get() == null) {
                // busy wait
            }
        }
        node.next.get().locked.set(false);
        env.unpark(node.next.get().thread);
    }

    static class Node {
        final Thread thread = Thread.currentThread();
        final AtomicReference<Boolean> locked = new AtomicReference<>(false);
        final AtomicReference<Node> next = new AtomicReference<>(null);
    }
}
