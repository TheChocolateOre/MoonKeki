package moonkeki.app.input;

import moonkeki.app.events.InstantEvent;

public interface Button {

    enum State {PRESSED, RELEASED}

    default boolean isPressed() {return this.getState() == State.PRESSED;}
    default boolean isReleased() {return this.getState() == State.RELEASED;}

    InstantEvent instantEvent(State triggerState);
    State getState();

}
