package moonkeki.geometry;

import java.awt.geom.AffineTransform;
import java.util.Objects;
import java.util.function.Predicate;

final class PredicateOriginLocus implements OriginLocus {

    static final class Builder {
        private Predicate<Point> membership;
        private AffineTransform inverseTransform;
        private Point origin = Point.CARTESIAN_ORIGIN;

        private Builder() {}

        Builder ofMembership(Predicate<Point> membership) {
            this.membership = Objects.requireNonNull(membership);
            return this;
        }

        Builder ofInverseTransform(AffineTransform inverseTransform) {
            this.inverseTransform = Objects.requireNonNull(inverseTransform);
            return this;
        }

        Builder ofOrigin(Point origin) {
            this.origin = Objects.requireNonNull(origin);
            return this;
        }

        PredicateOriginLocus build() {
            if (null == this.inverseTransform) {
                this.inverseTransform = new AffineTransform();
            }

            final Locus LOCUS = PredicateLocus.builder()
                    .ofMembership(this.membership)
                    .ofInverseTransform(this.inverseTransform)
                    .build();
            return new PredicateOriginLocus(LOCUS, this.origin);
        }
    }

    static Builder builder() {
        return new Builder();
    }

    private final Locus LOCUS;
    private final Point ORIGIN;

    private PredicateOriginLocus(Locus locus, Point origin) {
        this.LOCUS = locus;
        this.ORIGIN = origin;
    }

    @Override
    public boolean intersects(Point point) {
        return this.LOCUS.intersects(point);
    }

    @Override
    public Point origin() {
        return this.ORIGIN;
    }

    @Override
    public OriginLocus reattachOrigin(Point origin) {
        return this.origin().equals(origin) ?
               this :
               new PredicateOriginLocus(this.LOCUS, origin);
    }

    @Override
    public OriginLocus position(Point point) {
        final double dx = point.x() - this.x();
        final double dy = point.y() - this.y();
        final AffineTransform TRANSFORM =
                AffineTransform.getTranslateInstance(dx, dy);
        return new PredicateOriginLocus(this.LOCUS.map(TRANSFORM), point);
    }

    @Override
    public OriginLocus map(AffineTransform transform) {
        return new PredicateOriginLocus(this.LOCUS.map(transform),
                                        this.ORIGIN.map(transform));
    }

}
