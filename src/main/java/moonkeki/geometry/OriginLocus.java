package moonkeki.geometry;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public interface OriginLocus extends Locus, Translatable {

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

    //If locus is already an PredicateOriginLocus it re-attaches the new origin
    static OriginLocus attachOrigin(Locus locus, Point origin) {
        if (locus instanceof PredicateOriginLocus o) {
            return o.reattachOrigin(origin);
        }
        return OriginLocus.of(locus.toPredicate(), origin);
    }

    @Override
    default OriginLocus reattachOrigin(Point origin) {
        return this.origin().equals(origin) ?
               this :
               OriginLocus.attachOrigin(this, origin);
    }

    @Override
    default OriginLocus position(Point point) {
        final double dx = point.x() - this.x();
        final double dy = point.y() - this.y();
        return this.map(AffineTransform.getTranslateInstance(dx, dy));
    }

    @Override
    default OriginLocus positionX(double x) {
        return (OriginLocus) Translatable.super.positionX(x);
    }

    @Override
    default OriginLocus positionY(double y) {
        return (OriginLocus) Translatable.super.positionY(y);
    }

    @Override
    default OriginLocus translate(double xAmount, double yAmount) {
        return (OriginLocus) Translatable.super.translate(xAmount, yAmount);
    }

    @Override
    default OriginLocus translateX(double amount) {
        return (OriginLocus) Translatable.super.translateX(amount);
    }

    @Override
    default OriginLocus translateY(double amount) {
        return (OriginLocus) Translatable.super.translateY(amount);
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
