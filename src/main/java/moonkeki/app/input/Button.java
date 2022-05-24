package moonkeki.app.input;

import moonkeki.app.events.Event;
import moonkeki.app.events.InstantEvent;

public interface Button {

    enum State {PRESSED, RELEASED}

    default boolean isPressed() {return this.getState() == State.PRESSED;}
    default boolean isReleased() {return this.getState() == State.RELEASED;}

    Event.Hub eventHub(State triggerState);
    InstantEvent.Hub instantEventHub(State triggerState);
    State getState();

}
