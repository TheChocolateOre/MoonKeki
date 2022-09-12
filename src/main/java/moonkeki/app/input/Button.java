package moonkeki.app.input;

import moonkeki.app.events.IntervalEvent;

public interface Button {

    enum State {
        PRESSED, RELEASED;

        public State negate() {
            return this == PRESSED ? RELEASED : PRESSED;
        }
    }

    default boolean isPressed() {return this.getState() == State.PRESSED;}
    default boolean isReleased() {return this.getState() == State.RELEASED;}

    IntervalEvent.Hub eventHub(State triggerState);
    State getState();

}
