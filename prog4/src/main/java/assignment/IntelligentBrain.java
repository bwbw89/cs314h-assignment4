package assignment;

public class IntelligentBrain implements Brain {

    private int bestScore;
    private final int[] SCORING_WEIGHTS = {5, 2, 10, 5};
    private Board.Action bestMove;

    /**
     * Decide what the next move should be based on the state of the board.
     */
    public Board.Action nextMove(Board currentBoard) {
        bestScore = Integer.MIN_VALUE;
        bestMove = Board.Action.NOTHING;

        Board rotatedBoard = currentBoard;

        // unlike LameBrain, evaluate rotations as well
        for (int rotations = 0; rotations < 4; rotations++) {
            // rotate object if not inital pos
            if (rotations > 0) {
                rotatedBoard = rotatedBoard.testMove(Board.Action.CLOCKWISE);

                // in case of collision skip/continue
                if (rotatedBoard.getLastResult() != Board.Result.SUCCESS) {
                    continue; 
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

            // check all left
            while (BoardR.getLastResult() == Board.Result.SUCCESS) {
                evaluateBoard(BoardR.testMove(Board.Action.DROP), 
                                                    rightFirst);
                BoardR.move(Board.Action.RIGHT);
            }
        }

        return bestMove;
    }

    private void evaluateBoard(Board currentBoard, Board.Action Move) {

        int curScore = scoreBoard(currentBoard);

        //update if the current action is better based on score
        if (bestScore < curScore) {
            bestScore = curScore;
            bestMove = Move;
        }
    }

    private int scoreBoard(Board newBoard) {

        int height_score = 100 - (newBoard.getMaxHeight() * SCORING_WEIGHTS[0]);

        int jagged_score = 50;
        for (int x = 0; x < newBoard.getWidth() - 1; x++) {
            jagged_score -= SCORING_WEIGHTS[1] * Math.abs(
                                                    newBoard.getColumnHeight(x)
                                                    - newBoard.getColumnHeight(x+1));
            
        }
        
        int clear_score = newBoard.getRowsCleared() * SCORING_WEIGHTS[2];

        int hole_score = 50;
        // although this can be optimized, 20x10 lookups is still O(1) and negligible
        for (int x = 0; x < newBoard.getWidth(); x++) {
            boolean blockFound = false;
            for (int y = newBoard.getColumnHeight(x) - 1; y >= 0; y--) {
                if (newBoard.getGrid(x, y) != null) {
                    blockFound = true;
                } else if (blockFound) {
                    hole_score -= SCORING_WEIGHTS[3];
                }
            }
        }

        return height_score + jagged_score + clear_score + hole_score;
    }

}
