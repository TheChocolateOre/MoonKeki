package moonkeki.app.events;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

@Deprecated(forRemoval = true)
public interface InstantEventQueue extends Event {

    interface Builder {
        Builder ofCapacity(int capacity);
        Builder ofReplacementRule(ReplacementRule replacementRule);
        InstantEventQueue build();
    }

    interface Listener {
        void onTrigger(Instant timestamp);
        //Will be called only on the first close() of the underlying source
        void onClose();
    }

    final class CompositeBuilder {
        private final Set<Hub> HUBS = new HashSet<>();

        private CompositeBuilder() {}

        public CompositeBuilder add(Hub hub) {
            if (hub.getClosureState() == ClosureState.UNDETERMINED) {
                this.HUBS.add(hub);
            }
            return this;
        }

        public Hub.Closeable build() {
            if (this.HUBS.isEmpty()) {
                return Hub.EMPTY;
            }

            final Signal SIGNAL = new Signal();
            final AtomicInteger COUNTER = new AtomicInteger(this.HUBS.size());
            final Runnable DECREMENT = () -> {
                if (COUNTER.decrementAndGet() == 0) {
                    SIGNAL.close();
                }
            };
            this.HUBS.forEach(h -> {
                Listener LISTENER = new Listener() {
                    @Override
                    public void onTrigger(Instant timestamp) {
                        SIGNAL.triggerElseNow(timestamp);
                    }

                    @Override
                    public void onClose() {
                        DECREMENT.run();
                    }
                };
                if (!h.attachListener(LISTENER)) {
                    DECREMENT.run();
                }
            });

            return new Hub.Closeable() {
                final Hub HUB = SIGNAL.HUB;

                @Override
                public boolean attachListener(Listener listener) {
                    return this.HUB.attachListener(listener);
                }

                @Override
                public void detachListener(Listener listener) {
                    this.HUB.detachListener(listener);
                }

                @Override
                public InstantEventQueue unbounded() {
                    return this.HUB.unbounded();
                }

                @Override
                public Builder eventBuilder() {
                    return this.HUB.eventBuilder();
                }

                @Override
                public ClosureState getClosureState() {
                    return this.HUB.getClosureState();
                }

                @Override
                public void close() {
                    SIGNAL.close();
                }
            };
        }
    }

    interface Snapshot {
        Snapshot EMPTY = new Snapshot() {
            @Override
            public boolean hasOccurred() {
                return false;
            }

            @Override
            public Stream<Instant> stream() {
                return Stream.empty();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return true;
            }

            @Override
            public String toString() {
                return "InstantEvent.Snapshot.EMPTY";
            }
        };

        default boolean hasOccurred() {
            return !this.isEmpty();
        }

        default boolean isEmpty() {
            return this.size() == 0;
        }

        Stream<Instant> stream();
        int size();
    }

    interface Hub {
        interface Closeable extends Hub, AutoCloseable {
            @Override
            void close();
        }

        Hub.Closeable EMPTY = new Closeable() {
            @Override
            public boolean attachListener(Listener listener) {return false;}

            @Override
            public void detachListener(Listener listener) {}

            @Override
            public InstantEventQueue unbounded() {
                return InstantEventQueue.EMPTY;
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
                    public InstantEventQueue build() {
                        return InstantEventQueue.EMPTY;
                    }
                };
            }

            @Override
            public ClosureState getClosureState() {
                return ClosureState.CLOSED;
            }

            @Override
            public void close() {}

            @Override
            public String toString() {
                return "InstantEvent.Hub.EMPTY";
            }
        };

        //Builder convenience for ofCapacity(1) & replacementRule(NONE)
        default InstantEventQueue singletonLeastRecent() {
            return this.eventBuilder()
                       .ofCapacity(1)
                       .ofReplacementRule(ReplacementRule.NONE)
                       .build();
        }

        //Builder convenience for ofCapacity(1) & replacementRule(LEAST_RECENT)
        default InstantEventQueue singletonMostRecent() {
            return this.eventBuilder()
                       .ofCapacity(1)
                       .ofReplacementRule(ReplacementRule.LEAST_RECENT)
                       .build();
        }

        //false if this Hub is closed, otherwise true
        boolean attachListener(Listener listener);
        void detachListener(Listener listener);
        //ofCapacity(+inf) & replacementRule(NONE). Throws if too much for the
        //underlying collection
        InstantEventQueue unbounded();
        Builder eventBuilder();
        ClosureState getClosureState();
    }

    final class Signal implements AutoCloseable {
        private static abstract class AbstractInstantEvent implements
                InstantEventQueue {
            final Lock LOCK = new ReentrantLock(true);
            //If null, this InstantEvent is disconnected, otherwise this Signal
            //won't be closed (it could be closing, but that's not a problem)
            volatile InstantEventQueue.Signal signal;
            Deque<Instant> timestamps = new LinkedList<>();

            //signal can't be null
            AbstractInstantEvent(InstantEventQueue.Signal signal) {
                this.signal = Objects.requireNonNull(signal);
            }

            @Override
            public boolean hasOccurred() {
                this.LOCK.lock();
                try {
                    final boolean OCCURRED = !this.timestamps.isEmpty();
                    this.timestamps.clear();
                    return OCCURRED;
                } finally {
                    this.LOCK.unlock();
                }
            }

            @Override
            public Snapshot snapshot() {
                this.LOCK.lock();
                try {
                    if (this.timestamps.isEmpty()) {
                        return Snapshot.EMPTY;
                    }

                    final Collection<Instant> TIMESTAMPS = this.timestamps;
                    this.timestamps = new LinkedList<>();
                    return new Snapshot() {
                        @Override
                        public Stream<Instant> stream() {
                            return TIMESTAMPS.stream();
                        }

                        @Override
                        public int size() {
                            return TIMESTAMPS.size();
                        }
                    };
                } finally {
                    this.LOCK.unlock();
                }
            }

            @Override
            public void reset() {
                this.LOCK.lock();
                try {
                    this.timestamps.clear();
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
                final InstantEventQueue.Signal SIGNAL = this.signal;
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
            }

            //this.LOCK must be acquired before calling this - helper method.
            //timestamp can't be before last
            abstract void add(Instant timestamp);
        }

        private final Lock LOCK = new ReentrantLock(true);
        //only of positive capacity
        private final Set<AbstractInstantEvent> EVENTS = new LinkedHashSet<>();
        private final Set<Listener> LISTENERS = new LinkedHashSet<>();
        //Irrelevant when both EVENTS and LISTENERS Set's' are empty
        private Instant lastTimestamp;
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
            public InstantEventQueue unbounded() {
                Signal.this.LOCK.lock();
                try {
                    if (Signal.this.closed) {
                        return InstantEventQueue.EMPTY;
                    }

                    final AbstractInstantEvent EVENT =
                            new AbstractInstantEvent(Signal.this) {
                                @Override
                                void add(Instant timestamp) {
                                    this.LOCK.lock();
                                    try {
                                        this.timestamps.add(timestamp);
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

        public void trigger() {
            this.LOCK.lock();
            try {
                if (this.closed) {
                    return;
                }

                if (this.EVENTS.isEmpty() && this.LISTENERS.isEmpty()) {
                    return;
                }

                this.lastTimestamp = Instant.now();
                this.forwardLastTimestamp();
            } finally {
                this.LOCK.unlock();
            }
        }

        public void triggerElseNow(Instant timestamp) {
            this.LOCK.lock();
            try {
                if (this.closed) {
                    return;
                }

                if (this.EVENTS.isEmpty() && this.LISTENERS.isEmpty()) {
                    return;
                }

                final Instant NOW = Instant.now();
                this.lastTimestamp = null == this.lastTimestamp ||
                                     this.lastTimestamp.isBefore(timestamp) ?
                                     timestamp : NOW;
                this.forwardLastTimestamp();
            } finally {
                this.LOCK.unlock();
            }
        }

        public void triggerElseThrow(Instant timestamp) {
            this.LOCK.lock();
            try {
                if (this.closed) {
                    return;
                }

                if (this.EVENTS.isEmpty() && this.LISTENERS.isEmpty()) {
                    return;
                }

                if (this.lastTimestamp != null &&
                   !this.lastTimestamp.isBefore(timestamp)) {
                    throw new IllegalArgumentException("Argument timestamp " +
                            "must be after the last timestamp of this Signal.");
                }

                this.lastTimestamp = timestamp;
                this.forwardLastTimestamp();
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
                if (this.closed) {
                    return;
                }

                this.LISTENERS.forEach(Listener::onClose);
                this.EVENTS.forEach(e -> e.signal = null);

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
                if (this.closed) {
                    return false;
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
                if (this.closed) {
                    return;
                }
                this.LISTENERS.remove(listener);
            } finally {
                this.LOCK.unlock();
            }
        }

        private InstantEventQueue.Builder eventBuilder() {
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
                public InstantEventQueue build() {
                    if (null == this.capacity) {
                        return Signal.this.HUB.unbounded();
                    }

                    if (0 == this.capacity) {
                        return InstantEventQueue.EMPTY;
                    }

                    Signal.this.LOCK.lock();
                    try {
                        if (Signal.this.closed) {
                            return InstantEventQueue.EMPTY;
                        }

                        final int CAPACITY = this.capacity;
                        final ReplacementRule REPLACEMENT_RULE =
                                this.replacementRule;
                        final AbstractInstantEvent EVENT =
                                switch (REPLACEMENT_RULE) {
                            case NONE -> new AbstractInstantEvent(Signal.this) {
                                @Override
                                void add(Instant timestamp) {
                                    this.LOCK.lock();
                                    try {
                                        if (this.timestamps.size() ==
                                            CAPACITY) {
                                            return;
                                        }
                                        this.timestamps.add(timestamp);
                                    } finally {
                                        this.LOCK.unlock();
                                    }
                                }
                            };
                            case LEAST_RECENT -> new AbstractInstantEvent(
                                    Signal.this) {
                                @Override
                                void add(Instant timestamp) {
                                    this.LOCK.lock();
                                    try {
                                        if (this.timestamps.size() ==
                                            CAPACITY) {
                                            //CAPACITY is positive, so it's safe
                                            this.timestamps.removeFirst();
                                        }
                                        this.timestamps.add(timestamp);
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
            };
        }

        //this.LOCK must be acquired before calling this - helper method
        private void forwardLastTimestamp() {
            this.EVENTS.forEach(e -> e.add(this.lastTimestamp));
            this.LISTENERS.forEach(l -> l.onTrigger(this.lastTimestamp));
        }
    }

    InstantEventQueue EMPTY = new InstantEventQueue() {
        @Override
        public boolean hasOccurred() {
            return false;
        }

        @Override
        public Snapshot snapshot() {
            return Snapshot.EMPTY;
        }

        @Override
        public void reset() {}

        @Override
        public ConnectionState getConnectionState() {
            return ConnectionState.DISCONNECTED;
        }

        @Override
        public void disconnect() {}

        @Override
        public String toString() {
            return "InstantEvent.EMPTY";
        }
    };
    static CompositeBuilder compositeOR() {
        return new CompositeBuilder();
    }

    Snapshot snapshot();

}
