package moonkeki.geometry;

public interface Rectangle extends Translatable {

    enum Side {MIN, MAX}

    enum Corner {
        MIN_X_MIN_Y, MIN_X_MAX_Y, MAX_X_MIN_Y, MAX_X_MAX_Y;
        public Corner opposite() {
            return switch (this) {
                case MIN_X_MIN_Y -> MAX_X_MAX_Y;
                case MIN_X_MAX_Y -> MAX_X_MIN_Y;
                case MAX_X_MIN_Y -> MIN_X_MAX_Y;
                case MAX_X_MAX_Y -> MIN_X_MIN_Y;
            };
        }
    }

    double minX();
    double minY();

    default double maxX() {
        return this.minX() + this.width();
    }

    default double maxY() {
        return this.minY() + this.height();
    }

    default Point corner(Corner corner) {
        return switch (corner) {
            case MIN_X_MIN_Y -> this.origin();
            case MIN_X_MAX_Y -> new Point(this.minX(), this.maxY());
            case MAX_X_MIN_Y -> new Point(this.maxX(), this.minY());
            case MAX_X_MAX_Y -> new Point(this.maxX(), this.maxY());
        };
    }

    default double sideX(Side side) {
        return switch (side) {
            case MIN -> this.minX();
            case MAX -> this.maxX();
        };
    }

    default double sideY(Side side) {
        return switch (side) {
            case MIN -> this.minY();
            case MAX -> this.maxY();
        };
    }

    @Override
    Rectangle position(Point point);

    @Override
    default Rectangle positionX(double x) {
        return (Rectangle) Translatable.super.positionX(x);
    }

    @Override
    default Rectangle positionY(double y) {
        return (Rectangle) Translatable.super.positionY(y);
    }

    default Rectangle positionXSide(Side side, double x) {
        return this.positionX(switch (side) {
            case MIN -> x;
            case MAX -> x - this.width();
        });
    }

    default Rectangle positionYSide(Side side, double y) {
        return this.positionY(switch (side) {
            case MIN -> y;
            case MAX -> y - this.height();
        });
    }

    default Rectangle positionCorner(Corner corner, Point point) {
        return this.position(switch (corner) {
            case MIN_X_MIN_Y -> point;
            case MIN_X_MAX_Y -> point.translateY(-this.height());
            case MAX_X_MIN_Y -> point.translateX(-this.width());
            case MAX_X_MAX_Y -> point.translate(-this.width(), -this.height());
        });
    }

    default Rectangle translate(double xAmount, double yAmount) {
        return (Rectangle) Translatable.super.translate(xAmount, yAmount);
    }

    default Rectangle translateX(double amount) {
        return (Rectangle) Translatable.super.translateX(amount);
    }

    default Rectangle translateY(double amount) {
        return (Rectangle) Translatable.super.translateY(amount);
    }

    default Point center() {
        final double CENTER_X = this.minX() + (this.maxX() - this.minX()) / 2.0;
        final double CENTER_Y = this.minY() + (this.maxY() - this.minY()) / 2.0;
        return new Point(CENTER_X, CENTER_Y);
    }

    default Rectangle positionCenter(Point point) {
        final double X_AMOUNT = -this.width() / 2.0;
        final double Y_AMOUNT = -this.height() / 2.0;
        return this.position(point.translate(X_AMOUNT, Y_AMOUNT));
    }

    default Rectangle positionCenterX(double x) {
        return this.position(new Point(x - this.width() / 2.0, this.y()));
    }

    default Rectangle positionCenterY(double y) {
        return this.position(new Point(this.x(), y - this.height() / 2.0));
    }

    //Size
    double width();
    double height();

    Rectangle withSize(double width, double height);

    default Rectangle withSize(double sideSize) {
        return this.withSize(sideSize, sideSize);
    }

    default Rectangle withWidth(double width) {
        return this.withSize(width, this.height());
    }

    default Rectangle withHeight(double height) {
        return this.withSize(this.width(), height);
    }

    //need an anchor point - start
    default Rectangle withSize(double width, double height, Corner anchor) {
        return switch (anchor) {
            case MIN_X_MIN_Y -> this.withSize(width, height);
            case MIN_X_MAX_Y -> {
                final double MAX_Y = this.maxY();
                yield this.withSize(width, height)
                          .positionYSide(Side.MAX, MAX_Y);
            }
            case MAX_X_MIN_Y -> {
                final double MAX_X = this.maxX();
                yield this.withSize(width, height)
                          .positionXSide(Side.MAX, MAX_X);
            }
            case MAX_X_MAX_Y -> {
                final Point CORNER = this.corner(Corner.MAX_X_MAX_Y);
                yield this.withSize(width, height)
                          .positionCorner(Corner.MAX_X_MAX_Y, CORNER);
            }
        };
    }

    default Rectangle withSize(double sideSize, Corner anchor) {
        return this.withSize(sideSize, sideSize, anchor);
    }

    default Rectangle withWidth(double width, Side anchor) {
        return switch (anchor) {
            case MIN -> this.withWidth(width);
            case MAX -> {
                final double MAX_X = this.maxX();
                yield this.withWidth(width)
                          .positionXSide(Side.MAX, MAX_X);
            }
        };
    }

    default Rectangle withHeight(double height, Side anchor) {
        return switch (anchor) {
            case MIN -> this.withHeight(height);
            case MAX -> {
                final double MAX_Y = this.maxY();
                yield this.withHeight(height)
                          .positionYSide(Side.MAX, MAX_Y);
            }
        };
    }

    default Rectangle varySize(double widthAmount, double heightAmount,
                               Corner anchor) {
        return this.withSize(this.width() + widthAmount,
                             this.height() + heightAmount,
                             anchor);
    }

    default Rectangle varySize(double amount, Corner anchor) {
        return this.varySize(amount, amount, anchor);
    }

    default Rectangle varyWidth(double amount, Side anchor) {
        return this.withWidth(this.width() + amount, anchor);
    }

    default Rectangle varyHeight(double amount, Side anchor) {
        return this.withHeight(this.height() + amount, anchor);
    }

    default Rectangle scale(double widthFactor, double heightFactor,
                            Corner anchor) {
        return this.withSize(this.width() * widthFactor,
                             this.height() * heightFactor,
                             anchor);
    }

    default Rectangle scale(double factor, Corner anchor) {
        return this.scale(factor, factor, anchor);
    }

    default Rectangle scaleWidth(double factor, Side anchor) {
        return this.withWidth(this.width() * factor, anchor);
    }

    default Rectangle scaleHeight(double factor, Side anchor) {
        return this.withHeight(this.height() * factor, anchor);
    }
    //need an anchor point - start

    default Rectangle stretchCornerTo(Corner corner, Point dest) {
        return switch (corner) {
            case MIN_X_MIN_Y -> {
                final double WIDTH = this.maxX() - dest.x();
                final double HEIGHT = this.maxY() - dest.y();
                yield this.withSize(WIDTH, HEIGHT)
                          .position(dest);
            }
            case MIN_X_MAX_Y -> {
                final double WIDTH = this.maxX() - dest.x();
                final double HEIGHT = dest.y() - this.minY();
                yield this.withSize(WIDTH, HEIGHT)
                          .positionCorner(Corner.MIN_X_MAX_Y, dest);
            }
            case MAX_X_MIN_Y -> {
                final double WIDTH = dest.x() - this.minX();
                final double HEIGHT = this.maxY() - dest.y();
                yield this.withSize(WIDTH, HEIGHT)
                          .positionCorner(Corner.MAX_X_MIN_Y, dest);
            }
            case MAX_X_MAX_Y -> {
                final double WIDTH = dest.x() - this.minX();
                final double HEIGHT = dest.y() - this.minY();
                yield this.withSize(WIDTH, HEIGHT);
            }
        };
    }

    default Rectangle stretchXSideTo(Side side, double destX) {
        return switch (side) {
            case MIN -> this.withWidth(this.maxX() - destX)
                            .positionX(destX);
            case MAX -> this.withWidth(destX - this.minX());
        };
    }

    default Rectangle stretchYSideTo(Side side, double destY) {
        return switch (side) {
            case MIN -> this.withHeight(this.maxY() - destY)
                            .positionY(destY);
            case MAX -> this.withHeight(destY - this.minY());
        };
    }

    default Rectangle stretchCornerBy(Corner corner,
                                      double xAmount, double yAmount) {
        return this.stretchCornerTo(corner, this.corner(corner)
                                                .translate(xAmount, yAmount));
    }

    default Rectangle stretchXSideBy(Side side, double xAmount) {
        return this.stretchXSideTo(side, this.sideX(side) + xAmount);
    }

    default Rectangle stretchYSideBy(Side side, double yAmount) {
        return this.stretchYSideTo(side, this.sideY(side) + yAmount);
    }

    default boolean isDegenerate() {
        return this.width() == 0.0 || this.height() == 0.0;
    }

}
