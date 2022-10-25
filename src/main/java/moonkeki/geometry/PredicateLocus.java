package moonkeki.geometry;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

class PredicateLocus implements Locus {

    static final class Builder {
        private Predicate<Point> membership;
        private AffineTransform inverseTransform;

        private Builder() {}

        Builder ofMembership(Predicate<Point> membership) {
            this.membership = Objects.requireNonNull(membership);
            return this;
        }

        Builder ofInverseTransform(AffineTransform inverseTransform) {
            this.inverseTransform = Objects.requireNonNull(inverseTransform);
            return this;
        }

        PredicateLocus build() {
            if (null == this.membership) {
                throw new IllegalStateException("There is no membership " +
                        "Predicate set.");
            }

            if (null == this.inverseTransform) {
                this.inverseTransform = new AffineTransform();
            }

            return new PredicateLocus(this.membership, this.inverseTransform);
        }
    }

    static Builder builder() {
        return new Builder();
    }

    @Deprecated
    static PredicateLocus map(Predicate<Point> membership,
                              AffineTransform prevTransform,
                              AffineTransform transform) {
        final AffineTransform INVERSE = new AffineTransform(transform);
        try {
            INVERSE.invert();
            INVERSE.preConcatenate(prevTransform);
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException("Argument transform must be " +
                    "invertible.");
        }

        return new PredicateLocus(membership, INVERSE);
    }

    @Deprecated
    static PredicateLocus mapWithInverse(Predicate<Point> membership,
            UnaryOperator<Point> inverseTransform) {
        return PredicateLocus.builder()
                .ofMembership(p -> membership.test(inverseTransform.apply(p)))
                .build();
    }

    private final Predicate<Point> MEMBERSHIP;
    private final AffineTransform INVERSE_TRANSFORM;

    private PredicateLocus(Predicate<Point> membership,
                   AffineTransform inverseTransform) {
        this.MEMBERSHIP = membership;
        this.INVERSE_TRANSFORM = inverseTransform;
    }

    @Override
    public boolean intersects(Point point) {
        final double[] COORDINATES = {point.x(), point.y()};
        this.INVERSE_TRANSFORM.transform(COORDINATES, 0, COORDINATES, 0, 1);
        final Point TRANSFORMED_POINT = new Point(COORDINATES[0],
                                                  COORDINATES[1]);
        return this.MEMBERSHIP.test(TRANSFORMED_POINT);
    }

    @Override
    public Locus map(AffineTransform transform) {
        final AffineTransform INVERSE = new AffineTransform(transform);
        try {
            INVERSE.invert();
            INVERSE.preConcatenate(this.INVERSE_TRANSFORM);
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException("Argument transform must be " +
                    "invertible.");
        }

        return new PredicateLocus(this.MEMBERSHIP, INVERSE);
    }

    @Override
    public Locus complement() {
        return new PredicateLocus(this.MEMBERSHIP.negate(),
                                  this.INVERSE_TRANSFORM) {
            @Override
            public Locus complement() {return PredicateLocus.this;}
        };
    }

}
