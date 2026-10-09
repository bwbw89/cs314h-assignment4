package assignment;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.Random;

import org.junit.jupiter.api.Test;

import assignment.Board.Action;
import assignment.Board.Result;
import assignment.Piece.PieceType;

class TetrisBoardTest {

    // helpers

    /* Builds a board top to bottom '#' is a placed block, '.' is empty. */
    static TetrisBoard board(String... rows) {
        int h = rows.length, w = rows[0].length();
        PieceType[][] cells = new PieceType[h][w];
        for (int i = 0; i < h; i++) {
            for (int x = 0; x < w; x++) {
                if (rows[i].charAt(x) == '#') cells[h - 1 - i][x] = PieceType.SQUARE;
            }
        }
        return new TetrisBoard(cells);
    }

    /* An empty 8 x 10 board with a T in spawn rotation. */
    static TetrisBoard withT(int x, int y) {
        TetrisBoard b = new TetrisBoard(8, 10);
        b.nextPiece(new TetrisPiece(PieceType.T), new Point(x, y));
        return b;
    }

    /* Vertical stick rotation */
    static Piece verticalStick() {
        return new TetrisPiece(PieceType.STICK).clockwisePiece();
    }

    /* Recomputes row widths, column heights and max height from the grid and compares. */
    // comparison to cached is important
    static void checkStoredValues(Board b) {
        int max = 0;
        for (int x = 0; x < b.getWidth(); x++) {
            int h = 0;
            for (int y = 0; y < b.getHeight(); y++) {
                if (b.getGrid(x, y) != null) h = y + 1;
            }
            // height still the same per column
            assertEquals(h, b.getColumnHeight(x), "column height " + x);
            max = Math.max(max, h);
        }

        // max height still same for skirt
        assertEquals(max, b.getMaxHeight(), "max height");
        for (int y = 0; y < b.getHeight(); y++) {
            int w = 0;
            for (int x = 0; x < b.getWidth(); x++) {
                if (b.getGrid(x, y) != null) w++;
            }

            // occupied blocks in row still same
            assertEquals(w, b.getRowWidth(y), "row width " + y);
        }
    }

    // new board

    // check if all blocks empty
    @Test
    void newBoardIsEmpty() {
        TetrisBoard b = new TetrisBoard(10, 24);
        // check if defaults are correct
        assertEquals(10, b.getWidth());
        assertEquals(24, b.getHeight());
        assertEquals(0, b.getMaxHeight());
        assertEquals(0, b.getRowsCleared());
        assertNull(b.getCurrentPiece());
        assertNull(b.getCurrentPiecePosition());
        checkStoredValues(b);
    }

    // movement

    // simple left and right movement no obstacles
    @Test
    void leftAndRightInOpenSpace() {
        TetrisBoard b = withT(3, 4);
        assertEquals(Result.SUCCESS, b.move(Action.LEFT));
        assertEquals(new Point(2, 4), b.getCurrentPiecePosition());
        assertEquals(Result.SUCCESS, b.move(Action.RIGHT));
        assertEquals(new Point(3, 4), b.getCurrentPiecePosition());
    }

    // left and right movement with wall
    @Test
    void leftAndRightBlocked() {
        TetrisBoard leftWall = withT(0, 4);
        assertEquals(Result.OUT_BOUNDS, leftWall.move(Action.LEFT));
        assertEquals(new Point(0, 4), leftWall.getCurrentPiecePosition());

        TetrisBoard rightWall = withT(5, 4);   // blocks in columns 5..7
        assertEquals(Result.OUT_BOUNDS, rightWall.move(Action.RIGHT));
        assertEquals(new Point(5, 4), rightWall.getCurrentPiecePosition());

        TetrisBoard block = board(
            "......",
            "#.....",   // y = 2, beside the T's flat row
            "......",
            "......");
        // blocked by existing block
        block.nextPiece(new TetrisPiece(PieceType.T), new Point(1, 1));
        assertEquals(Result.OUT_BOUNDS, block.move(Action.LEFT));
        assertEquals(new Point(1, 1), block.getCurrentPiecePosition());
    }

    // test dropping
    @Test
    void downAndDropPlaceThePiece() {
        TetrisBoard down = withT(0, -1);   // flat row on y = 0
        assertEquals(Result.PLACE, down.move(Action.DOWN));
        assertNull(down.getCurrentPiece());
        assertEquals(PieceType.T, down.getGrid(0, 0));
        assertEquals(PieceType.T, down.getGrid(2, 0));
        assertEquals(PieceType.T, down.getGrid(1, 1));

        TetrisBoard drop = board(
            "......",
            "......",
            "......",
            "......",
            "..#...");
        // dropping on top
        drop.nextPiece(new TetrisPiece(PieceType.T), new Point(1, 2));
        assertEquals(Result.PLACE, drop.move(Action.DROP));
        assertEquals(PieceType.T, drop.getGrid(1, 1));   // lands on top of the block
        assertEquals(PieceType.T, drop.getGrid(2, 2));
    }

    // if no current piece, should return no piece
    @Test
    void noCurrentPieceReturnsNoPiece() {
        TetrisBoard b = new TetrisBoard(8, 10);
        for (Action a : Action.values()) {
            assertEquals(Result.NO_PIECE, b.move(a), a.toString());
        }
    }

    // no action results in nothing happening
    @Test
    void nothingChangesNothing() {
        TetrisBoard b = withT(3, 4);
        Board before = b.testMove(Action.NOTHING);
        assertEquals(Result.SUCCESS, b.move(Action.NOTHING));
        assertEquals(before, b);
        assertEquals(new Point(3, 4), b.getCurrentPiecePosition());
    }

    // checking last action fetchers
    @Test
    void lastActionAndResultTrackEveryMove() {
        TetrisBoard b = withT(0, 4);
        Object[][] steps = {
            {Action.LEFT, Result.OUT_BOUNDS},   // failed moves are recorded too
            {Action.RIGHT, Result.SUCCESS},
            {Action.CLOCKWISE, Result.SUCCESS},
            {Action.DOWN, Result.SUCCESS},
            {Action.DROP, Result.PLACE},
            {Action.LEFT, Result.NO_PIECE},
        };
        for (Object[] step : steps) {
            b.move((Action) step[0]);
            assertEquals(step[0], b.getLastAction());
            assertEquals(step[1], b.getLastResult(), step[0].toString());
        }
    }

    // bounding box might go below the floor (should still place)
    @Test
    void tDropsToNegativeY() {
        TetrisBoard b = withT(3, 6);
        while (b.testMove(Action.DOWN).getLastResult() == Result.SUCCESS) {
            b.move(Action.DOWN);
        }
        // The T's bottom box row is empty, so the box ends one row below the floor.
        assertEquals(new Point(3, -1), b.getCurrentPiecePosition());
    }

    // rotation

    // checking if srs wall kick logic is proper
    @Test
    void wallKickUsesFirstOffsetThatFits() {
        TetrisBoard b = new TetrisBoard(8, 10);
        b.nextPiece(new TetrisPiece(PieceType.T).clockwisePiece(), new Point(-1, 4));
        // R -> 2: offset (0,0) puts a block at x = -1; the next offset, (+1,0), fits.
        assertEquals(Result.SUCCESS, b.move(Action.CLOCKWISE));
        assertEquals(2, b.getCurrentPiece().getRotationIndex());
        assertEquals(new Point(0, 4), b.getCurrentPiecePosition());
    }

    // counterclockwise kick
    @Test
    void counterclockwiseKickUsesItsOwnOffsets() {
        TetrisBoard b = board(
            "........",
            "........",
            "........",
            "........",
            "........",
            "....#...",   // y = 4: blocks the (0,0) test for 0 -> L
            "........",
            "........",
            "........",
            "........");
        b.nextPiece(new TetrisPiece(PieceType.T), new Point(3, 4));
        // 0 -> L: the second offset is (+1,0). The clockwise table would give (-1,0) instead.
        assertEquals(Result.SUCCESS, b.move(Action.COUNTERCLOCKWISE));
        assertEquals(3, b.getCurrentPiece().getRotationIndex());
        assertEquals(new Point(4, 4), b.getCurrentPiecePosition());
    }

    @Test
    void stickUsesItsOwnKickTable() {
        TetrisBoard b = new TetrisBoard(8, 10);
        b.nextPiece(verticalStick(), new Point(-2, 4));   // against the left wall
        // R -> 2 needs the stick table's (+2,0) offset; every offset in the normal table fails
        assertEquals(Result.SUCCESS, b.move(Action.CLOCKWISE));
        assertEquals(2, b.getCurrentPiece().getRotationIndex());
        assertEquals(new Point(0, 4), b.getCurrentPiecePosition());
    }

    // square kicks shouldn't move the square
    @Test
    void squareRotatesWithoutMoving() {
        // The board is exactly the square's size, so any kick would push it off the board.
        TetrisBoard b = new TetrisBoard(2, 2);
        b.nextPiece(new TetrisPiece(PieceType.SQUARE), new Point(0, 0));
        assertEquals(Result.SUCCESS, b.move(Action.CLOCKWISE));
        assertEquals(1, b.getCurrentPiece().getRotationIndex());
        assertEquals(new Point(0, 0), b.getCurrentPiecePosition());
        assertEquals(Result.SUCCESS, b.move(Action.COUNTERCLOCKWISE));
        assertEquals(0, b.getCurrentPiece().getRotationIndex());
        assertEquals(new Point(0, 0), b.getCurrentPiecePosition());
    }

    // if the rotation shouldn't be possible
    @Test
    void blockedRotationChangesNothing() {
        TetrisBoard b = board(
            "#.##",
            "...#",
            "###.");
        b.nextPiece(new TetrisPiece(PieceType.T), new Point(0, 0));
        for (Action a : new Action[] {Action.CLOCKWISE, Action.COUNTERCLOCKWISE}) {
            // check if the rotation would result in block out of bounds
            assertEquals(Result.OUT_BOUNDS, b.move(a));
            assertEquals(0, b.getCurrentPiece().getRotationIndex());
            assertEquals(new Point(0, 0), b.getCurrentPiecePosition());
        }
    }

    // line clearing

    @Test
    void clearOneRow() {
        TetrisBoard b = board(
            "....",
            "....",
            "....",
            "....",
            "#...",
            "###.");
        b.nextPiece(verticalStick(), new Point(1, 2));   // drops into column 3
        b.move(Action.DROP);
        assertEquals(1, b.getRowsCleared());
        assertNotNull(b.getGrid(0, 0), "row above moved down 1");
        assertNull(b.getGrid(0, 1));
    }

    // 4 rows max edge case
    @Test
    void clearFourRows() {
        TetrisBoard b = board(
            "....",
            "....",
            "###.",
            "###.",
            "###.",
            "###.");
        b.nextPiece(verticalStick(), new Point(1, 2));   // fills column 3, rows 0..3
        b.move(Action.DROP);
        assertEquals(4, b.getRowsCleared());
        assertEquals(0, b.getMaxHeight(), "board is empty again");
        checkStoredValues(b);
    }

    // square clearing test + top row filling
    @Test
    void clearTopRow() {
        // A square filling a 2 x 2 board clears both rows, including the board's top row.
        TetrisBoard b = new TetrisBoard(2, 2);
        b.nextPiece(new TetrisPiece(PieceType.SQUARE), new Point(0, 0));
        assertEquals(Result.PLACE, b.move(Action.DOWN));
        assertEquals(2, b.getRowsCleared());
        assertNull(b.getGrid(0, 1));
        assertNull(b.getGrid(1, 1));
        checkStoredValues(b);
    }

    @Test // checks if cleared rows are reset
    void rowsClearedResetsOnNextMove() {
        TetrisBoard b = new TetrisBoard(2, 2);
        b.nextPiece(new TetrisPiece(PieceType.SQUARE), new Point(0, 0));
        b.move(Action.DROP);
        assertEquals(2, b.getRowsCleared());
        b.nextPiece(new TetrisPiece(PieceType.SQUARE), new Point(0, 0));
        b.move(Action.NOTHING);
        assertEquals(0, b.getRowsCleared(), "a move that clears nothing reports 0");
    }

    // check if non consecutive row clearing logic works
    @Test
    void nonAdjacentClears() {
        TetrisBoard b = board(
            "....",
            "....",
            ".#..",   // y = 3: two cleared rows below, ends at y = 1
            "###.",   // y = 2: full after the drop
            "#...",   // y = 1: one cleared row below, ends at y = 0
            "###.");  // y = 0: full after the drop
        b.nextPiece(verticalStick(), new Point(1, 2));
        b.move(Action.DROP);
        assertEquals(2, b.getRowsCleared());
        assertNotNull(b.getGrid(0, 0), "old row 1 moved down 1");
        assertNotNull(b.getGrid(1, 1), "old row 3 moved down 2");
        assertNull(b.getGrid(1, 0), "block above the hole stays floating");
    }

    // dropHeight

    // checking drop height values vs known testing height
    @Test
    void dropHeight() {
        TetrisBoard empty = new TetrisBoard(8, 10);
        assertEquals(0, empty.dropHeight(new TetrisPiece(PieceType.SQUARE), 0));
        assertEquals(-1, empty.dropHeight(new TetrisPiece(PieceType.T), 0));

        TetrisBoard uneven = board(
            "....",
            "....",
            "#...",
            "#.#.",
            "#.#.");   // column heights [3, 0, 2, 0]
        assertEquals(2, uneven.dropHeight(new TetrisPiece(PieceType.T), 0));
        assertEquals(2, uneven.dropHeight(new TetrisPiece(PieceType.SQUARE), 2));
    }

    // equals

    // various piece equals testing (orientation, type, position)
    @Test
    void boardEquality() {
        // Same grid, piece and position are equal
        TetrisBoard a = withT(3, 4), b = withT(3, 4);
        b.move(Action.LEFT);
        b.move(Action.RIGHT);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        assertNotEquals(a, withT(2, 4), "different position");
        TetrisBoard rotated = withT(3, 4);
        rotated.move(Action.CLOCKWISE);
        assertNotEquals(a, rotated, "different rotation");
        assertNotEquals(new TetrisBoard(4, 2), board("....", "#..."), "different grid");
        assertNotEquals(a, new TetrisBoard(8, 10), "no current piece");
        assertNotEquals(a, null);
    }

    // invalid input

    // check all invalid spawn positions
    @Test
    void invalidSpawnAndDropHeight() {
        TetrisBoard b = new TetrisBoard(4, 4);
        Piece square = new TetrisPiece(PieceType.SQUARE);
        // Pieces off the board are rejected on every side.
        assertThrows(IllegalArgumentException.class, () -> b.nextPiece(square, new Point(-1, 0)));
        assertThrows(IllegalArgumentException.class, () -> b.nextPiece(square, new Point(3, 0)));
        assertThrows(IllegalArgumentException.class, () -> b.nextPiece(square, new Point(0, -1)));
        assertThrows(IllegalArgumentException.class, () -> b.nextPiece(square, new Point(0, 3)));
        // Null arguments are rejected.
        assertThrows(IllegalArgumentException.class, () -> b.nextPiece(null, new Point(0, 0)));
        assertThrows(IllegalArgumentException.class, () -> b.nextPiece(square, null));
        assertNull(b.getCurrentPiece(), "failed spawns leave no piece behind");

        // dropHeight rejects a position that puts a filled column off the board.
        assertThrows(IllegalArgumentException.class, () -> b.dropHeight(new TetrisPiece(PieceType.T), -1));
        assertThrows(IllegalArgumentException.class, () -> b.dropHeight(new TetrisPiece(PieceType.T), 2));
    }

    // invalid input testing
    @Test
    void invalidInput() {
        TetrisBoard b = board(
            "....",
            "....",
            ".#..");
        // Out-of-range queries return null or 0 instead of throwing.
        assertNull(b.getGrid(-1, 0));
        assertNull(b.getGrid(0, 3));
        assertEquals(0, b.getColumnHeight(-1));
        assertEquals(0, b.getColumnHeight(4));
        assertEquals(0, b.getRowWidth(-1));
        assertEquals(0, b.getRowWidth(3));

        // A piece placed over existing blocks is rejected.
        assertThrows(IllegalArgumentException.class,
            () -> b.nextPiece(new TetrisPiece(PieceType.SQUARE), new Point(1, 0)));

        // move(null) throws and leaves the piece where it was.
        b.nextPiece(new TetrisPiece(PieceType.SQUARE), new Point(2, 1));
        assertThrows(NullPointerException.class, () -> b.move(null));
        assertEquals(new Point(2, 1), b.getCurrentPiecePosition());
    }

    // white box


    @Test
    void storedValuesMatchTheGrid() {
        // After each move, recount everything from the grid and compare with the stored values.
        TetrisBoard b = board(
            "......",
            "......",
            "......",
            "......",
            "......",
            "......",
            "......",
            "......",
            "#.....",
            "##.##.");
        Action[] moves = {Action.LEFT, Action.CLOCKWISE, Action.DOWN, Action.RIGHT, Action.RIGHT, Action.DROP};
        for (Piece p : new Piece[] {new TetrisPiece(PieceType.T), verticalStick(), new TetrisPiece(PieceType.SQUARE)}) {
            b.nextPiece(p, new Point(1, 5));
            for (Action a : moves) {
                b.move(a);
                checkStoredValues(b);
            }
        }
    }


    // check if test move actually just copies
    @Test
    void testMoveLeavesOriginalUnchanged() {
        TetrisBoard b = withT(3, 4);
        Board copy = b.testMove(Action.DROP);
        assertEquals(Result.PLACE, copy.getLastResult());
        // Keep playing on the copy
        copy.nextPiece(new TetrisPiece(PieceType.SQUARE), new Point(0, 4));
        copy.move(Action.DROP);

        assertEquals(new Point(3, 4), b.getCurrentPiecePosition());
        assertEquals(0, b.getMaxHeight());
        // original should be unchanged
        assertNull(b.getGrid(4, 0));
        assertNull(b.getGrid(0, 0));
    }

    // statistical

    // checks if cached counts stay correct after a lot of random moves
    @Test
    void randomActionsKeepStoredValuesCorrect() {
        // Runs the white-box stored-value check after every one of 10,000 random moves.
        Random r = new Random(100);   // fixed seed
        TetrisBoard b = new TetrisBoard(5, 24);   // narrow board on purpose
        for (int step = 0; step < 10_000; step++) {
            if (b.getCurrentPiece() == null) {
                try {
                    b.nextPiece(new TetrisPiece(PieceType.values()[r.nextInt(7)]), new Point(1, 20));
                } catch (IllegalArgumentException gameOver) {
                    b = new TetrisBoard(5, 24);
                    continue;
                }
            }
            b.move(Action.values()[r.nextInt(7)]);
            checkStoredValues(b);
        }
    }
}
