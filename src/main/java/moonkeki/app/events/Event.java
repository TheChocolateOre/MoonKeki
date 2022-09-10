package moonkeki.app.events;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public interface Event {

    interface Listener {
        void onTrigger(Instant timestamp);
        //Will be called only on the first close() of the underlying source
        void onClose(Instant timestamp);
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
            public Event event() {return Event.EMPTY;}
            @Override
            public ClosureState getClosureState() {return ClosureState.CLOSED;}
            @Override
            public void close() {}
            @Override
            public String toString() {return "Event.Hub.EMPTY";}
        };

        boolean attachListener(Listener listener);
        void detachListener(Listener listener);
        Event event();
        ClosureState getClosureState();
    }

    final class Signal implements AutoCloseable {
        private static final class EventImpl implements Event {
            //If null, this Event is disconnected, otherwise this Signal won't
            //be closed (it could be closing, but that's not a problem)
            volatile Signal signal;
            final AtomicBoolean OCCURRED = new AtomicBoolean();

            //signal can't be null
            EventImpl(Signal signal) {
                this.signal = Objects.requireNonNull(signal);
            }

            @Override
            public boolean hasOccurred() {
                return this.OCCURRED.getAndSet(false);
            }

            @Override
            public void reset() {
                this.OCCURRED.set(false);
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
        }

        private final Lock LOCK = new ReentrantLock(true);
        //only of positive capacity
        private final Set<EventImpl> EVENTS = new LinkedHashSet<>();
        private final Set<Listener> LISTENERS = new LinkedHashSet<>();
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
            public Event event() {
                return Signal.this.event();
            }

            @Override
            public ClosureState getClosureState() {
                return Signal.this.getClosureState();
            }
        };
        private Instant lastTimestamp;
        private volatile boolean closed;

        public void trigger() {
            this.LOCK.lock();
            try {
                final Instant NOW = Instant.now();
                if (this.closed) {
                    return;
                }

                this.lastTimestamp = NOW;
                this.EVENTS.forEach(e -> e.OCCURRED.set(true));
                this.LISTENERS.forEach(l -> l.onTrigger(NOW));
            } finally {
                this.LOCK.unlock();
            }
        }

        public void triggerElseNow(final Instant timestamp) {
            this.LOCK.lock();
            try {
                final Instant NOW = Instant.now();
                if (this.closed) {
                    return;
                }

                this.lastTimestamp = this.succeedsLast(timestamp) ?
                                     timestamp : NOW;
                this.EVENTS.forEach(e -> e.OCCURRED.set(true));
                this.LISTENERS.forEach(l -> l.onTrigger(this.lastTimestamp));
            } finally {
                this.LOCK.unlock();
            }
        }

        public void triggerElseThrow(final Instant timestamp) {
            this.LOCK.lock();
            try {
                if (this.closed) {
                    return;
                }

                if (!this.succeedsLast(timestamp)) {
                    throw new IllegalArgumentException("Argument timestamp " +
                            "must succeed the last timestamp of this Signal.");
                }

                this.lastTimestamp = timestamp;
                this.EVENTS.forEach(e -> e.OCCURRED.set(true));
                this.LISTENERS.forEach(l -> l.onTrigger(timestamp));
            } finally {
                this.LOCK.unlock();
            }
        }

        public Hub hub() {
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
                this.EVENTS.forEach(e -> e.signal = null);

                this.EVENTS.clear();
                this.LISTENERS.clear();
                this.closed = true;
            } finally {
                this.LOCK.unlock();
            }
        }

        public void closeElseNow(Instant timestamp) {
            this.LOCK.lock();
            try {
                final Instant NOW = Instant.now();
                if (this.closed) {
                    return;
                }

                if (!this.succeedsLast(timestamp)) {
                    timestamp = NOW;
                }

                //can't forEach, cause timestamp is not effectively final
                for (var l : this.LISTENERS) {
                    l.onClose(timestamp);
                }
                this.EVENTS.forEach(e -> e.signal = null);

                this.EVENTS.clear();
                this.LISTENERS.clear();
                this.closed = true;
            } finally {
                this.LOCK.unlock();
            }
        }

        public void closeElseThrow(final Instant timestamp) {
            this.LOCK.lock();
            try {
                if (this.closed) {
                    return;
                }

                if (!this.succeedsLast(timestamp)) {
                    throw new IllegalArgumentException("Argument timestamp " +
                            "must succeed the last timestamp of this Signal.");
                }

                this.LISTENERS.forEach(l -> l.onClose(timestamp));
                this.EVENTS.forEach(e -> e.signal = null);

                this.EVENTS.clear();
                this.LISTENERS.clear();
                this.closed = true;
            } finally {
                this.LOCK.unlock();
            }
        }

        private boolean succeedsLast(final Instant timestamp) {
            return Objects.compare(timestamp, this.lastTimestamp,
                    Comparator.nullsFirst(Instant::compareTo)) > 0;
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

        private Event event() {
            this.LOCK.lock();
            try {
                if (this.closed) {
                    return Event.EMPTY;
                }

                final EventImpl EVENT = new EventImpl(this);
                this.EVENTS.add(EVENT);
                return EVENT;
            } finally {
                this.LOCK.unlock();
            }
        }
    }

    final class CompositeOR {
        private final Set<Event.Hub> HUBS = new HashSet<>();

        private CompositeOR() {}

        public CompositeOR add(Event.Hub hub) {
            if (hub.getClosureState() == ClosureState.UNDETERMINED) {
                this.HUBS.add(hub);
            }
            return this;
        }

        public Event.Hub.Closeable build() {
            if (this.HUBS.isEmpty()) {
                return Event.Hub.EMPTY;
            }

            final Event.Signal SIGNAL = new Signal();
            final AtomicInteger COUNTER = new AtomicInteger(this.HUBS.size());
            final Runnable DECREMENT = () -> {
                if (COUNTER.decrementAndGet() == 0) {
                    SIGNAL.close();
                }
            };
            this.HUBS.forEach(h -> {
                Event.Listener LISTENER = new Event.Listener() {
                    @Override
                    public void onTrigger(Instant timestamp) {
                        SIGNAL.triggerElseNow(timestamp);
                    }

                    @Override
                    public void onClose(Instant timestamp) {
                        DECREMENT.run();
                    }
                };
                if (!h.attachListener(LISTENER)) {
                    DECREMENT.run();
                }
            });

            return new Event.Hub.Closeable() {
                final Event.Hub HUB = SIGNAL.HUB;

                @Override
                public boolean attachListener(Event.Listener listener) {
                    return this.HUB.attachListener(listener);
                }

                @Override
                public void detachListener(Event.Listener listener) {
                    this.HUB.detachListener(listener);
                }

                @Override
                public Event event() {
                    return this.HUB.event();
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

    static CompositeOR compositeOR() {
        return new CompositeOR();
    }

    Event EMPTY = new Event() {
        @Override
        public boolean hasOccurred() {return false;}
        @Override
        public void reset() {}
        @Override
        public ConnectionState getConnectionState() {
            return ConnectionState.DISCONNECTED;
        }
        @Override
        public void disconnect() {}
        @Override
        public String toString() {return "Event.EMPTY";}
    };

    boolean hasOccurred(); //Destructive
    void reset();
    ConnectionState getConnectionState();
    void disconnect();

}
