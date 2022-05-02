package moonkeki.util.rpacking;

import java.util.Map;
import java.util.stream.Stream;

public interface PackingSolution<T> {

    default long area() {
        return (long) this.getWidth() * this.getHeight();
    }

    default long occupiedArea() {
        return this.area() - this.emptyArea();
    }

    default double efficiency() {
        return (double) this.occupiedArea() / this.area();
    }

    default int itemCount() {
        return (int) this.itemStream().count();
    }

    int getWidth();
    int getHeight();
    long emptyArea();
    Stream<Map.Entry<T, Position>> itemStream();

}
