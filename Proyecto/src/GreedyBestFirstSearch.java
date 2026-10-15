import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

public final class GreedyBestFirstSearch<S> {

    public interface Graph<S> {
        Iterable<S> successors(S state);
    }

    public interface Heuristic<S> {
        double estimate(S state);
    }

    public static final class Result<S> {
        private final List<S> path;
        private final int expandedNodes;

        private Result(List<S> path, int expandedNodes) {
            this.path = Collections.unmodifiableList(path);
            this.expandedNodes = expandedNodes;
        }

        public boolean found() {
            return !path.isEmpty();
        }

        public List<S> getPath() {
            return path;
        }

        public int getExpandedNodes() {
            return expandedNodes;
        }
    }

    private static final class Node<S> {
        private final S state;
        private final Node<S> parent;
        private final double heuristic;
        private final long order;

        private Node(S state, Node<S> parent, double heuristic, long order) {
            this.state = state;
            this.parent = parent;
            this.heuristic = heuristic;
            this.order = order;
        }
    }

    public Result<S> search(S start, S goal, Graph<S> graph, Heuristic<S> heuristic) {
        Objects.requireNonNull(start, "El estado inicial no puede ser nulo.");
        Objects.requireNonNull(goal, "La meta no puede ser nula.");
        Objects.requireNonNull(graph, "El grafo no puede ser nulo.");
        Objects.requireNonNull(heuristic, "La heurística no puede ser nula.");

        Comparator<Node<S>> byHeuristic = Comparator
                .comparingDouble((Node<S> node) -> node.heuristic)
                .thenComparingLong(node -> node.order);
        PriorityQueue<Node<S>> frontier = new PriorityQueue<>(byHeuristic);
        Set<S> discovered = new HashSet<>();
        long insertionOrder = 0;

        frontier.add(new Node<>(start, null, estimate(heuristic, start), insertionOrder++));
        discovered.add(start);
        int expandedNodes = 0;

        while (!frontier.isEmpty()) {
            Node<S> current = frontier.remove();
            expandedNodes++;

            if (current.state.equals(goal)) {
                return new Result<>(buildPath(current), expandedNodes);
            }

            for (S successor : graph.successors(current.state)) {
                if (discovered.add(successor)) {
                    frontier.add(new Node<>(successor, current, estimate(heuristic, successor), insertionOrder++));
                }
            }
        }

        return new Result<>(Collections.<S>emptyList(), expandedNodes);
    }

    private double estimate(Heuristic<S> heuristic, S state) {
        double value = heuristic.estimate(state);
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
            throw new IllegalArgumentException("La heurística debe producir un valor finito no negativo.");
        }
        return value;
    }

    private List<S> buildPath(Node<S> goalNode) {
        List<S> path = new ArrayList<>();
        for (Node<S> node = goalNode; node != null; node = node.parent) {
            path.add(node.state);
        }
        Collections.reverse(path);
        return path;
    }
}
