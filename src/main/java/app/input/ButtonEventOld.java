package app.input;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Deprecated
public class ButtonEventOld implements AutoCloseable {

    public static final class Builder {
        private final Set<Button.Snapshot> BUTTON_SNAPSHOTS = new HashSet<>();
        private final Set<Button> BUTTONS = new HashSet<>();

        private Builder() {}

        public Builder add(Keyboard.Key key, boolean pressed) {
            return this.add(key.asButton().orElseThrow(), pressed);
        }

        public Builder add(Button button, boolean pressed) {
            Button.Snapshot snapshot = new Button.Snapshot(button, pressed);
            if (this.BUTTONS.contains(button) &&
               !this.BUTTON_SNAPSHOTS.contains(snapshot)) {
                throw new IllegalArgumentException("The given Button is " +
                        "being tracked, but with a different pressed value.");
            }//end if

            this.BUTTON_SNAPSHOTS.add(snapshot);
            this.BUTTONS.add(button);

            return this;
        }

        public ButtonEventOld build() {
            ButtonEventOld event = new ButtonEventOld(this.BUTTON_SNAPSHOTS);
            for (Button b : this.BUTTONS) {
                //TODO Pattern Matching for switch refactor candidate (16)
                if (b instanceof Keyboard.Button kb) {
                    //Keyboard.addButtonEvent(kb, event);
                } else if (b instanceof Mouse.Button mb) {
                    Mouse.addButtonEvent(mb, event);
                }//end if
            }//end for

            return event;
        }
    }//end static nested class Builder

    private final ReadWriteLock LOCK = new ReentrantReadWriteLock(true);
    private final Set<Button.Snapshot> DESIRED_SNAPSHOTS;
    private final Set<Button> TRACKED_BUTTONS;
    private final Set<Button> CURRENT_PROGRESS;
    private Instant lastOccurrence;
    private double maxDuration; //seconds
    private boolean consumed = true;
    private boolean closed;

    public static Builder builder() {
        return new Builder();
    }

    public static ButtonEventOld of(Keyboard.Key key, boolean pressed) {
        return ButtonEventOld.of(key.asButton().orElseThrow(), pressed);
    }

    public static ButtonEventOld of(Button button, boolean pressed) {
        return ButtonEventOld.builder()
                          .add(button, pressed)
                          .build();
    }

    private ButtonEventOld(Set<Button.Snapshot> buttonSnapshots) {
        this.DESIRED_SNAPSHOTS = Set.of(buttonSnapshots.toArray(
                new Button.Snapshot[0]));
        this.TRACKED_BUTTONS = Set.of(buttonSnapshots.stream()
                .map(Button.Snapshot::button)
                .toArray(Button[]::new));

        if (buttonSnapshots.size() != this.TRACKED_BUTTONS.size()) {
            throw new IllegalArgumentException("Argument Set " +
                    "buttonSnapshots can't have 2 different Snapshot's' " +
                    "with the same Button.");
        }//end if

        this.CURRENT_PROGRESS = buttonSnapshots.stream()
                .filter(s -> s.button().isPressed() == s.pressed())
                .map(Button.Snapshot::button)
                .collect(Collectors.toSet());
    }

    //Non-Destructive
    public boolean isHappening() {
        this.ensureOpen();

        this.LOCK.readLock().lock();
        try {
            return this.CURRENT_PROGRESS.size() ==
                   this.DESIRED_SNAPSHOTS.size();
        } finally {
            this.LOCK.readLock().unlock();
        }//end try
    }

    //Destructive on call
    public boolean hasOccurred() {
        return this.hasOccurredForAtLeast(0.0, false);
    }

    /*
        Special cases:
        [x] NaN duration. (Exception)
        [x] Negative duration. (Exception)
        [3] Zero inclusive duration. (Turns into zero exclusive duration)
        [x] Inclusive positive infinity. (No action needed)
        [x] Exclusive positive infinity. (No action needed)
    */
    public boolean hasOccurredForAtLeast(double duration, boolean inclusive) {
        this.ensureOpen();

        if (Double.isNaN(duration)) {
            throw new IllegalArgumentException("Argument duration can't be " +
                    "NaN.");
        }//end if

        if (duration < 0.0) {
            throw new IllegalArgumentException("Argument duration can't be " +
                    "negative.");
        }//end if

        if (0.0 == duration && inclusive) {
            inclusive = false;
        }//end if

        this.LOCK.writeLock().lock();
        try {
            if (this.consumed) {
                return false;
            }//end if

            final Instant NOW = Instant.now();
            final double EVENT_DURATION;
            if (this.isHappening()) {
                Duration d = Duration.between(this.lastOccurrence, NOW);
                EVENT_DURATION = Math.max(d.getSeconds() + d.getNano() /
                        1_000_000_000.0, this.maxDuration);
            } else {
                EVENT_DURATION = this.maxDuration;
            }//end if

            if (inclusive ? EVENT_DURATION < duration :
                            EVENT_DURATION <= duration) {
                return false;
            }//end if

            this.reset();
            return true;
        } finally {
            this.LOCK.writeLock().unlock();
        }//end try
    }

    public void reset() {
        this.ensureOpen();

        this.LOCK.writeLock().lock();
        try {
            this.consumed = true;
            this.maxDuration = 0.0;
            this.lastOccurrence = null;
        } finally {
            this.LOCK.writeLock().unlock();
        }//end try
    }

    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        if (this.isClosed()) {
            return;
        }//end if

        for (Button b : this.TRACKED_BUTTONS) {
            //TODO Pattern Matching for switch refactor candidate (16)
            if (b instanceof Keyboard.Button kb) {
                //Keyboard.removeButtonEvent(kb, this);
            } else if (b instanceof Mouse.Button mb) {
                Mouse.removeButtonEvent(mb, this);
            }//end if
        }//end for

        this.closed = true;
    }

    void update(Button.Snapshot buttonSnapshot) {
        this.ensureOpen();

        //We assume .contains() is thread-safe
        if (!this.TRACKED_BUTTONS.contains(buttonSnapshot.button())) {
            return;
        }//end if

        this.LOCK.writeLock().lock();
        try {
            final Instant NOW = Instant.now();
            final int OLD_SIZE = this.CURRENT_PROGRESS.size();
            if (this.DESIRED_SNAPSHOTS.contains(buttonSnapshot)) {
                this.CURRENT_PROGRESS.add(buttonSnapshot.button());
                if (this.CURRENT_PROGRESS.size() ==
                        this.DESIRED_SNAPSHOTS.size()) {
                    this.lastOccurrence = NOW;
                }//end if
            } else {
                final int PREV_SIZE = this.CURRENT_PROGRESS.size();
                this.CURRENT_PROGRESS.remove(buttonSnapshot.button());
                if (PREV_SIZE == this.DESIRED_SNAPSHOTS.size() &&
                        this.CURRENT_PROGRESS.size() != PREV_SIZE) {
                    if (!this.consumed) {
                        Duration duration = Duration.between(
                                this.lastOccurrence, NOW);
                        this.maxDuration = Math.max(duration.getSeconds() +
                                        duration.getNano() / 1_000_000_000.0,
                                this.maxDuration);
                    }//end if
                }//end if
            }//end if

            if (this.CURRENT_PROGRESS.size() != OLD_SIZE) {
                this.consumed = false;
            }//end if
        } finally {
            this.LOCK.writeLock().unlock();
        }//end try
    }

    private void ensureOpen() throws IllegalStateException {
        if (this.isClosed()) {
            throw new IllegalStateException("This ButtonEvent is closed.");
        }//end if
    }

}//end static nested class KeyboardButtonEvent
