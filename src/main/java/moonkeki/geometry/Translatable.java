package moonkeki.geometry;

public interface Translatable {

    Point origin();

    default Translatable reattachOrigin(Point origin) {
        if (this.origin().equals(origin)) {
            return this;
        }

        record Impl(Point origin, Translatable translatable) implements
                Translatable {
            @Override
            public Translatable reattachOrigin(Point origin) {
                return new Impl(origin, this.translatable());
            }

            @Override
            public Translatable position(Point point) {
                final double dx = point.x() - this.x();
                final double dy = point.y() - this.y();
                return new Impl(point, this.translatable().translate(dx, dy));
            }
        }
        return this instanceof Impl(Point o, Translatable t) ?
               new Impl(origin, t) :
               new Impl(origin, this);
    }

    default double x() {
        return this.origin().x();
    }

    default double y() {
        return this.origin().y();
    }

    Translatable position(Point point);

    default Translatable positionX(double x) {
        return this.position(new Point(x, this.y()));
    }

    default Translatable positionY(double y) {
        return this.position(new Point(this.x(), y));
    }

    default Translatable translate(double xAmount, double yAmount) {
        return this.position(this.origin().translate(xAmount, yAmount));
    }

    default Translatable translateX(double amount) {
        return this.translate(amount, 0.0);
    }

    default Translatable translateY(double amount) {
        return this.translate(0.0, amount);
    }

}
