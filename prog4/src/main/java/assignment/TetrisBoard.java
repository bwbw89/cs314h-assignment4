package assignment;

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;

import assignment.Board.Result;

/**
 * Represents a Tetris board -- essentially a 2D grid of piece types (or nulls). Supports
 * tetris pieces and row clearing.  Does not do any drawing or have any idea of
 * pixels. Instead, just represents the abstract 2D board.
 */
public final class TetrisBoard implements Board {

    // No wall kick pieces
    private static final Point[] NO_KICKS = {new Point(0,0)};

    private final int width, height;

    // Placed blocks grid
    private Piece.PieceType[][] grid;

    private int[] rowWidths;
    private int[] columnHeights;
    private int maxHeight;

    // Falling piece, bottom left of bounding box
    private Piece currentPiece;
    private int pieceX, pieceY;

    private Action lastAction = Action.NOTHING;
    private Result lastResult = Result.NO_PIECE;
    private int rowsCleared;

    // JTetris will use this constructor
    public TetrisBoard(int width, int height) {
        this.width = width;
        this.height = height;
        grid = new Piece.PieceType[height][width];
        rowWidths = new int[height];
        columnHeights = new int[width];
    }

    // Copy for testMove
    private TetrisBoard(TetrisBoard other) {
        width = other.width;
        height = other.height;
        grid = new Piece.PieceType[height][];

        // clone pieces for sharing
        for (int y = 0; y < height; y++) {
            grid[y] = other.grid[y].clone();
        }

        rowWidths = other.rowWidths.clone();
        columnHeights = other.columnHeights.clone();
        maxHeight = other.maxHeight;
        currentPiece = other.currentPiece;
        pieceX = other.pieceX;
        pieceY = other.pieceY;
        lastAction = other.lastAction;
        lastResult = other.lastResult;
        rowsCleared = other.rowsCleared;
    }

    // Applies the action and returns the result
    @Override
    public Result move(Action act) { 
        lastAction = act;
        rowsCleared = 0;

        Result result;
        if (currentPiece == null) {
            result = Result.NO_PIECE;
        }
        else {
            // user action
            switch(act) {
                case LEFT:
                    result = shift(-1);
                    break;
                case RIGHT:
                    result = shift(1);
                    break;
                case DOWN:
                    if(fits(currentPiece, pieceX, pieceY-1)) {
                        pieceY--;
                        result = Result.SUCCESS;
                    }
                    else {
                        placePiece();
                        result = Result.PLACE;
                    }
                    break;
                case DROP:
                    while(fits(currentPiece, pieceX, pieceY-1)) {
                        pieceY--;
                    }
                    placePiece();
                    result = Result.PLACE;
                    break;
                case CLOCKWISE:
                    result = rotate(true);
                    break;
                case COUNTERCLOCKWISE:
                    result = rotate(false);
                    break;
                default: // Do nothing
                    result = Result.SUCCESS;
            }
        }
        lastResult = result;
        return result;
    }

    // Moves current piece horizontally
    private Result shift(int dx) {
        if(!fits(currentPiece, pieceX+dx, pieceY)) return Result.OUT_BOUNDS;
        pieceX += dx;
        return Result.SUCCESS;
    }

    // Rotates using the first SRS wall kick
    private Result rotate(boolean clockwise) {
        Piece rotated = clockwise ? currentPiece.clockwisePiece() : currentPiece.counterclockwisePiece();
        for(Point kick : wallKicks(currentPiece, clockwise)) {
            if(fits(rotated, pieceX+kick.x, pieceY+kick.y)) {
                currentPiece = rotated;
                pieceX += kick.x;
                pieceY += kick.y;
                return Result.SUCCESS;
            }
        }
        return Result.OUT_BOUNDS;
    }

    // wallkick table indexed from start rotation index
    private static Point[] wallKicks(Piece piece, boolean clockwise) {
        int start = piece.getRotationIndex();
        switch(piece.getType()) {
            case SQUARE:
                return NO_KICKS;
            case STICK:
                return clockwise ? Piece.I_CLOCKWISE_WALL_KICKS[start] : Piece.I_COUNTERCLOCKWISE_WALL_KICKS[start];
            default:
                return clockwise ? Piece.NORMAL_CLOCKWISE_WALL_KICKS[start] : Piece.NORMAL_COUNTERCLOCKWISE_WALL_KICKS[start];
        }
    }

    // True if the entire piece in bounds and in empty cells
    private boolean fits(Piece piece, int x, int y) {
        for(Point b : piece.getBody()) {
            int bx = x+b.x, by = y+b.y;
            //out of bounds
            if(bx < 0 || bx >= width || by < 0 || by >= height) return false;
            //occupied
            if(grid[by][bx] != null) return false;
        }
        return true;
    }

    private void placePiece() {
        Piece.PieceType type = currentPiece.getType();
        boolean filledRow = false;
        // for each block in piece
        for (Point b : currentPiece.getBody()) {
            int x = pieceX + b.x, y = pieceY + b.y;
            grid[y][x] = type;
            rowWidths[y]++;
            columnHeights[x] = Math.max(columnHeights[x], y + 1);
            maxHeight = Math.max(maxHeight, y + 1);
            if(rowWidths[y] == width) filledRow = true;
        }

        currentPiece = null;

        if(filledRow) clearFullRows();
    }

    // Removes full rows, everything above drops by x cleared rows
    private void clearFullRows() {
        // slide rows down
        int write = 0;
        for(int read = 0; read < maxHeight; read++) {
            if(rowWidths[read] == width) continue;
            grid[write] = grid[read];
            rowWidths[write] = rowWidths[read];
            write++;
        }

        rowsCleared = maxHeight-write;
        // reset moved from rows
        for(int y = write; y < maxHeight; y++) {
            grid[y] = new Piece.PieceType[width];
            rowWidths[y] = 0;
        }

        // recheck top height, top to bottom
        maxHeight = 0;
        for(int x = 0; x < width; x++) {
            int h = Math.min(columnHeights[x], write);
            while (h > 0 && grid[h-1][x] == null) {
                h--;
            }
            columnHeights[x] = h;
            maxHeight = Math.max(maxHeight,h);
        }
    }

    // new board with action applied
    @Override
    public Board testMove(Action act) { 
        TetrisBoard copy = new TetrisBoard(this);
        copy.move(act);
        return copy;
    }

    @Override
    public Piece getCurrentPiece() { return currentPiece; }

    @Override
    public Point getCurrentPiecePosition() { 
        // New point for each call
        return currentPiece == null ? null : new Point(pieceX, pieceY);
    }

    // new falling piece
    @Override
    public void nextPiece(Piece p, Point spawnPosition) {
        if(p == null || spawnPosition == null || !fits(p, spawnPosition.x, spawnPosition.y)) {
            throw new IllegalArgumentException();
        }

        currentPiece = p;
        pieceX = spawnPosition.x;
        pieceY = spawnPosition.y;
    }

    // checks if two objects are equal
    @Override
    public boolean equals(Object other) { 
        if(this == other) return true;
        if(!(other instanceof TetrisBoard)) return false;
        TetrisBoard o = (TetrisBoard) other;

        if(width != o.width || height != o.height) return false; // diff sizes
        if(!Objects.equals(currentPiece, o.currentPiece)) return false; //diff piece/rotation
        // diff position
        if(currentPiece != null && (pieceX != o.pieceX || pieceY != o.pieceY)) return false;
        //compares every cell
        return Arrays.deepEquals(grid, o.grid);
    }

    @Override
    public Result getLastResult() { return lastResult; }

    @Override
    public Action getLastAction() { return lastAction; }

    @Override
    public int getRowsCleared() { return rowsCleared; }

    @Override
    public int getWidth() { return width; }

    @Override
    public int getHeight() { return height; }

    @Override
    public int getMaxHeight() { return maxHeight; }

    @Override
    public int dropHeight(Piece piece, int x) { 
        // Resting point is the lowest block at the highest column
        int[] skirt = piece.getSkirt();
        int y = Integer.MIN_VALUE;

        for (int c = 0; c < skirt.length; c++) {
            // nothing below
            if(skirt[c] == Integer.MAX_VALUE) continue;

            y = Math.max(y, columnHeights[x+c]-skirt[c]);
        }
        return y;
    }

    @Override
    public int getColumnHeight(int x) { return columnHeights[x]; }

    @Override
    public int getRowWidth(int y) { return rowWidths[y]; }

    @Override
    public Piece.PieceType getGrid(int x, int y) { 
        // out of grid
        if(x < 0 || x >= width || y < 0 || y >= height) return null;
        return grid[y][x];
    }

    // complement to equals
    @Override
    public int hashCode() {
        if(currentPiece == null) return Arrays.deepHashCode(grid);
        return Objects.hash(Arrays.deepHashCode(grid), currentPiece, pieceX, pieceY);
    }

}
