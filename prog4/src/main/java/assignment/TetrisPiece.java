package assignment;

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;

/**
 * An immutable representation of a tetris piece in a particular rotation.
 * 
 * All operations on a TetrisPiece should be constant time, except for its
 * initial construction. This means that rotations should also be fast - calling
 * clockwisePiece() and counterclockwisePiece() should be constant time! You may
 * need to do pre-computation in the constructor to make this possible.
 */
public final class TetrisPiece implements Piece {

    private final PieceType type;
    private final int rotationIndex;

    // SRS Bounding Box width and height (Stick is 4x4, Square is 2x2, else is 3x3)
    private final int width, height;

    // Coords of each block relative to lower left hand
    private final Point[] body;

    // Lowest y of each column in a piece's bounding box or Integer.MAX_VALUE
    private final int[] skirt;

    // neighbors either one clockwise or counterclockwise
    private TetrisPiece clockwisePiece, counterclockwisePiece;


    /**
     * Construct a tetris piece of the given type. The piece should be in its spawn orientation,
     * i.e., a rotation index of 0.
     * 
     * You may freely add additional constructors, but please leave this one - it is used both in
     * the runner code and testing code.
     */
    public TetrisPiece(PieceType type) {
        this(type, 0, copyPoints(type.getSpawnBody()));

        // Build all rotations and loop them
        TetrisPiece prevPiece = this;
        for(int i = 1; i < 4; i++) {
            TetrisPiece nextPiece = new TetrisPiece(type, i, rotateClockwise(prevPiece.body, width));

            // Adds next and prev rotation
            prevPiece.clockwisePiece = nextPiece;
            nextPiece.counterclockwisePiece = prevPiece;
            prevPiece = nextPiece;
        }

        prevPiece.clockwisePiece = this;
        this.counterclockwisePiece = prevPiece;
    }

    // Single rotation constructor without linking others for ring
    private TetrisPiece(PieceType type, int rotationIndex, Point[] body) {
        Dimension box = type.getBoundingBox();
        this.type = type;
        this.rotationIndex = rotationIndex;
        this.width = box.width;
        this.height = box.height;
        this.body = body;
        this.skirt = computeSkirt(body, width);
    }

    // Computes the skirt (lowest y that is filled by the bounding box)
    private static int[] computeSkirt(Point[] body, int width) {
        int[] skirt = new int[width];
        Arrays.fill(skirt, Integer.MAX_VALUE);
        for(Point p:body) {
            skirt[p.x] = Math.min(skirt[p.x], p.y);
        }

        return skirt;
    }

    // Copy array of points
    private static Point[] copyPoints(Point[] points) {
        Point[] copy = new Point[points.length];
        for(int i = 0; i < points.length; i++) {
            copy[i] = new Point(points[i]);
        }

        return copy;
    }

    // Rotates a body 1 time clockwise within nxn bounding box
    private static Point[] rotateClockwise(Point[] body, int n) {
        Point[] after = new Point[body.length];
        for(int i = 0; i < body.length; i++) {
            after[i] = new Point(body[i].y, n-1-body[i].x);
        }

        return after;
    }

    @Override
    public PieceType getType() {
        return type;
    }

    @Override
    public int getRotationIndex() {
        return rotationIndex;
    }

    @Override
    public Piece clockwisePiece() {
        return clockwisePiece;
    }

    @Override
    public Piece counterclockwisePiece() {
        return counterclockwisePiece;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public Point[] getBody() {
        return copyPoints(body);
    }

    @Override
    public int[] getSkirt() {
        return skirt.clone();
    }

    @Override
    public boolean equals(Object other) {
        // Ignore objects which aren't also tetris pieces.
        if(!(other instanceof TetrisPiece)) return false;
        TetrisPiece otherPiece = (TetrisPiece) other;

        return type == otherPiece.type && rotationIndex == otherPiece.rotationIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, rotationIndex);
    }
}
