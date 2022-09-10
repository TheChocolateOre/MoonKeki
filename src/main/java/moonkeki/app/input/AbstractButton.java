package moonkeki.app.input;

import moonkeki.app.events.Event;
import moonkeki.app.events.InstantEventQueue;
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
    @Deprecated(forRemoval = true)
    private final Map<Keyboard.Button.State, InstantEventQueue.Signal>
            INSTANT_EVENT_QUEUE_SIGNALS =
            new EnumMap<>(Map.of(Keyboard.Button.State.RELEASED,
                                 new InstantEventQueue.Signal(),
                                 Keyboard.Button.State.PRESSED,
                                 new InstantEventQueue.Signal()));

    @Override
    public IntervalEvent.Hub eventHub(State triggerState) {
        return this.EVENT_SIGNALS.get(triggerState).hub();
    }

    @Override
    public InstantEventQueue.Hub instantEventQueueHub(State triggerState) {
        return this.INSTANT_EVENT_QUEUE_SIGNALS.get(triggerState).getHub();
    }

    void registerEvent(Keyboard.Button.State state, final Instant timestamp) {
        this.EVENT_SIGNALS.get(state)
                          .startElseNow(timestamp);
        this.EVENT_SIGNALS.get(state.negate())
                          .stopElseNow(timestamp);
        this.INSTANT_EVENT_QUEUE_SIGNALS.get(state)
                                        .triggerElseNow(timestamp);
    }
}
