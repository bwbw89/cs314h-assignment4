package assignment;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.*;

import org.junit.jupiter.api.Test;

import assignment.Piece.PieceType;

class TetrisPieceTest {
    static final int MAX = Integer.MAX_VALUE;

    // Every rotation of every piece, copied from the SRS chart (tetris.wiki/SRS).
    // Each picture is the bounding box, top row first.
    static final Object[][] SRS_CHART = {
        {PieceType.STICK, new String[][] {
            {"....", "####", "....", "...."}, {"..#.", "..#.", "..#.", "..#."},
            {"....", "....", "####", "...."}, {".#..", ".#..", ".#..", ".#.."}}},
        {PieceType.LEFT_L, new String[][] {
            {"#..", "###", "..."}, {".##", ".#.", ".#."}, {"...", "###", "..#"}, {".#.", ".#.", "##."}}},
        {PieceType.RIGHT_L, new String[][] {
            {"..#", "###", "..."}, {".#.", ".#.", ".##"}, {"...", "###", "#.."}, {"##.", ".#.", ".#."}}},
        {PieceType.SQUARE, new String[][] {
            {"##", "##"}, {"##", "##"}, {"##", "##"}, {"##", "##"}}},
        {PieceType.RIGHT_DOG, new String[][] {
            {".##", "##.", "..."}, {".#.", ".##", "..#"}, {"...", ".##", "##."}, {"#..", "##.", ".#."}}},
        {PieceType.T, new String[][] {
            {".#.", "###", "..."}, {".#.", ".##", ".#."}, {"...", "###", ".#."}, {".#.", "##.", ".#."}}},
        {PieceType.LEFT_DOG, new String[][] {
            {"##.", ".##", "..."}, {"..#", ".##", ".#."}, {"...", "##.", ".##"}, {".#.", "##.", "#.."}}},
    };

    static Set<Point> body(Piece p) {
        return new HashSet<>(Arrays.asList(p.getBody()));
    }

    /** Block positions from a picture of the bounding box, top row first. */
    static Set<Point> picture(String... rows) {
        Set<Point> blocks = new HashSet<>();
        for (int i = 0; i < rows.length; i++) {
            for (int x = 0; x < rows[i].length(); x++) {
                if (rows[i].charAt(x) == '#') blocks.add(new Point(x, rows.length - 1 - i));
            }
        }
        return blocks;
    }

    /** Lowest block in each column, or MAX for an empty column. */
    static int[] skirtOf(Set<Point> blocks, int width) {
        int[] skirt = new int[width];
        Arrays.fill(skirt, MAX);
        for (Point p : blocks) skirt[p.x] = Math.min(skirt[p.x], p.y);
        return skirt;
    }

    @Test
    void bodyAndSkirtMatchSrsChart() {
        for (Object[] row : SRS_CHART) {
            PieceType type = (PieceType) row[0];
            String[][] rotations = (String[][]) row[1];
            Piece p = new TetrisPiece(type);
            for (int r = 0; r < 4; r++) {
                String name = type + " rotation " + r;
                Set<Point> expected = picture(rotations[r]);
                assertEquals(type, p.getType(), name);
                assertEquals(r, p.getRotationIndex(), name);
                assertEquals(rotations[r].length, p.getWidth(), name);
                assertEquals(rotations[r].length, p.getHeight(), name);
                assertEquals(expected, body(p), name);
                assertArrayEquals(skirtOf(expected, p.getWidth()), p.getSkirt(), name);
                p = p.clockwisePiece();
            }
        }
    }

    @Test
    void specExamples() {
        assertArrayEquals(new int[] {0, 0}, new TetrisPiece(PieceType.SQUARE).getSkirt());
        // The spec's Right Dog example is rotation 3 of the provided spawn body.
        Piece dog = new TetrisPiece(PieceType.RIGHT_DOG).counterclockwisePiece();
        assertEquals(picture("#..", "##.", ".#."), body(dog));   // [(0,1), (0,2), (1,0), (1,1)]
        assertArrayEquals(new int[] {1, 0, MAX}, dog.getSkirt());
    }

    @Test
    void equalsAndHashCode() {
        Piece t = new TetrisPiece(PieceType.T);
        // Separately built pieces of the same type and rotation are equal.
        assertEquals(t, new TetrisPiece(PieceType.T));
        assertEquals(t.hashCode(), new TetrisPiece(PieceType.T).hashCode());
        assertEquals(t.clockwisePiece(), new TetrisPiece(PieceType.T).clockwisePiece());
        // A different rotation or type is not equal, even when the body looks the same (the square).
        assertNotEquals(t, t.clockwisePiece());
        assertNotEquals(t, new TetrisPiece(PieceType.STICK));
        Piece square = new TetrisPiece(PieceType.SQUARE);
        assertNotEquals(square, square.clockwisePiece());
        assertNotEquals(t, null);
    }

    @Test
    void nullTypeThrows() {
        assertThrows(NullPointerException.class, () -> new TetrisPiece(null));
    }

    // white box
    // These tests depend on how TetrisPiece is built
    // rotations are precomputed once and linked in a ring, and each skirt is computed
    // from its body in the constructor.

    @Test
    void rotationCycleReturnsTheSameObject() {
        // Same object (==), not just an equal one: rotating follows a link instead of
        // building a new piece, which is what makes rotation constant time.
        for (PieceType type : PieceType.values()) {
            Piece p = new TetrisPiece(type);
            assertSame(p, p.clockwisePiece().clockwisePiece().clockwisePiece().clockwisePiece(), type.toString());
            assertSame(p, p.clockwisePiece().counterclockwisePiece(), type.toString());
        }
    }

    @Test
    void skirtMatchesBody() {
        // Recompute each stored skirt from the piece's own body.
        for (PieceType type : PieceType.values()) {
            Piece p = new TetrisPiece(type);
            for (int r = 0; r < 4; r++) {
                assertArrayEquals(skirtOf(body(p), p.getWidth()), p.getSkirt(), type + " rotation " + r);
                p = p.clockwisePiece();
            }
        }
    }
}
