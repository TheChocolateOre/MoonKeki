package moonkeki.geometry;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public interface OriginLocus extends Locus {

    interface InvertibleUnaryOperator<T> extends UnaryOperator<T> {
        static <T> InvertibleUnaryOperator<T> of(UnaryOperator<T> operator,
                                                 UnaryOperator<T> inverse) {
            return new InvertibleUnaryOperator<>() {
                @Override
                public T apply(T t) {return operator.apply(t);}
                @Override
                public T applyInverse(T t) {return inverse.apply(t);}
            };
        }

        static <T> InvertibleUnaryOperator<T> identity() {
            return InvertibleUnaryOperator.of(t -> t, t -> t);
        }

        T applyInverse(T t);

        default InvertibleUnaryOperator<T> inverse() {
            return new InvertibleUnaryOperator<>() {
                @Override
                public T apply(T t) {
                    return InvertibleUnaryOperator.this.applyInverse(t);
                }
                @Override
                public T applyInverse(T t) {
                    return InvertibleUnaryOperator.this.apply(t);
                }
                @Override
                public InvertibleUnaryOperator<T> inverse() {
                    return InvertibleUnaryOperator.this;
                }
            };
        }
    }

    static OriginLocus of(Predicate<Point> membership, Point origin) {
        return PredicateOriginLocus.builder()
                                   .ofMembership(membership)
                                   .ofOrigin(origin)
                                   .build();
    }

    //If locus is already an OriginLocus it re-attaches the new origin
    static OriginLocus attachOrigin(Locus locus, Point origin) {
        if (locus instanceof PredicateOriginLocus o) {
            return o.reattachOrigin(origin);
        }
        return OriginLocus.of(locus.toPredicate(), origin);
    }

    Point origin();

    default OriginLocus reattachOrigin(Point origin) {
        return this.origin().equals(origin) ?
               this :
               OriginLocus.attachOrigin(this, origin);
    }

    default double x() {
        return this.origin().x();
    }

    default double y() {
        return this.origin().y();
    }

    default OriginLocus position(Point point) {
        final double dx = point.x() - this.x();
        final double dy = point.y() - this.y();
        return this.map(AffineTransform.getTranslateInstance(dx, dy));
    }

    default OriginLocus positionAtCartesianOrigin() {
        return this.position(Point.CARTESIAN_ORIGIN);
    }

    default OriginLocus positionX(double x) {
        return this.position(new Point(x, this.y()));
    }

    default OriginLocus positionY(double y) {
        return this.position(new Point(this.x(), y));
    }

    default OriginLocus translate(double xAmount, double yAmount) {
        return this.position(new Point(this.x() + xAmount, this.y() + yAmount));
    }

    default OriginLocus translateX(double amount) {
        return this.translate(amount, 0.0);
    }

    default OriginLocus translateY(double amount) {
        return this.translate(0.0, amount);
    }

    //covariant override
    @Override
    default OriginLocus map(AffineTransform transform) {
        final AffineTransform INVERSE = new AffineTransform(transform);
        try {
            INVERSE.invert();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException("Argument transform must be " +
                    "invertible.");
        }

        return PredicateOriginLocus.builder()
                                   .ofMembership(this.toPredicate())
                                   .ofInverseTransform(INVERSE)
                                   .ofOrigin(this.origin().map(transform))
                                   .build();
    }

    default OriginLocus map(InvertibleUnaryOperator<Point> transform) {
        final Predicate<Point> MEMBERSHIP = this.toPredicate();
        return PredicateOriginLocus.builder()
                .ofMembership(p -> MEMBERSHIP.test(transform.applyInverse(p)))
                .ofOrigin(this.origin().map(transform))
                .build();
    }

    //relative to R2, covariant override
    @Override
    default OriginLocus complement() {
        return OriginLocus.of(this.toPredicate().negate(), this.origin());
    }
    
}
