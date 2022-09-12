package moonkeki.app.input;

import moonkeki.app.events.IntervalEvent;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

abstract class AbstractButton implements Button {
    private final Map<State, IntervalEvent.Signal> EVENT_SIGNALS =
            new EnumMap<>(Map.of(Keyboard.Button.State.RELEASED,
                                 new IntervalEvent.Signal(),
                                 Keyboard.Button.State.PRESSED,
                                 new IntervalEvent.Signal()));

    @Override
    public IntervalEvent.Hub eventHub(State triggerState) {
        return this.EVENT_SIGNALS.get(triggerState).hub();
    }

    void registerEvent(Keyboard.Button.State state, final Instant timestamp) {
        this.EVENT_SIGNALS.get(state)
                          .startElseNow(timestamp);
        this.EVENT_SIGNALS.get(state.negate())
                          .stopElseNow(timestamp);
    }
}
