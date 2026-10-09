package assignment;

public class IntelligentBrain implements Brain {

    // indices into SCORING_WEIGHTS
    private static final int W_MAX_HEIGHT = 0;
    private static final int W_JAGGED = 1;
    private static final int W_CLEAR = 2;
    private static final int W_HOLES = 3;
    private static final int W_AGG_HEIGHT = 4;
    private static final int W_DANGER_SCALE = 5;
    private static final int W_DANGER_START = 6;

    // {maxHeight, jaggedness, rowsCleared, holes, aggregateHeight,
    //  dangerScale, dangerStartHeight}
    private static final double[] SCORING_WEIGHTS = {2.53, 8.52, 21.14, 27.37, 16.29, 
                                                     6.87, 20.01};

    // a losing stack is only picked if every other move loses too
    private static final double LOSS_PENALTY = 1_000_000;

    private double bestScore;
    private Board.Action bestMove;

    /**
     * Decide what the next move should be based on the state of the board.
     */
    public Board.Action nextMove(Board currentBoard) {
        bestScore = Double.NEGATIVE_INFINITY;
        bestMove = Board.Action.NOTHING;

        Board rotatedBoard = currentBoard;

        // unlike LameBrain, evaluate rotations as well
        for (int rotations = 0; rotations < 4; rotations++) {
            // rotate object if not inital pos
            if (rotations > 0) {
                rotatedBoard = rotatedBoard.testMove(Board.Action.CLOCKWISE);

                // in case of collision, leave
                if (rotatedBoard.getLastResult() != Board.Result.SUCCESS) {
                    break;
                }
            }
            // first move check
            Board.Action baseFirst = (rotations > 0) ? Board.Action.CLOCKWISE
                                                            : Board.Action.DROP;

            // check dropping off as given
            evaluateBoard(rotatedBoard.testMove(Board.Action.DROP),
                                                baseFirst);

            // first left
            Board BoardL = rotatedBoard.testMove(Board.Action.LEFT);
            Board.Action leftFirst = (rotations > 0) ? Board.Action.CLOCKWISE
                                            : Board.Action.LEFT;

            // check all left
            while (BoardL.getLastResult() == Board.Result.SUCCESS) {
                evaluateBoard(BoardL.testMove(Board.Action.DROP),
                                                    leftFirst);
                BoardL.move(Board.Action.LEFT);
            }

            // first right
            Board BoardR = rotatedBoard.testMove(Board.Action.RIGHT);
            Board.Action rightFirst = (rotations > 0) ? Board.Action.CLOCKWISE
                                            : Board.Action.RIGHT;

            // check all right
            while (BoardR.getLastResult() == Board.Result.SUCCESS) {
                evaluateBoard(BoardR.testMove(Board.Action.DROP),
                                                    rightFirst);
                BoardR.move(Board.Action.RIGHT);
            }
        }

        return bestMove;
    }

    private void evaluateBoard(Board currentBoard, Board.Action Move) {

        double current = scoreBoard(currentBoard);

        //update if the current action is better based on score
        if (bestScore < current) {
            bestScore = current;
            bestMove = Move;
        }
    }

    private double scoreBoard(Board board) {

        int width = board.getWidth();
        int height = board.getHeight();

        // worst possible values, only here to keep every score positive
        int worstAggregate = width * height;
        int worstJagged = (width - 1) * height;
        int worstHoles = width * height;

        // max height times a weight
        int maxHeight = board.getMaxHeight();
        double height_score = (height - maxHeight) * SCORING_WEIGHTS[W_MAX_HEIGHT];

        // how bumpy/jagged the top level of the current board is times a weight
        int jagged = 0;
        for (int x = 0; x < width - 1; x++) {
            jagged += Math.abs(board.getColumnHeight(x) - board.getColumnHeight(x + 1));
        }
        double jagged_score = (worstJagged - jagged) * SCORING_WEIGHTS[W_JAGGED];

        // amount of rows cleared times a weight
        double clear_score = board.getRowsCleared() * SCORING_WEIGHTS[W_CLEAR];

        // sum of height at each collumn and buried empty spaces (holes)
        int aggregate = 0;
        int holes = 0;

        // although this can be optimized, 20x10 lookups is still O(1) and pretty
        // much negligible and we are only hand testing weights
        for (int x = 0; x < width; x++) {

            aggregate += board.getColumnHeight(x);

            boolean blockFound = false;
            for (int y = board.getColumnHeight(x) - 1; y >= 0; y--) {
                if (board.getGrid(x, y) != null) {
                    blockFound = true;
                } else if (blockFound) {
                    holes++;
                }
            }
        }

        // multiplying the weights
        double ag_height_score = (worstAggregate - aggregate) * SCORING_WEIGHTS[W_AGG_HEIGHT];
        double hole_score = (worstHoles - holes) * SCORING_WEIGHTS[W_HOLES];

        // punish stacks near the top harder the closer they get (both tuned)
        double danger_penalty = 0;
        double over = maxHeight - SCORING_WEIGHTS[W_DANGER_START];
        if (over > 0) {
            danger_penalty = SCORING_WEIGHTS[W_DANGER_SCALE] * over * over;
        }

        // stack past the top of the board means game over
        if (maxHeight > JTetris.HEIGHT) {
            danger_penalty += LOSS_PENALTY;
        }

        return height_score + ag_height_score + jagged_score + clear_score + hole_score
                - danger_penalty;
    }

}