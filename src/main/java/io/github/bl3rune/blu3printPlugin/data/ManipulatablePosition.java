package io.github.bl3rune.blu3printPlugin.data;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.github.bl3rune.blu3printPlugin.enums.Direction;
import io.github.bl3rune.blu3printPlugin.enums.Orientation;
import io.github.bl3rune.blu3printPlugin.enums.Rotation;
import io.github.bl3rune.blu3printPlugin.enums.Turn;
import io.github.bl3rune.blu3printPlugin.utils.Pair;

public class ManipulatablePosition {

    private int xMax;
    private int yMax;
    private int zMax;
    private Orientation orientation;
    private Rotation rotation;
    private int scale;

    // Temporary fields for iterations
    private Iterator<Integer> outerLoop;
    private Iterator<Integer> middleLoop;
    private Iterator<Integer> innerLoop;
    private int outerIndex;
    private int middleIndex;
    private int innerIndex;
    private Direction[] ordering;
    private boolean usingScaling;
    private int [] lastXYZScaling = new int [] { 1, 1, 1 };

    public ManipulatablePosition(ManipulatablePosition p, int scale) {
        this(p.getZSize(), p.getYSize(), p.getXSize(), p.getOrientation(), p.getRotation(), scale);
    }

    public ManipulatablePosition(int z, int y, int x, Orientation o, Rotation r) {
        this(z, y, x, o, r, 1);
    }

    public ManipulatablePosition(int z, int y, int x, Orientation o, Rotation r, int scale) {
        this.xMax = x;
        this.yMax = y;
        this.zMax = z;
        this.orientation = o;
        this.rotation = r;
        this.scale = scale;
        this.ordering = calculateOrdering(o, r);
    }

    public int getXSize() {
        return xMax;
    }

    public int getYSize() {
        return yMax;
    }

    public int getZSize() {
        return zMax;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public int getScale() {
        return scale;
    }

    public int getScalingIngredientsMultiplier() {
        return scale * scale * scale;
    }

    // CONVERSION METHODS

    public int[] getNewSizes(Rotation newRotation) {
        return getNewSizes(newRotation, new int[] { zMax, yMax, xMax });
    }

    public int[] getNewSizes(Rotation newRotation, int[] sizes) {
        if (newRotation == rotation || newRotation == rotation.getOpposite()) {
            return sizes;
        }
        int outer = sizes[ordering[0].getPosIndex()];
        int middle = sizes[ordering[1].getPosIndex()];
        int inner = sizes[ordering[2].getPosIndex()];
        return unscramble(outer, inner, middle);
    }

    public int[] getNewSizes(Orientation newOrientation) {
        int[] newSizes = new int[3];
        if (newOrientation == orientation || newOrientation == orientation.getOpposite()) {
            return new int[] { zMax, yMax, xMax };
        }
        switch (orientation) {
            default:
            case NORTH:
            case SOUTH:
            case EAST:
            case WEST:
                if (newOrientation == Orientation.UP || newOrientation == Orientation.DOWN) {
                    newSizes[0] = yMax;
                    if (newOrientation == Orientation.EAST || newOrientation == Orientation.WEST) {
                        newSizes[1] = xMax;
                        newSizes[2] = zMax;
                    } else {
                        newSizes[1] = zMax;
                        newSizes[2] = xMax;
                    }
                } else {
                    newSizes[0] = xMax;
                    newSizes[1] = yMax;
                    newSizes[2] = zMax;
                }
                break;
            case UP:
            case DOWN:
                newSizes[1] = zMax;
                if (newOrientation == Orientation.EAST || newOrientation == Orientation.WEST) {
                    newSizes[0] = xMax;
                    newSizes[2] = yMax;
                } else {
                    newSizes[0] = yMax;
                    newSizes[2] = xMax;
                }
                break;
        }

        return newSizes;
    }

    public Pair<Orientation, Rotation> calculateTurn(Turn turn) {
        switch (orientation) {
            default:
            case NORTH:
                return calculatePossibleNewOrientations(new Orientation[] { // UP, RIGHT, DOWN, LEFT
                        Orientation.DOWN,
                        Orientation.EAST,
                        Orientation.UP,
                        Orientation.WEST
                }, turn);
            case SOUTH:
                return calculatePossibleNewOrientations(new Orientation[] {
                        Orientation.DOWN,
                        Orientation.WEST,
                        Orientation.UP,
                        Orientation.EAST
                }, turn);
            case EAST:
                return calculatePossibleNewOrientations(new Orientation[] {
                        Orientation.DOWN,
                        Orientation.SOUTH,
                        Orientation.UP,
                        Orientation.NORTH
                }, turn);
            case WEST:
                return calculatePossibleNewOrientations(new Orientation[] {
                        Orientation.DOWN,
                        Orientation.NORTH,
                        Orientation.UP,
                        Orientation.SOUTH
                }, turn);
            case UP:
                return calculatePossibleNewOrientations(new Orientation[] {
                        Orientation.NORTH,
                        Orientation.EAST,
                        Orientation.SOUTH,
                        Orientation.WEST
                }, turn);
            case DOWN:
                return calculatePossibleNewOrientations(new Orientation[] {
                        Orientation.NORTH,
                        Orientation.WEST,
                        Orientation.SOUTH,
                        Orientation.EAST
                }, turn);
        }
    }

    /**
     * 
     * @param newOrientations New Orientations when turning from UP, RIGHT, DOWN,
     *                        LEFT in the TOP rotaiton for each side
     * @param turn            Which direction to turn
     * @return
     */
    private Pair<Orientation, Rotation> calculatePossibleNewOrientations(Orientation[] newOrientations, Turn turn) {
        turn = turn.plusRotation(rotation);
        Orientation o = newOrientations[turn.getIndex()];
        Rotation r = null;

        if (o.isCompass() && orientation.isCompass()) {
            r = rotation;
        } else {
            Orientation other = o.isCompass() ? o : orientation;
            boolean isUp = o == Orientation.UP || orientation == Orientation.UP;
            boolean turningUp = o == Orientation.UP || orientation == Orientation.DOWN;
            switch (other) {
                default: // NORTH
                    r = isUp ? rotation : rotation.getOpposite();
                    break;
                case EAST:
                    r = turningUp ? rotation.getNextRotation() : Rotation.getRotation(rotation.getIndex() + 3);
                    break;
                case SOUTH:
                    r = isUp ? rotation.getOpposite() : rotation;
                    break;
                case WEST:
                    r = turningUp ? Rotation.getRotation(rotation.getIndex() + 3) : rotation.getNextRotation();
                    break;
            }
        }

        return new Pair<>(o, r);
    }

    // ITERATION METHODS

    public boolean endOfMiddleLoop() {
        return middleLoop != null && !middleLoop.hasNext();
    }

    public boolean endOfInnerLoop() {
        return innerLoop != null && !innerLoop.hasNext();
    }

    public int[] next(boolean scaling, int [] xyzScale) {
        if (usingScaling) {
            if (xyzScale[0] != lastXYZScaling[0] || xyzScale[1] != lastXYZScaling[1] || xyzScale[2] != lastXYZScaling[2]) {
                lastXYZScaling = xyzScale;
                resetLoops(scaling);
            }
        }
        return next(scaling);
    }

    /**
     * Gets the next position in the 3D sequence based on the iterators
     * - outerLoop
     * - middleLoop
     * - innerLoop
     * 
     * @return the next position in the 3D sequence.
     */
    public int[] next(boolean scaling) {

        if (outerLoop == null || usingScaling != scaling) {
            resetLoops(scaling);
        }

        if (!innerLoop.hasNext()) {
            if (!middleLoop.hasNext()) {
                if (!outerLoop.hasNext()) {
                    outerLoop = null;
                    return null;
                }
                outerIndex = outerLoop.next();
                middleLoop = getLoop(ordering[1], (scaling ? scale : 1));
            }
            middleIndex = middleLoop.next();
            innerLoop = getLoop(ordering[2], (scaling ? scale : 1));
        }
        innerIndex = innerLoop.next();
        return unscramble(outerIndex, middleIndex, innerIndex);
    }

    /**
     * Calculates the Directions in order of [outer,middle,inner] for loops for
     * given orientation and rotation.
     * 
     * @param orientation Orientation front of design is facing towards
     * @param rotation    3D rotation the camera is facing towards
     *                    POSITION = (outer loop) (middle loop) (inner loop)
     *                    NORTH = Z- Y+ X- SOUTH = Z+ X- Y+
     *                    EAST = X- Z+ Y+ WEST = X+ Z- Y+
     *                    UP = Y- X- Z+ DOWN = Y+ X+ Z-
     * @return the directions to iterate over in [outer,middle,inner] order
     */
    private Direction[] calculateOrdering(Orientation orientation, Rotation rotation) {
        boolean positive = true;
        Direction inner, middle = Direction.Y_POS, outer;
        switch (orientation) {
            case NORTH:
                positive = false;
            case SOUTH:
            default:
                outer = positive ? Direction.Z_POS : Direction.Z_NEG;
                inner = positive ? Direction.X_POS : Direction.X_NEG;
                break;
            case EAST:
                positive = false;
            case WEST:
                outer = positive ? Direction.X_POS : Direction.X_NEG;
                inner = positive ? Direction.Z_POS : Direction.Z_NEG;
                break;
            case UP:
                positive = false;
            case DOWN:
                outer = positive ? Direction.Y_POS : Direction.Y_NEG;
                middle = Direction.Z_POS;
                inner = positive ? Direction.X_POS : Direction.X_NEG;
                break;
        }
        return calculateRotation(outer, middle, inner, rotation);
    }

    /**
     * Applies roatation to the directions based on these rules:
     * - ROTATION TOP = (outer) (middle) (inner)
     * - ROTATION RIGHT = (outer) -(inner) (middle)
     * - ROTATION BOTTOM = (outer) -(middle) -(inner)
     * - ROTATION LEFT = (outer) (inner) -(middle)
     * 
     * @param outer    Direction for the outer loop in top roation
     * @param middle   Direction for the middle loop in top roation
     * @param inner    Direction for the inners loop in top roation
     * @param rotation Rotation to apply
     * @return the newly ordered directions to iterate over in [outer,middle,inner]
     *         order
     */
    private Direction[] calculateRotation(Direction outer, Direction middle, Direction inner, Rotation rotation) {
        Direction[] order = new Direction[3];
        order[0] = outer;
        switch (rotation) {
            case TOP:
            default:
                order[1] = middle;
                order[2] = inner;
                break;
            case RIGHT:
                order[2] = middle;
                order[1] = inner.getInverted();
                break;
            case BOTTOM:
                order[1] = middle.getInverted();
                order[2] = inner.getInverted();
                break;
            case LEFT:
                order[2] = middle.getInverted();
                order[1] = inner;
                break;
        }
        return order;
    }

    /**
     * Resets all iterators back to the beginning
     * 
     * @param scaling should loops be scaled by `this.scale`
     */
    private void resetLoops(boolean scaling) {
        usingScaling = scaling;
        int [] scales = lastXYZScaling;
        outerLoop = getLoop(ordering[0], (scaling ? (scale * scales[0]) : 1));
        middleLoop = getLoop(ordering[1], (scaling ? (scale * scales[1]) : 1));
        innerLoop = getLoop(ordering[2], (scaling ? (scale * scales[2]) : 1));
        outerIndex = outerLoop.next();
        middleIndex = middleLoop.next();
    }

    /**
     * Builds an iterator of integers based on the size of direction and the scaling
     * factor
     * 
     * @param direction Direction of the integers along a particular axis
     * @param scaling   scaling factor to multiply the maximum distance in a
     *                  direction by
     * @return Iterator from (start of direction to end of direction, multiplied by
     *         the scaling factor)
     */
    private Iterator<Integer> getLoop(Direction direction, int scaling) {
        int loopEnd = 0;
        switch (direction) {
            case X_POS:
            case X_NEG:
                loopEnd = xMax * scaling;
                break;
            case Y_POS:
            case Y_NEG:
                loopEnd = yMax * scaling;
                break;
            case Z_POS:
            case Z_NEG:
            default:
                loopEnd = zMax * scaling;
                break;
        }
        List<Integer> list = IntStream.range(0, loopEnd).boxed().collect(Collectors.toList());
        if (direction.isNegative()) {
            Collections.reverse(list);
        }
        return list.iterator();
    }

    /**
     * Unscrambles values using ordering.
     * 
     * @param outer  the outer value.
     * @param middle the middle value.
     * @param inner  the inner value.
     * @return the unscrambled values in [z][y][x] order.
     */
    private int[] unscramble(int outer, int middle, int inner) {
        int[] result = new int[3];
        result[ordering[0].getPosIndex()] = outer;
        result[ordering[1].getPosIndex()] = middle;
        result[ordering[2].getPosIndex()] = inner;
        return result;
    }
}
