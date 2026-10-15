import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EightPuzzleSolver {
    private static final int WIDTH = 3;
    private static final int CELL_COUNT = WIDTH * WIDTH;
    private static final int[] GOAL = {1, 2, 3, 4, 5, 6, 7, 8, 0};

    private EightPuzzleSolver() {
    }

    public static final class Result {
        private final List<int[]> path;
        private final int expandedNodes;

        private Result(List<State> states, int expandedNodes) {
            List<int[]> boardPath = new ArrayList<>();
            for (State state : states) {
                boardPath.add(state.board.clone());
            }
            this.path = Collections.unmodifiableList(boardPath);
            this.expandedNodes = expandedNodes;
        }

        public boolean found() {
            return !path.isEmpty();
        }

        public List<int[]> getPath() {
            List<int[]> copy = new ArrayList<>();
            for (int[] board : path) {
                copy.add(board.clone());
            }
            return Collections.unmodifiableList(copy);
        }

        public int getExpandedNodes() {
            return expandedNodes;
        }
    }

    private static final class State {
        private final int[] board;
        private final int blankPosition;

        private State(int[] board) {
            this.board = board;
            int blank = -1;
            for (int i = 0; i < board.length; i++) {
                if (board[i] == 0) {
                    blank = i;
                    break;
                }
            }
            this.blankPosition = blank;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof State)) {
                return false;
            }
            return Arrays.equals(board, ((State) other).board);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(board);
        }
    }

    public static Result solve(int[] initialBoard) {
        validateBoard(initialBoard, "El tablero inicial");
        State start = new State(initialBoard.clone());
        State goal = new State(GOAL.clone());

        if (!isSolvable(start.board, goal.board)) {
            return new Result(Collections.<State>emptyList(), 0);
        }

        GreedyBestFirstSearch<State> search = new GreedyBestFirstSearch<>();
        GreedyBestFirstSearch.Result<State> result = search.search(
                start,
                goal,
                EightPuzzleSolver::successors,
                state -> euclideanDistance(state.board, goal.board));
        return new Result(result.getPath(), result.getExpandedNodes());
    }

    public static double euclideanDistance(int[] board) {
        validateBoard(board, "El tablero");
        return euclideanDistance(board, GOAL);
    }

    private static double euclideanDistance(int[] board, int[] goal) {
        double distance = 0;
        for (int position = 0; position < board.length; position++) {
            int tile = board[position];
            if (tile == 0) {
                continue;
            }

            int goalPosition = indexOf(goal, tile);
            int rowDifference = position / WIDTH - goalPosition / WIDTH;
            int columnDifference = position % WIDTH - goalPosition % WIDTH;
            distance += Math.sqrt(rowDifference * rowDifference + columnDifference * columnDifference);
        }
        return distance;
    }

    private static Iterable<State> successors(State state) {
        List<State> successors = new ArrayList<>(4);
        int row = state.blankPosition / WIDTH;
        int column = state.blankPosition % WIDTH;

        if (row > 0) {
            successors.add(moveBlank(state, state.blankPosition - WIDTH));
        }
        if (row < WIDTH - 1) {
            successors.add(moveBlank(state, state.blankPosition + WIDTH));
        }
        if (column > 0) {
            successors.add(moveBlank(state, state.blankPosition - 1));
        }
        if (column < WIDTH - 1) {
            successors.add(moveBlank(state, state.blankPosition + 1));
        }
        return successors;
    }

    private static State moveBlank(State state, int neighborPosition) {
        int[] nextBoard = state.board.clone();
        nextBoard[state.blankPosition] = nextBoard[neighborPosition];
        nextBoard[neighborPosition] = 0;
        return new State(nextBoard);
    }

    private static boolean isSolvable(int[] board, int[] goal) {
        Map<Integer, Integer> goalOrder = new HashMap<>();
        int order = 0;
        for (int tile : goal) {
            if (tile != 0) {
                goalOrder.put(tile, order++);
            }
        }

        int inversions = 0;
        for (int i = 0; i < board.length; i++) {
            if (board[i] == 0) {
                continue;
            }
            for (int j = i + 1; j < board.length; j++) {
                if (board[j] != 0 && goalOrder.get(board[i]) > goalOrder.get(board[j])) {
                    inversions++;
                }
            }
        }
        return inversions % 2 == 0;
    }

    private static void validateBoard(int[] board, String label) {
        if (board == null || board.length != CELL_COUNT) {
            throw new IllegalArgumentException(label + " debe contener exactamente 9 valores.");
        }

        boolean[] seen = new boolean[CELL_COUNT];
        for (int value : board) {
            if (value < 0 || value >= CELL_COUNT || seen[value]) {
                throw new IllegalArgumentException(label + " debe contener una vez cada valor del 0 al 8.");
            }
            seen[value] = true;
        }
    }

    private static int indexOf(int[] board, int value) {
        for (int i = 0; i < board.length; i++) {
            if (board[i] == value) {
                return i;
            }
        }
        throw new IllegalArgumentException("La ficha no existe en el tablero meta: " + value);
    }
}
