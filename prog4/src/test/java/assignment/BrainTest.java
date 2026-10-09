package assignment;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

import assignment.Board.Action;
import assignment.Board.Result;
import assignment.Piece.PieceType;

class BrainTest {

    /* A 10 x 24 board */
    static TetrisBoard boardWithBottom(String... bottomRows) {
        String[] rows = new String[24];
        Arrays.fill(rows, "..........");
        System.arraycopy(bottomRows, 0, rows, 24 - bottomRows.length, bottomRows.length);
        return TetrisBoardTest.board(rows);
    }

    /* Lets the brain play the current piece until it is placed. */
    static void playPiece(Brain brain, Board b) {
        for (int i = 0; i < 200; i++) {
            if (b.move(brain.nextMove(b)) == Result.PLACE) return;
        }
        fail("brain never placed the piece");
    }

    /* Empty cells with a block somewhere above them in the same column. */
    // Essentially sandwiched holes
    static int countHoles(Board b) {
        int holes = 0;
        for (int x = 0; x < b.getWidth(); x++) {
            for (int y = 0; y < b.getColumnHeight(x); y++) {
                if (b.getGrid(x, y) == null) holes++;
            }
        }
        return holes;
    }

    // check if brain places a stick vertically through a gap
    @Test
    void stickClearsRowWithOneWideGap() {
        TetrisBoard b = boardWithBottom("#########.");
        b.nextPiece(new TetrisPiece(PieceType.STICK), new Point(3, 20));
        playPiece(new IntelligentBrain(), b);
        assertEquals(1, b.getRowsCleared());
    }

    // tests to NOT place in a way that would make holes
    @Test
    void prefersFlatPlacementOverHole() {
        // A square at x = 1 would leave a hole under its right half.
        TetrisBoard b = boardWithBottom("##........");
        b.nextPiece(new TetrisPiece(PieceType.SQUARE), new Point(4, 20));
        playPiece(new IntelligentBrain(), b);
        assertEquals(0, countHoles(b));
    }

    // if no current piece error
    @Test
    void noCurrentPieceDoesNotThrow() {
        assertDoesNotThrow(() -> new IntelligentBrain().nextMove(new TetrisBoard(10, 24)));
    }

    // white box

    @Test
    void nextMoveHasNoSideEffects() {
        // generic board
        TetrisBoard b = boardWithBottom("#.........", "###..##.##");
        b.nextPiece(new TetrisPiece(PieceType.T), new Point(4, 20));
        Board before = b.testMove(Action.NOTHING);
        new IntelligentBrain().nextMove(b);
        assertEquals(before, b);
        // move() would have changed the last action; testMove() never touches this board.
        assertEquals(Action.NOTHING, b.getLastAction(), "nextMove should only use testMove");
    }

    // checks if brain works on an empty game
    @Test
    void jBrainTetrisPlaysWithoutAWindow() {
        JBrainTetris game = new JBrainTetris(true, 1);
        game.createControlPanel();   // creates the labels startGame() writes to
        game.startGame();
        game.timer.stop();           // call tick ourselves instead of waiting on the timer
        for (int i = 0; i < 2000 && game.gameOn; i++) {
            game.tick(Action.DOWN);
        }
        assertTrue(game.count > 20, "pieces played: " + game.count);
    }

    // statistical

    /* Plays one game of at most maxPieces pieces and returns the rows cleared. */
    // used for comparison with lamebrain
    static int playGame(Brain brain, long seed, int maxPieces) {
        // can set seed
        Random r = new Random(seed);
        TetrisBoard b = new TetrisBoard(10, 24);
        int cleared = 0;
        for (int n = 0; n < maxPieces; n++) {
            Piece p = new TetrisPiece(PieceType.values()[r.nextInt(7)]);
            try {
                b.nextPiece(p, new Point(5 - p.getWidth() / 2, 20));
            } catch (IllegalArgumentException gameOver) {
                break;
            }
            playPiece(brain, b);
            // want to see how many rows cleared for comparison
            cleared += b.getRowsCleared();
            if (b.getMaxHeight() > 20) break;
        }
        return cleared;
    }

    // test to see if lamebrain is beaten
    @Test
    void beatsLameBrainOnTheSameSeeds() {
        int games = 50;
        // intell vs lame
        double[] ours = new double[games], lame = new double[games];
        for (int g = 0; g < games; g++) {
            ours[g] = playGame(new IntelligentBrain(), g, 500);
            lame[g] = playGame(new LameBrain(), g, 500);
        }
        System.out.printf("Lines cleared over %d games: ours %.1f +/- %.1f, LameBrain %.1f +/- %.1f%n",
            games, mean(ours), std(ours), mean(lame), std(lame));
        // see if ours better
        assertTrue(mean(ours) > mean(lame));
    }

    static double mean(double[] a) {
        return Arrays.stream(a).average().orElse(0);
    }

    static double std(double[] a) {
        double m = mean(a);
        return Math.sqrt(Arrays.stream(a).map(v -> (v - m) * (v - m)).sum() / a.length);
    }
}
