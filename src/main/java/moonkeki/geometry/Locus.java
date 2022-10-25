package moonkeki.geometry;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public interface Locus {

    Locus EMPTY = new Locus() {
        @Override
        public boolean intersects(Point point) {return false;}
        @Override
        public Locus map(AffineTransform transform) {return this;}
        @Override
        public Locus mapWithInverse(UnaryOperator<Point> inverseTransform) {
            return this;
        }
        @Override
        public Locus complement() {return Locus.R2;}
    };
    Locus R2 = new Locus() {
        @Override
        public boolean intersects(Point point) {return true;}
        @Override
        public Locus map(AffineTransform transform) {return this;}
        @Override
        public Locus mapWithInverse(UnaryOperator<Point> inverseTransform) {
            return this;
        }
        @Override
        public Locus complement() {return Locus.EMPTY;}
    };

    static Locus of(Predicate<Point> membership) {
        return PredicateLocus.builder()
                             .ofMembership(membership)
                             .build();
    }

    boolean intersects(Point point);

    //throws if it doesn't have an inverse
    default Locus map(AffineTransform transform) {
        final AffineTransform INVERSE = new AffineTransform(transform);
        try {
            INVERSE.invert();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException("Argument transform must be " +
                    "invertible.");
        }

        return PredicateLocus.builder()
                             .ofMembership(this.toPredicate())
                             .ofInverseTransform(INVERSE)
                             .build();
    }

    //must be bijective
    default Locus mapWithInverse(UnaryOperator<Point> inverseTransform) {
        final Predicate<Point> MEMBERSHIP = this.toPredicate();
        return PredicateLocus.builder()
                .ofMembership(p -> MEMBERSHIP.test(inverseTransform.apply(p)))
                .build();
    }

    //relative to R^2
    default Locus complement() {
        return Locus.of(this.toPredicate().negate());
    }

    default Predicate<Point> toPredicate() {
        return this::intersects;
    }

}
