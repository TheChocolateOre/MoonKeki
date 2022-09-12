package moonkeki.app.events;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiPredicate;
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
        interface Closeable extends IntervalEvent.Hub, Event.Hub.Closeable {
            @Override
            void close();
            void closeElseNow(Instant timestamp);
            void closeElseThrow(Instant timestamp);
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
            public void closeElseNow(Instant timestamp) {}
            @Override
            public void closeElseThrow(Instant timestamp) {}
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

                this.stop(NOW);
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

                timestamp = this.succeedsLast(timestamp) ? timestamp : NOW;
                this.start(timestamp);
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

                if (!this.succeedsLast(timestamp)) {
                    throw new IllegalArgumentException("Argument timestamp " +
                            "must succeed the last timestamp of this Signal.");
                }

                this.start(timestamp);
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

                this.stop(NOW);
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

                timestamp = this.succeedsLast(timestamp) ? timestamp : NOW;
                this.stop(timestamp);
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

                if (!this.succeedsLast(timestamp)) {
                    throw new IllegalArgumentException("Argument timestamp " +
                            "must succeed the last timestamp of this Signal.");
                }

                this.stop(timestamp);
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

        //LOCK must be acquired
        private void start(final Instant timestamp) {
            if (null == this.HUB.headTimestamp) {
                this.HUB.asStart(timestamp);
                this.lastTimestamp = timestamp;
            }
            if (this.NEGATED_HUB.headTimestamp != null) {
                this.NEGATED_HUB.asStop(timestamp);
                this.lastTimestamp = timestamp;
            }
        }

        //LOCK must be acquired
        private void stop(Instant timestamp) {
            if (this.HUB.headTimestamp != null) {
                this.HUB.asStop(timestamp);
                this.lastTimestamp = timestamp;
            }
            if (null == this.NEGATED_HUB.headTimestamp) {
                this.NEGATED_HUB.asStart(timestamp);
                this.lastTimestamp = timestamp;
            }
        }
    }

    final class CompositeBuilder {
        @Deprecated
        private static abstract class AbstractComposite {
            final int SIZE;
            int started;

            AbstractComposite(Collection<? extends IntervalEvent.Hub> hubs) {
                this.SIZE = hubs.size();
                final Lock LOCK = new ReentrantLock(true);
                final IntervalEvent.Signal SIGNAL = new IntervalEvent.Signal();

                for (var h : hubs) {
                    final IntervalEvent.Listener LISTENER =
                            new IntervalEvent.Listener() {
                        @Override
                        public void onStart(Instant timestamp) {
                            LOCK.lock();
                            try {
                                AbstractComposite.this.started++;
                                AbstractComposite.this.onAdd();
                            } finally {
                                LOCK.unlock();
                            }
                        }
                        @Override
                        public void onStop(Instant timestamp) {
                            LOCK.lock();
                            try {
                                AbstractComposite.this.started--;
                                AbstractComposite.this.onRemove();
                            } finally {
                                LOCK.unlock();
                            }
                        }
                        @Override
                        public void onClose(Instant timestamp) {
                            SIGNAL.closeElseNow(timestamp);
                        }
                    };
                    if (!h.attachListener(LISTENER)) {
                        SIGNAL.close();
                        break;
                    }
                }
            }

            abstract void onAdd();
            abstract void onRemove();
        }

        private sealed class Composite {
            sealed class Listener implements IntervalEvent.Listener {
                @Override
                public void onStart(Instant timestamp) {
                    Composite.this.LOCK.lock();
                    try {
                        Composite.this.happening++;
                        this.refresh(timestamp);
                    } finally {
                        Composite.this.LOCK.unlock();
                    }
                }

                @Override
                public void onStop(Instant timestamp) {
                    Composite.this.LOCK.lock();
                    try {
                        Composite.this.happening--;
                        this.refresh(timestamp);
                    } finally {
                        Composite.this.LOCK.unlock();
                    }
                }

                @Override
                public void onClose(Instant timestamp) {
                    Composite.this.close(timestamp);
                }

                void refresh(Instant timestamp) {
                    if (CompositeBuilder.this.ON_CHANGE.test(
                            Composite.this.happening,
                            Composite.this.HUBS.size())) {
                        Composite.this.SIGNAL.startElseNow(timestamp);
                    } else {
                        Composite.this.SIGNAL.stopElseNow(timestamp);
                    }
                }
            }

            final Lock LOCK = new ReentrantLock(true);
            final IntervalEvent.Signal SIGNAL = new IntervalEvent.Signal();
            final List<IntervalEvent.Hub> HUBS;
            final List<Composite.Listener> LISTENERS;
            int happening;

            Composite(Collection<IntervalEvent.Hub> hubs) {
                this(hubs, 1);
            }

            Composite(Collection<IntervalEvent.Hub> hubs, int listenerSize) {
                this.HUBS = List.copyOf(hubs);
                this.LISTENERS = new ArrayList<>(listenerSize);
            }

            void close() {
                this.close(Instant.now());
            }

            void close(Instant timestamp) {
                this.SIGNAL.closeElseNow(timestamp);
                this.removeListeners();
            }

            Composite.Listener listener() {
                if (this.LISTENERS.isEmpty()) {
                    this.LISTENERS.add(new Composite.Listener());
                }

                return this.LISTENERS.get(0);
            }

            void removeListeners() {
                this.HUBS.forEach(h ->
                        h.detachListener(Composite.this.LISTENERS.get(0)));
            }
        }

        private final class OrderedComposite extends Composite {
            final class OrderedListener extends Composite.Listener {
                final int INDEX;

                OrderedListener(int index) {
                    this.INDEX = index;
                }

                @Override
                public void onStart(Instant timestamp) {
                    OrderedComposite.this.LOCK.lock();
                    try {
                        if (this.INDEX == OrderedComposite.this.nextIndex) {
                            OrderedComposite.this.nextIndex++;
                            super.onStart(timestamp);
                        }
                    } finally {
                        OrderedComposite.this.LOCK.unlock();
                    }
                }

                @Override
                public void onStop(Instant timestamp) {
                    OrderedComposite.this.LOCK.lock();
                    try {
                        if (this.INDEX < OrderedComposite.this.nextIndex) {
                            OrderedComposite.this.nextIndex = this.INDEX;
                            OrderedComposite.this.happening = this.INDEX;
                            this.refresh(timestamp);
                        }
                    } finally {
                        OrderedComposite.this.LOCK.unlock();
                    }
                }
            }

            int nextIndex;

            OrderedComposite(Collection<IntervalEvent.Hub> hubs) {
                super(hubs, hubs.size());
            }

            @Override
            OrderedComposite.OrderedListener listener() {
                final OrderedComposite.OrderedListener LISTENER =
                        new OrderedComposite.OrderedListener(
                        this.LISTENERS.size());
                this.LISTENERS.add(LISTENER);
                return LISTENER;
            }

            @Override
            void removeListeners() {
                final Iterator<Composite.Listener> LISTENER_ITR =
                        this.LISTENERS.iterator();
                for (Hub hub : this.HUBS) {
                    hub.detachListener(LISTENER_ITR.next());
                }
            }
        }

        //onChange: returns true if the signal should be started, and false if
        //          it should be stopped
        @Deprecated
        private static IntervalEvent.Hub.Closeable composite(
                Collection<? extends IntervalEvent.Hub> hubs,
                BiPredicate<Integer, Integer> onChange) {
            final class ListenerImpl implements IntervalEvent.Listener {
                final Lock LOCK = new ReentrantLock(true);
                final IntervalEvent.Signal SIGNAL = new IntervalEvent.Signal();
                int happening;

                @Override
                public void onStart(Instant timestamp) {
                    this.LOCK.lock();
                    try {
                        this.happening++;
                        this.refresh(timestamp);
                    } finally {
                        this.LOCK.unlock();
                    }
                }

                @Override
                public void onStop(Instant timestamp) {
                    this.LOCK.lock();
                    try {
                        this.happening--;
                        this.refresh(timestamp);
                    } finally {
                        this.LOCK.unlock();
                    }
                }

                @Override
                public void onClose(Instant timestamp) {
                    this.SIGNAL.closeElseNow(timestamp);
                }

                private void refresh(Instant timestamp) {
                    if (onChange.test(this.happening, hubs.size())) {
                        this.SIGNAL.startElseNow(timestamp);
                    } else {
                        this.SIGNAL.stopElseNow(timestamp);
                    }
                }
            }
            final ListenerImpl LISTENER = new ListenerImpl();
            hubs.forEach(h -> h.attachListener(LISTENER));
            return new Hub.Closeable() {
                @Override
                public void close() {LISTENER.SIGNAL.close();}
                @Override
                public void closeElseNow(Instant timestamp) {
                    LISTENER.SIGNAL.closeElseNow(timestamp);
                }
                @Override
                public void closeElseThrow(Instant timestamp) {
                    LISTENER.SIGNAL.closeElseThrow(timestamp);
                }
                @Override
                public boolean attachListener(Listener listener) {
                    return LISTENER.SIGNAL.hub().attachListener(listener);
                }
                @Override
                public void detachListener(Listener listener) {
                    LISTENER.SIGNAL.hub().detachListener(listener);
                }
                @Override
                public IntervalEvent event() {
                    return LISTENER.SIGNAL.hub().event();
                }
                @Override
                public Hub negate() {
                    return LISTENER.SIGNAL.hub().negate();
                }
                @Override
                public ClosureState getClosureState() {
                    return LISTENER.SIGNAL.hub().getClosureState();
                }
                @Override
                public boolean attachListener(Event.Listener listener) {
                    return LISTENER.SIGNAL.hub().attachListener(listener);
                }
                @Override
                public void detachListener(Event.Listener listener) {
                    LISTENER.SIGNAL.hub().detachListener(listener);
                }
            };
        }

        private final Set<IntervalEvent.Hub> HUBS = new LinkedHashSet<>();
        //(happening, total)
        private final BiPredicate<Integer, Integer> ON_CHANGE;
        private final boolean ORDERED;

        private CompositeBuilder(BiPredicate<Integer, Integer> onChange,
                                 boolean ordered) {
            this.ON_CHANGE = onChange;
            this.ORDERED = ordered;
        }

        public CompositeBuilder add(IntervalEvent.Hub hub) {
            if (hub.getClosureState() == ClosureState.UNDETERMINED) {
                this.HUBS.add(hub);
            }
            return this;
        }

        public IntervalEvent.Hub.Closeable build() {
            if (this.HUBS.isEmpty()) {
                return IntervalEvent.Hub.EMPTY;
            }

            final Composite COMPOSITE = this.ORDERED ?
                                        new OrderedComposite(this.HUBS) :
                                        new Composite(this.HUBS);
            for (var h : COMPOSITE.HUBS) {
                if (!h.attachListener(COMPOSITE.listener())) {
                    COMPOSITE.close();
                    break;
                }
            }
            return new Hub.Closeable() {
                @Override
                public void close() {COMPOSITE.SIGNAL.close();}
                @Override
                public void closeElseNow(Instant timestamp) {
                    COMPOSITE.SIGNAL.closeElseNow(timestamp);
                }
                @Override
                public void closeElseThrow(Instant timestamp) {
                    COMPOSITE.SIGNAL.closeElseThrow(timestamp);
                }
                @Override
                public boolean attachListener(Listener listener) {
                    return COMPOSITE.SIGNAL.hub().attachListener(listener);
                }
                @Override
                public void detachListener(Listener listener) {
                    COMPOSITE.SIGNAL.hub().detachListener(listener);
                }
                @Override
                public IntervalEvent event() {
                    return COMPOSITE.SIGNAL.hub().event();
                }
                @Override
                public Hub negate() {
                    return COMPOSITE.SIGNAL.hub().negate();
                }
                @Override
                public ClosureState getClosureState() {
                    return COMPOSITE.SIGNAL.hub().getClosureState();
                }
                @Override
                public boolean attachListener(Event.Listener listener) {
                    return COMPOSITE.SIGNAL.hub().attachListener(listener);
                }
                @Override
                public void detachListener(Event.Listener listener) {
                    COMPOSITE.SIGNAL.hub().detachListener(listener);
                }
            };
        }
    }

    static CompositeBuilder compositeBuilder(
            BiPredicate<Integer, Integer> statePredicate, boolean ordered) {
        return new CompositeBuilder(statePredicate, ordered);
    }

    static CompositeBuilder compositeORBuilder() {
        return IntervalEvent.compositeBuilder(
                (happening, total) -> happening != 0, false);
    }

    static CompositeBuilder compositeXORBuilder() {
        return IntervalEvent.compositeBuilder(
                (happening, total) -> happening % 2 == 1, false);
    }

    static IntervalEvent.CompositeBuilder compositeANDBuilder(boolean ordered) {
        return IntervalEvent.compositeBuilder(Integer::equals, ordered);
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
