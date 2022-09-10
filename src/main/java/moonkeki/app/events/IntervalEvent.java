package moonkeki.app.events;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

public interface IntervalEvent extends Event {

    interface Listener {
        static IntervalEvent.Listener toIntervalEventListener(
                Event.Listener eventListener) {
            return new IntervalEvent.Listener() {
                @Override
                public void onStart(Instant timestamp) {
                    eventListener.onTrigger(timestamp);
                }
                @Override
                public void onStop(Instant timestamp) {}
                @Override
                public void onClose(Instant timestamp) {
                    eventListener.onClose(timestamp);
                }
            };
        }

        void onStart(Instant timestamp);
        void onStop(Instant timestamp);
        //Will be called only on the first close() of the underlying source
        void onClose(Instant timestamp);
    }

    interface Hub extends Event.Hub {
        interface Closeable extends IntervalEvent.Hub, AutoCloseable {
            @Override
            void close();
        }

        IntervalEvent.Hub.Closeable EMPTY = new IntervalEvent.Hub.Closeable() {
            @Override
            public boolean attachListener(Event.Listener listener) {
                return false;
            }
            @Override
            public void detachListener(Event.Listener listener) {}
            @Override
            public boolean attachListener(IntervalEvent.Listener listener) {
                return false;}
            @Override
            public void detachListener(IntervalEvent.Listener listener) {}
            @Override
            public IntervalEvent event() {return IntervalEvent.EMPTY;}
            @Override
            public IntervalEvent.Hub negate() {return this;}
            @Override
            public ClosureState getClosureState() {return ClosureState.CLOSED;}
            @Override
            public void close() {}
            @Override
            public String toString() {return "IntervalEvent.Hub.EMPTY";}
        };

        //false if this Hub is closed, otherwise true
        boolean attachListener(IntervalEvent.Listener listener);
        void detachListener(IntervalEvent.Listener listener);
        IntervalEvent event();
        IntervalEvent.Hub negate();
        ClosureState getClosureState();
    }

    final class Signal implements AutoCloseable {
        private static final class EventImpl implements IntervalEvent {
            //If null, this Event is disconnected, otherwise this Signal won't
            //be closed (it could be closing, but that's not a problem)
            volatile AbstractHub hub;
            final AtomicBoolean OCCURRED = new AtomicBoolean();

            //signal can't be null
            EventImpl(AbstractHub hub) {
                this.hub = Objects.requireNonNull(hub);
            }

            @Override
            public boolean hasOccurred() {
                return this.OCCURRED.getAndSet(false);
            }

            @Override
            public boolean isHappening() {
                final AbstractHub HUB = this.hub;
                if (HUB == null) {
                    return false;
                }

                return HUB.headTimestamp != null;
            }

            @Override
            public boolean isHappeningFor(
                    Predicate<Duration> durationPredicate) {
                final AbstractHub HUB = this.hub;
                if (HUB == null) {
                    return false;
                }

                final Instant HEAD_TIMESTAMP = HUB.headTimestamp;
                if (HEAD_TIMESTAMP == null) {
                    return false;
                }

                return durationPredicate.test(Duration.between(HEAD_TIMESTAMP,
                                                               Instant.now()));
            }

            @Override
            public void reset() {
                this.OCCURRED.set(false);
            }

            @Override
            public ConnectionState getConnectionState() {
                return null == this.hub ? ConnectionState.DISCONNECTED :
                                          ConnectionState.UNDETERMINED;
            }

            @Override
            public void disconnect() {
                final AbstractHub HUB = this.hub;
                if (HUB == null) {
                    return;
                }

                HUB.EVENT_LOCK.lock();
                try {
                    HUB.EVENTS.remove(this);
                    this.hub = null;
                } finally {
                    HUB.EVENT_LOCK.unlock();
                }
            }
        }

        private abstract class AbstractHub implements IntervalEvent.Hub {
            @Deprecated
            final Lock EVENT_LOCK = new ReentrantLock(true);
            //only of positive capacity
            final Set<EventImpl> EVENTS = new LinkedHashSet<>();
            @Deprecated
            final Lock LISTENER_LOCK = new ReentrantLock(true);
            final Set<IntervalEvent.Listener> LISTENERS = new LinkedHashSet<>();
            volatile Instant headTimestamp;

            @Override
            public IntervalEvent event() {
                this.EVENT_LOCK.lock();
                try {
                    if (this.getClosureState() == ClosureState.CLOSED) {
                        return IntervalEvent.EMPTY;
                    }

                    final EventImpl EVENT = new EventImpl(this);
                    this.EVENTS.add(EVENT);
                    return EVENT;
                } finally {
                    this.EVENT_LOCK.unlock();
                }
            }

            @Override
            public boolean attachListener(Event.Listener listener) {
                return this.attachListener(
                        IntervalEvent.Listener.toIntervalEventListener(
                        listener));
            }

            @Override
            public void detachListener(Event.Listener listener) {
                this.detachListener(
                        IntervalEvent.Listener.toIntervalEventListener(
                        listener));
            }

            public boolean attachListener(IntervalEvent.Listener listener) {
                this.LISTENER_LOCK.lock();
                try {
                    if (this.getClosureState() == ClosureState.CLOSED) {
                        return false;
                    }
                    this.LISTENERS.add(listener);
                    return true;
                } finally {
                    this.LISTENER_LOCK.unlock();
                }
            }

            public void detachListener(IntervalEvent.Listener listener) {
                this.LISTENER_LOCK.lock();
                try {
                    if (this.getClosureState() == ClosureState.CLOSED) {
                        return;
                    }
                    this.LISTENERS.remove(listener);
                } finally {
                    this.LISTENER_LOCK.unlock();
                }
            }

            @Override
            public ClosureState getClosureState() {
                return Signal.this.getClosureState();
            }

            //Signal.LOCK must be acquired
            //The timestamp must be externally validated
            //We also assume that this Hub is in the stopped state
            private void asStart(final Instant timestamp) {
                this.headTimestamp = timestamp;
                this.EVENTS.forEach(e -> e.OCCURRED.set(true));
                this.LISTENERS.forEach(l -> l.onStart(timestamp));
            }

            //Signal.LOCK must be acquired
            //The timestamp must be externally validated
            //We also assume that this Hub is in the started state
            private void asStop(final Instant timestamp) {
                this.headTimestamp = null;
                this.LISTENERS.forEach(l -> l.onStop(timestamp));
            }

            //Signal.LOCK must be acquired
            //The timestamp must be externally validated
            private void close(final Instant timestamp) {
                this.LISTENERS.forEach(l -> l.onClose(timestamp));
                this.EVENTS.forEach(e -> e.hub = null);
                this.EVENTS.clear();
                this.LISTENERS.clear();
            }
        }

        //used for everything
        final Lock LOCK = new ReentrantLock(true);
        //Irrelevant when both EVENTS and LISTENERS Set's' are empty
        private Instant lastTimestamp;
        private final AbstractHub HUB = new AbstractHub() {
            @Override
            public Hub negate() {
                return Signal.this.NEGATED_HUB;
            }
        };
        private final AbstractHub NEGATED_HUB = new AbstractHub() {
            @Override
            public Hub negate() {
                return Signal.this.HUB;
            }
        };
        private volatile boolean closed;

        public void start() {
            this.LOCK.lock();
            try {
                final Instant NOW = Instant.now();
                if (this.closed) {
                    return;
                }

                //already started
                if (this.HUB.headTimestamp != null) {
                    return;
                }

                this.lastTimestamp = NOW;
                this.HUB.asStart(NOW);
                this.NEGATED_HUB.asStop(NOW);
            } finally {
                this.LOCK.unlock();
            }
        }

        public void startElseNow(final Instant timestamp) {
            this.LOCK.lock();
            try {
                final Instant NOW = Instant.now();
                if (this.closed) {
                    return;
                }

                //already started
                if (this.HUB.headTimestamp != null) {
                    return;
                }

                this.lastTimestamp = this.succeedsLast(timestamp) ?
                                     timestamp : NOW;
                this.HUB.asStart(this.lastTimestamp);
                this.NEGATED_HUB.asStop(this.lastTimestamp);
            } finally {
                this.LOCK.unlock();
            }
        }

        public void startElseThrow(final Instant timestamp) {
            this.LOCK.lock();
            try {
                if (this.closed) {
                    return;
                }

                //already started
                if (this.HUB.headTimestamp != null) {
                    return;
                }

                if (!this.succeedsLast(timestamp)) {
                    throw new IllegalArgumentException("Argument timestamp " +
                            "must succeed the last timestamp of this Signal.");
                }

                this.lastTimestamp = timestamp;
                this.HUB.asStart(timestamp);
                this.NEGATED_HUB.asStop(timestamp);
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

                //already stopped
                if (null == this.HUB.headTimestamp) {
                    return;
                }

                this.lastTimestamp = NOW;
                this.HUB.asStop(NOW);
                this.NEGATED_HUB.asStart(NOW);
            } finally {
                this.LOCK.unlock();
            }
        }

        public void stopElseNow(final Instant timestamp) {
            this.LOCK.lock();
            try {
                final Instant NOW = Instant.now();
                if (this.closed) {
                    return;
                }

                //already stopped
                if (null == this.HUB.headTimestamp) {
                    return;
                }

                //lastTimestamp can't be null here;
                //headTimestamp != null => lastTimestamp != null
                this.lastTimestamp = this.lastTimestamp.isBefore(timestamp) ?
                                     timestamp : NOW;
                this.HUB.asStop(this.lastTimestamp);
                this.NEGATED_HUB.asStart(this.lastTimestamp);
            } finally {
                this.LOCK.unlock();
            }
        }

        public void stopElseThrow(final Instant timestamp) {
            this.LOCK.lock();
            try {
                if (this.closed) {
                    return;
                }

                //Already stopped
                if (null == this.HUB.headTimestamp) {
                    return;
                }

                //lastTimestamp can't be null here;
                //headTimestamp != null => lastTimestamp != null
                if (!this.lastTimestamp.isBefore(timestamp)) {
                    throw new IllegalArgumentException("Argument timestamp " +
                            "must succeed the last timestamp of this Signal.");
                }

                this.lastTimestamp = timestamp;
                this.HUB.asStop(timestamp);
                this.NEGATED_HUB.asStart(timestamp);
            } finally {
                this.LOCK.unlock();
            }
        }

        public IntervalEvent.Hub hub() {
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

                this.HUB.close(NOW);
                this.NEGATED_HUB.close(NOW);
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

                this.HUB.close(timestamp);
                this.NEGATED_HUB.close(timestamp);
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

                this.HUB.close(timestamp);
                this.NEGATED_HUB.close(timestamp);
                this.closed = true;
            } finally {
                this.LOCK.unlock();
            }
        }

        private boolean succeedsLast(final Instant timestamp) {
            return Objects.compare(timestamp, this.lastTimestamp,
                    Comparator.nullsFirst(Instant::compareTo)) > 0;
        }
    }

    IntervalEvent EMPTY = new IntervalEvent() {
        @Override
        public boolean hasOccurred() {return false;}
        @Override
        public boolean isHappeningFor(Predicate<Duration> durationPredicate) {
            return false;
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
        public String toString() {return "IntervalEvent.EMPTY";}
    };

    default boolean isHappening() {
        return this.isHappeningFor(d -> true);
    }

    boolean isHappeningFor(Predicate<Duration> durationPredicate);

}
