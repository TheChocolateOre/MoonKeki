package geometry;

import java.util.Iterator;

/**
 * Represents an object that lies in a 2d plane and its position is described by
 * its origin.
 * @author TheChocolateOre
 */
public interface Spatial {

    /**
     * Translates/moves all the objects of the given Iterator, by the given
     * amounts.
     * @param itr An Iterator to iterate the objects.
     * @param xAmount The amount to move at the x axis the objects of the
     * Iterator.
     * @param yAmount The amount to move at the y axis the objects of the
     * Iterator.
     */
    static void translate(Iterator<? extends Spatial> itr,
            double xAmount, double yAmount) {
        if (!itr.hasNext()) {
            return;
        }//end if

        Spatial pivot = itr.next();
        final double NEW_PIVOT_X = pivot.getX() + xAmount;
        final double NEW_PIVOT_Y = pivot.getY() + yAmount;
        while (itr.hasNext()) {
            Spatial temp = itr.next();
            final double DIST_X = temp.getX() - pivot.getX();
            final double DIST_Y = temp.getY() - pivot.getY();
            temp.setPosition(NEW_PIVOT_X + DIST_X, NEW_PIVOT_Y + DIST_Y);
        }//end while

        pivot.setPosition(NEW_PIVOT_X, NEW_PIVOT_Y);
    }

    /**
     * Sets the x coordinate of the origin of this Spacial.
     * @param x The new x coordinate of the origin of this Spacial.
     */
    default void setX(double x) {
        this.setPosition(x, this.getY());
    }

    /**
     * Translates/moves the origin of this Spacial at its x axis.
     * @param amount The amount to move at the x axis this Spacial.
     */
    default void translateX(double amount) {
        this.translate(amount, 0.0);
    }

    /**
     * Sets the y coordinate of the origin of this Spacial.
     * @param y The new y coordinate of the origin of this Spacial.
     */
    default void setY(double y) {
        this.setPosition(this.getX(), y);
    }

    /**
     * Translates/moves the origin of this Spacial at its y axis.
     * @param amount The amount to move at the y axis this Spacial.
     */
    default void translateY(double amount) {
        this.translate(0.0, amount);
    }

    /**
     * Translates/moves the origin of this Spacial.
     * @param xAmount The amount to move at the x axis this Spacial.
     * @param yAmount The amount to move at the y axis this Spacial.
     */
    default void translate(double xAmount, double yAmount) {
        this.setPosition(this.getX() + xAmount, this.getY() + yAmount);
    }

    /**
     * Gets the x coordinate of the origin of this Spacial.
     * @return The x coordinate of the origin of this Spacial.
     */
    double getX();

    /**
     * Gets the y coordinate of the origin of this Spacial.
     * @return The y coordinate of the origin of this Spacial.
     */
    double getY();

    /**
     * Sets the position of the origin of this Spacial.
     * This method will always be called when the position of the origin of this
     * Spacial needs to be changed.
     * @param x The new x coordinate of the origin of this Spacial.
     * @param y The new y coordinate of the origin of this Spacial.
     */
    void setPosition(double x, double y);

}//end class Spacial
