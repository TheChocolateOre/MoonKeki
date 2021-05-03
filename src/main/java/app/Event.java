package app;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Event implements AutoCloseable {

    public static final class CompositeBuilder {
        private final Set<Consumer> CONSUMERS = new LinkedHashSet<>();

        private CompositeBuilder() {}

        public CompositeBuilder add(Consumer consumer) {
            this.CONSUMERS.add(consumer);
            return this;
        }

        public Consumer build() {
            if (this.CONSUMERS.isEmpty()) {
                throw new IllegalStateException("There are no Consumer's' in " +
                        "this CompositeBuilder.");
            }//end if

            if (this.CONSUMERS.size() == 1) {
                return this.CONSUMERS.stream()
                                     .findFirst()
                                     .get();
            }//end if

            this.CONSUMERS.forEach(c -> c.getEvent().LOCK.readLock().lock());
            try {
                //Object-less state
                final Set<Consumer> CURRENT_PROGRESS = this.CONSUMERS.stream()
                        .filter(Consumer::isHappening)
                        .collect(Collectors.toSet());
                final int SIZE = this.CONSUMERS.size();
                final Event COMPOSITE_EVENT = new Event(CURRENT_PROGRESS.size()
                        == SIZE) {
                    @Override
                    public void close() {
                        if (this.isClosed()) {
                            return;
                        }//end if

                        super.close();
                        CompositeBuilder.this
                                        .CONSUMERS
                                        .forEach(Consumer::close);
                    }
                };
                for (final Consumer c : this.CONSUMERS) {
                    c.getEvent().setStartCallback(() -> {
                        COMPOSITE_EVENT.ensureOpen();
                        COMPOSITE_EVENT.LOCK.writeLock().lock();
                        try {
                            CURRENT_PROGRESS.add(c);
                            if (CURRENT_PROGRESS.size() == SIZE) {
                                COMPOSITE_EVENT.start();
                            }//end if
                        } finally {
                            COMPOSITE_EVENT.LOCK.writeLock().unlock();
                        }//end try
                    });
                    c.getEvent().setStopCallback(() -> {
                        COMPOSITE_EVENT.ensureOpen();
                        COMPOSITE_EVENT.LOCK.writeLock().lock();
                        try {
                            CURRENT_PROGRESS.remove(c);
                            COMPOSITE_EVENT.stop();
                        } finally {
                            COMPOSITE_EVENT.LOCK.writeLock().unlock();
                        }//end try
                    });
                }//end for

                return COMPOSITE_EVENT.asConsumer();
            } finally {
                this.CONSUMERS.forEach(c -> c.getEvent().LOCK.readLock()
                                                             .unlock());
            }//end try
        }
    }//end static nested class CompositeBuilder

    public final class Consumer {
        private Consumer() {}

        public boolean isHappening() {
            Event.this.ensureOpen();
            Event.this.LOCK.readLock().lock();
            try {
                return Event.this.happening;
            } finally {
                Event.this.LOCK.readLock().unlock();
            }//end try
        }

        public boolean hasOccurred() {
            return this.hasOccurredFor(d -> true);
        }

        public boolean hasOccurredFor(Predicate<Duration> durationCondition) {
            Event.this.ensureOpen();
            Event.this.LOCK.writeLock().lock();
            try {
                if (Event.this.consumed) {
                    return false;
                }//end if

                Stream<Duration> durationStream =
                        Stream.concat(Event.this.DURATIONS.stream(),
                                      Stream.of(Event.this.currentDuration())
                                            .filter(d -> !d.isZero()));
                if (durationStream.anyMatch(durationCondition)) {
                    this.reset();
                    return true;
                } else {
                    return false;
                }//end if
            } finally {
                Event.this.LOCK.writeLock().unlock();
            }//end try
        }

        public void reset() {
            Event.this.ensureOpen();
            Event.this.LOCK.writeLock().lock();
            try {
                Event.this.DURATIONS.clear();
                Event.this.lastOccurrence = null;
                Event.this.consumed = true;
            } finally {
                Event.this.LOCK.writeLock().unlock();
            }//end try
        }

        public boolean isClosed() {
            return Event.this.isClosed();
        }

        public void close() {
            Event.this.close();
        }

        private Event getEvent() {
            return Event.this;
        }
    }//end inner class Consumer

    private final ReadWriteLock LOCK = new ReentrantReadWriteLock(true);
    private final List<Duration> DURATIONS = new LinkedList<>(); //Only > 0.0
    private final Consumer CONSUMER = new Consumer();
    private boolean happening;
    private Instant lastOccurrence;
    private boolean consumed = true;
    private Runnable startCallback;
    private Runnable stopCallback;
    private boolean closed;

    public static CompositeBuilder composite() {
        return new CompositeBuilder();
    }

    public Event(boolean happening) {
        this.happening = happening;
    }

    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        this.closed = true;
    }

    public void start() {
        if (this.CONSUMER.isHappening()) {
            return;
        }//end if

        this.LOCK.writeLock().lock();
        try {
            this.lastOccurrence = Instant.now();
            this.happening = true;
            this.consumed = false;
            if (this.startCallback != null) {
                this.startCallback.run();
            }//end if
        } finally {
            this.LOCK.writeLock().unlock();
        }//end try
    }

    public void stop() {
        if (!this.CONSUMER.isHappening()) {
            return;
        }//end if

        this.LOCK.writeLock().lock();
        try {
            final Duration DURATION = this.currentDuration();
            if (!DURATION.isZero()) {
                this.DURATIONS.add(DURATION);
            }//end if
            this.lastOccurrence = null;
            this.happening = false;
            if (this.stopCallback != null) {
                this.stopCallback.run();
            }//end if
        } finally {
            this.LOCK.writeLock().unlock();
        }//end try
    }

    public void setStartCallback(Runnable runnable) {
        this.ensureOpen();
        this.startCallback = runnable;
    }

    public void setStopCallback(Runnable runnable) {
        this.ensureOpen();
        this.stopCallback = runnable;
    }

    public Consumer asConsumer() {
        return this.CONSUMER;
    }

    protected void ensureOpen() throws IllegalStateException {
        if (this.isClosed()) {
            throw new IllegalStateException("This Event is closed.");
        }//end if
    }

    private Duration currentDuration() {
        this.LOCK.readLock().lock();
        try {
            final Instant NOW = Instant.now();
            if (null == this.lastOccurrence) {
                return Duration.ZERO;
            }//end if

            return Duration.between(this.lastOccurrence, NOW);
        } finally {
            this.LOCK.readLock().unlock();
        }//end try
    }

}//end class Event
