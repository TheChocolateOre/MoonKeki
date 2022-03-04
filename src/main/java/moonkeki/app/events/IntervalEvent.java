package moonkeki.app.events;

import java.time.Instant;

public final class IntervalEvent implements Event {

    public record Interval(Instant start, Instant end) {
        public Interval {
            if (start.isAfter(end)) {
                throw new IllegalArgumentException("Argument start can't be " +
                        "after end.");
            }//end if
        }
    }//end nested record Interval

    abstract class Signal implements AutoCloseable {
        public interface Listener {
            @FunctionalInterface
            interface Start extends Listener {
                @Override
                default void onStop() {}
            }//end nested interface Start

            @FunctionalInterface
            interface Stop extends Listener {
                @Override
                default void onStart() {}
            }//end nested interface Stop

            void onStart();
            void onStop();
        }//end nested interface Listener

        public void start() {
            throw new UnsupportedOperationException();
        }

        public void stop() {
            throw new UnsupportedOperationException();
        }

        public void attachListener(Event.Signal.Listener listener) {
            throw new UnsupportedOperationException();
        }

        public void detachListener(Event.Signal.Listener listener) {
            throw new UnsupportedOperationException();
        }

        public IntervalEvent event() {
            throw new UnsupportedOperationException();
        }
    }//end static nested class Signal

}
