package geometry;

import java.util.Iterator;
import java.util.Objects;

/**
 * Represents an object that lies in a 2d plane and its position is described by
 * its origin.
 * @author TheChocolateOre
 */
public abstract class Spacial {

    /**
     * The x coordinate of the origin of this Spacial.
     */
    private double x;

    /**
     * The y coordinate of the origin of this Spacial.
     */
    private double y;

    /**
     * Translates/moves all the objects of the given Iterator, by the given
     * amounts.
     * @param itr An Iterator to iterate the objects.
     * @param xAmount The amount to move at the x axis the objects of the
     * Iterator.
     * @param yAmount The amount to move at the y axis the objects of the
     * Iterator.
     */
    public static void translate(Iterator<? extends Spacial> itr,
            double xAmount, double yAmount) {
        if (!itr.hasNext()) {
            return;
        }//end if

        Spacial pivot = itr.next();
        final double NEW_PIVOT_X = pivot.getX() + xAmount;
        final double NEW_PIVOT_Y = pivot.getY() + yAmount;
        while (itr.hasNext()) {
            Spacial temp = itr.next();
            final double DIST_X = temp.getX() - pivot.getX();
            final double DIST_Y = temp.getY() - pivot.getY();
            temp.setPosition(NEW_PIVOT_X + DIST_X, NEW_PIVOT_Y + DIST_Y);
        }//end while

        pivot.setPosition(NEW_PIVOT_X, NEW_PIVOT_Y);
    }

    /**
     * Creates a Spacial with its origin at (0.0, 0.0).
     */
    public Spacial() {}

    /**
     * Creates a Spacial with its origin at the given x and y coordinates.
     * @param x The x coordinate of the origin of this Spacial.
     * @param y The y coordinate of the origin of this Spacial.
     */
    public Spacial(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Creates a Spacial that is a deep copy of a given Spacial.
     * @param other A Spacial to create a copy out of it.
     */
    public Spacial(Spacial other) {
        this(other.getX(), other.getY());
    }

    /**
     * Gets the x coordinate of the origin of this Spacial.
     * @return The x coordinate of the origin of this Spacial.
     */
    public double getX() {
        return this.x;
    }

    /**
     * Sets the x coordinate of the origin of this Spacial.
     * @param x The new x coordinate of the origin of this Spacial.
     */
    public final void setX(double x) {
        this.setPosition(x, this.getY());
    }

    /**
     * Translates/moves the origin of this Spacial at its x axis.
     * @param amount The amount to move at the x axis this Spacial.
     */
    public final void translateX(double amount) {
        this.translate(amount, 0.0);
    }

    /**
     * Gets the y coordinate of the origin of this Spacial.
     * @return The y coordinate of the origin of this Spacial.
     */
    public double getY() {
        return this.y;
    }

    /**
     * Sets the y coordinate of the origin of this Spacial.
     * @param y The new y coordinate of the origin of this Spacial.
     */
    public final void setY(double y) {
        this.setPosition(this.getX(), y);
    }

    /**
     * Translates/moves the origin of this Spacial at its y axis.
     * @param amount The amount to move at the y axis this Spacial.
     */
    public final void translateY(double amount) {
        this.translate(0.0, amount);
    }

    /**
     * Sets the position of the origin of this Spacial.
     * This method will always be called when the position of the origin of this
     * Spacial needs to be changed.
     * @param x The new x coordinate of the origin of this Spacial.
     * @param y The new y coordinate of the origin of this Spacial.
     */
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Translates/moves the origin of this Spacial.
     * @param xAmount The amount to move at the x axis this Spacial.
     * @param yAmount The amount to move at the y axis this Spacial.
     */
    public final void translate(double xAmount, double yAmount) {
        this.setPosition(this.getX() + xAmount, this.getY() + yAmount);
    }

    /**
     * Creates a String representation of this Spacial.
     * @return A String representation of this Spacial.
     */
    @Override
    public String toString() {
        return String.format("x: %f, y: %f", this.getX(), this.getY());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Spacial spacial = (Spacial) o;
        return Double.compare(spacial.getX(), getX()) == 0 &&
                Double.compare(spacial.getY(), getY()) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getX(), getY());
    }

}//end class Spacial
