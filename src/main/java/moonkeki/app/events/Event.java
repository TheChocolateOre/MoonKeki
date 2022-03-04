package moonkeki.app.events;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.List;
import java.util.function.IntPredicate;

public sealed interface Event extends AutoCloseable permits IntervalEvent {

    abstract class Signal implements AutoCloseable {
        @FunctionalInterface
        public interface Listener {
            void onTrigger();
        }//end nested interface Listener

        public void trigger() {
            throw new UnsupportedOperationException();
        }

        public void attachListener(Listener listener) {
            throw new UnsupportedOperationException();
        }

        public void detachListener(Listener listener) {
            throw new UnsupportedOperationException();
        }

        public Event event() {
            throw new UnsupportedOperationException();
        }
    }//end static nested class Signal

    boolean hasOccurred();
    boolean hasOccurredFor(IntPredicate frequencyPredicate);
    int drainTo(Collection<? extends TemporalAccessor> c);
    List<Instant> drain();
    Event getClean();
    int capacity();
    void setCapacity(int capacity);
    void declutter();
    void reset();
    boolean isClosed();
    void close();

}
