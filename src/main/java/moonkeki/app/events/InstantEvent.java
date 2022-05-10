package moonkeki.app.events;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

public interface InstantEvent {

    interface Builder {
        //Convenience for ofCapacity(1) & replacementRule(NONE)
        default InstantEvent singletonLeastRecent() {
            return this.ofCapacity(1)
                       .ofReplacementRule(ReplacementRule.NONE)
                       .build();
        }

        //Convenience for ofCapacity(1) & replacementRule(LEAST_RECENT)
        default InstantEvent singletonMostRecent() {
            return this.ofCapacity(1)
                       .ofReplacementRule(ReplacementRule.LEAST_RECENT)
                       .build();
        }

        //ofCapacity(+inf) & replacementRule(NONE). Throws if too much for the
        //underlying collection
        InstantEvent unbounded();
        Builder ofCapacity(int capacity);
        Builder ofReplacementRule(ReplacementRule replacementRule);
        InstantEvent build();
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

    final class Signal implements AutoCloseable {
        interface Listener {
            void onTrigger(Instant timestamp);
            void onClose();
        }

        private static abstract class AbstractInstantEvent implements
                                                           InstantEvent {
            final Lock LOCK = new ReentrantLock(true);
            //If null, this InstantEvent is disconnected, otherwise this Signal
            //won't be closed (it could be closing, but that's not a problem)
            volatile Signal signal;
            Deque<Instant> timestamps = new LinkedList<>();

            //signal can't be null
            AbstractInstantEvent(Signal signal) {
                this.signal = Objects.requireNonNull(signal);
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

            public InstantEvent toClean() {
                return this.cleanBuilder().build();
            }

            @Override
            public void clear() {
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
                final Signal SIGNAL = this.signal;
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
        private Instant lastTimestamp;
        private volatile boolean closed;

        public void trigger() {
            this.LOCK.lock();
            try {
                if (this.closed) {
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

        public void attachListener(Listener listener) {
            this.LOCK.lock();
            try {
                if (this.closed) {
                    return;
                }
                this.LISTENERS.add(listener);
            } finally {
                this.LOCK.unlock();
            }
        }

        public void detachListener(Listener listener) {
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

        public InstantEvent.Builder eventBuilder() {
            //for performance reasons, does not impact correctness
            if (this.closed) {
                return InstantEvent.EMPTY.cleanBuilder();
            }

            return new Builder() {
                int capacity = 20;
                ReplacementRule replacementRule = ReplacementRule.LEAST_RECENT;

                @Override
                public InstantEvent unbounded() {
                    Signal.this.LOCK.lock();
                    try {
                        if (Signal.this.closed) {
                            return InstantEvent.EMPTY;
                        }

                        final AbstractInstantEvent EVENT =
                          new AbstractInstantEvent(Signal.this) {
                            @Override
                            public Builder cleanBuilder() {
                                final Signal SIGNAL = this.signal;
                                if (SIGNAL == null) {
                                    return InstantEvent.EMPTY.cleanBuilder();
                                }

                                return new Builder() {
                                    final Builder BUILDER =
                                            SIGNAL.eventBuilder()
                                            .ofCapacity(Integer.MAX_VALUE)
                                            .ofReplacementRule(
                                               ReplacementRule.NONE);
                                    boolean dirty;

                                    @Override
                                    public InstantEvent unbounded() {
                                        return this.BUILDER.unbounded();
                                    }

                                    @Override
                                    public Builder ofCapacity(int capacity) {
                                        this.BUILDER.ofCapacity(capacity);
                                        this.dirty = true;
                                        return this;
                                    }

                                    @Override
                                    public Builder ofReplacementRule(
                                     ReplacementRule replacementRule) {
                                        this.BUILDER
                                            .ofReplacementRule(replacementRule);
                                        this.dirty = true;
                                        return this;
                                    }

                                    @Override
                                    public InstantEvent build() {
                                        return this.dirty ?
                                               this.BUILDER.build() :
                                               this.BUILDER.unbounded();
                                    }
                                };
                            }

                            @Override
                            void add(Instant timestamp) {
                                this.timestamps.add(timestamp);
                            }
                        };

                        Signal.this.EVENTS.add(EVENT);
                        return EVENT;
                    } finally {
                        Signal.this.LOCK.unlock();
                    }
                }

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
                public InstantEvent build() {
                    if (0 == this.capacity) {
                        return InstantEvent.EMPTY;
                    }

                    Signal.this.LOCK.lock();
                    try {
                        if (Signal.this.closed) {
                            return InstantEvent.EMPTY;
                        }

                        final int CAPACITY = this.capacity;
                        final ReplacementRule REPLACEMENT_RULE =
                                this.replacementRule;
                        final AbstractInstantEvent EVENT =
                                switch (REPLACEMENT_RULE) {
                            case NONE -> new AbstractInstantEvent(Signal.this) {
                                @Override
                                public Builder cleanBuilder() {
                                    final Signal SIGNAL = this.signal;
                                    if (SIGNAL == null) {
                                        return InstantEvent.EMPTY
                                                           .cleanBuilder();
                                    }

                                    return SIGNAL.eventBuilder()
                                                 .ofCapacity(CAPACITY)
                                                 .ofReplacementRule(
                                                  ReplacementRule.NONE);
                                }

                                @Override
                                void add(Instant timestamp) {
                                    if (this.timestamps.size() == CAPACITY) {
                                        return;
                                    }
                                    this.timestamps.add(timestamp);
                                }
                            };
                            case LEAST_RECENT -> new AbstractInstantEvent(
                                    Signal.this) {
                                @Override
                                public Builder cleanBuilder() {
                                    final Signal SIGNAL = this.signal;
                                    if (SIGNAL == null) {
                                        return InstantEvent.EMPTY
                                                           .cleanBuilder();
                                    }

                                    return SIGNAL.eventBuilder()
                                                 .ofCapacity(CAPACITY)
                                                 .ofReplacementRule(
                                                  ReplacementRule.LEAST_RECENT);
                                }

                                @Override
                                void add(Instant timestamp) {
                                    if (this.timestamps.size() == CAPACITY) {
                                        //CAPACITY is positive, so it's safe
                                        this.timestamps.removeFirst();
                                    }
                                    this.timestamps.add(timestamp);
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
                for (AbstractInstantEvent e : this.EVENTS) {
                    e.LOCK.lock();
                    try {
                        e.signal = null;
                    } finally {
                        e.LOCK.unlock();
                    }
                }

                this.EVENTS.clear();
                this.LISTENERS.clear();
                this.closed = true;
            } finally {
                this.LOCK.unlock();
            }
        }

        //this.LOCK must be acquired before calling this - helper method
        private void forwardLastTimestamp() {
            this.EVENTS.forEach(e -> e.add(this.lastTimestamp));
            this.LISTENERS.forEach(l -> l.onTrigger(this.lastTimestamp));
        }
    }

    InstantEvent EMPTY = new InstantEvent() {
        @Override
        public Snapshot snapshot() {
            return Snapshot.EMPTY;
        }

        @Override
        public InstantEvent toClean() {
            return this;
        }

        @Override
        public InstantEvent.Builder cleanBuilder() {
            return new Builder() {
                @Override
                public InstantEvent singletonLeastRecent() {
                    return InstantEvent.EMPTY;
                }

                @Override
                public InstantEvent singletonMostRecent() {
                    return InstantEvent.EMPTY;
                }

                @Override
                public InstantEvent unbounded() {
                    return InstantEvent.EMPTY;
                }

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
                public InstantEvent build() {
                    return InstantEvent.EMPTY;
                }
            };
        }

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
            return "InstantEvent.EMPTY";
        }
    };

    Snapshot snapshot();
    InstantEvent toClean();
    InstantEvent.Builder cleanBuilder();
    void clear();
    ConnectionState getConnectionState();
    void disconnect();

}
