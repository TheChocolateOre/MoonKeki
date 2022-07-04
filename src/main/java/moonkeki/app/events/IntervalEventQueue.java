package moonkeki.app.events;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface IntervalEventQueue extends Event {

    record Interval(Instant startIncluded, Instant endExcluded) {
        public Interval {
            if (endExcluded.isBefore(startIncluded)) {
                throw new IllegalArgumentException("Argument endExcluded " +
                        "can't lie before startIncluded.");
            }
        }

        public Duration duration() {
            return Duration.between(startIncluded, endExcluded);
        }
    }

    interface Builder {
        Builder ofCapacity(int capacity);
        Builder ofReplacementRule(ReplacementRule replacementRule);
        IntervalEventQueue build();
    }

    interface Listener {
        void onStart(Instant timestamp);
        void onStop(Instant timestamp);
        //Will be called only on the first close() of the underlying source
        void onClose(Instant timestamp);
    }

    interface Snapshot {
        Snapshot EMPTY = new Snapshot() {
            @Override
            public boolean hasOccurredFor(Predicate<Duration> durationPredicate,
                                          boolean headIncluded) {
                return false;
            }

            @Override
            public Stream<Interval> stream() {
                return Stream.empty();
            }

            @Override
            public int size() {
                return 0;
            }
        };

        @Deprecated
        default boolean hasOccurred(boolean headIncluded) {
            return hasOccurredFor(d -> true, headIncluded);
        }

        @Deprecated
        boolean hasOccurredFor(Predicate<Duration> durationPredicate,
                               boolean headIncluded);

        Stream<Interval> stream();
        int size();
        default boolean isEmpty() {
            return this.size() == 0;
        }
    }

    interface Hub {
        interface Closeable extends Hub, AutoCloseable {
            @Override
            void close();
        }

        Hub.Closeable EMPTY = new Closeable() {
            @Override
            public void close() {}

            @Override
            public boolean attachListener(Listener listener) {
                return false;
            }

            @Override
            public void detachListener(Listener listener) {}

            @Override
            public Hub negate() {
                return this;
            }

            @Override
            public IntervalEventQueue unbounded() {
                return IntervalEventQueue.EMPTY;
            }

            @Override
            public Builder eventBuilder() {
                return new Builder() {
                    @Override
                    public Builder ofCapacity(int capacity) {
                        return this;
                    }

                    @Override
                    public Builder ofReplacementRule(ReplacementRule
                                                     replacementRule) {
                        return this;
                    }

                    @Override
                    public IntervalEventQueue build() {
                        return IntervalEventQueue.EMPTY;
                    }
                };
            }

            @Override
            public ClosureState getClosureState() {
                return ClosureState.CLOSED;
            }
        };

        //Builder convenience for ofCapacity(1) & replacementRule(NONE)
        default IntervalEventQueue singletonLeastRecent() {
            return this.eventBuilder()
                       .ofCapacity(1)
                       .ofReplacementRule(ReplacementRule.NONE)
                       .build();
        }

        //Builder convenience for ofCapacity(1) & replacementRule(LEAST_RECENT)
        default IntervalEventQueue singletonMostRecent() {
            return this.eventBuilder()
                       .ofCapacity(1)
                       .ofReplacementRule(ReplacementRule.LEAST_RECENT)
                       .build();
        }

        //false if this Hub is closed, otherwise true
        boolean attachListener(Listener listener);
        void detachListener(Listener listener);
        Hub negate();
        //ofCapacity(+inf) & replacementRule(NONE). Throws if too much for the
        //underlying collection
        IntervalEventQueue unbounded();
        Builder eventBuilder();
        ClosureState getClosureState();
    }

    final class Signal implements AutoCloseable {
        private static abstract class AbstractIntervalEvent implements
                                                            IntervalEventQueue {
            final Lock LOCK = new ReentrantLock(true);
            //If null, this InstantEvent is disconnected, otherwise this Signal
            //won't be closed (it could be closing, but that's not a problem)
            volatile IntervalEventQueue.Signal signal;
            volatile Instant headTimestamp;
            //Indicates if the headTimestamp has been used in a hasOccurred()
            //method
            boolean consumed; //false when headTimestamp is null
            Deque<Interval> intervals = new LinkedList<>();

            //signal can't be null
            //headTimestamp can be null
            AbstractIntervalEvent(IntervalEventQueue.Signal signal,
                                  Instant headTimestamp) {
                this.signal = Objects.requireNonNull(signal);
                this.headTimestamp = headTimestamp;
            }

            @Override
            public boolean hasOccurredFor(Predicate<Duration> durationPredicate,
                                          boolean headIncluded) {
                this.LOCK.lock();
                try {
                    final Stream<Duration> HEAD_STREAM =
                            headIncluded ? this.headStream() : Stream.empty();
                    final boolean OCCURRED = Stream.concat(
                            this.intervals.stream().map(Interval::duration),
                            HEAD_STREAM)
                                                   .anyMatch(durationPredicate);
                    if (OCCURRED) {
                        this.clear(headIncluded);
                    }

                    return OCCURRED;
                } finally {
                    this.LOCK.unlock();
                }
            }

            @Override
            public boolean isHappeningFor(Predicate<Duration>
                                          durationPredicate) {
                final IntervalEventQueue.Signal SIGNAL = this.signal;
                if (SIGNAL == null) {
                    return false;
                }

                final Instant HEAD_TIMESTAMP = SIGNAL.headTimestamp;
                if (HEAD_TIMESTAMP == null) {
                    return false;
                }

                return durationPredicate.test(Duration.between(HEAD_TIMESTAMP,
                                                               Instant.now()));
            }

            @Override
            public Snapshot snapshot() {
                this.LOCK.lock();
                try {
                    if (this.intervals.isEmpty()) {
                        return Snapshot.EMPTY;
                    }

                    final Collection<Interval> INTERVALS = this.intervals;
                    this.intervals = new LinkedList<>();
                    return new Snapshot() {
                        @Override
                        public boolean hasOccurredFor(
                                Predicate<Duration> durationPredicate,
                                boolean headIncluded) {
                            final Stream<Duration> HEAD_STREAM = headIncluded ?
                                    AbstractIntervalEvent.this.headStream() :
                                    Stream.empty();
                            final boolean OCCURRED =
                                    Stream.concat(
                                    INTERVALS.stream()
                                             .map(Interval::duration),
                                    HEAD_STREAM)
                                          .anyMatch(durationPredicate);
                            if (OCCURRED && headIncluded) {
                                AbstractIntervalEvent.this.consumed = true;
                            }

                            return OCCURRED;
                        }

                        @Override
                        public Stream<Interval> stream() {
                            return INTERVALS.stream();
                        }

                        @Override
                        public int size() {
                            return INTERVALS.size();
                        }
                    };
                } finally {
                    this.LOCK.unlock();
                }
            }

            @Override
            public void clear() {
                this.clear(true);
            }

            @Override
            public void clear(boolean headIncluded) {
                this.LOCK.lock();
                try {
                    this.intervals.clear();
                    this.consumed = headIncluded || this.consumed;
                } finally {
                    this.LOCK.unlock();
                }
            }

            @Override
            public ConnectionState getConnectionState() {
                return null == this.signal ? ConnectionState.DISCONNECTED :
                                             ConnectionState.UNDETERMINED;
            }

            @Override
            public void disconnect() {
                final IntervalEventQueue.Signal SIGNAL = this.signal;
                if (SIGNAL == null) {
                    return;
                }

                SIGNAL.LOCK.lock();
                try {
                    SIGNAL.EVENTS.remove(this);
                    this.signal = null;
                } finally {
                    SIGNAL.LOCK.unlock();
                }

                if (this.headTimestamp != null && !this.consumed) {
                    this.intervals.add(new Interval(this.headTimestamp,
                                                    Instant.now()));
                }
                this.headTimestamp = null;
            }

            //this.LOCK must be acquired
            private Stream<Duration> headStream() {
                //If we capture an Instant here, headTimestamp may end up after
                //it

                return this.headTimestamp != null && !this.consumed ?
                       Stream.of(Duration.between(this.headTimestamp,
                                                  Instant.now())) :
                       Stream.empty();
            }

            //this.LOCK must be acquired before calling this - helper method.
            //timestamp can't be before last
            abstract void add(Interval interval);
        }

        private final Lock LOCK = new ReentrantLock(true);
        //only of positive capacity
        private final Set<AbstractIntervalEvent> EVENTS = new LinkedHashSet<>();
        private final Set<Listener> LISTENERS = new LinkedHashSet<>();
        //Irrelevant when both EVENTS and LISTENERS Set's' are empty
        private Instant lastTimestamp;
        private volatile Instant headTimestamp;
        private volatile boolean closed;
        private final Hub HUB = new Hub() {
            @Override
            public boolean attachListener(Listener listener) {
                return Signal.this.attachListener(listener);
            }

            @Override
            public void detachListener(Listener listener) {
                Signal.this.detachListener(listener);
            }

            @Override
            public Hub negate() {
                throw new UnsupportedOperationException();
            }

            @Override
            public IntervalEventQueue unbounded() {
                Signal.this.LOCK.lock();
                try {
                    if (Signal.this.closed) {
                        return IntervalEventQueue.EMPTY;
                    }

                    final AbstractIntervalEvent EVENT =
                            new AbstractIntervalEvent(Signal.this,
                            Signal.this.headTimestamp) {
                        @Override
                        void add(Interval interval) {
                            this.LOCK.lock();
                            try {
                                this.headTimestamp = null;
                                this.consumed = false;
                                this.intervals.add(interval);
                            } finally {
                                this.LOCK.unlock();
                            }
                        }
                    };

                    Signal.this.EVENTS.add(EVENT);
                    return EVENT;
                } finally {
                    Signal.this.LOCK.unlock();
                }
            }

            @Override
            public Builder eventBuilder() {
                return Signal.this.eventBuilder();
            }

            @Override
            public ClosureState getClosureState() {
                return Signal.this.getClosureState();
            }
        };

        public void start() {
            this.LOCK.lock();
            try {
                final Instant NOW = Instant.now();
                if (this.closed) {
                    return;
                }

                //Already started
                if (this.headTimestamp != null) {
                    return;
                }

                if (this.EVENTS.isEmpty() && this.LISTENERS.isEmpty()) {
                    return;
                }

                this.lastTimestamp = this.headTimestamp = NOW;
                this.forwardHeadTimestamp();
            } finally {
                this.LOCK.unlock();
            }
        }

        public void startElseNow(Instant timestamp) {
            this.LOCK.lock();
            try {
                final Instant NOW = Instant.now();
                if (this.closed) {
                    return;
                }

                //Already started
                if (this.headTimestamp != null) {
                    return;
                }

                this.lastTimestamp = this.headTimestamp =
                                     null == this.lastTimestamp ||
                                     this.lastTimestamp.isBefore(timestamp) ?
                                     timestamp : NOW;
                this.forwardHeadTimestamp();
            } finally {
                this.LOCK.unlock();
            }
        }

        public void startElseThrow(Instant timestamp) {
            this.LOCK.lock();
            try {
                if (this.closed) {
                    return;
                }

                //Already started
                if (this.headTimestamp != null) {
                    return;
                }

                if (this.lastTimestamp != null &&
                    !this.lastTimestamp.isBefore(timestamp)) {
                    throw new IllegalArgumentException("Argument timestamp " +
                            "must be after the last timestamp of this Signal.");
                }

                this.lastTimestamp = this.headTimestamp = timestamp;
                this.forwardHeadTimestamp();
            } finally {
                this.LOCK.unlock();
            }
        }

        public void stop() {
            this.LOCK.lock();
            try {
                final Instant NOW = Instant.now();
                if (this.closed) {
                    return;
                }

                //Already stopped
                if (null == this.headTimestamp) {
                    return;
                }

                final Instant PREV_HEAD_TIMESTAMP = this.headTimestamp;
                this.headTimestamp = null;
                this.lastTimestamp = NOW;
                this.forwardInterval(PREV_HEAD_TIMESTAMP, NOW);
            } finally {
                this.LOCK.unlock();
            }
        }

        public void stopElseNow(Instant timestamp) {
            this.LOCK.lock();
            try {
                final Instant NOW = Instant.now();
                if (this.closed) {
                    return;
                }

                //Already stopped
                if (null == this.headTimestamp) {
                    return;
                }

                final Instant PREV_HEAD_TIMESTAMP = this.headTimestamp;
                this.headTimestamp = null;
                this.lastTimestamp = NOW;
                //lastTimestamp can't be null here;
                //headTimestamp != null => lastTimestamp != null
                this.lastTimestamp = this.lastTimestamp.isBefore(timestamp) ?
                                     timestamp : NOW;
                this.forwardInterval(PREV_HEAD_TIMESTAMP, NOW);
            } finally {
                this.LOCK.unlock();
            }
        }

        public void stopElseThrow(Instant timestamp) {
            this.LOCK.lock();
            try {
                final Instant NOW = Instant.now();
                if (this.closed) {
                    return;
                }

                //Already stopped
                if (null == this.headTimestamp) {
                    return;
                }

                //lastTimestamp can't be null here;
                //headTimestamp != null => lastTimestamp != null
                if (!this.lastTimestamp.isBefore(timestamp)) {
                    throw new IllegalArgumentException("Argument timestamp " +
                            "must be after the last timestamp of this Signal.");
                }

                final Instant PREV_HEAD_TIMESTAMP = this.headTimestamp;
                this.headTimestamp = null;
                this.lastTimestamp = NOW;
                this.forwardInterval(PREV_HEAD_TIMESTAMP, NOW);
            } finally {
                this.LOCK.unlock();
            }
        }

        public Hub getHub() {
            return this.HUB;
        }

        public ClosureState getClosureState() {
            return this.closed ? ClosureState.CLOSED :
                                 ClosureState.UNDETERMINED;
        }

        @Override
        public void close() {
            this.LOCK.lock();
            try {
                final Instant NOW = Instant.now();
                if (this.closed) {
                    return;
                }

                this.LISTENERS.forEach(l -> l.onClose(NOW));
                if (this.headTimestamp != null) {
                    this.EVENTS.forEach(e -> {
                        e.LOCK.lock();
                        try {
                            e.add(new Interval(this.headTimestamp, NOW));
                            e.signal = null;
                        } finally {
                            e.LOCK.unlock();
                        }
                    });
                } else {
                    this.EVENTS.forEach(e -> e.signal = null);
                }

                this.EVENTS.clear();
                this.LISTENERS.clear();
                this.closed = true;
            } finally {
                this.LOCK.unlock();
            }
        }

        //false if this Signal is closed, otherwise true
        private boolean attachListener(Listener listener) {
            this.LOCK.lock();
            try {
                final Instant NOW = Instant.now();
                if (this.closed) {
                    return false;
                }

                if (this.headTimestamp != null) {
                    listener.onStart(NOW);
                }

                this.LISTENERS.add(listener);
                //listener may be already present in LISTENERS Set, that's why
                //we can't just return the result of Set::add()
                return true;
            } finally {
                this.LOCK.unlock();
            }
        }

        private void detachListener(Listener listener) {
            this.LOCK.lock();
            try {
                final Instant NOW = Instant.now();
                if (this.closed) {
                    return;
                }

                if (this.headTimestamp != null) {
                    listener.onStop(NOW);
                }

                this.LISTENERS.remove(listener);
            } finally {
                this.LOCK.unlock();
            }
        }

        private IntervalEventQueue.Builder eventBuilder() {
            //for performance reasons, does not impact correctness
            if (this.closed) {
                return Hub.EMPTY.eventBuilder();
            }

            return new Builder() {
                Integer capacity; //if null, unbounded (the default)
                ReplacementRule replacementRule = ReplacementRule.NONE;

                @Override
                public Builder ofCapacity(int capacity) {
                    if (capacity < 0) {
                        throw new IllegalArgumentException("Argument " +
                                "capacity can't be negative.");
                    }

                    this.capacity = capacity;
                    return this;
                }

                @Override
                public Builder ofReplacementRule(ReplacementRule
                                                 replacementRule) {
                    this.replacementRule =
                            Objects.requireNonNull(replacementRule);
                    return this;
                }

                @Override
                public IntervalEventQueue build() {
                    return Signal.this.add(this.capacity, this.replacementRule);
                }
            };
        }

        private IntervalEventQueue add(Integer capacity,
                                       ReplacementRule replacementRule) {
            if (null == capacity) {
                return Signal.this.HUB.unbounded();
            }

            if (0 == capacity) {
                return IntervalEventQueue.EMPTY;
            }

            Signal.this.LOCK.lock();
            try {
                if (Signal.this.closed) {
                    return IntervalEventQueue.EMPTY;
                }

                final int CAPACITY = capacity;
                final AbstractIntervalEvent EVENT = switch (replacementRule) {
                    case NONE -> new AbstractIntervalEvent(
                            Signal.this, Signal.this.headTimestamp) {
                        @Override
                        void add(Interval interval) {
                            this.LOCK.lock();
                            try {
                                this.headTimestamp = null;
                                this.consumed = false;

                                if (this.intervals.size() == CAPACITY) {
                                    return;
                                }
                                this.intervals.add(interval);
                            } finally {
                                this.LOCK.unlock();
                            }
                        }
                    };
                    case LEAST_RECENT -> new AbstractIntervalEvent(
                            Signal.this, Signal.this.headTimestamp) {
                        @Override
                        void add(Interval interval) {
                            this.LOCK.lock();
                            try {
                                this.headTimestamp = null;
                                this.consumed = false;

                                if (this.intervals.size() == CAPACITY) {
                                    //CAPACITY is positive, so it's safe
                                    this.intervals.removeFirst();
                                }
                                this.intervals.add(interval);
                            } finally {
                                this.LOCK.unlock();
                            }
                        }
                    };
                };

                Signal.this.EVENTS.add(EVENT);
                return EVENT;
            } finally {
                Signal.this.LOCK.unlock();
            }
        }

        //this.LOCK must be acquired before calling this - helper method
        private void forwardHeadTimestamp() {
            this.EVENTS.forEach(e -> e.headTimestamp = this.headTimestamp);
            this.LISTENERS.forEach(l -> l.onStart(this.headTimestamp));
        }

        //this.LOCK must be acquired before calling this - helper method
        private void forwardInterval(Instant startIncluded, Instant now) {
            this.EVENTS.forEach(e -> e.add(new Interval(startIncluded,
                                                        now)));
            this.LISTENERS.forEach(l -> l.onStop(this.headTimestamp));
        }
    }

    IntervalEventQueue EMPTY = new IntervalEventQueue() {
        @Override
        public boolean hasOccurredFor(Predicate<Duration> durationPredicate,
                                      boolean headIncluded) {
            return false;
        }

        @Override
        public boolean isHappeningFor(Predicate<Duration> durationPredicate) {
            return false;
        }

        @Override
        public Snapshot snapshot() {
            return Snapshot.EMPTY;
        }

        @Override
        public void clear(boolean headIncluded) {}

        @Override
        public void clear() {}

        @Override
        public ConnectionState getConnectionState() {
            return ConnectionState.DISCONNECTED;
        }

        @Override
        public void disconnect() {}

        @Override
        public String toString() {
            return "IntervalEventQueue.EMPTY";
        }
    };

    @Override
    default boolean hasOccurred() {
        return this.hasOccurred(true);
    }

    default boolean hasOccurred(boolean headIncluded) {
        return hasOccurredFor(d -> true, headIncluded);
    }

    boolean hasOccurredFor(Predicate<Duration> durationPredicate,
                           boolean headIncluded);

    default boolean isHappening() {
        return this.isHappeningFor(d -> true);
    }

    boolean isHappeningFor(Predicate<Duration> durationPredicate);
    Snapshot snapshot();
    void clear(boolean headIncluded);

}
