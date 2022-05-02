package moonkeki.util.rpacking;

import java.util.function.ToIntFunction;
import java.util.stream.Stream;

public interface RectanglePacker {

    static RectanglePacker getDefault() {
        throw new UnsupportedOperationException();
    }

    <T> PackingSolution<T> pack(Stream<T> items,
                                ToIntFunction<T> widthExtractor,
                                ToIntFunction<T> heightExtractor,
                                Size maxSize) throws PackingFailedException;

}
