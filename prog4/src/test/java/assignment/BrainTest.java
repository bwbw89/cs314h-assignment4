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

    /** A 10 x 24 board whose bottom rows match the picture (top row first); the rest is empty. */
    static TetrisBoard boardWithBottom(String... bottomRows) {
        String[] rows = new String[24];
        Arrays.fill(rows, "..........");
        System.arraycopy(bottomRows, 0, rows, 24 - bottomRows.length, bottomRows.length);
        return TetrisBoardTest.board(rows);
    }

    /** Lets the brain play the current piece until it is placed. */
    static void playPiece(Brain brain, Board b) {
        for (int i = 0; i < 200; i++) {
            if (b.move(brain.nextMove(b)) == Result.PLACE) return;
        }
        fail("brain never placed the piece");
    }

    /** Empty cells with a block somewhere above them in the same column. */
    static int countHoles(Board b) {
        int holes = 0;
        for (int x = 0; x < b.getWidth(); x++) {
            for (int y = 0; y < b.getColumnHeight(x); y++) {
                if (b.getGrid(x, y) == null) holes++;
            }
        }
        return holes;
    }

    @Test
    void stickClearsRowWithOneWideGap() {
        TetrisBoard b = boardWithBottom("#########.");
        b.nextPiece(new TetrisPiece(PieceType.STICK), new Point(3, 20));
        playPiece(new IntelligentBrain(), b);
        assertEquals(1, b.getRowsCleared());
    }

    @Test
    void prefersFlatPlacementOverHole() {
        // A square at x = 1 would leave a hole under its right half.
        TetrisBoard b = boardWithBottom("##........");
        b.nextPiece(new TetrisPiece(PieceType.SQUARE), new Point(4, 20));
        playPiece(new IntelligentBrain(), b);
        assertEquals(0, countHoles(b));
    }

    @Test
    void noCurrentPieceDoesNotThrow() {
        assertDoesNotThrow(() -> new IntelligentBrain().nextMove(new TetrisBoard(10, 24)));
    }

    // white box
    // These tests depend on how the brain and JBrainTetris work inside: the brain should
    // only explore with testMove, and the JBrainTetris test drives JTetris's protected
    // timer and game-state fields directly.

    @Test
    void nextMoveHasNoSideEffects() {
        TetrisBoard b = boardWithBottom("#.........", "###..##.##");
        b.nextPiece(new TetrisPiece(PieceType.T), new Point(4, 20));
        Board before = b.testMove(Action.NOTHING);
        new IntelligentBrain().nextMove(b);
        assertEquals(before, b);
        // move() would have changed the last action; testMove() never touches this board.
        assertEquals(Action.NOTHING, b.getLastAction(), "nextMove should only use testMove");
    }

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

    /** Plays one game of at most maxPieces pieces and returns the rows cleared. */
    static int playGame(Brain brain, long seed, int maxPieces) {
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
            cleared += b.getRowsCleared();
            if (b.getMaxHeight() > 20) break;
        }
        return cleared;
    }

    @Test
    void beatsLameBrainOnTheSameSeeds() {
        int games = 50;
        double[] ours = new double[games], lame = new double[games];
        for (int g = 0; g < games; g++) {
            ours[g] = playGame(new IntelligentBrain(), g, 500);
            lame[g] = playGame(new LameBrain(), g, 500);
        }
        System.out.printf("Lines cleared over %d games: ours %.1f +/- %.1f, LameBrain %.1f +/- %.1f%n",
            games, mean(ours), std(ours), mean(lame), std(lame));
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
