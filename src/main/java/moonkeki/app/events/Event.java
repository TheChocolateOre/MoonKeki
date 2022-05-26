package moonkeki.app.events;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public interface Event {

    interface Listener {
        void onTrigger();
        //Will be called only on the first close() of the underlying source
        void onClose();
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
            public Event event() {
                return Event.EMPTY;
            }

            @Override
            public ClosureState getClosureState() {
                return ClosureState.CLOSED;
            }

            @Override
            public void close() {}

            @Override
            public String toString() {
                return "Event.Hub.EMPTY";
            }
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
            public void clear() {
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
            public Event event() {
                return Signal.this.event();
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

                this.EVENTS.forEach(e -> e.OCCURRED.set(true));
                this.LISTENERS.forEach(Listener::onTrigger);
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

    final class CompositeORBuilder {
        private final Set<Event.Hub> HUBS = new HashSet<>();

        private CompositeORBuilder() {}

        public Event.CompositeORBuilder add(Event.Hub hub) {
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
                    public void onTrigger() {
                        SIGNAL.trigger();
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

    Event EMPTY = new Event() {
        @Override
        public boolean hasOccurred() {return false;}

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
            return "Event.EMPTY";
        }
    };

    static CompositeORBuilder compositeOR() {
        return new CompositeORBuilder();
    }

    boolean hasOccurred(); //Destructive
    void clear();
    ConnectionState getConnectionState();
    void disconnect();

}
