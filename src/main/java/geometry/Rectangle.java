package geometry;

import java.util.Collection;
import java.util.Objects;

/**
 * Represents a rectangle in a 2d plane, with its origin at its bottom left
 * corner.
 */
public class Rectangle implements Spatial {

    /**
     * An immutable {@link Rectangle}, i.e. its position or size can't change.
     */
    public static class Immutable extends Rectangle {

        public Immutable() {
            super();
        }

        public Immutable(Rectangle other) {
            super(other);
        }

        public Immutable(double x, double y, double width, double height) {
            super(x, y, width, height);
        }

        @Override
        public void setPosition(double x, double y) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSize(double width, double height) {
            throw new UnsupportedOperationException();
        }

    }//end static nested class Immutable

    /**
     * The x coordinate of the bottom left corner of this Rectangle.
     */
    private double x;

    /**
     * The y coordinate of the bottom left corner of this Rectangle.
     */
    private double y;

    /**
     * The width of this Rectangle.
     */
    private double width;

    /**
     * The height of this Rectangle.
     */
    private double height;

    /**
     * Computes the intersection Rectangle between 2 given Rectangle's'.
     * @param r1 The 1st Rectangle of the intersection.
     * @param r2 The 2nd Rectangle of the intersection.
     * @return The intersection Rectangle between 2 given Rectangle's' or null
     * if they don't overlap.
     */
    public static Rectangle intersection(Rectangle r1,
            Rectangle r2) {
        final double X = Math.max(r1.getX(), r2.getX());
        final double RIGHT_X = Math.min(r1.getRightX(), r2.getRightX());
        final double WIDTH = RIGHT_X - X;
        if (WIDTH < 0.0) {
            return null;
        }//end if

        final double Y = Math.max(r1.getY(), r2.getY());
        final double TOP_Y = Math.min(r1.getTopY(), r2.getTopY());
        final double HEIGHT = TOP_Y - Y;
        if (HEIGHT < 0.0) {
            return null;
        }//end if

        return new Rectangle(X, Y, WIDTH, HEIGHT);
    }

    /**
     * Computes the minimum bounding rectangle of a given Collection of
     * Rectangle's'.
     * @param rectangles A Collection of Rectangle's' to compute their minimum
     * bounding rectangle.
     * @return A new Rectangle that represents the minimum bounding rectangle of
     * the given Rectangle's'.
     * @throws IllegalArgumentException If the given rectangles Collection is
     * empty.
     */
    public static Rectangle mbr(
            Collection<? extends Rectangle> rectangles) {
        if (rectangles.isEmpty()) {
            throw new IllegalArgumentException("Argument Collection " +
                    "rectangles can't be empty.");
        }//end if

        final double X = rectangles.parallelStream()
                                   .mapToDouble(Rectangle::getX)
                                   .min()
                                   .orElseThrow();
        final double RIGHT_X = rectangles.parallelStream()
                                         .mapToDouble(Rectangle::getRightX)
                                         .max()
                                         .orElseThrow();
        final double Y = rectangles.parallelStream()
                                   .mapToDouble(Rectangle::getY)
                                   .min()
                                   .orElseThrow();
        final double TOP_Y = rectangles.parallelStream()
                                       .mapToDouble(Rectangle::getTopY)
                                       .max()
                                       .orElseThrow();

        return new Rectangle(X, Y, RIGHT_X - X, TOP_Y - Y);
    }

    /**
     * Creates a Rectangle with its bottom left corner at (0.0, 0.0), width
     * and height of 0.0.
     */
    public Rectangle() {}

    /**
     * Creates a Rectangle that is a deep copy of a given Rectangle.
     * @param other A Rectangle to create a copy out of it.
     */
    public Rectangle(Rectangle other) {
        this.x = other.x;
        this.y = other.y;
        this.width = other.width;
        this.height = other.height;
    }

    /**
     * Creates a Rectangle given the coordinates of its bottom left corner and
     * its size.
     * @param x The x coordinate of the bottom left corner of this Rectangle.
     * @param y The y coordinate of the bottom left corner of this Rectangle.
     * @param width The width of this Rectangle.
     * @param height The height of this Rectangle.
     * @throws IllegalArgumentException If x is not finite.
     * @throws IllegalArgumentException If y is not finite.
     * @throws IllegalArgumentException If width is not finite.
     * @throws IllegalArgumentException If height is not finite.
     * @throws IllegalArgumentException If width < 0.0.
     * @throws IllegalArgumentException If height < 0.0.
     */
    public Rectangle(double x, double y, double width, double height) {
        if (!Double.isFinite(x)) {
            throw new IllegalArgumentException("Argument x must be finite.");
        }//end if

        if (!Double.isFinite(y)) {
            throw new IllegalArgumentException("Argument y must be finite.");
        }//end if

        if (!Double.isFinite(width)) {
            throw new IllegalArgumentException("Argument width must be " +
                    "finite.");
        }//end if

        if (!Double.isFinite(height)) {
            throw new IllegalArgumentException("Argument height must be " +
                    "finite.");
        }//end if

        //Validates that width >= 0.0
        if (width < 0.0) {
            throw new IllegalArgumentException("Argument width must be >= " +
                    "0.0.");
        }//end if

        //Validates that height >= 0.0
        if (height < 0.0) {
            throw new IllegalArgumentException("Argument height must be >= " +
                    "0.0.");
        }//end if

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Gets the x coordinate of the bottom left corner of this Rectangle.
     * @return The x coordinate of the bottom left corner of this Rectangle.
     */
    @Override
    public double getX() {
        return this.x;
    }

    /**
     * Gets the y coordinate of the bottom left corner of this Rectangle.
     * @return The y coordinate of the bottom left corner of this Rectangle.
     */
    @Override
    public double getY() {
        return this.y;
    }

    /**
     * Sets the position of the bottom left corner of this Rectangle.
     * This method will always be called when the position of the bottom left
     * corner of this Rectangle needs to be changed.
     * @param x The new x coordinate of the bottom left corner of this
     * Rectangle.
     * @param y The new y coordinate of the bottom left corner of this
     * Rectangle.
     * @throws IllegalArgumentException If x is not finite.
     * @throws IllegalArgumentException If y is not finite.
     */
    @Override
    public void setPosition(double x, double y) {
        if (!Double.isFinite(x)) {
            throw new IllegalArgumentException("Argument x must be finite.");
        }//end if

        if (!Double.isFinite(y)) {
            throw new IllegalArgumentException("Argument y must be finite.");
        }//end if

        this.x = x;
        this.y = y;
    }

    /**
     * Gets the x coordinate of the center of this Rectangle.
     * @return The x coordinate of the center of this Rectangle.
     */
    public double getCenterX() {
        return this.getX() + this.getWidth() / 2.0;
    }

    /**
     * Gets the x coordinate of the right edge of this Rectangle.
     * @return The x coordinate of the right edge of this Rectangle.
     */
    public double getRightX() {
        return this.getX() + this.getWidth();
    }

    /**
     * Sets the x coordinate of the right side (without stretching, i.e. moves
     * the origin along) of this Rectangle.
     * @param rightX The new x coordinate of the right side of this Rectangle.
     */
    public final void setRightX(double rightX) {
        //Protects against unstable double operations
        if (this.getRightX() == rightX) {
            return;
        }//end if

        this.setX(rightX - this.getWidth());
    }

    /**
     * Sets the x coordinate of the center of this Rectangle at the given
     * x.
     * @param x The new x coordinate of the center of this Rectangle.
     */
    public final void setCenterX(double x) {
        this.setCenter(x, this.getCenterY());
    }

    /**
     * Sets the x coordinate of the center of this Rectangle to the x
     * coordinate of this Rectangle.
     */
    public final void centerAtX() {
        this.setCenterX(this.getX());
    }

    /**
     * Sets the x coordinate of the right side (without stretching, i.e. moves
     * the origin along) of this Rectangle, to the x coordinate of the left side
     * of a given Rectangle.
     * @param other A Rectangle to snap the right side of this Rectangle to its
     * left side.
     */
    public final void snapLeftOf(Rectangle other) {
        this.setRightX(other.getX());
    }

    /**
     * Sets the x coordinate of the right side (without stretching, i.e. moves
     * the origin along) of this Rectangle, to the x coordinate of the left side
     * of a given Rectangle and translates it to the left by a given padding
     * amount.
     * @param other A Rectangle to snap the right side of this Rectangle to its
     * left side.
     * @param padding The amount to translate this {@link Rectangle} towards the
     * left, after it is snapped to the given other {@link Rectangle}. Positive
     * values indicate left translation, whereas negative values indicate right
     * translation.
     */
    public final void snapLeftOf(Rectangle other, double padding) {
        this.setRightX(other.getX());
        this.translateX(-padding);
    }

    /**
     * Sets the x coordinate of the left side (without stretching, i.e. moves
     * the origin along) of this Rectangle, to the x coordinate of the right
     * side of a given Rectangle.
     * @param other A Rectangle to snap the left side of this Rectangle to its
     * right side.
     */
    public final void snapRightOf(Rectangle other) {
        this.setX(other.getRightX());
    }

    /**
     * Sets the x coordinate of the left side (without stretching, i.e. moves
     * the origin along) of this Rectangle, to the x coordinate of the right
     * side of a given Rectangle and translates it to the right by a given
     * padding amount.
     * @param other A Rectangle to snap the left side of this Rectangle to its
     * right side.
     * @param padding The amount to translate this {@link Rectangle} towards the
     * right, after it is snapped to the given other {@link Rectangle}. Positive
     * values indicate right translation, whereas negative values indicate left
     * translation.
     */
    public final void snapRightOf(Rectangle other, double padding) {
        this.setX(other.getRightX());
        this.translateX(padding);
    }

    /**
     * Sets the y coordinate of the bottom side (without stretching, i.e. moves
     * the origin along) of this Rectangle, to the y coordinate of the top side
     * of a given Rectangle.
     * @param other A Rectangle to snap the bottom side of this Rectangle to its
     * top side.
     */
    public final void snapAboveOf(Rectangle other) {
        this.setY(other.getTopY());
    }

    /**
     * Sets the y coordinate of the bottom side (without stretching, i.e. moves
     * the origin along) of this Rectangle, to the y coordinate of the top side
     * of a given Rectangle and translates it upwards by a given padding amount.
     * @param other A Rectangle to snap the bottom side of this Rectangle to its
     * top side.
     * @param padding The amount to translate this {@link Rectangle} upwards,
     * after it is snapped to the given other {@link Rectangle}. Positive values
     * indicate upward translation, whereas negative values indicate downward
     * translation.
     */
    public final void snapAboveOf(Rectangle other, double padding) {
        this.setY(other.getTopY());
        this.translateY(padding);
    }

    /**
     * Sets the y coordinate of the top side (without stretching, i.e. moves the
     * origin along) of this Rectangle, to the y coordinate of the bottom side
     * of a given Rectangle.
     * @param other A Rectangle to snap the top side of this Rectangle to its
     * bottom side.
     */
    public final void snapBelowOf(Rectangle other) {
        this.setTopY(other.getY());
    }

    /**
     * Sets the y coordinate of the top side (without stretching, i.e. moves the
     * origin along) of this Rectangle, to the y coordinate of the bottom side
     * of a given Rectangle and translates it downwards by a given padding
     * amount.
     * @param other A Rectangle to snap the top side of this Rectangle to its
     * bottom side.
     * @param padding The amount to translate this {@link Rectangle} downwards,
     * after it is snapped to the given other {@link Rectangle}. Positive values
     * indicate downward translation, whereas negative values indicate upwards
     * translation.
     */
    public final void snapBelowOf(Rectangle other, double padding) {
        this.setTopY(other.getY());
        this.translateY(-padding);
    }

    /**
     * Gets the y coordinate of the center of this Rectangle.
     * @return The y coordinate of the center of this Rectangle.
     */
    public double getCenterY() {
        return this.getY() + this.getHeight() / 2.0;
    }

    /**
     * Gets the y coordinate of the top edge of this Rectangle.
     * @return The y coordinate of the top edge of this Rectangle.
     */
    public double getTopY() {
        return this.getY() + this.getHeight();
    }

    /**
     * Sets the y coordinate of the top side (without stretching, i.e. moves the
     * origin along) of this Rectangle.
     * @param topY The new y coordinate of the top side of this Rectangle.
     */
    public final void setTopY(double topY) {
        //Protects against unstable double operations
        if (this.getTopY() == topY) {
            return;
        }//end if

        this.setY(topY - this.getHeight());
    }

    /**
     * Sets the y coordinate of the center of this Rectangle at the given
     * y.
     * @param y The new y coordinate of the center of this Rectangle.
     */
    public final void setCenterY(double y) {
        this.setCenter(this.getCenterX(), y);
    }

    /**
     * Sets the y coordinate of the center of this Rectangle to the y
     * coordinate of this Rectangle.
     */
    public final void centerAtY() {
        this.setCenterY(this.getY());
    }

    /**
     * Sets the center of this Rectangle at the given coordinates.
     * @param x The new x coordinate of the center of this Rectangle.
     * @param y The new y coordinate of the center of this Rectangle.
     */
    public final void setCenter(double x, double y) {
        this.setPosition((this.getCenterX() != x) ? x - this.getWidth() / 2.0 :
                this.getX(), (this.getCenterY() != y) ? y - this.getHeight() /
                2.0 : this.getY());
    }

    /**
     * Sets the center of this {@link Rectangle} at the center of the given
     * {@link Rectangle}.
     * @param other A {@link Rectangle} to set the center of this {@link
     * Rectangle} at its center.
     */
    public final void setCenter(Rectangle other) {
        this.setCenter(other.getCenterX(), other.getCenterY());
    }

    /**
     * Sets the center of this Rectangle to its bottom left corner.
     */
    public final void center() {
        //Sets the x coordinate of the center of this Rectangle at the x
        //coordinate of its bottom left corner
        this.centerAtX();
        //Sets the y coordinate of the center of this Rectangle at the y
        //coordinate of its bottom left corner
        this.centerAtY();
    }

    /**
     * Sets the width of this Rectangle.
     * @param width The new width of this Rectangle.
     * @throws IllegalArgumentException If width < 0.0.
     */
    public final void setWidth(double width) {
        this.setSize(width, this.getHeight());
    }

    /**
     * Sets the width of this Rectangle and keeps the center at the same
     * point.
     * @param width The new width of this Rectangle.
     * @throws IllegalArgumentException If width < 0.0.
     */
    public final void setStableWidth(double width) {
        this.setStableSize(width, this.getHeight());
    }

    /**
     * Sets the height of this Rectangle.
     * @param height The new height of this Rectangle.
     * @throws IllegalArgumentException If height < 0.0.
     */
    public final void setHeight(double height) {
        this.setSize(this.getWidth(), height);
    }

    /**
     * Sets the height of this Rectangle and keeps the center at the same
     * point.
     * @param height The new height of this Rectangle.
     * @throws IllegalArgumentException If height < 0.0.
     */
    public final void setStableHeight(double height) {
        this.setStableSize(this.getWidth(), height);
    }

    /**
     * Sets the size of this Rectangle and keeps the center at the same
     * point.
     * @param width The new width of this Rectangle.
     * @param height The new height of this Rectangle.
     * @throws IllegalArgumentException If width < 0.0.
     * @throws IllegalArgumentException If height < 0.0.
     */
    public final void setStableSize(double width, double height) {
        //Protects against unstable double operations
        if (this.getWidth() != width) {
            this.setX(this.getCenterX());
            this.setWidth(width);
            this.centerAtX();
        }//end if

        //Protects against unstable double operations
        if (this.getHeight() != height) {
            this.setY(this.getCenterY());
            this.setHeight(height);
            this.centerAtY();
        }//end if
    }

    /**
     * Gets the width of this Rectangle.
     * @return The width of this Rectangle.
     */
    public double getWidth() {
        return this.width;
    }

    /**
     * Gets the height of this Rectangle.
     * @return The height of this Rectangle.
     */
    public double getHeight() {
        return this.height;
    }

    /**
     * Sets the position and size of this {@link Rectangle} to the respective
     * values of a given {@link Rectangle}.
     * @param other A {@link Rectangle} to copy its position and size into this
     * {@link Rectangle}.
     */
    public void set(Rectangle other) {
        this.setPosition(other.getX(), other.getY());
        this.setSize(other.getWidth(), other.getHeight());
    }

    /**
     * Sets the size of this Rectangle. This method will always be called
     * when the size of this Rectangle needs to be changed.
     * @param width The new width of this Rectangle.
     * @param height The new height of this Rectangle.
     * @throws IllegalArgumentException If width is not finite.
     * @throws IllegalArgumentException If height is not finite.
     * @throws IllegalArgumentException If width < 0.0.
     * @throws IllegalArgumentException If height < 0.0.
     */
    public void setSize(double width, double height) {
        if (!Double.isFinite(width)) {
            throw new IllegalArgumentException("Argument width must be " +
                    "finite.");
        }//end if

        if (!Double.isFinite(height)) {
            throw new IllegalArgumentException("Argument height must be " +
                    "finite.");
        }//end if

        //Validates that width >= 0.0
        if (width < 0.0) {
            throw new IllegalArgumentException("Argument width must be >= " +
                    "0.0.");
        }//end if

        //Validates that height >= 0.0
        if (height < 0.0) {
            throw new IllegalArgumentException("Argument height must be >= " +
                    "0.0.");
        }//end if

        this.width = width;
        this.height = height;
    }

    /**
     * Scales the width of this Rectangle by a given factor.
     * @param factor A value to scale the width of this Rectangle, i.e. how many
     * times to scale the current width.
     * @throws IllegalArgumentException If factor < 0.0.
     */
    public final void scaleWidth(double factor) {
        this.scale(factor, 1.0);
    }

    /**
     * Scales the width of this Rectangle by a given factor, while keeping its
     * center at the same point.
     * @param factor A value to scale the width of this Rectangle, i.e. how many
     * times to scale the current width.
     * @throws IllegalArgumentException If factor < 0.0.
     */
    public final void scaleStableWidth(double factor) {
        this.scaleStable(factor, 1.0);
    }

    /**
     * Scales the height of this Rectangle by a given factor.
     * @param factor A value to scale the height of this Rectangle, i.e. how
     * many times to scale the current height.
     * @throws IllegalArgumentException If factor < 0.0.
     */
    public final void scaleHeight(double factor) {
        this.scale(1.0, factor);
    }

    /**
     * Scales the height of this Rectangle by a given factor, while keeping its
     * center at the same point.
     * @param factor A value to scale the height of this Rectangle, i.e. how
     * many times to scale the current height.
     * @throws IllegalArgumentException If factor < 0.0.
     */
    public final void scaleStableHeight(double factor) {
        this.scaleStable(1.0, factor);
    }

    /**
     * Scales the width and height of this Rectangle by a given factor.
     * @param factor A value to scale the width and height of this Rectangle,
     * i.e. how many times to scale the current width and height.
     * @throws IllegalArgumentException If factor < 0.0.
     */
    public final void scale(double factor) {
        this.scale(factor, factor);
    }

    /**
     * Scales the width and height of this Rectangle by a given factor, while
     * keeping its center at the same point.
     * @param factor A value to scale the width and height of this Rectangle,
     * i.e. how many times to scale the current width and height.
     * @throws IllegalArgumentException If factor < 0.0.
     */
    public final void scaleStable(double factor) {
        this.scaleStable(factor, factor);
    }

    /**
     * Scales the width and height of this Rectangle by 2 given factors,
     * respectively.
     * @param xFactor A value to scale the width of this Rectangle, i.e. how
     * many times to scale the current width.
     * @param yFactor A value to scale the height of this Rectangle, i.e. how
     * many times to scale the current height.
     * @throws IllegalArgumentException If xFactor < 0.0.
     * @throws IllegalArgumentException If yFactor < 0.0.
     */
    public final void scale(double xFactor, double yFactor) {
        this.setSize(xFactor * this.getWidth(), yFactor *
                this.getHeight());
    }

    /**
     * Scales the width and height of this Rectangle by 2 given factors,
     * respectively, while keeping its center at the same point.
     * @param xFactor A value to scale the width of this Rectangle, i.e. how
     * many times to scale the current width.
     * @param yFactor A value to scale the height of this Rectangle, i.e. how
     * many times to scale the current height.
     * @throws IllegalArgumentException If xFactor < 0.0.
     * @throws IllegalArgumentException If yFactor < 0.0.
     */
    public final void scaleStable(double xFactor, double yFactor) {
        this.setStableSize(xFactor * this.getWidth(), yFactor *
                this.getHeight());
    }

    /**
     * Indicates if this Rectangle is above a given Rectangle. A Rectangle r1 is
     * said to be above a Rectangle r2, if and only if the y coordinate of the
     * bottom side of r1 is greater or equal to the y coordinate of the top side
     * of r2.
     * @param other A Rectangle to check if this Rectangle is above it.
     * @return True if and only if this Rectangle is above the given Rectangle,
     * otherwise false.
     */
    public boolean isAboveOf(Rectangle other) {
        return this.isStrictlyAboveOf(other) || this.getY() == other.getTopY();
    }

    /**
     * Indicates if this Rectangle is strictly above a given Rectangle. A
     * Rectangle r1 is said to be strictly above a Rectangle r2, if and only if
     * the y coordinate of the bottom side of r1 is strictly greater than the y
     * coordinate of the top side of r2.
     * @param other A Rectangle to check if this Rectangle is strictly above it.
     * @return True if and only if this Rectangle is strictly above the given
     * Rectangle, otherwise false.
     */
    public boolean isStrictlyAboveOf(Rectangle other) {
        return this.getY() > other.getTopY();
    }

    /**
     * Indicates if this Rectangle is below a given Rectangle. A Rectangle r1 is
     * said to be below a Rectangle r2, if and only if the y coordinate of the
     * top side of r1 is smaller or equal to the y coordinate of the bottom side
     * of r2.
     * @param other A Rectangle to check if this Rectangle is below it.
     * @return True if and only if this Rectangle is below the given
     * Rectangle, otherwise false.
     */
    public boolean isBelowOf(Rectangle other) {
        return this.isStrictlyBelowOf(other) || this.getTopY() == other.getY();
    }

    /**
     * Indicates if this Rectangle is strictly below a given Rectangle. A
     * Rectangle r1 is said to be strictly below a Rectangle r2, if and only if
     * the y coordinate of the top side of r1 is strictly smaller than the y
     * coordinate of the bottom side of r2.
     * @param other A Rectangle to check if this Rectangle is strictly below it.
     * @return True if and only if this Rectangle is strictly below the given
     * Rectangle, otherwise false.
     */
    public boolean isStrictlyBelowOf(Rectangle other) {
        return other.isStrictlyAboveOf(this);
    }

    /**
     * Indicates if this Rectangle is at the right of a given Rectangle. A
     * Rectangle r1 is said to be at the right of a Rectangle r2, if and only if
     * the x coordinate of the left side of r1 is greater or equal to the x
     * coordinate of the right side of r2.
     * @param other A Rectangle to check if this Rectangle is at the right of
     *              it.
     * @return True if and only if this Rectangle is at the right of the given
     * Rectangle, otherwise false.
     */
    public boolean isRightOf(Rectangle other) {
        return this.isStrictlyRightOf(other) || other.getRightX() ==
                this.getX();
    }

    /**
     * Indicates if this Rectangle is strictly at the right of a given
     * Rectangle. A Rectangle r1 is said to be strictly at the right of a
     * Rectangle r2, if and only if the x coordinate of the left side of r1 is
     * strictly greater than the x coordinate of the right side of r2.
     * @param other A Rectangle to check if this Rectangle is strictly at the
     * right of it.
     * @return True if and only if this Rectangle is strictly at the right of
     * the given Rectangle, otherwise false.
     */
    public boolean isStrictlyRightOf(Rectangle other) {
        return other.getRightX() < this.getX();
    }

    /**
     * Indicates if this Rectangle is at the left of a given Rectangle. A
     * Rectangle r1 is said to be at the left of a Rectangle r2, if and only if
     * the x coordinate of the right side of r1 is smaller or equal to the x
     * coordinate of the left side of r2.
     * @param other A Rectangle to check if this Rectangle is at the left of it.
     * @return True if and only if this Rectangle is at the left of the given
     * Rectangle, otherwise false.
     */
    public boolean isLeftOf(Rectangle other) {
        return this.isStrictlyLeftOf(other) || this.getRightX() == other.getX();
    }

    /**
     * Indicates if this Rectangle is strictly at the left of a given
     * Rectangle. A Rectangle r1 is said to be strictly at the left of a
     * Rectangle r2, if and only if the x coordinate of the right side of r1 is
     * strictly smaller than the x coordinate of the left side of r2.
     * @param other A Rectangle to check if this Rectangle is strictly at the
     * left of it.
     * @return True if and only if this Rectangle is strictly at the left of
     * the given Rectangle, otherwise false.
     */
    public boolean isStrictlyLeftOf(Rectangle other) {
        return other.isStrictlyRightOf(this);
    }

    /**
     * Indicates if this Rectangle overlaps with a given Rectangle. 2
     * Rectangle's' are said to be overlapping if at least 1 corner of a
     * Rectangle is contained in the other.
     * @param other A Rectangle to check if it overlaps with this Rectangle.
     * @return True if and only if this Rectangle overlaps with the given
     * Rectangle, otherwise false.
     */
    public boolean overlaps(Rectangle other) {
        return this.overlapsX(other) && this.overlapsY(other);
    }

    /**
     * Indicates if this Rectangle strictly overlaps with a given Rectangle. 2
     * Rectangle's' are said to be strictly overlapping if at least 1 corner of
     * a Rectangle is strictly contained in the other, i.e. it's inside its
     * boundary, but not on it.
     * @param other A Rectangle to check if it strictly overlaps with this
     * Rectangle.
     * @return True if and only if this Rectangle strictly overlaps with the
     * given Rectangle, otherwise false.
     */
    public boolean overlapsStrict(Rectangle other) {
        return this.strOverlapsX(other) && this.strOverlapsY(other);
    }

    /**
     * Indicates if this Rectangle is on the left side of a given Rectangle. A
     * Rectangle r1 is said to be on the left side of a Rectangle r2, if the x
     * coordinate of the left side of r2 is equal to the x coordinate of the
     * right side of r1 and at the same time r1 lies in the left region of r2,
     * or equivalently r2 lies in the right region of r1.
     * @param other A Rectangle to check if this Rectangle is on its left side.
     * @return True if this Rectangle is on the left side of the given
     * Rectangle, otherwise false.
     */
    public boolean isOnLeftOf(Rectangle other) {
        return this.getRightX() == other.getX() &&
                !this.isStrictlyAboveOf(other) &&
                !this.isStrictlyBelowOf(other);
    }

    /**
     * Indicates if this Rectangle is on the right side of a given Rectangle. A
     * Rectangle r1 is said to be on the right side of a Rectangle r2, if the x
     * coordinate of the right side of r2 is equal to the x coordinate of the
     * left side of r1 and at the same time r1 lies in the right region of r2,
     * or equivalently r2 lies in the left region of r1.
     * @param other A Rectangle to check if this Rectangle is on its right side.
     * @return True if this Rectangle is on the right side of the given
     * Rectangle, otherwise false.
     */
    public boolean isOnRightOf(Rectangle other) {
        return other.isOnLeftOf(this);
    }

    /**
     * Indicates if this Rectangle is on the top side of a given Rectangle. A
     * Rectangle r1 is said to be on the top side of a Rectangle r2, if the y
     * coordinate of the top side of r2 is equal to the y coordinate of the
     * bottom side of r1 and at the same time r1 lies in the top region of r2,
     * or equivalently r2 lies in the bottom region of r1.
     * @param other A Rectangle to check if this Rectangle is on its top side.
     * @return True if this Rectangle is on the top side of the given Rectangle,
     * otherwise false.
     */
    public boolean isOnTopOf(Rectangle other) {
        return this.getY() == other.getTopY() && !this.isStrictlyLeftOf(other)
                && !this.isStrictlyRightOf(other);
    }

    /**
     * Indicates if this Rectangle is on the bottom side of a given Rectangle. A
     * Rectangle r1 is said to be on the bottom side of a Rectangle r2, if the y
     * coordinate of the bottom side of r2 is equal to the y coordinate of the
     * top side of r1 and at the same time r1 lies in the bottom region of r2,
     * or equivalently r2 lies in the top region of r1.
     * @param other A Rectangle to check if this Rectangle is on its bottom
     * side.
     * @return True if this Rectangle is on the bottom side of the given
     * Rectangle, otherwise false.
     */
    public boolean isOnBottomOf(Rectangle other) {
        return other.isOnTopOf(this);
    }

    /**
     * Indicates if the x areas of this and a given Rectangle, strictly overlap.
     * The x area of a Rectangle r is the range: [r.getX(), r.getRightX()]. The
     * strictly here implies that the range don't contain its lower and upper
     * bounds, i.e. (r.getX(), r.getRightX()). Let r1 and r2, 2 Rectangle's',
     * then r1.strOverlapsX(r2) <=> r2.strOverlapsX(r1).
     * @param other A Rectangle to check if its x area strictly overlaps with
     * the x area of this Rectangle.
     * @return True if the x areas of this and the given Rectangle, strictly
     * overlap, otherwise false.
     */
    public boolean strOverlapsX(Rectangle other) {
        return !(this.isLeftOf(other) || this.isRightOf(other));
    }

    /**
     * Indicates if the y areas of this and a given Rectangle, strictly overlap.
     * The y area of a Rectangle r is the range: [r.getY(), r.getTopY()]. The
     * strictly here implies that the range don't contain its lower and upper
     * bounds, i.e. (r.getY(), r.getTopY()). Let r1 and r2, 2 Rectangle's', then
     * r1.strOverlapsX(r2) <=> r2.strOverlapsX(r1).
     * @param other A Rectangle to check if its y area strictly overlaps with
     * the y area of this Rectangle.
     * @return True if the y areas of this and the given Rectangle, strictly
     * overlap, otherwise false.
     */
    public boolean strOverlapsY(Rectangle other) {
        return !(this.isBelowOf(other) || this.isAboveOf(other));
    }

    /**
     * Indicates if the x areas of this and a given Rectangle, overlap. The x
     * area of a Rectangle r is the range: [r.getX(), r.getRightX()]. Let r1 and
     * r2, 2 Rectangle's', then r1.overlapsX(r2) <=> r2.overlapsX(r1).
     * @param other A Rectangle to check if its x area overlaps with the x area
     * of this Rectangle.
     * @return True if the x areas of this and the given Rectangle, overlap,
     * otherwise false.
     */
    public boolean overlapsX(Rectangle other) {
        return !(this.isStrictlyLeftOf(other) || this.isStrictlyRightOf(other));
    }

    /**
     * Indicates if the y areas of this and a given Rectangle, overlap. The y
     * area of a Rectangle r is the range: [r.getY(), r.getTopY()]. Let r1 and
     * r2, 2 Rectangle's', then r1.overlapsY(r2) <=> r2.overlapsY(r1).
     * @param other A Rectangle to check if its y area overlaps with the y area
     * of this Rectangle.
     * @return True if the y areas of this and the given Rectangle, overlap,
     * otherwise false.
     */
    public boolean overlapsY(Rectangle other) {
        return !(this.isStrictlyBelowOf(other) ||
                this.isStrictlyAboveOf(other));
    }

    /**
     * Indicates if this Rectangle contains a given point.
     * @param x The x coordinate of the point to check if it is contained in
     * this Rectangle.
     * @param y The y coordinate of the point to check if it is contained in
     * this Rectangle.
     * @return True if the given point is contained in this Rectangle, otherwise
     * false.
     */
    public boolean contains(double x, double y) {
        return !(x < this.getX() || x > this.getRightX() || y < this.getY() ||
                y > this.getTopY());
    }

    /**
     * Sets the x coordinate of the origin of this Rectangle without moving its
     * right x coordinate.
     * @param x The new x coordinate of the origin of this Rectangle.
     * @throws IllegalArgumentException If x > right x.
     */
    public final void stretchX(double x) {
        //Validates that x <= right x
        if (x > this.getRightX()) {
            throw new IllegalArgumentException("Argument x can't be > the " +
                    "right x of this Rectangle.");
        }//end if

        //Protects against unstable double operations
        if (this.getX() == x) {
            return;
        }//end if

        this.setWidth(this.getRightX() - x);
        this.setX(x);
    }

    /**
     * Translates the x coordinate of the origin of this Rectangle without
     * moving its right x coordinate, by a given amount.
     * @param amount An amount to translate the x coordinate of the origin of
     * this Rectangle by.
     * @throws IllegalArgumentException If x + amount > right x.
     */
    public final void stretchXBy(double amount) {
        this.stretchX(this.getX() + amount);
    }

    /**
     * Sets the x coordinate of the right side of this Rectangle without moving
     * its origin, i.e. the Rectangle is being resized.
     * @param rightX The new x coordinate of the right side of this Rectangle.
     * @throws IllegalArgumentException If rightX < origin x.
     */
    public final void stretchRightX(double rightX) {
        //Validates that rightX >= origin rightX
        if (rightX < this.getX()) {
            throw new IllegalArgumentException("Argument rightX can't be < " +
                    "the x coordinate of this Rectangle.");
        }//end if

        //Protects against unstable double operations
        if (this.getRightX() == rightX) {
            return;
        }//end if

        this.setWidth(rightX - this.getX());
    }

    /**
     * Translates the x coordinate of the right side of this Rectangle without
     * moving its origin, by a given amount.
     * @param amount An amount to translate the x coordinate of the right side
     * of this Rectangle by.
     * @throws IllegalArgumentException If right x + amount < origin x.
     */
    public final void stretchRightXBy(double amount) {
        this.stretchRightX(this.getRightX() + amount);
    }

    /**
     * Sets the y coordinate of the origin of this Rectangle without moving its
     * top y coordinate.
     * @param y The new y coordinate of the origin of this Rectangle.
     * @throws IllegalArgumentException If y > top y.
     */
    public final void stretchY(double y) {
        //Validates that y <= top y
        if (y > this.getTopY()) {
            throw new IllegalArgumentException("Argument y can't be > the " +
                    "top y of this Rectangle.");
        }//end if

        //Protects against unstable double operations
        if (this.getY() == y) {
            return;
        }//end if

        this.setHeight(this.getTopY() - y);
        this.setY(y);
    }

    /**
     * Translates the y coordinate of the origin of this Rectangle without
     * moving its top y coordinate, by a given amount
     * @param amount An amount to translate the y coordinate of the origin of
     * this Rectangle by.
     * @throws IllegalArgumentException If y + amount > top y.
     */
    public final void stretchYBy(double amount) {
        this.stretchY(this.getY() + amount);
    }

    /**
     * Sets the y coordinate of the top side of this Rectangle without moving
     * its origin, i.e. the Rectangle is being resized.
     * @param topY The new y coordinate of the top side of this Rectangle.
     * @throws IllegalArgumentException If topY < origin y.
     */
    public final void stretchTopY(double topY) {
        //Validates that topY >= origin y
        if (topY < this.getY()) {
            throw new IllegalArgumentException("Argument topY can't be < " +
                    "the y coordinate of this Rectangle.");
        }//end if

        //Protects against unstable double operations
        if (this.getTopY() == topY) {
            return;
        }//end if

        this.setHeight(topY - this.getY());
    }

    /**
     * Translates the y coordinate of the top side of this Rectangle without
     * moving its origin, by a given amount.
     * @param amount An amount to translate the y coordinate of the top side of
     * this Rectangle by.
     * @throws IllegalArgumentException If top y + amount < origin y.
     */
    public final void stretchTopYBy(double amount) {
        this.stretchTopY(this.getTopY() + amount);
    }

    /**
     * Expands this {@link Rectangle}, in a minimum way, in order to completely
     * contain a given {@link Rectangle}.
     * @param other A {@link Rectangle} to expand this {@link Rectangle} in
     * order to completely contain it.
     */
    public final void expand(Rectangle other) {
        this.stretchX(Math.min(this.getX(), other.getX()));
        this.stretchRightX(Math.max(this.getRightX(), other.getRightX()));
        this.stretchY(Math.min(this.getY(), other.getY()));
        this.stretchTopY(Math.max(this.getTopY(), other.getTopY()));
    }

    /**
     * Calculates the area of this Rectangle.
     * @return The area of this Rectangle.
     */
    public double area() {
        return this.getWidth() * this.getHeight();
    }

    /**
     * Calculates the perimeter of this Rectangle.
     * @return The perimeter of this Rectangle.
     */
    public double perimeter() {
        return 2.0 * (this.getWidth() + this.getHeight());
    }

    /**
     * Creates a String representation of this Rectangle.
     * @return A String representation of this Rectangle.
     */
    @Override
    public String toString() {
        return String.format("x :%f, y: %f, width: %f, height: %f", this.getX(),
                this.getY(), this.getWidth(), this.getHeight());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }//end if

        if (null == o) {
            return false;
        }//end if

        return (o instanceof Rectangle r) &&
                Double.compare(this.getX(), r.getX()) == 0 &&
                Double.compare(this.getY(), r.getY()) == 0 &&
                Double.compare(this.getWidth(), r.getWidth()) == 0 &&
                Double.compare(this.getHeight(), r.getHeight()) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getX(), this.getY(), this.getWidth(),
                this.getHeight());
    }

}//end class Rectangle
