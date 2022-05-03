package moonkeki.app.events;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

public interface InstantEvent {

    abstract class Builder {
        private int capacity = 20;
        private ReplacementRule replacementRule = ReplacementRule.LEAST_RECENT;

        private Builder() {}

        public Builder ofCapacity(int capacity) {
            this.capacity = capacity;
            return this;
        }

        public Builder ofReplacementRule(ReplacementRule replacementRule) {
            this.replacementRule = replacementRule;
            return this;
        }

        public abstract InstantEvent build();
    }

    abstract class Snapshot {
        private static final Snapshot EMPTY = new Snapshot() {
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

        public boolean hasOccurred() {
            return !this.isEmpty();
        }

        public boolean isEmpty() {
            return this.size() == 0;
        }

        public abstract Stream<Instant> stream();
        public abstract int size();
    }

    @FunctionalInterface
    interface Listener {
        void onTrigger(Instant instant);
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
        public Builder cleanBuilder() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReplacementRule getReplacementRule() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {}

        @Override
        public int capacity() {
            return 0;
        }

        @Override
        public boolean isDisconnected() {
            return false;
        }

        @Override
        public void disconnect() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "InstantEvent.EMPTY";
        }
    };

    Snapshot snapshot();
    InstantEvent toClean();
    InstantEvent.Builder cleanBuilder();
    ReplacementRule getReplacementRule();
    void clear();
    int capacity();
    boolean isDisconnected();
    void disconnect();

}
