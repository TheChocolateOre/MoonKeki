package moonkeki.geometry;

import java.awt.geom.AffineTransform;
import java.util.function.UnaryOperator;

public record Point(double x, double y) implements OriginLocus {

    public static final Point CARTESIAN_ORIGIN = new Point(0, 0);

    @Override
    public boolean intersects(Point point) {
        return this.equals(point);
    }

    @Override
    public Point origin() {
        return this;
    }

    @Override
    public Point position(Point point) {
        return point;
    }

    @Override
    public Point map(AffineTransform transform) {
        final double[] COORDINATES = {this.x(), this.y()};
        transform.transform(COORDINATES, 0, COORDINATES, 0, 1);
        return new Point(COORDINATES[0], COORDINATES[1]);
    }

    public Point map(UnaryOperator<Point> transform) {
        return transform.apply(this);
    }

    @Override
    public Point map(InvertibleUnaryOperator<Point> transform) {
        return this.map((UnaryOperator<Point>) transform);
    }

}
